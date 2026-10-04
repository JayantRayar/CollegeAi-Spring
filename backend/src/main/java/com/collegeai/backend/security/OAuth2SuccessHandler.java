
package com.collegeai.backend.security;

import com.collegeai.backend.entity.User;
import com.collegeai.backend.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * 3. Creates a short-lived, one-time authorization code.
 * 4. Redirects the user back to the React frontend with the code.
 *
 * The JWT is NOT placed in the URL.
 */
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    private final UserRepository userRepository;

    private final OAuth2AuthorizationCodeService
            authorizationCodeService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        // Get the authenticated Google/OIDC user.
        OidcUser oidcUser =
                (OidcUser) authentication.getPrincipal();

        // Get the email provided by Google.
        String email =
                oidcUser.getAttribute("email");

        // Find the corresponding local user in PostgreSQL.
        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "OAuth user was not found in database"
                                )
                        );

        // Create a short-lived, one-time authorization code.
        String code =
                authorizationCodeService.createCode(
                        user.getEmail()
                );

        // Log the successful OAuth authentication.
        // Never log the authorization code or JWT.
        logger.info(
                "Google OAuth authentication successful for user: {}",
                user.getEmail()
        );

        // Redirect the browser back to React.
        //
        // Only the temporary authorization code is placed
        // in the URL. The JWT is NOT placed in the URL.
        String frontendUrl =
                "http://localhost:5173/oauth-success?code="
                        + code;

        getRedirectStrategy().sendRedirect(
                request,
                response,
                frontendUrl
        );
    }
}

