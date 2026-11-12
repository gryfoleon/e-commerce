package com.gila.api.ecommerce.configuration;

import com.gila.api.ecommerce.configuration.constants.GlobalConstants;
import com.gila.api.ecommerce.configuration.properties.CorsProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Sort;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class WebConfiguration implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    public WebConfiguration(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (corsProperties.allowedOrigins().isEmpty()) {
            return;
        }
        registry.addMapping("/**")
                .allowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders(
                        HttpHeaders.CONTENT_TYPE,
                        HttpHeaders.ACCEPT,
                        HttpHeaders.IF_MATCH,
                        HttpHeaders.IF_NONE_MATCH,
                        GlobalConstants.HEADER_IDEMPOTENCY_KEY)
                .exposedHeaders(
                        HttpHeaders.ETAG,
                        HttpHeaders.LOCATION,
                        GlobalConstants.HEADER_IDEMPOTENT_REPLAYED)
                .maxAge(corsProperties.maxAge().toSeconds());
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new SortDirectionConverter());
    }

    static final class SortDirectionConverter implements Converter<String, Sort.Direction> {

        @Override
        public Sort.Direction convert(String source) {
            return Sort.Direction.fromString(source.strip());
        }
    }
}
