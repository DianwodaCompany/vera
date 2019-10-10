package com.dianwoda.usercenter.vera.piper.redis.command;

/**
 * @program: vera
 * @description: Redis命令类型
 * @author: zhouqi1
 * @create: 2018-11-08 15:39
 **/
public enum CommandType {
    SET(1),
    SET_EX(2),
    SET_NX(3),
    INCR(4),
    DECR(5),
    SADD(6),
    HSET(7),
    HMSET(8),
    LSET(9),
    DEL(10),
    ;

    CommandType(int value) {
        this.value = (byte)value;
    }

    private byte value;

    public byte getValue() {
        return value;
    }

    public static CommandType toEnum(byte value){
        CommandType[] commandTypes = CommandType.values();
        if(commandTypes != null && commandTypes.length > 0){
            for(CommandType commandType : commandTypes){
                if(commandType.getValue() == value){
                    return commandType;
                }
            }
        }
        return null;
    }
}
