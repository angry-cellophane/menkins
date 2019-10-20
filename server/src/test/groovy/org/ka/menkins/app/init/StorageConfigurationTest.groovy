package org.ka.menkins.app.init

import spock.lang.Specification

class StorageConfigurationTest extends Specification {

    void 'load all properties from env vars'() {
        given:
        def props = new Properties()
        def env = [
                (StorageConfiguration.TYPE.envName): 'local',
                (StorageConfiguration.HZ_NODES.envName): 'localhost:5701',
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
        props.put(StorageConfiguration.HZ_NODES.propertyName, 'localhost:5701')
        props.put(StorageConfiguration.HZ_CLUSTER_NAME.propertyName, 'menkins')
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
        props.put(StorageConfiguration.HZ_NODES.propertyName, 'localhost:5701')
        def env = [:]
        def holder = new PropertiesHolder(env, props)

        when:
        def builder = StorageConfiguration.load(holder).apply(AppConfig.builder())

        then:
        builder.storageType == AppConfig.StorageType.LOCAL
    }

    void 'error when hazelcast seed nodes not set'() {
        given:
        def props = new Properties()
        props.put(StorageConfiguration.TYPE.propertyName, 'remote')
        props.put(StorageConfiguration.HZ_CLUSTER_NAME.propertyName, 'menkins')
        def env = [:]
        def holder = new PropertiesHolder(env, props)

        when:
        StorageConfiguration.load(holder).apply(AppConfig.builder())

        then:
        thrown(PropertiesHolder.PropertyNotFoundException)
    }

    void 'error when hazelcast cluster name not set'() {
        given:
        def props = new Properties()
        props.put(StorageConfiguration.TYPE.propertyName, 'remote')
        props.put(StorageConfiguration.HZ_NODES.propertyName, 'localhost:5701, localhost:5702')
        def env = [:]
        def holder = new PropertiesHolder(env, props)

        when:
        StorageConfiguration.load(holder).apply(AppConfig.builder())

        then:
        thrown(PropertiesHolder.PropertyNotFoundException)
    }

    void 'hazelcast nodes split by ,'() {
        given:
        def props = new Properties()
        props.put(StorageConfiguration.TYPE.propertyName, 'remote')
        props.put(StorageConfiguration.HZ_NODES.propertyName, 'localhost:5701, localhost:5702')
        props.put(StorageConfiguration.HZ_CLUSTER_NAME.propertyName, 'menkins')
        def env = [:]
        def holder = new PropertiesHolder(env, props)

        when:
        def builder = StorageConfiguration.load(holder).apply(AppConfig.builder())

        then:
        builder.hazelcast.networkConfig.addresses == ['localhost:5701', 'localhost:5702']
    }

    void 'hazelcast cluster name is set'() {
        given:
        def props = new Properties()
        props.put(StorageConfiguration.TYPE.propertyName, 'remote')
        props.put(StorageConfiguration.HZ_NODES.propertyName, 'localhost:5701, localhost:5702')
        props.put(StorageConfiguration.HZ_CLUSTER_NAME.propertyName, 'menkins')
        def env = [:]
        def holder = new PropertiesHolder(env, props)

        when:
        def builder = StorageConfiguration.load(holder).apply(AppConfig.builder())

        then:
        builder.hazelcast.groupConfig.name == 'menkins'
    }
}
