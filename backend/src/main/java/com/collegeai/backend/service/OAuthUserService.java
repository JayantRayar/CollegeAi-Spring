package com.collegeai.backend.service;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import com.collegeai.backend.entity.Role;
import com.collegeai.backend.entity.User;
import com.collegeai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * Handles users authenticated through OpenID Connect providers
 * such as Google.
 *
 * This service loads the authenticated Google user and
 * creates a corresponding local account in PostgreSQL.
 */
@Service
@RequiredArgsConstructor
public class OAuthUserService extends OidcUserService {

    private final UserRepository userRepository;

    /**
     * Called by Spring Security after successful Google authentication.
     */
    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {

        // Let Spring Security load the user's information from Google.
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getAttribute("email");
        String name = oidcUser.getAttribute("name");

        // Create a local account if this Google email
        // does not already exist.
        userRepository.findByEmail(email)
                .orElseGet(() -> {

                    User user = new User();

                    user.setName(name);
                    user.setEmail(email);

                    // OAuth users don't use a local password.
                    user.setPassword(null);

                    // New OAuth users are normal users.
                    user.setRole(Role.USER);

                    return userRepository.save(user);
                });

        return oidcUser;
    }
}