package com.seip.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test — verifies that the Spring application context loads without errors.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.jwt.secret=test-secret-key-that-is-at-least-64-characters-long-for-hs512-algo",
        "spring.cloud.gateway.routes[0].id=auth-test",
        "spring.cloud.gateway.routes[0].uri=http://localhost:9999",
        "spring.cloud.gateway.routes[0].predicates[0]=Path=/auth/**"
})
class ApiGatewayApplicationTest {

    @Test
    void contextLoads() {
        // If the context fails to start, this test will fail with a descriptive error.
    }
}
