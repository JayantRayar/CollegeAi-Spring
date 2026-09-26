import { BrowserRouter, Routes, Route } from "react-router-dom";
import OAuthSuccess from "./pages/OAuthSuccess";

function App() {
    return (
        <BrowserRouter>
            <Routes>

                {/* Home page */}
                <Route
                    path="/"
                    element={<h1>College AI Assistant</h1>}
                />

                {/* Google OAuth success */}
                <Route
                    path="/oauth-success"
                    element={<OAuthSuccess />}
                />

            </Routes>
        </BrowserRouter>
    );
}

export default App;