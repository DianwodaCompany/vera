package com.dianwoda.usercenter.vera.namer.aspect;

import com.alibaba.fastjson.JSON;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

@Aspect
@Component
public class WebLogAspect {
  protected static final Logger log = LoggerFactory.getLogger(WebLogAspect.class);

  @Pointcut("execution(public * com.dianwoda.usercenter.vera.namer.controller.*.*(..)) && @annotation(org.springframework.web.bind.annotation.RequestMapping)")
  public void webLog() {
  }

  @AfterReturning(returning = "ret", pointcut = "webLog()")
  public void doAfterReturning(Object ret) throws Throwable {
    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    HttpServletRequest request = attributes.getRequest();
    StringBuffer param = new StringBuffer();
    Enumeration<String> e = request.getParameterNames();
    while (e.hasMoreElements()) {
      String key = e.nextElement();
      param.append(key).append("=").append(request.getParameter(key));
      if (e.hasMoreElements()) {
          param.append("&");
      }
    }
    String result;
    try {
        result = JSON.toJSONString(ret);
    } catch (Exception e1) {
        log.error("afterReturning occors error", e1);
        result = "JsonProcessingException";
    }
    log.info(String.format("uri:%s, remoteIp:%s,param:%s, result:%s",
            request.getRequestURI(), request.getRemoteAddr(),param.toString(), result));
  }
}
