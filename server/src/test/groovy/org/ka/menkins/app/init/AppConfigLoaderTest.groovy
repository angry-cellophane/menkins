package org.ka.menkins.app.init

import spock.lang.Specification

class AppConfigLoaderTest extends Specification {

    void 'load all properties from env vars'() {
        given:
        def props = new Properties()
        def env = [
                (AppConfigLoader.PORT.envName): '9999',
        ]
        def holder = new PropertiesHolder(env, props)

        when:
        def builder = AppConfigLoader.load(holder).apply(AppConfig.builder())

        then:
        builder.port == 9999
    }

    void 'load all properties from system properties'() {
        given:
        def props = new Properties()
        props.put(AppConfigLoader.PORT.propertyName, '8888')
        def env = [:]
        def holder = new PropertiesHolder(env, props)

        when:
        def builder = AppConfigLoader.load(holder).apply(AppConfig.builder())

        then:
        builder.port == 8888
    }

    void 'check default values are set'() {
        given:
        def props = new Properties()
        def env = [:]
        def holder = new PropertiesHolder(env, props)

        when:
        def builder = AppConfigLoader.load(holder).apply(AppConfig.builder())

        then:
        builder.port == 5678
    }
}
