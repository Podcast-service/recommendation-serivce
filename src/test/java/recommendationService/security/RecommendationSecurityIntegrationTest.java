package recommendationService.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = {
        "app.security.enabled=true",
        "app.security.jwt.secret=unit-test-signing-key",
        "app.security.jwt.issuer=auth-service"
})
@ActiveProfiles("test")
class RecommendationSecurityIntegrationTest {

    private static final String SECRET = "unit-test-signing-key";
    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID OTHER_USER_ID = UUID.fromString("650e8400-e29b-41d4-a716-446655440000");

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void recommendationEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/recommendation/v1/podcasts").param("userId", USER_ID.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        mockMvc.perform(get("/recommendation/v1/podcasts")
                        .param("userId", USER_ID.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenCanReadOwnRecommendations() throws Exception {
        mockMvc.perform(get("/recommendation/v1/podcasts")
                        .param("userId", USER_ID.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(USER_ID, "USER")))
                .andExpect(status().isOk());
    }

    @Test
    void validTokenCannotReadAnotherUsersRecommendations() throws Exception {
        mockMvc.perform(get("/recommendation/v1/podcasts")
                        .param("userId", OTHER_USER_ID.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(USER_ID, "USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadAnotherUsersRecommendations() throws Exception {
        mockMvc.perform(get("/recommendation/v1/podcasts")
                        .param("userId", OTHER_USER_ID.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(USER_ID, "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void healthEndpointDoesNotRequireToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    private String bearer(UUID userId, String role) throws Exception {
        String header = encode(""" 
                {"alg":"HS256","typ":"JWT"}
                """);
        String payload = encode("""
                {"user_id":"%s","email":"user@example.com","roles":["%s"],"iss":"auth-service","exp":%d}
                """.formatted(userId, role, Instant.now().plusSeconds(60).getEpochSecond()));
        String signingInput = header + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "Bearer " + signingInput + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
