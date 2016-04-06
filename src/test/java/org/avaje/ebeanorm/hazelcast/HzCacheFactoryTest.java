package org.avaje.ebeanorm.hazelcast;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.cache.ServerCache;
import com.avaje.ebean.cache.ServerCacheManager;
import org.example.domain.EFoo;
import org.testng.annotations.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.testng.Assert.assertNotNull;


public class HzCacheFactoryTest {

  private final EbeanServer server;

  public HzCacheFactoryTest() {
    server = Ebean.getDefaultServer();
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