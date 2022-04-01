package io.ebean.hazelcast;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.ebean.cache.ServerCache;
import io.ebean.cache.ServerCacheStatistics;
import io.ebean.cache.TenantAwareKey;

public class TenantAwareCache implements ServerCache {

  private final ServerCache delegate;
  private final TenantAwareKey tenantAwareKey;
  
  public TenantAwareCache(ServerCache delegate, TenantAwareKey tenantAwareKey) {
    this.delegate = delegate;
    this.tenantAwareKey = tenantAwareKey;
  }

  private Object key(Object key) {
    return tenantAwareKey.key(key);
  }
  @Override
  public Object get(Object id) {
    return delegate.get(key(id));
  }

  @Override
  public void put(Object id, Object value) {
    delegate.put(key(id), value);
  }

  @Override
  public void remove(Object id) {
    delegate.remove(key(id));
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public int getHitRatio() {
    return delegate.getHitRatio();
  }

  @Override
  public ServerCacheStatistics getStatistics(boolean reset) {
    return delegate.getStatistics(reset);
  }
  
  @Override
  public Map<Object, Object> getAll(Set<Object> keys) {
    Map<Object, Object> keyMapping = new HashMap<>();
    keys.forEach(k -> keyMapping.put(key(k),k));
    Map<Object, Object> tmp = delegate.getAll(keyMapping.keySet());
    Map<Object, Object> ret = new HashMap<Object, Object>();
    tmp.forEach((k,v)-> ret.put(keyMapping.get(k), v)); // remove tenant info here
    return ret;
  }

  @Override
  public void putAll(Map<Object, Object> keyValues) {
    Map<Object, Object> tmp = new HashMap<Object, Object>();
    keyValues.forEach((k,v)-> tmp.put(key(k), v)); 
    delegate.putAll(tmp);
  }
  
  @Override
  public void removeAll(Set<Object> keys) {
    delegate.removeAll(keys.stream().map(this::key).collect(Collectors.toSet()));
  }
  
}
