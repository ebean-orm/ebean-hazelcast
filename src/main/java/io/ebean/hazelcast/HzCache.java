package io.ebean.hazelcast;

import com.hazelcast.core.IMap;
import io.ebean.cache.ServerCache;
import io.ebean.cache.ServerCacheStatistics;
import io.ebean.cache.TenantAwareKey;
import io.ebean.config.CurrentTenantProvider;

import java.util.Map;
import java.util.Set;

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
	public Map<Object, Object> getAll(Set<Object> keys) {
		return map.getAll(keys);
	}

	@Override
	public void putAll(Map<Object, Object> keyValues) {
		map.putAll(keyValues);
	}

	@Override
	public Object get(Object id) {
		return map.get(key(id));
	}

	@Override
	public void put(Object id, Object value) {
		map.put(key(id), value);
	}

	@Override
	public void remove(Object id) {
		map.delete(key(id));
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
