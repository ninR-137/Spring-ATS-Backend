package com.example.demo.config;

import com.example.demo.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = sanitizeJwt(authHeader.substring(7));

            if (jwt.isBlank()) {
                clearAndUnauthorized(response, "Invalid token format");
                return;
            }

            final String userEmail = jwtService.extractUsername(jwt);

            if (userEmail == null || userEmail.isBlank()) {
                clearAndUnauthorized(response, "Invalid or expired token");
                return;
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                if (!jwtService.isTokenValid(jwt, userDetails)) {
                    clearAndUnauthorized(response, "Invalid or expired token");
                    return;
                }

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException exception) {
            logger.debug("JWT expired for path {}", request.getRequestURI());
            clearAndUnauthorized(response, "Token expired");
        } catch (SignatureException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException exception) {
            logger.debug("JWT parse/validation failure for path {}: {}", request.getRequestURI(), exception.getMessage());
            clearAndUnauthorized(response, "Invalid token");
        } catch (UsernameNotFoundException exception) {
            logger.debug("JWT subject user not found for path {}", request.getRequestURI());
            clearAndUnauthorized(response, "Token user not found");
        } catch (Exception exception) {
            logger.error("Unexpected JWT authentication failure on path {}", request.getRequestURI(), exception);
            clearAndUnauthorized(response, "Invalid or expired token");
        }
    }

    private String sanitizeJwt(String rawToken) {
        String token = rawToken == null ? "" : rawToken.trim();
        if (token.length() >= 2) {
            char first = token.charAt(0);
            char last = token.charAt(token.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                token = token.substring(1, token.length() - 1).trim();
            }
        }
        return token;
    }

    private void clearAndUnauthorized(HttpServletResponse response, String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
        response.getWriter().flush();
    }
}