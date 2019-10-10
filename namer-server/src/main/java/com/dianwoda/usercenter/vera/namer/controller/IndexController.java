package com.dianwoda.usercenter.vera.namer.controller;

import com.dianwoda.usercenter.vera.common.Pagination;
import com.dianwoda.usercenter.vera.common.protocol.ResponseCode;
import com.dianwoda.usercenter.vera.common.protocol.hearbeat.PiperTaskData;
import com.dianwoda.usercenter.vera.common.protocol.route.PiperData;
import com.dianwoda.usercenter.vera.namer.dto.Action;
import com.dianwoda.usercenter.vera.namer.dto.InterResponse;
import com.dianwoda.usercenter.vera.namer.dto.PiperInfoDTO;
import com.dianwoda.usercenter.vera.namer.processor.DefaultRequestProcessor;
import com.dianwoda.usercenter.vera.namer.routeinfo.ActionManager;
import com.dianwoda.usercenter.vera.namer.routeinfo.RouteInfoManager;
import com.dianwoda.usercenter.vera.namer.tools.DefaultAdminExtImpl;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author seam
 */
@Controller
@RequestMapping(value="/index")
public class IndexController {
  private static final Logger log = LoggerFactory.getLogger(IndexController.class);

  private DefaultAdminExtImpl defaultAdminExtImpl = new DefaultAdminExtImpl();

  @RequestMapping("/list")
  public String index() {
    return "index/list";
  }

  @RequestMapping("piperList")
  public @ResponseBody
  Object piperList(@RequestParam(defaultValue = "1", required = false) int page,
                   @RequestParam(defaultValue = "1000", required = false) int pageSize,
                   @RequestParam(name = "name", required = false) String searchText) {
    Pagination<PiperInfoDTO> pagination = new Pagination(page, pageSize);
    RouteInfoManager routeInfoManager = RouteInfoManager.getIntance();
    List<PiperInfoDTO> piperInfoDTOList = new ArrayList<PiperInfoDTO>();
    Map<String, RouteInfoManager.PiperLiveInfo> piperLiveInfoMap = routeInfoManager.getPiperLiveInfoMap();
    log.info("live piper:" + Lists.newArrayList(piperLiveInfoMap.entrySet()) + " size:" + piperLiveInfoMap.size());
    for (Map.Entry<String, RouteInfoManager.PiperLiveInfo> entry : piperLiveInfoMap.entrySet()) {
      String location = entry.getKey();
      PiperData piperData = routeInfoManager.getPiperData(location);
      if (piperData == null) {
        log.info("filter location:" + location);

        continue;
      }
      PiperTaskData piperTaskData = routeInfoManager.getPiperTaskData(location);
      long updateTime = entry.getValue().getLastUpdateTimestamp();
      PiperInfoDTO piperInfoDTO = new PiperInfoDTO(piperData, piperTaskData, updateTime);
      piperInfoDTOList.add(piperInfoDTO);
    }

    piperInfoDTOList = piperInfoDTOList.stream().filter(a ->
            StringUtils.isEmpty(searchText) ? true : a.getLocation().equals(searchText)).collect(Collectors.toList());

    pagination.setList(piperInfoDTOList);
    pagination.setTotalCount(piperInfoDTOList.size());
    pagination.setCurrentPage(1);
    return pagination;
  }

  @RequestMapping("listenRedisSave")
  public @ResponseBody
  Object listenRedisSave(@RequestParam(name = "location") String location,
                         @RequestParam(name = "masterName") String masterName,
                         @RequestParam(name = "sentinels") String sentinels,
                         @RequestParam(name = "password") String password) {

    Preconditions.checkNotNull(location);
    Preconditions.checkNotNull(masterName);
    Preconditions.checkNotNull(sentinels);

    List<String> sentinelList = Lists.newArrayList(sentinels.split(","));
    RouteInfoManager routeInfoManager = RouteInfoManager.getIntance();
    boolean existListenRedis = routeInfoManager.existListenRedisTask(location, masterName,
            sentinelList, password);
    if (existListenRedis) {
      return InterResponse.createInterResponse(ResponseCode.SYSTEM_ERROR, "redis侦听已经存在，请先关闭!");
    }
    PiperData piperData = routeInfoManager.getPiperData(location);
    return this.defaultAdminExtImpl.addAction(Action.buildListenRedisAction(piperData, masterName, sentinelList, password));
  }


  @RequestMapping("syncPiperSave")
  public @ResponseBody
  Object syncPiperSave(@RequestParam(name = "location") String location,
                       @RequestParam(name = "syncPiperLocation") String syncPiperLocation) {

    RouteInfoManager routeInfoManager = RouteInfoManager.getIntance();
    Preconditions.checkNotNull(location);
    Preconditions.checkNotNull(syncPiperLocation);
    if (!routeInfoManager.existListenRedisTask(location)) {
      InterResponse interResponse = InterResponse.createInterResponse(ResponseCode.SYSTEM_ERROR, "redis未初始化完成");
      return interResponse;
    }
    PiperData piperData = routeInfoManager.getPiperData(location);
    return this.defaultAdminExtImpl.addAction(Action.buildSyncPiperAction(piperData, syncPiperLocation));
  }


  @RequestMapping("stopRedisListen")
  public @ResponseBody
  Object stopRedisListen(@RequestParam(name = "location") String location,
                         @RequestParam(name = "masterName") String masterName,
                         @RequestParam(name = "sentinels") String sentinels,
                         @RequestParam(name = "password", required = false) String password) {

    Preconditions.checkNotNull(location);
    Preconditions.checkNotNull(sentinels);
    List<String> sentinelList = Lists.newArrayList(sentinels.split(","));
    return this.defaultAdminExtImpl.redisListen(location, masterName, sentinelList, password, 2);
  }

  @RequestMapping("stopSyncPiper")
  public @ResponseBody
  Object stopSyncPiper(@RequestParam(name = "location") String location,
                       @RequestParam(name = "syncPiperLocation") String syncPiperLocation) {

    Preconditions.checkNotNull(location);
    Preconditions.checkNotNull(syncPiperLocation);
    return this.defaultAdminExtImpl.syncPiper(location, syncPiperLocation, 2);
  }

  @RequestMapping("runningInfo")
  public @ResponseBody
  Object runningInfo(@RequestParam(name = "location") String location) {
    return this.defaultAdminExtImpl.runningInfo(location);
  }
}
