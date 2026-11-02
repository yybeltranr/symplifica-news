package com.symplifica.symplifica_news;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "jobrunr.job-scheduler.enabled=false",
        "jobrunr.background-job-server.enabled=false",
        "jobrunr.dashboard.enabled=false"
})
class SymplificaNewsApplicationTests {

    @Test
    void contextLoads() {
    }
}
