package org.ka.menkins.app.init

import spock.lang.Specification

class MesosConfigLoaderTest extends Specification {

    void 'load all properties from env vars'() {
        given:
        def props = new Properties()
        def env = [
                (MesosConfigLoader.LIB_PATH.envName)     : '/var/path',
                (MesosConfigLoader.MASTER_URL.envName)   : 'master:5050',
                (MesosConfigLoader.ROLE.envName)         : '*',
                (MesosConfigLoader.SLAVE_USER.envName)   : 'jenkins',
                (MesosConfigLoader.FRAMEWORK_ID.envName) : '1234',
                (MesosConfigLoader.CHECKPOINT_ON.envName): 'false',
                (MesosConfigLoader.WEBUI_URL.envName)    : 'http://webui',
                (MesosConfigLoader.PRINCIPLE.envName)    : 'principle',
                (MesosConfigLoader.SECRET.envName)       : 'secret',
        ]
        def holder = new PropertiesHolder(env, props)

        when:
        def builder = MesosConfigLoader.load(holder).apply(AppConfig.builder())
        def mesos = builder.mesos

        then:
        mesos.with {
            pathToMesosLib == '/var/path'
            mesosMasterUrl == 'master:5050'
            role == '*'
            slaveUser == 'jenkins'
            frameworkName == 'jenkins-1234'
            checkpoint == false
            webUiUrl == 'http://webui'
            principal == 'principle'
            secret == 'secret'
        }
    }

    void 'load all properties from system properties'() {
        given:
        def props = new Properties()
        props.put(MesosConfigLoader.LIB_PATH.propertyName, '/var/path')
        props.put(MesosConfigLoader.MASTER_URL.propertyName, 'master:5050')
        props.put(MesosConfigLoader.ROLE.propertyName, '*')
        props.put(MesosConfigLoader.SLAVE_USER.propertyName, 'jenkins')
        props.put(MesosConfigLoader.FRAMEWORK_ID.propertyName, '1234')
        props.put(MesosConfigLoader.CHECKPOINT_ON.propertyName, 'false')
        props.put(MesosConfigLoader.WEBUI_URL.propertyName, 'http://webui')
        props.put(MesosConfigLoader.PRINCIPLE.propertyName, 'principle')
        props.put(MesosConfigLoader.SECRET.propertyName, 'secret')
        def env = [:]

        def holder = new PropertiesHolder(env, props)

        when:
        def builder = MesosConfigLoader.load(holder).apply(AppConfig.builder())
        def mesos = builder.mesos

        then:
        mesos.with {
            pathToMesosLib == '/var/path'
            mesosMasterUrl == 'master:5050'
            role == '*'
            slaveUser == 'jenkins'
            frameworkName == 'jenkins-1234'
            checkpoint == false
            webUiUrl == 'http://webui'
            principal == 'principle'
            secret == 'secret'
        }
    }

    void 'default value is used if property not set'() {
        given:
        def props = new Properties()
        def env = [
                (MesosConfigLoader.LIB_PATH.envName)  : '/var/path',
                (MesosConfigLoader.MASTER_URL.envName): 'master:5050',
                (MesosConfigLoader.ROLE.envName)      : '*',
                (MesosConfigLoader.SLAVE_USER.envName): 'jenkins',
        ]
        def holder = new PropertiesHolder(env, props)

        when:
        def builder = MesosConfigLoader.load(holder).apply(AppConfig.builder())
        def mesos = builder.mesos

        then:
        mesos.with {
            frameworkName =~ /menkins-.+/
            webUiUrl =~ /menkins-.+/
            checkpoint == true
            principal == 'jenkins'
            secret == ''
        }
    }

    void 'fail if mandatory property not defined'() {
        given:
        def props = new Properties()
        def env = [
                (MesosConfigLoader.LIB_PATH.envName)  : '/var/path',
                (MesosConfigLoader.MASTER_URL.envName): 'master:5050',
                (MesosConfigLoader.ROLE.envName)      : '*',
                (MesosConfigLoader.SLAVE_USER.envName): 'jenkins',
        ]
        env.remove(toRemove)
        def holder = new PropertiesHolder(env, props)

        when:
        def builder = MesosConfigLoader.load(holder).apply(AppConfig.builder())
        def mesos = builder.mesos

        then:
        thrown(PropertiesHolder.PropertyNotFoundException)

        where:
        toRemove                             | _
        MesosConfigLoader.LIB_PATH.envName   | _
        MesosConfigLoader.MASTER_URL.envName | _
        MesosConfigLoader.ROLE.envName       | _
        MesosConfigLoader.SLAVE_USER.envName | _
    }
}
