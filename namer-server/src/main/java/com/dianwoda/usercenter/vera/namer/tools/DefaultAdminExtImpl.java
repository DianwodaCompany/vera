package com.dianwoda.usercenter.vera.namer.tools;

import com.dianwoda.usercenter.vera.common.protocol.ResponseCode;
import com.dianwoda.usercenter.vera.common.protocol.body.ConsumerRunningInfo;
import com.dianwoda.usercenter.vera.common.protocol.hearbeat.PiperTaskData;
import com.dianwoda.usercenter.vera.namer.NamerFactory;
import com.dianwoda.usercenter.vera.namer.dto.Action;
import com.dianwoda.usercenter.vera.namer.dto.InterResponse;
import com.dianwoda.usercenter.vera.namer.enums.ActionTaskEnum;
import com.dianwoda.usercenter.vera.namer.routeinfo.ActionManager;
import com.dianwoda.usercenter.vera.namer.routeinfo.RouteInfoManager;
import com.dianwoda.usercenter.vera.namer.routeinfo.ToolsManager;
import com.dianwoda.usercenter.vera.remoting.protocol.RemotingCommand;
import com.google.common.base.Preconditions;

import java.util.List;

/**
 * 封装controler过来的请求操作
 * @author seam
 */
public class DefaultAdminExtImpl {
  private RouteInfoManager routeInfoManager;
  private ActionManager actionManager;
  private ToolsManager toolsManager;

  public DefaultAdminExtImpl() {
    this.routeInfoManager = RouteInfoManager.getIntance();
    this.actionManager = ActionManager.getInstance();
    this.toolsManager = new ToolsManager();
  }

  public RemotingCommand redisListen(String location, String masterName, List<String> sentinels,
                                     String password, int operationType) {
    return toolsManager.redisListen(location, masterName, sentinels, password, operationType);
  }

  public RemotingCommand syncPiper(String srcLocation, String syncPiperLocation, String group, int operationType) {
    return toolsManager.syncPiper(srcLocation, syncPiperLocation, group, operationType);
  }

  public InterResponse addAction(Action action) {
    return this.actionManager.addAction(action);
  }

  public InterResponse actionAgree(int actionId) {
    return this.actionManager.actionAgree(actionId);
  }

  public InterResponse actionStart(int actionId) {
    return this.actionManager.actionStart(actionId);
  }

  public InterResponse actionReject(int actionId) {
    Action action = this.checkAction(actionId);
    PiperTaskData piperTaskData = this.routeInfoManager.getPiperTaskData(action.getSrcLocation());

    return this.actionManager.actionReject(action, piperTaskData);
  }

  private Action checkAction(int actionId) {
    Action action = this.actionManager.getAction(actionId);
    Preconditions.checkNotNull(action);
    return action;
  }

  public InterResponse runningInfo(String location) {
    RemotingCommand response = toolsManager.runningInfo(location);
    InterResponse interResponse = null;
    if (response.getBody() != null) {
      ConsumerRunningInfo runningInfo = ConsumerRunningInfo.decode(response.getBody(), ConsumerRunningInfo.class);
      interResponse = InterResponse.createInterResponse(ResponseCode.SUCCESS);
      interResponse.setData(runningInfo.toString());
    } else {
      interResponse = InterResponse.createInterResponse(ResponseCode.SYSTEM_ERROR);
      interResponse.setRemark(response.getRemark());
    }

    return interResponse;
  }
}
