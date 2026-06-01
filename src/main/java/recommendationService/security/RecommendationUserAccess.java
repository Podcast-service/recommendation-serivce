package recommendationService.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("recommendationUserAccess")
public class RecommendationUserAccess {

    private final RecommendationSecurityProperties properties;

    public RecommendationUserAccess(RecommendationSecurityProperties properties) {
        this.properties = properties;
    }

    public boolean canAccessUser(String requestedUserId, Authentication authentication) {
        if (!properties.enabled()) {
            return true;
        }
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return false;
        }
        return user.userId().toString().equals(requestedUserId)
                || authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
