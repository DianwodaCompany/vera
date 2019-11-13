package com.dianwoda.usercenter.vera.namer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author seam
 */
@Controller
@RequestMapping(value = "/config")
public class ConfigController {

  private String namerServerAddrs = null;

  @RequestMapping("/setNamerAddrs")
  public @ResponseBody
  Object setNamerAddrs(@RequestParam(name = "namerAddrs") String namerServerAddrs) {
    this.namerServerAddrs = namerServerAddrs;
    return this.namerServerAddrs;
  }

  @RequestMapping("/getNamerAddrs")
  public @ResponseBody
  Object getNamerAdds() {
    return this.namerServerAddrs;
  }

}
