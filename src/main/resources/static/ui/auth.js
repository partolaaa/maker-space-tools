export function createAuth({ api, showToast, onAuthenticated, onUnauthenticated }) {
    const loginButton = document.getElementById("loginButton");
    const logoutButton = document.getElementById("logoutButton");
    const authModal = document.getElementById("authModal");
    const authBackdrop = document.getElementById("authBackdrop");
    const authClose = document.getElementById("authClose");
    const authForm = document.getElementById("authForm");
    const authEmail = document.getElementById("authEmail");
    const authPassword = document.getElementById("authPassword");
    const authTotp = document.getElementById("authTotp");
    const authClientId = document.getElementById("authClientId");
    const authStatus = document.getElementById("authStatus");
    const togglePassword = document.getElementById("togglePassword");

    let unauthorizedNotified = false;

    function init() {
        if (loginButton) {
            loginButton.addEventListener("click", () => openAuthModal());
        }

        if (logoutButton) {
            logoutButton.addEventListener("click", () => submitLogout());
        }

        if (authBackdrop) {
            authBackdrop.addEventListener("click", () => closeAuthModal());
        }

        if (authClose) {
            authClose.addEventListener("click", () => closeAuthModal());
        }

        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape") {
                closeAuthModal();
            }
        });

        if (authForm) {
            authForm.addEventListener("submit", (event) => {
                event.preventDefault();
                submitAuthForm();
            });
        }

        if (togglePassword && authPassword) {
            togglePassword.addEventListener("click", () => {
                const isHidden = authPassword.type === "password";
                authPassword.type = isHidden ? "text" : "password";
                togglePassword.textContent = isHidden ? "Hide" : "Show";
            });
        }

        if (authEmail && authClientId) {
            authEmail.addEventListener("input", () => {
                if (!authClientId.value) {
                    authClientId.value = authEmail.value ? `nexudus.portal.${authEmail.value}` : "";
                }
            });
        }
    }

    function openAuthModal() {
        if (!authModal) {
            return;
        }
        authModal.classList.remove("is-hidden");
        authModal.setAttribute("aria-hidden", "false");
        setAuthStatus("", true);
    }

    function closeAuthModal() {
        if (!authModal || authModal.classList.contains("is-hidden")) {
            return;
        }
        authModal.classList.add("is-hidden");
        authModal.setAttribute("aria-hidden", "true");
    }

    async function submitAuthForm() {
        const username = authEmail ? authEmail.value.trim() : "";
        const password = authPassword ? authPassword.value : "";
        const totp = authTotp ? authTotp.value.trim() : "";
        const clientId = authClientId ? authClientId.value.trim() : "";
        if (!username || !password) {
            setAuthStatus("Email and password are required.", false);
            return;
        }
        setAuthStatus("Authenticating...", true);
        try {
            const response = await api.login({
                username,
                password,
                totp: totp || null,
                clientId: clientId || null
            });
            setAuthStatus(response && response.message ? response.message : "Authenticated.", true);
            showToast("Authentication successful.", "success");
            refreshAuthStatus();
            closeAuthModal();
        } catch (error) {
            setAuthStatus(error.message || "Authentication failed.", false);
            showToast(error.message || "Authentication failed.", "error");
        }
    }

    async function submitLogout() {
        try {
            await api.logout();
            showToast("Logged out.", "success");
            refreshAuthStatus();
        } catch (error) {
            showToast(error.message || "Logout failed.", "error");
        }
    }

    function handleUnauthorized() {
        if (!unauthorizedNotified) {
            showToast("Session expired. Please sign in.", "error");
            unauthorizedNotified = true;
            setTimeout(() => {
                unauthorizedNotified = false;
            }, 5000);
        }
        openAuthModal();
        setAuthButtons(false);
        if (onUnauthenticated) {
            onUnauthenticated();
        }
        clearAuthSession();
    }

    async function refreshAuthStatus() {
        try {
            const status = await api.authStatus();
            const authenticated = Boolean(status && status.authenticated);
            setAuthButtons(authenticated);
            if (authenticated) {
                if (onAuthenticated) {
                    onAuthenticated();
                }
            } else if (onUnauthenticated) {
                onUnauthenticated();
            }
        } catch (error) {
            setAuthButtons(false);
            if (onUnauthenticated) {
                onUnauthenticated();
            }
        }
    }

    function setAuthButtons(isAuthenticated) {
        if (loginButton) {
            loginButton.classList.toggle("is-hidden", isAuthenticated);
        }
        if (logoutButton) {
            logoutButton.classList.toggle("is-hidden", !isAuthenticated);
        }
    }

    async function clearAuthSession() {
        try {
            await fetch("/api/auth/logout", { method: "POST" });
        } catch (error) {

        }
    }

    function setAuthStatus(message, success) {
        if (!authStatus) {
            return;
        }
        authStatus.textContent = message;
        authStatus.classList.toggle("is-success", success);
        authStatus.classList.toggle("is-error", !success);
    }

    return {
        init,
        handleUnauthorized,
        refreshAuthStatus
    };
}
