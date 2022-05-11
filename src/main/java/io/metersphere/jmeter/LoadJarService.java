package io.metersphere.jmeter;

import groovy.lang.GroovyClassLoader;

public interface LoadJarService {
    public void loadGroovyJar(GroovyClassLoader classLoader);
}
