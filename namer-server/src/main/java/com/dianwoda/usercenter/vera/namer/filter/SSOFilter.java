package com.dianwoda.usercenter.vera.namer.filter;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class SSOFilter implements Filter {
  private static final Logger logger = LoggerFactory.getLogger(SSOFilter.class);
  private static String IGNORE_PATH = "ignorePath";
  Set<Pattern> ignorePath = new HashSet<>();

  public void init(FilterConfig filterConfig) throws ServletException {
    String p = filterConfig.getInitParameter(IGNORE_PATH);
    if (p == null) {
      p = "";
    }
    p = p + ",*.jpg,*.jpeg,*.ico,*.png,*.gif,*.css,*.js,*.svg,*.ttf,*.woff,*.woff2";
    List<String> list = new ArrayList<>();
    for (String s : p.split(",")) {
      s = s.trim();
      if (s.length() == 0 || list.contains(s)) continue;
      list.add(s);
      ignorePath.add(Pattern.compile(s.replace("*", ".*")));
    }
  }

  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    HttpSession session = request.getSession(false);

    String uri = request.getRequestURI();

    for (Pattern pattern : ignorePath) {
      if (pattern.matcher(uri).matches()) {
        filterChain.doFilter(servletRequest, servletResponse);
        return;
      }
    }
    filterChain.doFilter(request, response);
  }

  public void destroy() {

  }
}
