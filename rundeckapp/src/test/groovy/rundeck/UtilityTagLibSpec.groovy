/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rundeck

import grails.test.mixin.TestFor
import rundeck.codecs.HTMLAttributeCodec
import rundeck.codecs.URIComponentCodec
import rundeck.services.ConfigurationService
import rundeck.services.FrameworkService
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 6/21/16.
 */
@TestFor(UtilityTagLib)
class UtilityTagLibSpec extends Specification {
    @Unroll
    def "text after"() {

        when:
        def result = tagLib.textAfterLine(text: text, marker: '---').toString()

        then:
            result == expect
        where:
            text                        | expect
            'abc\n123\n456\n---\n789\n' | '789\n'
            'abc\n---\n789\n'           | '789\n'
            '---\n789\n'                | '789\n'

    }

    def "text remaining lines"() {

        expect:
            expect == tagLib.textRemainingLines(text: text).toString()
        where:
            text                        | expect
            'abc\n123\n456\n---\n789\n' | '123\n' + '456\n' + '---\n' + '789\n'
            'abc\n---\n789\n'           | '---\n' + '789\n'

    }
    @Unroll
    def "text before"() {
        expect:
            expect == tagLib.textBeforeLine(text: text, marker: '---').toString()
        where:
            text                        | expect
            'abc\n123\n456\n---\n789\n' | 'abc\n123\n456'
            '---\n789\n' | ''
    }

    @Unroll
    def "relativeDateString optional html output"() {
        given:
        messageSource.addMessages(['format.time.sec.abbrev' : '{0}s',
                                   'format.time.min.abbrev' : '{0}m',
                                   'format.time.hour.abbrev': '{0}h',
                                   'format.time.day.abbrev' : '{0}d'], request.locale
        )

        Date now = new Date()
        Date then = new Date(now.time - diff)
        def codec1 = mockCodec(HTMLAttributeCodec)

        expect:
        expect == tagLib.relativeDateString(
                [
                        start     : reverse ? now : then, end: reverse ? then : now,
                        html      : isHtml,
                        agoClass  : agoClass,
                        untilClass: untilClass
                ]
        )



        where:
        isHtml | agoClass | untilClass | reverse | diff           || expect
        false  | null     | null       | false   | 12000          || '12s'
        false  | null     | null       | true    | 12000          || '12s'
        false  | null     | null       | false   | 120000         || '2m'
        false  | null     | null       | false   | 125000         || '2m5s'
        false  | null     | null       | false   | 3600000        || '1h'
        false  | null     | null       | false   | 5400000        || '1h30m'
        false  | null     | null       | false   | (24 * 3600000) || '1d'
        false  | null     | null       | false   | (26 * 3600000) || '1d2h'
        true   | null     | null       | false   | 12000          || '<span class="ago">12s</span>'
        true   | 'Xago2'  | null       | false   | 12000          || '<span class="Xago2">12s</span>'
        true   | null     | null       | true    | 12000          || '<span class="until">12s</span>'
        true   | null     | 'xUntil3'  | true    | 12000          || '<span class="xUntil3">12s</span>'
    }

    def "url params"() {
        when:
        mockCodec(URIComponentCodec)
        def result = tagLib.genUrlParam(input)
        then:
        result == expected

        where:
        input            | expected
        [a: 'b']         | 'a=b'
        [a: 'b', c: 'd'] | 'a=b&c=d'
        [a: 'b', c: 'd e'] | 'a=b&c=d%20e'
        [a: 'b', 'c f': 'd e'] | 'a=b&c%20f=d%20e'

    }

    def "shouldShowLogin show local after first login"() {
        when:
        File firstLoginFile = File.createTempFile("first","login")
        tagLib.configurationService = Mock(ConfigurationService) {
            getBoolean("login.localLogin.enabled",true) >> { false }
            getBoolean("login.showLocalLoginAfterFirstSSOLogin",false) >> { true }
        }
        tagLib.frameworkService = Mock(FrameworkService) {
            getFirstLoginFile() >> { return firstLoginFile }
        }
        def result = tagLib.showLocalLogin(null,"loginform").toString()

        then:
        result == "loginform"
    }

    def "shouldShowLogin suppress local if no first login"() {
        when:
        File firstLoginFile = new File("/tmp/doesnotexist")
        tagLib.configurationService = Mock(ConfigurationService) {
            getBoolean("login.localLogin.enabled",true) >> { false }
            getBoolean("login.showLocalLoginAfterFirstSSOLogin",false) >> { true }
        }
        tagLib.frameworkService = Mock(FrameworkService) {
            getFirstLoginFile() >> { return firstLoginFile }
        }
        def result = tagLib.showLocalLogin(null,"loginform").toString()

        then:
        !result
    }

    def "shouldShowLogin localLogin toggle"() {
        when:
        tagLib.configurationService = Mock(ConfigurationService) {
            getBoolean("login.localLogin.enabled",true) >> { toggle }
            getBoolean("login.showLocalLoginAfterFirstSSOLogin",false) >> { false }
        }
        def result = tagLib.showLocalLogin(null,"loginform").toString()

        then:
        result == check

        where:
        toggle | check
        true   | "loginform"
        false  | ""
    }
}
