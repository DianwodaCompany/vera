package com.dianwoda.usercenter.vera.piper;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * @author seam
 */
@EnableAutoConfiguration
@SpringBootApplication
public class HZUnit1Application {

  public static void main(String[] args) {
    new SpringApplicationBuilder(HZUnit1Application.class).profiles("hz-unit1").build(args).run(args);
    System.out.println("piper start success");
  }
}
