package org.ka.menkins.storage

import org.ka.menkins.mesos.MesosHelpers
import spock.lang.Specification

class MesosResourcesTest extends Specification implements MesosHelpers {

    void 'obj1 > obj2'() {
        expect:
        obj1.greaterThan(obj2)

        where:
        obj1                    | obj2
        mesosResource(0.2, 100) | mesosResource(0.1, 50)
        mesosResource(0.2, 100) | mesosResource(0.1, 100)
        mesosResource(0.2, 100) | mesosResource(0.2, 50)
        mesosResource(0.2, 100) | mesosResource(0.2, 100)
    }

    void 'obj1 < obj2'() {
        expect:
        obj1.greaterThan(obj2) == false

        where:
        obj1                   | obj2
        mesosResource(0.1, 50) | mesosResource(0.1, 100)
        mesosResource(0.2, 50) | mesosResource(0.1, 100)
        mesosResource(0.1, 50) | mesosResource(0.2, 100)
    }

    void 'check substract'() {
        when:
        def obj3 = obj1.subtract(obj2)

        then:
        obj3.cpus == cpus as double
        obj3.mem == mem as double
        obj3.role == obj1.role

        where:
        obj1                    | obj2                    || cpus | mem
        mesosResource(0.1, 200) | mesosResource(0.1, 100) || 0.0  | 100
        mesosResource(0.2, 100) | mesosResource(0.1, 100) || 0.1  | 0
        mesosResource(0.5, 500) | mesosResource(0.2, 100) || 0.3  | 400
    }

    void 'error when substract < 0'() {
        when:
        obj1.subtract(obj2)

        then:
        thrown(MesosResources.MesosResourceOperationException)

        where:
        obj1                    | obj2
        mesosResource(0.1, 100) | mesosResource(0.5, 50)
        mesosResource(0.1, 100) | mesosResource(0.1, 200)
        mesosResource(0.1, 100) | mesosResource(0.2, 200)
    }

}
