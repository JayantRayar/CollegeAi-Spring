
import { useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";

function OAuthSuccess() {

    const navigate = useNavigate();

    // Prevent the OAuth exchange from running twice
    // during React development Strict Mode.
    const exchangeStarted = useRef(false);

    useEffect(() => {

        // Stop if this effect has already started the exchange.
        if (exchangeStarted.current) {
            return;
        }

        exchangeStarted.current = true;

        const exchangeCodeForToken = async () => {

            // Read the temporary OAuth code from the URL.
            const params =
                new URLSearchParams(window.location.search);

            const code = params.get("code");

            // No OAuth code means the login flow failed.
            if (!code) {
                console.error(
                    "OAuth login failed: authorization code not found."
                );

                navigate("/");
                return;
            }

            try {

                // Exchange the temporary OAuth code
                // for our application's JWT.
                const response =
                    await fetch(
                        "http://localhost:8080/api/auth/oauth/exchange",
                        {
                            method: "POST",

                            headers: {
                                "Content-Type": "application/json"
                            },

                            body: JSON.stringify({
                                code: code
                            })
                        }
                    );

                // The authorization code was invalid or expired.
                if (!response.ok) {
                    throw new Error(
                        "OAuth authorization code exchange failed."
                    );
                }

                // Read JWT and user information.
                const data =
                    await response.json();

                // Store the JWT for authenticated API requests.
                localStorage.setItem(
                    "bmsce_token",
                    data.token
                );

                // Remove the old token key if it exists.
                localStorage.removeItem("token");

                // Remove the temporary OAuth code
                // from the browser URL.
                window.history.replaceState(
                    {},
                    document.title,
                    "/oauth-success"
                );

                // OAuth login is complete.
                navigate("/");

            } catch (error) {

                console.error(
                    "OAuth login failed:",
                    error
                );

                navigate("/");
            }
        };

        exchangeCodeForToken();

    }, [navigate]);

    return (
        <div>
            <h2>Completing Google login...</h2>
        </div>
    );
}

export default OAuthSuccess;

