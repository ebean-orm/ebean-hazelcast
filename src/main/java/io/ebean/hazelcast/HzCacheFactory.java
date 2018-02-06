package io.ebean.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;
import io.ebean.BackgroundExecutor;
import io.ebean.cache.ServerCache;
import io.ebean.cache.ServerCacheFactory;
import io.ebean.cache.ServerCacheOptions;
import io.ebean.cache.ServerCacheType;
import io.ebean.config.CurrentTenantProvider;
import io.ebean.config.ServerConfig;
import io.ebeaninternal.server.cache.DefaultServerCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating the various caches.
 */
public class HzCacheFactory implements ServerCacheFactory {

	/**
	 * This explicitly uses the common "io.ebean.cache" namespace.
	 */
	private static final Logger logger = LoggerFactory.getLogger("io.ebean.cache.HzCacheFactory");

	private final ConcurrentHashMap<String, HzQueryCache> queryCaches;

	private final HazelcastInstance instance;

	/**
	 * Topic used to broadcast query cache invalidation.
	 */
	private final ITopic<String> queryCacheInvalidation;

	private final BackgroundExecutor executor;

	public HzCacheFactory(ServerConfig serverConfig, BackgroundExecutor executor) {

		this.executor = executor;
		this.queryCaches = new ConcurrentHashMap<>();

		if (System.getProperty("hazelcast.logging.type") == null) {
			System.setProperty("hazelcast.logging.type", "slf4j");
		}

		Object hazelcastInstance = serverConfig.getServiceObject("hazelcast");
		if (hazelcastInstance != null) {
			instance = (HazelcastInstance) hazelcastInstance;
		} else {
			instance = createInstance(serverConfig);
		}

		queryCacheInvalidation = instance.getReliableTopic("queryCacheInvalidation");
		queryCacheInvalidation.addMessageListener(message -> processInvalidation(message.getMessageObject()));
	}

	/**
	 * Create a new HazelcastInstance based on configuration from serverConfig.
	 */
	private HazelcastInstance createInstance(ServerConfig serverConfig) {
		Object configuration = serverConfig.getServiceObject("hazelcastConfiguration");
		if (configuration != null) {
			// explicit configuration probably set via DI
			if (configuration instanceof ClientConfig) {
				return HazelcastClient.newHazelcastClient((ClientConfig) configuration);
			} else if (configuration instanceof Config) {
				return Hazelcast.newHazelcastInstance((Config) configuration);
			} else {
				throw new IllegalArgumentException("Invalid Hazelcast configuration type " + configuration.getClass());
			}
		} else {
			// implicit configuration via hazelcast-client.xml or hazelcast.xml
			if (isServerMode(serverConfig.getProperties())) {
				return Hazelcast.newHazelcastInstance();
			} else {
				return HazelcastClient.newHazelcastClient();
			}
		}
	}

	/**
	 * Return true if hazelcast should be used in server mode.
	 */
	private boolean isServerMode(Properties properties) {
		return properties != null && properties.getProperty("ebean.hazelcast.servermode", "").equals("true");
	}

	@Override
	public ServerCache createCache(ServerCacheType type, String key, CurrentTenantProvider tenantProvider, ServerCacheOptions options) {

		switch (type) {
			case QUERY:
				return createQueryCache(key, tenantProvider, options);
			default:
				return createNormalCache(type, key, tenantProvider, options);
		}
	}

	private ServerCache createNormalCache(ServerCacheType type, String key, CurrentTenantProvider tenantProvider, ServerCacheOptions options) {

		String fullName = type.name() + "-" + key;
		logger.debug("get cache [{}]", fullName);
		IMap<Object, Object> map = instance.getMap(fullName);
		return new HzCache(map, tenantProvider);
	}

	private ServerCache createQueryCache(String key, CurrentTenantProvider tenantProvider, ServerCacheOptions options) {

		synchronized (this) {
			HzQueryCache cache = queryCaches.get(key);
			if (cache == null) {
				logger.debug("create query cache [{}]", key);
				cache = new HzQueryCache(key, tenantProvider, options);
				cache.periodicTrim(executor);
				queryCaches.put(key, cache);
			}
			return cache;
		}
	}

	/**
	 * Extends normal default implementation with notification of clear() to cluster.
	 */
	private class HzQueryCache extends DefaultServerCache {

		HzQueryCache(String name, CurrentTenantProvider tenantProvider, ServerCacheOptions options) {
			super(name, tenantProvider, options);
		}

		@Override
		public void clear() {
			super.clear();
			sendInvalidation(name);
		}

		/**
		 * Process the invalidation message coming from the cluster.
		 */
		private void invalidate() {
			super.clear();
		}
	}

	/**
	 * Send the invalidation message to all members of the cluster.
	 */
	private void sendInvalidation(String key) {
		queryCacheInvalidation.publish(key);
	}

	/**
	 * Process a remote query cache invalidation.
	 */
	private void processInvalidation(String cacheName) {
		HzQueryCache cache = queryCaches.get(cacheName);
		if (cache != null) {
			cache.invalidate();
		}
	}

}
