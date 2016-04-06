package org.avaje.ebeanorm.hazelcast;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.cache.ServerCache;
import com.avaje.ebean.cache.ServerCacheFactory;
import com.avaje.ebean.cache.ServerCacheOptions;
import com.avaje.ebean.cache.ServerCacheType;
import com.avaje.ebeaninternal.server.cache.DefaultServerCache;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating the various caches.
 */
public class HzCacheFactory implements ServerCacheFactory {

  private HazelcastInstance instance;

  /**
   * Topic used to send invalidation messages for L2 Query cache.
   */
  private ITopic<Object> queryCacheInvalidation;

  private final ConcurrentHashMap<String,HzQueryCache> queryCaches;

  public HzCacheFactory() {
    queryCaches = new ConcurrentHashMap<String, HzQueryCache>();
  }

  @Override
  public void init(EbeanServer ebeanServer) {

    // use default xml config for client
    ClientConfig config = new ClientConfig();
    instance = HazelcastClient.newHazelcastClient(config);

    queryCacheInvalidation = instance.getReliableTopic("queryCacheInvalidation");

    queryCacheInvalidation.addMessageListener(new MessageListener<Object>() {
      @Override
      public void onMessage(Message<Object> message) {
        processInvalidation(message);
      }
    });
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

    IMap<Object, Object> map = instance.getMap(type.name() + ":" + key);
    return new HzCache(map);
  }

  private ServerCache createQueryCache(String key, ServerCacheOptions options) {

    HzQueryCache cache = new HzQueryCache(key, options);
    queryCaches.put(key, cache);
    return cache;
  }

  /**
   * Extends normal default implementation with notification of clear() to cluster.
   */
  class HzQueryCache extends DefaultServerCache {

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
  private void sendInvalidation(String name) {
    queryCacheInvalidation.publish(name);
  }

  private void processInvalidation(Message<Object> message) {

    String cacheName = (String)message.getMessageObject();
    HzQueryCache cache = queryCaches.get(cacheName);
    if (cache != null) {
      cache.invalidate();
    }
  }
}
