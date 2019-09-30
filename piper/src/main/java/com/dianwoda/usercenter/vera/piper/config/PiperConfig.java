package com.dianwoda.usercenter.vera.piper.config;



import com.dianwoda.usercenter.vera.common.util.IPUtil;
import org.springframework.core.env.Environment;

/**
 * @author seam
 */
public class PiperConfig {

  private int registerPiperTimeoutMills = 6000;
  private int unregisterPiperTimeoutMills = 500;
  private Environment environment;

  public PiperConfig(Environment environment) {
    this.environment = environment;
  }

  public String location() {
    return IPUtil.getServerIp() + ":" + getParam(PiperConfigKey.PIPER_BIND_PORT);
  }

  public <T> T getParam(String key, Class<T> valueClass) {
    return environment.getProperty(key, valueClass);
  }

  public String getParam(String key) {
    return environment.getProperty(key);
  }


  public String nameLocation() {
    return this.getParam(PiperConfigKey.PIPER_NAMER_LOCATION);
  }

  public String storePath() {
    return this.getParam(PiperConfigKey.DATA_STORE_PATH);
  }

  public int piperId() {
    return this.getParam(PiperConfigKey.PIPER_ID, Integer.class);
  }

  public String group() {
    return this.getParam(PiperConfigKey.PIPER_GROUP);
  }


  public int getRegisterPiperTimeoutMills() {
    return registerPiperTimeoutMills;
  }

  public int getUnregisterPiperTimeoutMills() {
    return unregisterPiperTimeoutMills;
  }
}
