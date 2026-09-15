package com.ubidict.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        properties = {
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.flyway.enabled=false",
            "app.messaging.mode=in-memory",
            "app.ai.dispatch.mode=in-process"
        })
@ActiveProfiles("test")
class BackendApplicationTests {

    @Test
    void contextLoads() {}
}
