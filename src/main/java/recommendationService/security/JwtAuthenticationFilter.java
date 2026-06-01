package recommendationService.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final RecommendationSecurityProperties properties;
    private final JwtAuthenticationService authenticationService;
    private final SecurityErrorResponseWriter responseWriter;

    public JwtAuthenticationFilter(
            RecommendationSecurityProperties properties,
            JwtAuthenticationService authenticationService,
            SecurityErrorResponseWriter responseWriter
    ) {
        this.properties = properties;
        this.authenticationService = authenticationService;
        this.responseWriter = responseWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!properties.enabled() || isPublicInfrastructurePath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token;
        try {
            token = resolveBearerToken(request.getHeader(HttpHeaders.AUTHORIZATION));
        } catch (UnauthorizedException exception) {
            responseWriter.writeUnauthorized(response, exception.getMessage());
            return;
        }
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            AuthenticatedUser user = authenticationService.parseAndValidate(token);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    user,
                    token,
                    user.roles().stream().map(this::toAuthority).toList()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (UnauthorizedException exception) {
            SecurityContextHolder.clearContext();
            responseWriter.writeUnauthorized(response, exception.getMessage());
        }
    }

    private String resolveBearerToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        String prefix = "Bearer ";
        if (!authorization.regionMatches(true, 0, prefix, 0, prefix.length())) {
            throw new UnauthorizedException("Authorization header must use Bearer scheme");
        }
        String token = authorization.substring(prefix.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private boolean isPublicInfrastructurePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/health")
                || path.startsWith("/actuator/info")
                || properties.prometheusPublic() && path.startsWith("/actuator/prometheus")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs")
                || path.equals("/error");
    }

    private SimpleGrantedAuthority toAuthority(String role) {
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return new SimpleGrantedAuthority(normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized);
    }
}
