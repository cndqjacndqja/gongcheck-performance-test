import groovy.json.JsonSlurper
import net.grinder.plugin.http.HTTPRequest
import net.grinder.script.GTest
import net.grinder.scriptengine.groovy.junit.GrinderRunner
import net.grinder.scriptengine.groovy.junit.annotation.BeforeProcess
import net.grinder.scriptengine.groovy.junit.annotation.BeforeThread
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.ngrinder.http.HTTPRequestControl
import org.ngrinder.http.HTTPResponse
import org.ngrinder.http.cookie.Cookie
import org.ngrinder.http.cookie.CookieManager

import static net.grinder.script.Grinder.grinder
import static org.hamcrest.Matchers.is
import static org.junit.Assert.assertThat

/**
 * 성능 부하 시나리오 테스트
 * 1. 호스트 방 입장
 * 2. 방 리스트 조회
 * 3. 작업 조회
 * 4. 진행중인 작업이 있는지 조회
 * 5. 진행중인 작업이 없다면, 진행 작업 생성 or 진행중인 작업이 있다면 체크 리스트 조회
 * 6. 진행 중인 작업 체크 */
@RunWith(GrinderRunner)
class TestRunner {

    public static GTest test1
    public static GTest test2
    public static GTest test3
    public static GTest test4
    public static GTest test5
    public static GTest test6
    public static HTTPRequest request
    public static Map<String, String> headers = [:]
    public static String body = "{\n    \"password\" : \"1234\"\n}"
    public static List<Cookie> cookies = []

    @BeforeProcess
    static void beforeProcess() {
        HTTPRequestControl.setConnectionTimeout(300000)
        test1 = new GTest(1, "POST api/hosts/1/enter")
        test2 = new GTest(2, "GET /api/spaces")
        test3 = new GTest(3, "GET /api/spaces/1/jobs")
        test4 = new GTest(4, "GET /api/jobs/1/active")
        test5 = new GTest(5, "GET /api/jobs/1/runningTasks or POST /api/jobs/1/runningTasks/new")
        test6 = new GTest(6, "POST /api/tasks/1/flip")
        request = new HTTPRequest()
        grinder.logger.info("before process.")
    }

    @BeforeThread
    void beforeThread() {
        test1.record(this, "test1")
        test2.record(this, "test2")
        test3.record(this, "test2")
        test4.record(this, "test2")
        test5.record(this, "test2")
        test6.record(this, "test2")

        grinder.statistics.delayReports = true
        grinder.logger.info("before thread.")
    }

    @Before
    void before() {
        request.setHeaders(headers)
        CookieManager.addCookies(cookies)
        grinder.logger.info("before. init headers and cookies")
    }

    private String accessToken

    @Test
    void test1() {
        request.setHeaders(headers)
        HTTPResponse response = request.POST("http://127.0.0.1:8080/api/hosts/1/enter", body.getBytes())
        def slurper = new JsonSlurper()
        def toJSON = { slurper.parseText(it) }
        def result = response.getBody(toJSON);
        accessToken = result.token
        if (response.statusCode == 301 || response.statusCode == 302) {
            grinder.logger.warn("Warning. The response may not be correct. The response code was {}.", response.statusCode)
        } else {
            assertThat(response.statusCode, is(200))
        }
        grinder.logger.info("POST(http://127.0.0.1:8080/api/hosts/1/enter) 완료")
    }

    @Test
    void test2() {
        headers.put("Authorization", "Bearer " + accessToken)
        request.setHeaders(headers)
        HTTPResponse response = request.GET("http://127.0.0.1:8080/api/spaces")

        if (response.statusCode == 301 || response.statusCode == 302) {
            grinder.logger.warn("Warning. The response may not be correct. The response code was {}.", response.statusCode)
        } else {
            assertThat(response.statusCode, is(200))
        }
        grinder.logger.info("GET(http://127.0.0.1:8080/api/spaces) 완료")
    }

    @Test
    void test3() {
        headers.put("Authorization", "Bearer " + accessToken)
        request.setHeaders(headers)
        HTTPResponse response = request.GET("http://127.0.0.1:8080/api/spaces/1/jobs")
        if (response.statusCode == 301 || response.statusCode == 302) {
            grinder.logger.warn("Warning. The response may not be correct. The response code was {}.", response.statusCode)
        } else {
            assertThat(response.statusCode, is(200))
        }
        grinder.logger.info("GET(127.0.0.1:8080/api/spaces/1/jobs) 완료")
    }
    private boolean active
    @Test
    void test4() {
        headers.put("Authorization", "Bearer " + accessToken)
        request.setHeaders(headers)
        HTTPResponse response = request.GET("http://127.0.0.1:8080/api/jobs/1/active")
        def slurper = new JsonSlurper()
        def toJSON = { slurper.parseText(it) }
        def result = response.getBody(toJSON);
        active = result.active
        if (response.statusCode == 301 || response.statusCode == 302) {
            grinder.logger.warn("Warning. The response may not be correct. The response code was {}.", response.statusCode)
        } else {
            assertThat(response.statusCode, is(200))
        }
        grinder.logger.info("GET(http://127.0.0.1:8080/api/jobs/1/active) 완료")
    }

    @Test
    void test5() {
        headers.put("Authorization", "Bearer " + accessToken)
        request.setHeaders(headers)
        HTTPResponse response
        if (active) {
            response = request.GET("http://127.0.0.1:8080/api/jobs/1/runningTasks")
            assertThat(response.statusCode, is(200))
            grinder.logger.info("GET(http://127.0.0.1:8080/api/jobs/1/runningTasks) 완료")
        } else {
            response = request.POST("http://127.0.0.1:8080/api/jobs/1/runningTasks/new")
            assertThat(response.statusCode, is(201))
            grinder.logger.info("POST(http://127.0.0.1:8080/api/jobs/1/runningTasks/new) 완료")
        }
        if (response.statusCode == 301 || response.statusCode == 302) {
            grinder.logger.warn("Warning. The response may not be correct. The response code was {}.", response.statusCode)
        }
    }

    @Test
    void test6() {
        headers.put("Authorization", "Bearer " + accessToken)
        request.setHeaders(headers)
        HTTPResponse response = request.POST("http://127.0.0.1:8080/api/tasks/1/flip")
        if (response.statusCode == 301 || response.statusCode == 302) {
            grinder.logger.warn("Warning. The response may not be correct. The response code was {}.", response.statusCode)
        } else {
            assertThat(response.statusCode, is(200))
            grinder.logger.info("POST(http://127.0.0.1:8080/api/tasks/1/flip) 완료")
        }
    }
}
