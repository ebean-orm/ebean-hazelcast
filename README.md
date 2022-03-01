# ebean-hazelcast
Hazelcast L2 cache option

# How to build and test

To run the unit tests, start a local hazelcast docker container

    docker run -p 5701:5701 hazelcast/hazelcast:4.0.6
    
maven build with

    mvn clean install

