package com.dianwoda.usercenter.vera.common.redis.command;

import com.google.common.base.Utf8;
import lombok.Data;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * @program: vera
 * @description: 命令数据
 * @author: zhouqi1
 * @create: 2018-10-18 19:46
 **/
@Data
@ToString
public class RedisCommand implements Serializable {
  protected static final Logger log = LoggerFactory.getLogger(RedisCommand.class);
  public static final RedisCommand DELETE_COMMAND = new RedisCommand();

  /**
   * 类型
   */
  private byte type = 0;

  /**
   * redis 命令 key
   */
  private byte[] key;

  private byte[][] delKeys;
  /**
   * LSET
   */
  private long index;

  /**
   * HSET
   */
  private byte[] field;

  /**
   * HMSET
   */
  private Map<byte[], byte[]> fields;

  /**
   * redis 命令序列化值
   */
  private byte[] value;

  /**
   * SET
   */
  private byte[][] members;

  private long expiredValue;

  private int expiredType;
//  private int expiredType;
//  private Long expiredValue;

  @Override
  public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      RedisCommand that = (RedisCommand) o;
      return type == that.type &&
              Arrays.equals(key, that.key) && Arrays.equals(value, that.value);
  }

  @Override
  public int hashCode() {

      int result = Objects.hash(super.hashCode(), type);
      result = 31 * result + Arrays.hashCode(key);
      if (value != null) {
        result = 31 * result + Arrays.hashCode(value);
      }
      return result;
  }

  public byte getType() {
      return type;
  }

  public byte[] getKey() {
      return key;
  }

  public long getIndex() {
      return index;
  }

  public byte[] getField() {
      return field;
  }

  public Map<byte[], byte[]> getFields() {
      return fields;
  }

  public byte[] getValue() {
      return value;
  }

  public byte[][] getMembers() {
      return members;
  }

  public long getExpiredValue() {
      return expiredValue;
  }

  public int getExpiredType() {
      return expiredType;
  }

  public void setType(byte type) {
      this.type = type;
  }

  public void setKey(byte[] key) {
      this.key = key;
  }

  public void setIndex(long index) {
      this.index = index;
  }

  public void setField(byte[] field) {
      this.field = field;
  }

  public void setFields(Map<byte[], byte[]> fields) {
      this.fields = fields;
  }

  public void setValue(byte[] value) {
      this.value = value;
  }

  public void setMembers(byte[][] members) {
      this.members = members;
  }

  public void setExpiredValue(long expiredValue) {
      this.expiredValue = expiredValue;
  }

  public void setExpiredType(int expiredType) {
      this.expiredType = expiredType;
  }

  public byte[][] getDelKeys() {
    return delKeys;
  }

  public void setDelKeys(byte[][] delKeys) {
    this.delKeys = delKeys;
  }

  @Override
  public String toString() {
    String value = null;
    try {
      value = (this.value == null || this.value.length == 0) ? "null" :
              new String(this.value, Charset.forName("utf8")).substring(0, Math.min(this.value.length/3, 50));
    } catch (Exception e) {
      log.error("str:" + new String(this.value));

      for (int i=0; i<this.value.length; i++) {
        log.error("i:" + this.value[i]);
      }
      value = new String(this.value);
    }
    return "type:" + this.type + " key:" + (this.key == null ? "null" : new String(this.key)) +
            " value:" + value + " delkeys:" + (this.delKeys == null ? "null" : new String(this.delKeys[0]));
  }

}
