package com.dianwoda.usercenter.vera.piper.client.listener;

import com.dianwoda.usercenter.vera.common.message.CommandExt;
import com.dianwoda.usercenter.vera.piper.enums.ConsumeOrderlyStatus;

import java.util.List;

/**
 * 顺序消费redis command接口
 * @author seam
 */
public interface CommandListenerOrderly extends CommandListener {

  ConsumeOrderlyStatus consumer(String syncPiperLocation, List<CommandExt> commands);
}
