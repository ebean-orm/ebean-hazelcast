package org.avaje.ebeanorm.hazelcast;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.cache.ServerCache;
import com.avaje.ebean.cache.ServerCacheFactory;
import com.avaje.ebean.cache.ServerCacheOptions;
import com.avaje.ebean.cache.ServerCacheType;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.plugin.SpiServer;
import com.avaje.ebeaninternal.server.cache.DefaultServerCache;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ISet;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating the various caches.
 */
public class HzCacheFactory implements ServerCacheFactory {

  /**
   * This explicitly uses the common "org.avaje.ebean.cache" namespace.
   */
  private static final Logger logger = LoggerFactory.getLogger("org.avaje.ebean.cache.HzCacheFactory");

  private final ConcurrentHashMap<String,HzQueryCache> queryCaches;

  private HazelcastInstance instance;

  /**
   * Topic used to broadcast query cache invalidation.
   */
  private ITopic<String> queryCacheInvalidation;

  /**
   * Topic used to broadcast query cache creation.
   */
  private ITopic<String> queryCacheCreated;

  private SpiServer pluginServer;

  /**
   * The set of query cache names.
   */
  private ISet<String> queryCacheNames;

  public HzCacheFactory() {
    queryCaches = new ConcurrentHashMap<String, HzQueryCache>();
  }

  @Override
  public void init(EbeanServer ebeanServer) {

    pluginServer = ebeanServer.getPluginApi();
    if (System.getProperty("hazelcast.logging.type") == null) {
      System.setProperty("hazelcast.logging.type", "slf4j");
    }

    ServerConfig serverConfig = pluginServer.getServerConfig();
    Object configuration = serverConfig.getServiceObject("hazelcastConfiguration");
    if (configuration != null) {
      // explicit configuration probably set via DI
      if (configuration instanceof ClientConfig) {
        instance = HazelcastClient.newHazelcastClient((ClientConfig)configuration);
      } else if (configuration instanceof Config) {
        instance = Hazelcast.newHazelcastInstance((Config)configuration);
      } else {
        throw new IllegalArgumentException("Invalid Hazelcast configuration type "+configuration.getClass());
      }
    } else {
      // implicit configuration via hazelcast-client.xml or hazelcast.xml
      if (isServerMode(serverConfig.getProperties())) {
        instance = Hazelcast.newHazelcastInstance();
      } else {
        instance = HazelcastClient.newHazelcastClient();
      }
    }

    queryCacheNames = instance.getSet("queryCacheNames");
    queryCacheCreated = instance.getReliableTopic("queryCacheCreated");
    queryCacheCreated.addMessageListener(new MessageListener<String>() {
      @Override
      public void onMessage(Message<String> message) {
        processQueryCacheCreated(message.getMessageObject());
      }
    });
    queryCacheInvalidation = instance.getReliableTopic("queryCacheInvalidation");
    queryCacheInvalidation.addMessageListener(new MessageListener<String>() {
      @Override
      public void onMessage(Message<String> message) {
        processInvalidation(message.getMessageObject());
      }
    });

    registerExistingQueryCaches();
  }

  /**
   * Return true if hazelcast should be used in server mode.
   */
  private boolean isServerMode(Properties properties) {
    return properties != null && properties.getProperty("ebean.hazelcast.servermode","").equals("true");
  }

  /**
   * Register the existing query caches held in the cluster.
   */
  private void registerExistingQueryCaches() {
    for (String key : queryCacheNames) {
      try {
        logger.trace("init query cache for {}", key);
        pluginServer.initQueryCache(key);
      } catch (Exception e) {
        logger.error("Failed to initiate query cache for " + key, e);
      }
    }
  }

  /**
   * Send the query cache created message to all members of the cluster.
   */
  private void sendQueryCacheCreated(String key) {
    logger.trace("send query cache created key[{}] ", key);
    queryCacheNames.add(key);
    queryCacheCreated.publish(key);
  }

  @Override
  public ServerCache createCache(ServerCacheType type, String key, ServerCacheOptions options) {

    switch (type) {
      case QUERY:
        return createQueryCache(key, options);
      default:
        return createNormalCache(type, key, options);
    }
  }

  private ServerCache createNormalCache(ServerCacheType type, String key, ServerCacheOptions options) {

    String fullName = type.name() + "-" + key;
    logger.debug("get cache [{}]", fullName);
    IMap<Object, Object> map = instance.getMap(fullName);
    return new HzCache(map);
  }

  private ServerCache createQueryCache(String key, ServerCacheOptions options) {

    synchronized (this) {
      HzQueryCache cache = queryCaches.get(key);
      if (cache == null) {
        logger.debug("create query cache [{}]", key);
        cache = new HzQueryCache(key, options);
        queryCaches.put(key, cache);
        sendQueryCacheCreated(key);
      }
      return cache;
    }
  }

  /**
   * Extends normal default implementation with notification of clear() to cluster.
   */
  private class HzQueryCache extends DefaultServerCache {

    HzQueryCache(String name, ServerCacheOptions options) {
      super(name, options);
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
   * Process a remote query cache creation.
   */
  private void processQueryCacheCreated(String cacheName) {
    pluginServer.initQueryCache(cacheName);
  }
}
