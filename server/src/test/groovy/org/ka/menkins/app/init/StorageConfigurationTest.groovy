package org.ka.menkins.app.init

import spock.lang.Specification

class StorageConfigurationTest extends Specification {

    void 'load all properties from env vars'() {
        given:
        def props = new Properties()
        def env = [
                (StorageConfiguration.TYPE.envName): 'local',
        ]
        def holder = new PropertiesHolder(env, props)

        when:
        def builder = StorageConfiguration.load(holder).apply(AppConfig.builder())

        then:
        builder.storageType == AppConfig.StorageType.LOCAL
    }

    void 'load all properties from system properties'() {
        given:
        def props = new Properties()
        props.put(StorageConfiguration.TYPE.propertyName, 'remote')
        def env = [:]
        def holder = new PropertiesHolder(env, props)

        when:
        def builder = StorageConfiguration.load(holder).apply(AppConfig.builder())

        then:
        builder.storageType == AppConfig.StorageType.REMOTE
    }

    void 'check default values are set'() {
        given:
        def props = new Properties()
        def env = [:]
        def holder = new PropertiesHolder(env, props)

        when:
        def builder = StorageConfiguration.load(holder).apply(AppConfig.builder())

        then:
        builder.storageType == AppConfig.StorageType.LOCAL
    }
}
