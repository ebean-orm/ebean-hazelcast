package io.ebean.hazelcast;

import com.hazelcast.core.ITopic;
import io.ebean.cache.ServerCacheNotification;
import io.ebean.cache.ServerCacheNotify;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Send the table modifications via Hazelcast topic.
 */
final class HzServerCacheNotify implements ServerCacheNotify {

  private static final Logger log = LoggerFactory.getLogger("io.ebean.cache.TABLEMODS");

  private final ITopic<String> tableModNotify;

  HzServerCacheNotify(ITopic<String> tableModNotify) {
    this.tableModNotify = tableModNotify;
  }

  @Override
  public void notify(ServerCacheNotification tableModifications) {
    Set<String> dependentTables = tableModifications.getDependentTables();
    if (dependentTables != null && !dependentTables.isEmpty()) {
      StringBuilder msg = new StringBuilder(50);
      for (String table : dependentTables) {
        msg.append(table).append(",");
      }
      String formattedMsg = msg.toString();
      if (log.isDebugEnabled()) {
        log.debug("Publish TableMods - {}", formattedMsg);
      }
      tableModNotify.publish(formattedMsg);
    }
  }
}
