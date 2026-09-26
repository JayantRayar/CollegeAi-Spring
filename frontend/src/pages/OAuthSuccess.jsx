import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

function OAuthSuccess() {

    const navigate = useNavigate();

    useEffect(() => {

        // Get the JWT token from the URL.
        const params = new URLSearchParams(window.location.search);
        const token = params.get("token");

        // If no token was received, OAuth login failed.
        if (!token) {
            console.error("OAuth login failed: JWT token not found.");
            navigate("/");
            return;
        }

        // Store the JWT for authenticated API requests.
        localStorage.setItem("bmsce_token", token);
        localStorage.removeItem("token");

        // Remove the token from the browser URL.
        window.history.replaceState(
            {},
            document.title,
            "/oauth-success"
        );

        // Go to the home page after successful login.
        navigate("/");

    }, [navigate]);

    return (
        <div>
            <h2>Completing Google login...</h2>
        </div>
    );
}

export default OAuthSuccess;