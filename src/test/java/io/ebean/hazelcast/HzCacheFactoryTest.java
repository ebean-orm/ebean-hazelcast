package io.ebean.hazelcast;

import io.ebean.Ebean;
import io.ebean.EbeanServer;
import io.ebean.cache.ServerCache;
import io.ebean.cache.ServerCacheManager;
import io.ebean.cache.ServerCacheOptions;
import io.ebean.cache.ServerCacheType;
import io.ebean.config.ServerConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.example.domain.EFoo;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertNotNull;


public class HzCacheFactoryTest {

  private final EbeanServer server;

  public HzCacheFactoryTest() {
    server = Ebean.getDefaultServer();
  }

  @Test
  public void supplyHazelcastInstance() {

    HazelcastInstance instance = Hazelcast.newHazelcastInstance();

    ServerConfig serverConfig = new ServerConfig();
    serverConfig.putServiceObject("hazelcast", instance);

    HzCacheFactory factory = new HzCacheFactory(serverConfig, null);
    factory.createCache(ServerCacheType.BEAN, "foo", new ServerCacheOptions());
  }

  @Test
  public void integration() {


    ServerCacheManager cacheManager = server.getServerCacheManager();
    ServerCache beanCache = cacheManager.getBeanCache(EFoo.class);

    assertThat(beanCache).isInstanceOf(HzCache.class);

    EFoo fetch1 = Ebean.find(EFoo.class, 1);

    System.out.println("f"+fetch1);
  }

  @Test(dependsOnMethods = "integration")
  public void putGet() {

    EFoo foo = new EFoo("hello");
    foo.save();

    EFoo fetch1 = Ebean.find(EFoo.class, foo.getId());
    EFoo fetch2 = Ebean.find(EFoo.class, foo.getId());

    assertNotNull(fetch1);
    assertNotNull(fetch2);
  }
}