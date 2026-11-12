package com.gila.api.ecommerce.configuration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gila.api.ecommerce.configuration.properties.CorsProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class WebConfigurationCorsTest {

    @RestController
    static class PingController {
        @GetMapping("/v1/ping")
        String ping() {
            return "ok";
        }
    }

    @EnableWebMvc
    @Configuration
    static class WebMvcSupport {
    }

    private MockMvc mockMvc(List<String> origins) {
        GenericWebApplicationContext context = new GenericWebApplicationContext(new MockServletContext());
        AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
        context.registerBean(CorsProperties.class, () -> new CorsProperties(origins, Duration.ofHours(1)));
        context.registerBean(WebMvcSupport.class);
        context.registerBean(WebConfiguration.class);
        context.registerBean(PingController.class);
        context.refresh();
        return MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void preflightFromAllowedOriginIsAccepted() throws Exception {
        mockMvc(List.of("http://localhost:5173"))
                .perform(options("/v1/ping")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,if-match"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"));
    }

    @Test
    void preflightFromUnknownOriginIsRejected() throws Exception {
        mockMvc(List.of("http://localhost:5173"))
                .perform(options("/v1/ping")
                        .header(HttpHeaders.ORIGIN, "http://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void exposesETagLocationAndReplayHeaders() throws Exception {
        mockMvc(List.of("http://localhost:5173"))
                .perform(get("/v1/ping").header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "ETag, Location, Idempotent-Replayed"));
    }
}
