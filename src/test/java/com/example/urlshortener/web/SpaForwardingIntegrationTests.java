package com.example.urlshortener.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpaForwardingIntegrationTests {
    @Autowired MockMvc mockMvc;

    @Test
    void rootAndAngularRoutesServeSpaIndex() throws Exception {
        assertSpa("/");
        assertSpa("/login");
        assertSpa("/register");
        assertSpa("/app/urls");
        assertSpa("/app/audit");
        assertSpa("/app/api-keys");
        assertSpa("/app/admin/outbox");
    }

    @Test
    void apiActuatorRedirectSwaggerAndOpenApiRoutesAreNotForwardedToSpa() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(not(containsString("<app-root>"))));

        mockMvc.perform(get("/r/test"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("<app-root>"))));

        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<app-root>"))));

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<app-root>"))));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<app-root>"))));
    }

    private void assertSpa(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }
}
