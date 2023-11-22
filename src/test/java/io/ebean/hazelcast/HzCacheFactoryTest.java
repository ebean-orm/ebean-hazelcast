package io.ebean.hazelcast;

import io.ebean.DB;
import io.ebean.Database;
import io.ebean.cache.ServerCache;
import io.ebean.cache.ServerCacheManager;
import io.ebean.cache.TenantAwareCache;
import org.example.domain.EAddress;
import org.example.domain.ECustomer;
import org.example.domain.EOrder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class HzCacheFactoryTest {

  private final Database server;

  public HzCacheFactoryTest() {
    server = DB.getDefault();
  }

//  @Test
//  public void supplyHazelcastInstance() {
//
//    HazelcastInstance instance = Hazelcast.newHazelcastInstance();
//
//    ServerConfig serverConfig = new ServerConfig();
//    serverConfig.putServiceObject("hazelcast", instance);
//
//    HzCacheFactory factory = new HzCacheFactory(serverConfig, null);
//    factory.createCache(ServerCacheType.BEAN, "foo", null, new ServerCacheOptions());
//  }


  @Test
  public void setup() {

    int count = DB.find(ECustomer.class).findCount();
    if (count == 0) {

      ECustomer customer = new ECustomer("Rob");
      customer.setAddress(new EAddress("Line1", "Auckland"));
      customer.save();

      for (int i = 0; i < 3; i++) {
        EOrder order = new EOrder(customer);
        order.setDescription("some " + 1);
        order.save();
      }
    }
  }


  @Test
  public void run() throws InterruptedException {

    ServerCacheManager cacheManager = server.cacheManager();
    cacheManager.clearAll();

//    ECustomer one = new ECustomer("hello");
//    one.save();


    for (int i = 0; i < 3; i++) {

      List<ECustomer> cust = DB.find(ECustomer.class)
        .where().eq("address.city", "Auckland")
        .setUseQueryCache(true)
        .setReadOnly(true)
        .findList();

      System.out.println("found " + cust);

//      ECustomer fetch1 = Ebean.find(ECustomer.class, 1);
//      System.out.println("got " + fetch1.getName() + " version:" + fetch1.getVersion());

      Thread.sleep(5000);
    }
  }

  @Test
  public void integration() throws InterruptedException {


    ServerCacheManager cacheManager = server.cacheManager();
    ServerCache beanCache = cacheManager.beanCache(ECustomer.class);

    assertThat(beanCache).isInstanceOf(TenantAwareCache.class);
    assertThat(beanCache.unwrap(HzCache.class)).isInstanceOf(HzCache.class);

//    Ebean.update(ECustomer.class)
//      .setRaw("name = 'x'")
//      .where().idEq(1)
//      .update();

//    Ebean.update(EAddress.class)
//      .setRaw("line1 = 's'")
//      .where().idEq(1)
//      .update();

    DB.sqlUpdate("update eaddress set line1 = line1 || 's'").execute();
    for (int i = 0; i < 5; i++) {

      DB.sqlUpdate("update eaddress set line1 = line1 || 's'").execute();

//      Ebean.update(EAddress.class)
//        .setRaw("line1 = line1 || 's'")
//        .where().idEq(1)
//        .update();

//      Ebean.update(ECustomer.class)
//      .setRaw("name = name || 's'")
//        .where().idEq(1)
//        .update();


//      EAddress addr = Ebean.find(EAddress.class, 1);
//      addr.setLine1(addr.getLine1() + "x");
//      addr.save();


//      ECustomer fetch1 = Ebean.find(ECustomer.class, 1);
//      fetch1.setName(fetch1.getName()+ "x");
//      fetch1.save();

      Thread.sleep(1000);
    }


    //putGet();
  }

  private void putGet() {

    ECustomer foo = new ECustomer("hello");
    foo.save();

    ECustomer fetch1 = DB.find(ECustomer.class, foo.getId());
    ECustomer fetch2 = DB.find(ECustomer.class, foo.getId());

    assertNotNull(fetch1);
    assertNotNull(fetch2);
  }
}
