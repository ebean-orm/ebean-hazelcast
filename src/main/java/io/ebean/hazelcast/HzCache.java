package io.ebean.hazelcast;

import io.ebean.cache.ServerCache;
import io.ebean.cache.ServerCacheStatistics;
import io.ebean.cache.TenantAwareKey;
import com.hazelcast.core.IMap;
import io.ebean.config.CurrentTenantProvider;

/**
 * IMap cache implementation for Ebean ServerCache interface.
 */
class HzCache implements ServerCache {

  private final TenantAwareKey tenantAwareKey;

  private final IMap<Object, Object> map;

  HzCache(IMap<Object, Object> map, CurrentTenantProvider tenantProvider) {
    this.map = map;
    this.tenantAwareKey = new TenantAwareKey(tenantProvider);
  }

  private Object key(Object key) {
    return tenantAwareKey.key(key);
  }

  @Override
  public Object get(Object id) {
    return map.get(key(id));
  }

  @Override
  public Object put(Object id, Object value) {
    return map.put(key(id), value);
  }

  @Override
  public Object remove(Object id) {
    return map.remove(key(id));
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
