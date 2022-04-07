package io.ebean.hazelcast;

import com.hazelcast.map.IMap;
import io.ebean.cache.ServerCache;

import java.util.Map;
import java.util.Set;

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

}
