package main;


import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class Server {

  public static void main(String[] args) {

    // run a server instance to talk to ...
    Server server = new Server();
    server.run();
  }

  HazelcastInstance instance;

  private Server() {
    Config config = new Config();
    instance = Hazelcast.newHazelcastInstance(config);
  }

  private void run() {

    System.out.println("running");
    while(true) {

    }
  }
}
