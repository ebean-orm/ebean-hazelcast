package org.avaje.ebeanorm.hazelcast;

import com.avaje.ebean.BackgroundExecutor;
import com.avaje.ebean.cache.ServerCacheFactory;
import com.avaje.ebean.cache.ServerCachePlugin;
import com.avaje.ebean.config.ServerConfig;

/**
 * Creates the Hazelcast ServerCacheFactory implementation.
 */
public class HzCachePlugin implements ServerCachePlugin {

  /**
   * Create and return the Hazelcast ServerCacheFactory implementation.
   * <p>
   * This is called before the EbeanServer instance is created. The factory
   * is used to create the ServerCache instances while the EbeanServer instance is
   * being defined.
   * </p>
   *
   * @param config   The configuration used when constructing the EbeanServer instance.
   * @param executor The background executor service that can be used if needed.
   * @return The server cache factory used to create the L2 caches.
   */
  @Override
  public ServerCacheFactory create(ServerConfig config, BackgroundExecutor executor) {
    return new HzCacheFactory(config, executor);
  }
}
