package io.ebean.hazelcast;

import io.ebean.BackgroundExecutor;
import io.ebean.DatabaseBuilder;
import io.ebean.cache.ServerCacheFactory;
import io.ebean.cache.ServerCachePlugin;

/**
 * Creates the Hazelcast ServerCacheFactory implementation.
 */
public final class HzCachePlugin implements ServerCachePlugin {

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
  public ServerCacheFactory create(DatabaseBuilder config, BackgroundExecutor executor) {
    return new HzCacheFactory(config, executor);
  }
}
