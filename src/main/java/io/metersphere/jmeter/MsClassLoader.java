package io.metersphere.jmeter;

import com.alibaba.fastjson.JSON;
import groovy.lang.GroovyClassLoader;
import io.metersphere.utils.LoggerUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

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
                if (StringUtils.isNotEmpty(path)) {
                    File jarFile = new File(path);
                    if (jarFile.exists()) {
                        URL jarUrl = jarFile.toURI().toURL();
                        urlClassLoader.addURLFile(jarUrl);
                    }
                }
                LoggerUtil.info("完成初始化JAR[ " + path + " ]");
            } catch (Exception ex) {
                LoggerUtil.error("加载JAR包失败：", ex);
            }
        });
        return urlClassLoader;
    }

    public static void loadJar(List<String> jarPaths, GroovyClassLoader groovyClassLoader) {
        if (CollectionUtils.isNotEmpty(jarPaths)) {
            LoggerUtil.info("开始初始化JAR[ " + JSON.toJSONString(jarPaths) + " ]");
            jarPaths.forEach(path -> {
                try {
                    if (StringUtils.isNotEmpty(path)) {
                        File jarFile = new File(path);
                        if (jarFile.exists()) {
                            groovyClassLoader.addURL(jarFile.toURI().toURL());
                        }
                    }
                    LoggerUtil.info("完成初始化JAR[ " + path + " ]");
                } catch (Exception ex) {
                    LoggerUtil.error("加载JAR包失败：", ex);
                }
            });
        }
    }

    private MsClassLoader() {
    }
}
