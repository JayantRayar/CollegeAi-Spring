package com.collegeai.backend.security;

import com.collegeai.backend.entity.User;
import com.collegeai.backend.repository.UserRepository;
import com.collegeai.backend.service.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles successful Google OAuth2/OIDC authentication.
 *
 * After Google authentication succeeds:
 * 1. Gets the authenticated Google user.
 * 2. Finds the corresponding local user from PostgreSQL.
 * 3. Generates our application's JWT.
 * 4. Redirects the user back to the React frontend.
 */
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        // Get the authenticated Google/OIDC user.
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

        // Google provides the user's email.
        String email = oidcUser.getAttribute("email");

        // Find the user that OAuthUserService created/saved.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "OAuth user was not found in database"
                        )
                );

        // Generate our application's JWT.
        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole()
        );

        // Redirect to React after successful login.
        String frontendUrl =
                "http://localhost:5173/oauth-success?token=" + token;

        getRedirectStrategy().sendRedirect(
                request,
                response,
                frontendUrl
        );
    }
}