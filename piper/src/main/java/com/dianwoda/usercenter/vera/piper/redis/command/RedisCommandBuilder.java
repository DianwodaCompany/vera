package com.dianwoda.usercenter.vera.piper.redis.command;

import com.dianwoda.usercenter.vera.common.redis.command.RedisCommand;
import com.moilioncircle.redis.replicator.cmd.Command;
import com.moilioncircle.redis.replicator.cmd.impl.*;

/**
 * @program: vera
 * @description: 命令解析器
 * @author: zhouqi1
 * @create: 2018-10-18 20:39
 **/
public class RedisCommandBuilder {
  /**
   * 解析redis命令
   *
   * @param command
   * @return
   */
  public static RedisCommand buildSwordCommand(Command command) {
    RedisCommand swordCommand = new RedisCommand();
    if (command instanceof SetCommand) {
      SetCommand setCommand = (SetCommand) command;
      swordCommand.setType(CommandType.SET.getValue());
      swordCommand.setKey(setCommand.getKey());
      swordCommand.setValue(setCommand.getValue());
      swordCommand.setEx(setCommand.getEx() == null ? 0 : setCommand.getEx());
      swordCommand.setPx(setCommand.getPx() == null ? 0 : setCommand.getPx());
    } else if (command instanceof SetExCommand) {
      SetExCommand setCommand = (SetExCommand) command;
      swordCommand.setType(CommandType.SET_EX.getValue());
      swordCommand.setKey(setCommand.getKey());
      swordCommand.setValue(setCommand.getValue());
      swordCommand.setEx(setCommand.getEx());
    } else if (command instanceof SetNxCommand) {
      SetNxCommand setCommand = (SetNxCommand) command;
      swordCommand.setType(CommandType.SET_NX.getValue());
      swordCommand.setKey(setCommand.getKey());
      swordCommand.setValue(setCommand.getValue());
    } else if (command instanceof SetRangeCommand) {
      SetRangeCommand setCommand = (SetRangeCommand) command;
      swordCommand.setType(CommandType.SET_NX.getValue());
      swordCommand.setKey(setCommand.getKey());
      swordCommand.setValue(setCommand.getValue());
      swordCommand.setIndex(setCommand.getIndex());
    } else if (command instanceof IncrCommand) {
      IncrCommand incrCommand = (IncrCommand) command;
      swordCommand.setType(CommandType.INCR.getValue());
      swordCommand.setKey(incrCommand.getKey());
    } else if (command instanceof DecrCommand) {
      DecrCommand decrCommand = (DecrCommand) command;
      swordCommand.setType(CommandType.DECR.getValue());
      swordCommand.setKey(decrCommand.getKey());
    } else if (command instanceof SAddCommand) {
      SAddCommand sAddCommand = (SAddCommand) command;
      swordCommand.setType(CommandType.SADD.getValue());
      swordCommand.setKey(sAddCommand.getKey());
      swordCommand.setMembers(sAddCommand.getMembers());
    } else if (command instanceof HSetCommand) {
      HSetCommand hSetCommand = (HSetCommand) command;
      swordCommand.setType(CommandType.HSET.getValue());
      swordCommand.setKey(hSetCommand.getKey());
      swordCommand.setField(hSetCommand.getField());
      swordCommand.setValue(hSetCommand.getValue());
    } else if (command instanceof HMSetCommand) {
      HMSetCommand hSetCommand = (HMSetCommand) command;
      swordCommand.setType(CommandType.HMSET.getValue());
      swordCommand.setKey(hSetCommand.getKey());
      swordCommand.setFields(hSetCommand.getFields());
    } else if (command instanceof LSetCommand) {
      LSetCommand lSetCommand = (LSetCommand) command;
      swordCommand.setType(CommandType.LSET.getValue());
      swordCommand.setKey(lSetCommand.getKey());
      swordCommand.setIndex(lSetCommand.getIndex());
      swordCommand.setValue(lSetCommand.getValue());
    }
    return swordCommand;
  }
}
