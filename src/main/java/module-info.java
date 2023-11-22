module io.ebean.hazelcast {

  requires io.ebean.api;
  requires io.ebean.core;
  requires com.hazelcast.core;
	requires org.slf4j;

	provides io.ebean.cache.ServerCachePlugin with io.ebean.hazelcast.HzCachePlugin;
}
