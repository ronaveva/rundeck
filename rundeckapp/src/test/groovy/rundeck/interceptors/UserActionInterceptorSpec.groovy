package rundeck.interceptors

import grails.testing.gorm.DataTest
import grails.testing.web.interceptor.InterceptorUnitTest
import rundeck.services.UserService
import spock.lang.Specification

class UserActionInterceptorSpec extends Specification implements InterceptorUnitTest<UserActionInterceptor>, DataTest {

    def setup() {
    }

    def cleanup() {

    }

    void "Test userAction interceptor matching"() {
        given:
            def userServiceMock = Mock(UserService) {

            }
            defineBeans {
                userService(userServiceMock)
            }
        when:"A request matches the interceptor"
            interceptor.userService = userServiceMock
            withRequest(controller:"menu")

        then:"The interceptor does match"
            interceptor.doesMatch()
    }
}
