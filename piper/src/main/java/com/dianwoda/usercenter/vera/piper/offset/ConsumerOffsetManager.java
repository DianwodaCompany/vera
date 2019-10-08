package com.dianwoda.usercenter.vera.piper.offset;


import com.dianwoda.usercenter.vera.common.ConfigManager;
import com.dianwoda.usercenter.vera.remoting.protocol.RemotingSerializable;
import com.dianwoda.usercenter.vera.store.config.PiperPathConfigHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * consumer offset
 * @author seam
 */
public class ConsumerOffsetManager extends ConfigManager {
  protected static final Logger log = LoggerFactory.getLogger(ConsumerOffsetManager.class);
  private static final String ID_GROUP_SEPARATOR = "@";
  private ConcurrentMap<String, Long> offsetTable =
          new ConcurrentHashMap<String, Long>();
  private String storePath;

  public ConsumerOffsetManager(){}
  public ConsumerOffsetManager(String storePath) {
    this.storePath = storePath;
  }

  public void commitOffset(final String location, final long offset) {
    String key = location;
    Long storeOffset = this.offsetTable.get(key);
    if (storeOffset != null && offset < storeOffset) {
      log.warn(String.format("[NOTIFY] update consumer offset less than store. key=%s, requestOffset=%s",
                key, offset));
    } else {
      storeOffset = this.offsetTable.put(key, offset);
      if (storeOffset != null && offset < storeOffset) {
        log.warn(String.format("[NOTIFY] update consumer offset less than store. key=%s, requestOffset=%s",
                key, offset));
      }
    }
  }

  public void commitOffsetForce(final String location, final long offset) {
    this.offsetTable.put(location, offset);
  }

  public long getOffset(final String location) {
    String key = location;
    Long storeOffset = this.offsetTable.get(key);
    if (storeOffset != null) {
      return storeOffset;
    }
    return 0;
  }

  @Override
  public String configFilePath() {
    return PiperPathConfigHelper.getConsumerOffsetPath(this.storePath);
  }

  @Override
  public String encode() {
    return encode(false);
  }

  @Override
  public void decode(String jsonString) {
    if (jsonString != null) {
      ConsumerOffsetManager obj = RemotingSerializable.fromJson(jsonString, ConsumerOffsetManager.class);
      if (obj != null) {
        this.offsetTable = obj.offsetTable;
      }
    }
  }

  @Override
  public String encode(boolean prettyFormat) {
    return RemotingSerializable.toJson(this, prettyFormat);
  }

  public ConcurrentMap<String, Long> getOffsetTable() {
    return offsetTable;
  }
}
