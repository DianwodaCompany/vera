package com.dianwoda.usercenter.vera.piper.redis.serializer;


import com.dianwoda.usercenter.vera.common.redis.command.RedisCommand;
import com.dianwoda.usercenter.vera.store.io.ObjectDeserializer;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * @program: vera
 * @description: 默认的数据序列化工具
 * @author: zhouqi1
 * @create: 2018-10-17 15:06
 **/
public class RedisCommandDeserializer implements ObjectDeserializer<RedisCommand> {

  @Override
  public RedisCommand deserialize(byte[] data) {
    if (data.length == 1 && data[0] == 0) {
      return RedisCommand.DELETE_COMMAND;
    }

    RedisCommand swordCommand = new RedisCommand();
    ByteBuffer byteBuffer = ByteBuffer.wrap(data);
    Byte type = byteBuffer.get();
    swordCommand.setType(type == 0 ? null : type);

    int keyLen = byteBuffer.getInt();
    if (keyLen > 0) {
      byte[] keyBytes = new byte[keyLen];
      byteBuffer.get(keyBytes);
      swordCommand.setKey(keyBytes);
    }

    int fieldLen = byteBuffer.getInt();
    if (fieldLen > 0) {
      byte[] fieldBytes = new byte[fieldLen];
      byteBuffer.get(fieldBytes);
      swordCommand.setField(fieldBytes);
    }

    swordCommand.setIndex(byteBuffer.getLong());

    int fieldsLen = byteBuffer.getInt();
    if (fieldsLen > 0) {
      Map<byte[], byte[]> fields = new HashMap<>();
      for (int i = 0; i < fieldsLen; i++) {
        int kLen = byteBuffer.getInt();
        byte[] k = new byte[kLen];
        byteBuffer.get(k);
        int vLen = byteBuffer.getInt();
        byte[] v = new byte[vLen];
        byteBuffer.get(v);
        fields.put(k, v);
      }
      swordCommand.setFields(fields);
    }


    int valueLen = byteBuffer.getInt();
    if (valueLen > 0) {
      byte[] valueBytes = new byte[valueLen];
      byteBuffer.get(valueBytes);
      swordCommand.setValue(valueBytes);
    }

    int memLen = byteBuffer.getInt();
    byte[][] members = new byte[memLen][];
    if (memLen > 0) {
      for (int i = 0; i < memLen; i++) {
        int memValueLen = byteBuffer.getInt();
        byte[] memValue = new byte[memValueLen];
        byteBuffer.get(memValue);
        members[i] = memValue;
      }
    }
    swordCommand.setMembers(members);

    swordCommand.setExpiredValue(byteBuffer.getLong());
    swordCommand.setExpiredType(byteBuffer.getInt());

    return swordCommand;
  }
}
