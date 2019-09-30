package com.dianwoda.usercenter.vera.namer.config;


import com.dianwoda.usercenter.vera.common.util.IPUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * @author seam
 */
@Configuration
public class NamerConfig {

  private Environment environment;


  public NamerConfig(Environment environment) {
    this.environment = environment;
  }

  public String location() {
    return IPUtil.getServerIp() + ":" + getParam(NamerConfigKey.NAMER_NAMER_PORT);
  }

  public <T> T getParam(String key, Class<T> valueClass) {
    return environment.getProperty(key, valueClass);
  }

  public String getParam(String key) {
    return environment.getProperty(key);
  }

}
