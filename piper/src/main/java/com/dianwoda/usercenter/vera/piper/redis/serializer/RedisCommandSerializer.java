package com.dianwoda.usercenter.vera.piper.redis.serializer;

import com.dianwoda.usercenter.vera.common.redis.command.RedisCommand;
import com.dianwoda.usercenter.vera.store.io.ObjectSerializer;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * @program: vera
 * @description: 命令序列化帮助类
 * @author: zhouqi1
 * @create: 2018-10-10 15:41
 **/
public class RedisCommandSerializer implements ObjectSerializer<RedisCommand> {

    @Override
    public byte[] serialize(RedisCommand commandSword) {
        if(commandSword == RedisCommand.DELETE_COMMAND){
            return new byte[]{0};
        }

        Byte type = commandSword.getType();
        if(type == null){
            type = 0;
        }

        int keyLen = 0;
        byte[] bytes = commandSword.getKey();
        if(bytes != null){
            keyLen = bytes.length;
        }

        int fieldLen = 0;
        byte[] field = commandSword.getField();
        if(field != null){
            fieldLen = field.length;
        }

        int fieldsLen = 0;
        int fieldsValueLen = 0;
        Map<byte[], byte[]> fields = commandSword.getFields();
        if(fields != null){
            fieldsLen = fields.size();
            for(byte[] k : fields.keySet()){
                fieldsValueLen += 8;
                fieldsValueLen += k.length;
                fieldsValueLen += 8;
                fieldsValueLen += fields.get(k).length;
            }
        }

        int valueLen = 0;
        byte[] valueBytes = commandSword.getValue();
        if(valueBytes != null){
            valueLen = valueBytes.length;
        }

        int memeLen = 0;
        int memValueLen = 0;
        byte[][] memebers = commandSword.getMembers();
        if(memebers != null){
            memeLen = memebers.length;
            for(int i = 0; i < memeLen; i++){
                memValueLen += 8;
                memValueLen += memebers[i].length;
            }
        }

        int capacity = 1 + 8 + keyLen + 16 + 8 + fieldLen + 8 + valueLen + memeLen * 8 + memValueLen + 8 + 16 + fieldsLen + fieldsValueLen;

        ByteBuffer byteBuffer = ByteBuffer.allocate(capacity);
        byteBuffer.put(type);
        byteBuffer.putInt(keyLen);
        if(bytes != null){
            byteBuffer.put(bytes);
        }
        byteBuffer.putLong(commandSword.getIndex());

        byteBuffer.putInt(fieldLen);
        if(field != null){
            byteBuffer.put(field);
        }

        byteBuffer.putInt(fieldsLen);
        if(fields != null){
            for(byte[] k : fields.keySet()){
                byteBuffer.putInt(k.length);
                byteBuffer.put(k);
                byteBuffer.putInt(fields.get(k).length);
                byteBuffer.put(fields.get(k));
            }
        }

        byteBuffer.putInt(valueLen);
        if(valueBytes != null){
            byteBuffer.put(valueBytes);
        }

        byteBuffer.putInt(memeLen);
        if(memebers != null){
            for(int i = 0; i < memeLen; i++){
                byteBuffer.putInt(memebers[i].length);
                byteBuffer.put(memebers[i]);
            }
        }

        byteBuffer.putLong(commandSword.getExpiredValue());
        byteBuffer.putInt(commandSword.getExpiredType());

        return byteBuffer.array();
    }
}
