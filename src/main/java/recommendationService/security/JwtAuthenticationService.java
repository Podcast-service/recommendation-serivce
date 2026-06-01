package recommendationService.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class JwtAuthenticationService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final long CLOCK_SKEW_SECONDS = 30;

    private final RecommendationSecurityProperties properties;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationService(RecommendationSecurityProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public AuthenticatedUser parseAndValidate(String token) {
        RecommendationSecurityProperties.Jwt jwt = properties.jwt();
        if (!properties.enabled()) {
            throw new UnauthorizedException("Authentication is disabled");
        }
        if (jwt == null || jwt.secret() == null || jwt.secret().isBlank()) {
            throw new UnauthorizedException("Authentication is not configured");
        }

        String[] parts = token == null ? new String[0] : token.split("\\.", -1);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            throw new UnauthorizedException("Access token is invalid");
        }

        JsonNode header = decodeJson(parts[0], "Access token header is invalid");
        JsonNode payload = decodeJson(parts[1], "Access token payload is invalid");
        if (!"HS256".equals(textClaim(header, "alg"))) {
            throw new UnauthorizedException("Access token signing algorithm is not supported");
        }

        verifySignature(parts[0] + "." + parts[1], parts[2], jwt.secret());
        validateRegisteredClaims(payload, jwt.issuer());
        return new AuthenticatedUser(
                uuidClaim(payload, "user_id"),
                textClaim(payload, "email"),
                rolesClaim(payload)
        );
    }

    private JsonNode decodeJson(String encoded, String errorMessage) {
        try {
            return objectMapper.readTree(Base64.getUrlDecoder().decode(encoded));
        } catch (Exception exception) {
            throw new UnauthorizedException(errorMessage);
        }
    }

    private void verifySignature(String signingInput, String encodedSignature, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] expected = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
            byte[] actual = Base64.getUrlDecoder().decode(encodedSignature);
            if (!MessageDigest.isEqual(expected, actual)) {
                throw new UnauthorizedException("Access token signature is invalid");
            }
        } catch (UnauthorizedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UnauthorizedException("Access token signature is invalid");
        }
    }

    private void validateRegisteredClaims(JsonNode payload, String expectedIssuer) {
        if (expectedIssuer != null && !expectedIssuer.isBlank() && !expectedIssuer.equals(textClaim(payload, "iss"))) {
            throw new UnauthorizedException("Access token issuer is invalid");
        }

        JsonNode expiresAt = payload.get("exp");
        if (expiresAt == null || !expiresAt.canConvertToLong()) {
            throw new UnauthorizedException("Access token expiration is missing");
        }
        Instant now = Instant.now();
        if (Instant.ofEpochSecond(expiresAt.asLong()).plusSeconds(CLOCK_SKEW_SECONDS).isBefore(now)) {
            throw new UnauthorizedException("Access token is missing or expired");
        }

        JsonNode notBefore = payload.get("nbf");
        if (notBefore != null) {
            if (!notBefore.canConvertToLong()) {
                throw new UnauthorizedException("Access token not-before claim is invalid");
            }
            if (Instant.ofEpochSecond(notBefore.asLong()).minusSeconds(CLOCK_SKEW_SECONDS).isAfter(now)) {
                throw new UnauthorizedException("Access token is not active yet");
            }
        }
    }

    private UUID uuidClaim(JsonNode payload, String claimName) {
        try {
            return UUID.fromString(textClaim(payload, claimName));
        } catch (IllegalArgumentException exception) {
            throw new UnauthorizedException("Access token " + claimName + " claim is invalid");
        }
    }

    private String textClaim(JsonNode node, String claimName) {
        JsonNode value = node.get(claimName);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new UnauthorizedException("Access token " + claimName + " claim is invalid");
        }
        return value.asText();
    }

    private List<String> rolesClaim(JsonNode payload) {
        JsonNode rolesNode = payload.get("roles");
        if (rolesNode == null || !rolesNode.isArray()) {
            return List.of();
        }
        List<String> roles = new ArrayList<>();
        for (JsonNode role : rolesNode) {
            if (!role.isTextual() || role.asText().isBlank()) {
                throw new UnauthorizedException("Access token roles claim is invalid");
            }
            roles.add(role.asText());
        }
        return List.copyOf(roles);
    }
}
