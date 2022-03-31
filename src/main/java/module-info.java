module io.ebean.hazelcast {

  requires io.ebean.api;
  requires io.ebean.core;
  requires com.hazelcast.core;

  provides io.ebean.cache.ServerCachePlugin with io.ebean.hazelcast.HzCachePlugin;
}
