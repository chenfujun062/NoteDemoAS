package com.rockchip.notedemo.util;

import java.lang.reflect.Method;

public class PropertyUtil {

    public static String getProperty(String key, String defaultValue) {
        String value = defaultValue;
        try {
            Class c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            value = (String)(get.invoke(c, key));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return value;
        }
    }

    public static void setProperty(String key, String penDrawMode) {
        try {
            Class c = Class.forName("android.os.SystemProperties");
            Method set = c.getMethod("set", String.class, String.class);
            set.invoke(c, key, penDrawMode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
