package com.dianwoda.usercenter.vera.namer;

import com.dianwoda.usercenter.vera.namer.filter.SSOFilter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author seam
 */
@SpringBootApplication
public class Application {

  public static void main(String[] args) {
    new SpringApplicationBuilder(Application.class).profiles("dwd-dev").build(args).run(args);
    System.out.println("namer start success");
  }

  @Bean
  public FilterRegistrationBean filterRegistrationBean() {
    FilterRegistrationBean registrationBean = new FilterRegistrationBean();
    SSOFilter ssoFilter = new SSOFilter();
    registrationBean.setFilter(ssoFilter);
    List<String> urlPatterns = new ArrayList<>();
    urlPatterns.add("/*");
    registrationBean.setUrlPatterns(urlPatterns);
    Map<String,String> params = new HashMap<>();
    params.put("ignorePath","/api/*");
    registrationBean.setInitParameters(params);
    return registrationBean;
  }

}
