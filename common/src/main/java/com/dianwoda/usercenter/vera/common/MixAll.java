package com.dianwoda.usercenter.vera.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Properties;

/**
 * @author seam
 */
public class MixAll {
  private static final Logger log = LoggerFactory.getLogger(MixAll.class);
  public static final long MASTER_ID = 0L;

  public static Properties object2Properties(final Object object) {
    Properties properties = new Properties();

    Field[] fields = object.getClass().getDeclaredFields();
    for (Field field : fields) {
      if (!Modifier.isStatic(field.getModifiers())) {
        String name = field.getName();
        if (!name.startsWith("this")) {
          Object value = null;
          try {
            field.setAccessible(true);
            value = field.get(object);
          } catch (IllegalAccessException e) {
            log.error("Failed to handle properties", e);
          }

          if (value != null) {
            properties.setProperty(name, value.toString());
          }
        }
      }
    }

    return properties;
  }

  public static void properties2Object(final Properties p, final Object object) {
    Method[] methods = object.getClass().getMethods();
    for (Method method : methods) {
      String mn = method.getName();
      if (mn.startsWith("set")) {
        try {
          String tmp = mn.substring(4);
          String first = mn.substring(3, 4);

          String key = first.toLowerCase() + tmp;
          String property = p.getProperty(key);
          if (property != null) {
            Class<?>[] pt = method.getParameterTypes();
            if (pt != null && pt.length > 0) {
              String cn = pt[0].getSimpleName();
              Object arg = null;
              if (cn.equals("int") || cn.equals("Integer")) {
                arg = Integer.parseInt(property);
              } else if (cn.equals("long") || cn.equals("Long")) {
                arg = Long.parseLong(property);
              } else if (cn.equals("double") || cn.equals("Double")) {
                arg = Double.parseDouble(property);
              } else if (cn.equals("boolean") || cn.equals("Boolean")) {
                arg = Boolean.parseBoolean(property);
              } else if (cn.equals("float") || cn.equals("Float")) {
                arg = Float.parseFloat(property);
              } else if (cn.equals("String")) {
                arg = property;
              } else {
                continue;
              }
              method.invoke(object, arg);
            }
          }
        } catch (Throwable ignored) {
        }
      }
    }
  }
}
