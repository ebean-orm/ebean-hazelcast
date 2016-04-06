package org.integration;


import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.NearCacheConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.example.domain.EFoo;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;

public class ClientTest {

  private HazelcastInstance client;

  ClientTest() {

    ClientConfig clientConfig = new ClientConfig();

    NearCacheConfig nearCacheConfig = new NearCacheConfig();
    nearCacheConfig.setName("mapName");
    clientConfig.addNearCacheConfig(nearCacheConfig);

    this.client = HazelcastClient.newHazelcastClient(clientConfig);
  }

  @Test
  public void test() {

    IMap<Object, Object> map = client.getMap("mapName");

    map.put("asd", new EFoo("asd"));
    Object asd = map.get("asd");

    assertNotNull(asd);

    IMap<Object, Object> remote = client.getMap("remote");
    remote.put("some", new EFoo("hello Rob"));
  }

  @Test
  public void testGet() {
    IMap<Object, Object> remote = client.getMap("remote");
    EFoo val = (EFoo)remote.get("some");
    System.out.println(""+val.getName());
  }
}
