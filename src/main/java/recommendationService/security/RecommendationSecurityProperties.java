package recommendationService.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record RecommendationSecurityProperties(
        boolean enabled,
        Jwt jwt,
        boolean prometheusPublic
) {

    public record Jwt(
            String secret,
            String issuer
    ) {
    }
}
