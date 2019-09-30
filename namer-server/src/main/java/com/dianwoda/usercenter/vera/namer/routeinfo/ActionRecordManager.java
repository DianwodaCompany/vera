package com.dianwoda.usercenter.vera.namer.routeinfo;

import com.dianwoda.usercenter.vera.common.SystemClock;
import com.dianwoda.usercenter.vera.common.protocol.ResponseCode;
import com.dianwoda.usercenter.vera.namer.dto.Action;
import com.dianwoda.usercenter.vera.namer.dto.InterResponse;
import com.dianwoda.usercenter.vera.namer.enums.ActionStateEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author seam
 */
public class ActionRecordManager {
  protected static final Logger log = LoggerFactory.getLogger(ActionRecordManager.class);

  private Map<Integer, Action> actionMap = new ConcurrentHashMap<>();
  private ReadWriteLock actionLock = new ReentrantReadWriteLock();

  public ActionRecordManager() {

  }

  public Action getAction(int id) {
    return this.actionMap.get(id);
  }

  public boolean containAction(int id) {
    return this.actionMap.containsKey(id);
  }

  public InterResponse addAction(Action action) {
    InterResponse interResponse = new InterResponse();
    try {
      actionLock.writeLock().lockInterruptibly();

      actionMap.put(action.getId(), action);
      interResponse.setCode(ResponseCode.SUCCESS);
    } catch (Exception e) {
      interResponse.setCode(ResponseCode.SYSTEM_ERROR);
      interResponse.setRemark("add action error: " + e.getMessage());
      log.error("add action error", e);
    } finally {
      actionLock.writeLock().unlock();
    }
    return interResponse;
  }

  public InterResponse actonAgree(int actionId) {
    InterResponse interResponse = new InterResponse();
    try {
      actionLock.writeLock().lockInterruptibly();
      Action action = this.actionMap.get(actionId);
      if (action == null || action.getActionStateEnum().isFinalState()) {
        interResponse.setCode(ResponseCode.SYSTEM_ERROR);
        interResponse.setRemark("记录信息不一致");
      } else {
        action.setActionStateEnum(ActionStateEnum.AGREE);
        action.setUpdateTime(SystemClock.now());
        interResponse.setCode(ResponseCode.SUCCESS);
      }
    } catch (Exception e) {
      interResponse.setCode(ResponseCode.SYSTEM_ERROR);
      interResponse.setRemark("action agree error, " + e.getMessage());
      log.error("action agree error", e);
    } finally {
      actionLock.writeLock().unlock();
    }
    return interResponse;
  }

  public InterResponse actonStart(int actionId) {
    InterResponse interResponse = new InterResponse();
    try {
      actionLock.writeLock().lockInterruptibly();
      Action action = this.actionMap.get(actionId);
      if (action == null || !action.getActionStateEnum().isAgree()) {
        interResponse.setCode(ResponseCode.SYSTEM_ERROR);
        interResponse.setRemark("记录信息不一致");
      } else {
        action.setActionStateEnum(ActionStateEnum.FINISH);
        action.setUpdateTime(SystemClock.now());
        interResponse.setCode(ResponseCode.SUCCESS);
      }
    } catch (Exception e) {
      interResponse.setCode(ResponseCode.SYSTEM_ERROR);
      interResponse.setRemark("action start error, " + e.getMessage());
      log.error("action start error", e);
    } finally {
      actionLock.writeLock().unlock();
    }
    return interResponse;
  }

  public InterResponse actionReject(int actionId) {
    InterResponse interResponse = new InterResponse();
    try {
      actionLock.writeLock().lockInterruptibly();
      Action action = this.actionMap.get(actionId);
      if (action == null) {
        interResponse.setCode(ResponseCode.SYSTEM_ERROR);
        interResponse.setRemark("记录信息不一致");
      } else {
        action.setActionStateEnum(ActionStateEnum.REJECT);
        action.setUpdateTime(SystemClock.now());
        interResponse.setCode(ResponseCode.SUCCESS);
      }

    } catch (Exception e) {
      interResponse.setCode(ResponseCode.SYSTEM_ERROR);
      interResponse.setRemark("action reject error, " + e.getMessage());
      log.error("action reject error", e);
    } finally {
      actionLock.writeLock().unlock();
    }
    return interResponse;
  }

  public Map<Integer, Action> getActionMap() {
    return actionMap;
  }

  public void setActionMap(Map<Integer, Action> actionMap) {
    this.actionMap = actionMap;
  }
}
