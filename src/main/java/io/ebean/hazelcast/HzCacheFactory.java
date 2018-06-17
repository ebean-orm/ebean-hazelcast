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
import io.ebean.cache.ServerCacheConfig;
import io.ebean.cache.ServerCacheFactory;
import io.ebean.cache.ServerCacheNotification;
import io.ebean.cache.ServerCacheNotify;
import io.ebean.config.ServerConfig;
import io.ebeaninternal.server.cache.DefaultServerCacheConfig;
import io.ebeaninternal.server.cache.DefaultServerQueryCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
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

	/**
	 * Topic used to broadcast table modifications.
	 */
	private final ITopic<String> tableModNotify;

	private final BackgroundExecutor executor;

	private ServerCacheNotify listener;

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

		tableModNotify = instance.getReliableTopic("tableModNotify");
		tableModNotify.addMessageListener(message -> processTableNotify(message.getMessageObject()));

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
	public ServerCacheNotify createCacheNotify(ServerCacheNotify listener) {
		this.listener = listener;
		return new HzServerCacheNotify(tableModNotify);
	}

	@Override
	public ServerCache createCache(ServerCacheConfig config) {
		if (config.isQueryCache()) {
			return createQueryCache(config);
		} else {
			return createNormalCache(config);
		}
	}

	private ServerCache createNormalCache(ServerCacheConfig config) {

		String fullName = config.getType().name() + "-" + config.getCacheKey();
		logger.debug("get cache [{}]", fullName);
		IMap<Object, Object> map = instance.getMap(fullName);
		return new HzCache(map, config.getTenantProvider());
	}

	private ServerCache createQueryCache(ServerCacheConfig config) {

		synchronized (this) {
			HzQueryCache cache = queryCaches.get(config.getCacheKey());
			if (cache == null) {
				logger.debug("create query cache [{}]", config.getCacheKey());
				cache = new HzQueryCache(new DefaultServerCacheConfig(config));
				cache.periodicTrim(executor);
				queryCaches.put(config.getCacheKey(), cache);
			}
			return cache;
		}
	}

	/**
	 * Extends normal default implementation with notification of clear() to cluster.
	 */
	private class HzQueryCache extends DefaultServerQueryCache {

		HzQueryCache(DefaultServerCacheConfig cacheConfig) {
			super(cacheConfig);
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

	/**
	 * Process a remote dependent table modify event.
	 */
	private void processTableNotify(String rawMessage) {

		if (logger.isDebugEnabled()) {
			logger.debug("processTableNotify {}", rawMessage);
		}

		String[] split = rawMessage.split(",");
		long modTimestamp = Long.parseLong(split[0]);

		Set<String> tables = new HashSet<>();
		tables.addAll(Arrays.asList(split).subList(1, split.length));

		listener.notify(new ServerCacheNotification(modTimestamp, tables));
	}

}
