package org.ka.menkins.app.init

import spock.lang.Specification

class HttpConfigLoaderTest extends Specification {

    void 'load all properties from env vars'() {
        given:
        def props = new Properties()
        def env = [
                (HttpConfigLoader.PORT.envName): '9999',
        ]
        def holder = new PropertiesHolder(env, props)

        when:
        def builder = HttpConfigLoader.load(holder).apply(AppConfig.builder())

        then:
        builder.http != null
        builder.http.port == 9999
    }

    void 'load all properties from system properties'() {
        given:
        def props = new Properties()
        props.put(HttpConfigLoader.PORT.propertyName, '8888')
        def env = [:]
        def holder = new PropertiesHolder(env, props)

        when:
        def builder = HttpConfigLoader.load(holder).apply(AppConfig.builder())

        then:
        builder.http != null
        builder.http.port == 8888
    }

    void 'check default values are set'() {
        given:
        def props = new Properties()
        def env = [:]
        def holder = new PropertiesHolder(env, props)

        when:
        def builder = HttpConfigLoader.load(holder).apply(AppConfig.builder())

        then:
        builder.http != null
        builder.http.port == 5678
    }
}
