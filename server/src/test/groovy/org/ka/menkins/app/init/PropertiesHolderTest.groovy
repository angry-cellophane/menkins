package org.ka.menkins.app.init

import org.ka.menkins.app.init.PropertiesHolder.PropertyNames
import spock.lang.Shared
import spock.lang.Specification

class PropertiesHolderTest extends Specification {

    @Shared PropertyNames APP_NAME = PropertiesHolder.name("APP_NAME_12345324", "app.name.123421312312")

    void 'error when value not found'() {
        given:
        def props = new PropertiesHolder([:], new Properties())

        when:
        props.getValue(APP_NAME)
        then:
        thrown(PropertiesHolder.PropertyNotFoundException)
    }

    void 'error when value is a blank env var'() {
        given:
        def props = new PropertiesHolder([(APP_NAME.envName): ''], new Properties())

        when:
        props.getValue(APP_NAME)

        then:
        thrown(PropertiesHolder.PropertyNotFoundException)
    }

    void 'error when value is a blank system property'() {
        given:
        def systemProps = new Properties()
        systemProps.put(APP_NAME.propertyName, '')
        def props = new PropertiesHolder([:], systemProps)

        when:
        props.getValue(APP_NAME)

        then:
        thrown(PropertiesHolder.PropertyNotFoundException)
    }

    void 'default value used when no other provided'() {
        given:
        def props = new PropertiesHolder([:], new Properties())

        when:
        def value = props.getValue(APP_NAME, { '1234' })

        then:
        value == '1234'
    }

    void 'value retrieved from env var'() {
        given:
        def props = new PropertiesHolder([(APP_NAME.envName):'1234'], new Properties())

        when:
        def value = props.getValue(APP_NAME)

        then:
        value == '1234'
    }

    void 'value retrieved from system property'() {
        given:
        def systemProps = new Properties()
        systemProps.put(APP_NAME.propertyName, '1234')
        def props = new PropertiesHolder([:], systemProps)

        when:
        def value = props.getValue(APP_NAME)

        then:
        value == '1234'
    }
}
