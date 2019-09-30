package com.dianwoda.usercenter.vera.namer.routeinfo;

import com.dianwoda.usercenter.vera.common.protocol.ResponseCode;
import com.dianwoda.usercenter.vera.common.protocol.hearbeat.PiperTaskData;
import com.dianwoda.usercenter.vera.common.protocol.route.TaskState;
import com.dianwoda.usercenter.vera.namer.dto.Action;
import com.dianwoda.usercenter.vera.namer.dto.InterResponse;
import com.dianwoda.usercenter.vera.namer.enums.ActionStateEnum;
import com.dianwoda.usercenter.vera.namer.enums.ActionTaskEnum;
import com.dianwoda.usercenter.vera.remoting.protocol.RemotingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author seam
 */
public class ActionManager {
  protected static final Logger log = LoggerFactory.getLogger(ActionManager.class);
  private ActionRecordManager actionRecordManager = new ActionRecordManager();
  private ToolsManager toolsManager = new ToolsManager();
  private static ActionManager actionManagerInstance = new ActionManager();

  public static ActionManager getInstance() {
    return actionManagerInstance;
  }
  private ActionManager() {

  }
  public InterResponse addAction(Action action) {
    return this.actionRecordManager.addAction(action);
  }


  public Map<Integer, Action> getActionMap() {
    return actionRecordManager.getActionMap();
  }

  public Action getAction(int id) {
    return this.actionRecordManager.getAction(id);
  }

  public InterResponse actionAgree(int id) {
    InterResponse interResponse = new InterResponse();
    Map<Integer, Action> actionMap = this.getActionMap();
    if (actionMap.containsKey(id)) {
      Action action = actionMap.get(id);
      if (!action.getActionStateEnum().isFinalState()) {
        // redis process
        if (action.getActionTaskEnum() == ActionTaskEnum.LISTEN_REDIS_ACTION) {
          RemotingCommand response = this.toolsManager.redisListen(action.getSrcLocation(), action.getMasterName(),
                  action.getSentinelList(), action.getPassword(), (short)0);
          if (response.getCode() == ResponseCode.SUCCESS) {
            return this.actionRecordManager.actonAgree(id);

          } else {
            interResponse.setCode(ResponseCode.SYSTEM_ERROR);
            interResponse.setRemark(response.getRemark());
            return interResponse;
          }

          // sync piper process
        } else if (action.getActionTaskEnum() == ActionTaskEnum.SYNC_PIPER_ACTION){
          RemotingCommand response = this.toolsManager.syncPiper(action.getSrcLocation(), action.getSyncPiperLocation(), (short)0);
          if (response.getCode() == ResponseCode.SUCCESS) {
            return this.actionRecordManager.actonAgree(id);

          } else {
            interResponse.setCode(ResponseCode.SYSTEM_ERROR);
            interResponse.setRemark(response.getRemark());
            return interResponse;
          }
        }
      } else {
        interResponse.setCode(ResponseCode.SYSTEM_ERROR);
        interResponse.setRemark("该action状态不一致");
      }
    } else {
      interResponse.setCode(ResponseCode.SYSTEM_ERROR);
      interResponse.setRemark("不存在该action记录");
    }
    return interResponse;
  }

  public InterResponse actionStart(int id) {
    InterResponse interResponse = new InterResponse();
    Map<Integer, Action> actionMap = this.getActionMap();
    if (actionMap.containsKey(id)) {
      Action action = actionMap.get(id);
      if (action.getActionStateEnum() == ActionStateEnum.AGREE) {
        // redis process
        if (action.getActionTaskEnum() == ActionTaskEnum.LISTEN_REDIS_ACTION) {
          RemotingCommand response = this.toolsManager.redisListen(action.getSrcLocation(), action.getMasterName(),
                  action.getSentinelList(), action.getPassword(), (short)1);
          if (response.getCode() == ResponseCode.SUCCESS) {
            return this.actionRecordManager.actonStart(id);

          } else {
            interResponse.setCode(ResponseCode.SYSTEM_ERROR);
            interResponse.setRemark(response.getRemark());
            return interResponse;
          }

        // sync piper process
        } else if (action.getActionTaskEnum() == ActionTaskEnum.SYNC_PIPER_ACTION){
          RemotingCommand response = this.toolsManager.syncPiper(action.getSrcLocation(), action.getSyncPiperLocation (), (short)1);
          if (response.getCode() == ResponseCode.SUCCESS) {
            return this.actionRecordManager.actonStart(id);

          } else {
            interResponse.setCode(ResponseCode.SYSTEM_ERROR);
            interResponse.setRemark(response.getRemark());
            return interResponse;
          }
        }
      } else {
        interResponse.setCode(ResponseCode.SYSTEM_ERROR);
        interResponse.setRemark("该action状态不一致");
      }
    } else {
      interResponse.setCode(ResponseCode.SYSTEM_ERROR);
      interResponse.setRemark("不存在该action记录");
    }
    return interResponse;
  }

  public InterResponse actionReject(Action action, PiperTaskData piperTaskData) {
    InterResponse interResponse = new InterResponse();

    // 如果PiperTaskData里面没有该action的信息，则忽略
    if (!this.existAction(piperTaskData, action)) {
      // 处理action
      this.actionRecordManager.actionReject(action.getId());
      interResponse.setCode(ResponseCode.SUCCESS);
      return interResponse;
    }

    if (!action.getActionStateEnum().isReject()) {
      RemotingCommand response = null;
        // redis process
      if (action.getActionTaskEnum() == ActionTaskEnum.LISTEN_REDIS_ACTION) {
        response = this.toolsManager.redisListen(action.getSrcLocation(), action.getMasterName(),
                action.getSentinelList(), action.getPassword(), (short) 2);

        // sync piper process
      } else if (action.getActionTaskEnum() == ActionTaskEnum.SYNC_PIPER_ACTION) {
        response = this.toolsManager.syncPiper(action.getSrcLocation(), action.getSyncPiperLocation(), (short)2);
      }
      if (response != null && response.getCode() == ResponseCode.SUCCESS) {
        return this.actionRecordManager.actionReject(action.getId());

      } else {
        interResponse.setCode(ResponseCode.SYSTEM_ERROR);
        interResponse.setRemark(response == null ? "" : response.getRemark());
        return interResponse;
      }


    } else {
      interResponse.setCode(ResponseCode.SYSTEM_ERROR);
      interResponse.setRemark("该action已经被拒绝");
    }

    return interResponse;
  }

  private boolean existAction(PiperTaskData piperTaskData, Action action) {
    if (piperTaskData == null) {
      return false;
    }
    if (action.getActionTaskEnum() == ActionTaskEnum.LISTEN_REDIS_ACTION) {
      if (piperTaskData.getMasterName() != null && piperTaskData.getMasterName().equals(action.getMasterName()) &&
              piperTaskData.getSentinelList().equals(action.getSentinelList()) &&
              piperTaskData.getListenRedisState() != TaskState.TASK_LISTEN_REDIS_ABORT) {
        return true;
      }
    } else if (action.getActionTaskEnum() == ActionTaskEnum.SYNC_PIPER_ACTION) {
      Map<String /* sync piper location */, TaskState> syncPiperTaskMap = piperTaskData.getSyncPiperStateMap();
      if (syncPiperTaskMap.containsKey(action.getSyncPiperLocation()) &&
              syncPiperTaskMap.get(action.getSyncPiperLocation()) != TaskState.TASK_SYNC_PIPER_ABORT) {
        return true;
      }
    }
    return false;
  }
}
