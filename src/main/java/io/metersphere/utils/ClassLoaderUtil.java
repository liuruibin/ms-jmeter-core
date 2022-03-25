package io.metersphere.utils;

import io.metersphere.jmeter.MsExecListener;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class ClassLoaderUtil {
    private static Map<String, Class<?>> classMap = new HashMap<>();

    public static Class<?> getClass(String className) throws Exception {
        if (StringUtils.isEmpty(className)) {
            className = MsExecListener.class.getCanonicalName();
        }
        if (classMap.containsKey(className) && classMap.get(className) != null) {
            return classMap.get(className);
        }
        Class<?> clazz = Class.forName(className);
        classMap.put(className, clazz);
        return clazz;
    }
}
