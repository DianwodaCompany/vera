package com.dianwoda.usercenter.vera.piper.controller;

import com.dianwoda.usercenter.vera.piper.test.RedisConfigure;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author seam
 */
@RestController
@RequestMapping(value = "/test")
public class TestController {
  RedisConfigure redisConfigure = new RedisConfigure();


  @RequestMapping("/hset2")
  @ResponseBody
  public Object hset2(@RequestParam(name="hash") String hash,
                     @RequestParam(name="key") String key,
                     @RequestParam(name="value") String value) {

    Object obj = null;
    if (value == null || value == "") {
      obj = System.currentTimeMillis();
    } else {
      obj = Long.parseLong(value);
    }

    RedisConfigure.cacheManager.hashPut(hash, key, obj);
    return "ok";

  }

  @RequestMapping("/hget2")
  @ResponseBody
  public Object hget2(@RequestParam(name="hash") String hash,
                     @RequestParam(name="key") String key) {

    Object obj = RedisConfigure.cacheManager.hashGet(hash, key);
    return obj;
  }
}
