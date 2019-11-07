package rundeck.interceptors

import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.UserService


class UserActionInterceptor {

    UserService userService

    UserActionInterceptor() {
        match(controller: ~/(menu|project)/).excludes(uri:'/**/**Ajax')
    }

    boolean before() { true }

    boolean after() {
        if(session && session?.user){
            userService.registerUserAction(session.user)
        }
        return true
    }

    void afterView() {
        // no-op
    }
}
