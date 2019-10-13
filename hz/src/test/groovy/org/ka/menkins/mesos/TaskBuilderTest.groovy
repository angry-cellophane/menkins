package org.ka.menkins.mesos


import spock.lang.Specification

class TaskBuilderTest extends Specification implements MesosHelpers {

    void 'no tasks if no accepted requests'() {
        given:
        def builder = TaskBuilder.newTaskBuilder(MESOS_CONFIG)
        def matcher = OfferRequestMatcher.from(offer())

        when:
        def tasks = builder.apply(matcher)

        then:
        tasks.size() == 0
    }

    void 'one offer with one request => one task'() {
        given:
        def builder = TaskBuilder.newTaskBuilder(MESOS_CONFIG)
        def matcher = matcher {
            cpus = 2.0
            mem = 512
            role = '*'
        }
        def request = request {
            docker {
                withResources(mesosResource(1.0, 128, '*'))
            }
        }

        when:
        matcher.accept(request)
        def tasks = builder.apply(matcher)

        then:
        tasks.size() == 1
    }
}
