package org.integration;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.NearCacheConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import org.example.domain.ECustomer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ClientTest {

  private HazelcastInstance client;

  public ClientTest() {

    ClientConfig clientConfig = new ClientConfig();

    NearCacheConfig nearCacheConfig = new NearCacheConfig();
    nearCacheConfig.setName("mapName");
    clientConfig.addNearCacheConfig(nearCacheConfig);

    this.client = HazelcastClient.newHazelcastClient(clientConfig);
  }

  @Test
  public void test() {

    IMap<Object, Object> map = client.getMap("mapName");

    map.put("asd", new ECustomer("asd"));
    Object asd = map.get("asd");

    assertNotNull(asd);

    IMap<Object, Object> remote = client.getMap("remote");
    remote.put("some", new ECustomer("hello Rob"));

    ECustomer val = (ECustomer)remote.get("some");
    System.out.println(""+val.getName());
  }

}
