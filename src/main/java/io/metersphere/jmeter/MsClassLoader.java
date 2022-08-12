package io.metersphere.jmeter;

import com.alibaba.fastjson.JSON;
import groovy.lang.GroovyClassLoader;
import io.metersphere.utils.LoggerUtil;
import org.apache.commons.collections.CollectionUtils;

import java.io.File;
import java.net.URL;
import java.util.List;

public class MsClassLoader {
    public static MsDynamicClassLoader loadJar(List<String> jarPaths) {
        if (CollectionUtils.isEmpty(jarPaths)) {
            return null;
        }
        LoggerUtil.info("开始初始化JAR[ " + JSON.toJSONString(jarPaths) + " ]");
        MsDynamicClassLoader urlClassLoader = new MsDynamicClassLoader();
        jarPaths.forEach(path -> {
            try {
                File jarFile = new File(path);
                URL jarUrl = jarFile.toURI().toURL();
                urlClassLoader.addURLFile(jarUrl);
                LoggerUtil.info("完成初始化JAR[ " + path + " ]");
            } catch (Exception ex) {
                LoggerUtil.error("加载JAR包失败：", ex);
            }
        });
        return urlClassLoader;
    }

    public static void loadJar(List<String> jarPaths, GroovyClassLoader classLoader) {
        if (CollectionUtils.isEmpty(jarPaths)) {
            return;
        }
        LoggerUtil.info("开始初始化GroovyJAR[ " + JSON.toJSONString(jarPaths) + " ]");
        jarPaths.forEach(path -> {
            try {
                File jarFile = new File(path);
                URL jarUrl = jarFile.toURI().toURL();
                classLoader.addURL(jarUrl);
                LoggerUtil.info("完成初始化JAR[ " + path + " ]");
            } catch (Exception ex) {
                LoggerUtil.error("加载JAR包失败：", ex);
            }
        });
    }

    private MsClassLoader() {
    }
}
