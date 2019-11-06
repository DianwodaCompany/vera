package com.dianwoda.usercenter.vera.namer.controller;

import com.dianwoda.usercenter.vera.namer.tools.DefaultAdminExtImpl;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * @author seam
 */
@RestController
@RequestMapping(value = "/test")
public class TestController {
  private DefaultAdminExtImpl defaultAdminExtImpl = new DefaultAdminExtImpl();

  @RequestMapping("/listenRedis")
  @ResponseBody
  public Object listenRedis(@RequestParam(name="operateType") int operateType) {
    String redisUri = "redis://127.0.0.1:6379";
    String masterName = "mymaster";
    List<String> sentinels = new ArrayList();
    sentinels.add("127.0.0.1:26379");
    String password = "foobared";
    String location = "192.168.102.254:8025";
    return defaultAdminExtImpl.redisListen(location, masterName, sentinels, password, operateType);
  }

  @RequestMapping("/syncPiper")
  @ResponseBody
  public Object syncPiper(@RequestParam(name="operateType") int operateType) {
    String srcLocation = "192.168.102.254:16571";
    String syncLocation = "192.168.102.254:16591";
    String group = "hz-unit2";
    return defaultAdminExtImpl.syncPiper(srcLocation, syncLocation, group, operateType);
  }


  @RequestMapping("/listenRedis2")
  @ResponseBody
  public Object listenRedis2(@RequestParam(name="operateType") int operateType) {
    String redisUri = "redis://127.0.0.1:6378";
    String masterName = "mymaster2";
    List<String> sentinels = new ArrayList();
    sentinels.add("127.0.0.1:26378");
    String password = "foobared";
    String location = "192.168.102.254:8026";
    return defaultAdminExtImpl.redisListen(location, masterName, sentinels, password, operateType);
  }

  @RequestMapping("/syncPiper2")
  @ResponseBody
  public Object syncPiper2(@RequestParam(name="operateType") int operateType) {
    String srcLocation = "192.168.102.254:16591";
    String syncLocation = "192.168.102.254:16571";
    String group = "hz-unit1";
    return defaultAdminExtImpl.syncPiper(srcLocation, syncLocation, group, operateType);
  }
}
