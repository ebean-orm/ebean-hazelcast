package io.ebean.hazelcast;

import io.ebean.cache.ServerCache;
import io.ebean.cache.ServerCacheStatistics;

import java.util.Map;
import java.util.Set;

import com.hazelcast.map.IMap;

/**
 * IMap cache implementation for Ebean ServerCache interface.
 */
final class HzCache implements ServerCache {

  private final IMap<Object, Object> map;

  HzCache(IMap<Object, Object> map) {
    this.map = map;
  }

  @Override
  public Map<Object, Object> getAll(Set<Object> keys) {
    return map.getAll(keys);
  }

  @Override
  public void putAll(Map<Object, Object> keyValues) {
    map.putAll(keyValues);
  }

  @Override
  public Object get(Object id) {
    return map.get(id);
  }

  @Override
  public void put(Object id, Object value) {
    map.put(id, value);
  }

  @Override
  public void remove(Object id) {
    map.delete(id);
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public int getHitRatio() {
    return 0;
  }

  @Override
  public ServerCacheStatistics getStatistics(boolean reset) {
    //LocalMapStats localMapStats = map.getLocalMapStats();
    return null;
  }

}
