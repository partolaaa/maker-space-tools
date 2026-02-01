export function isUnauthorizedError(error) {
    return error && error.code === "UNAUTHORIZED";
}

export function createApi(options = {}) {
    let unauthorizedHandler = options.onUnauthorized;

    function setUnauthorizedHandler(handler) {
        unauthorizedHandler = handler;
    }

    function shouldNotifyUnauthorized(url) {
        return !url.includes("/api/auth/login") && !url.includes("/api/auth/status");
    }

    async function fetchJson(url, fetchOptions) {
        const response = await fetch(url, fetchOptions);
        const text = await response.text();
        if (response.status === 401 && shouldNotifyUnauthorized(url)) {
            if (unauthorizedHandler) {
                unauthorizedHandler();
            }
            const error = new Error("Unauthorized");
            error.code = "UNAUTHORIZED";
            throw error;
        }
        if (!response.ok) {
            let message = text;
            try {
                const data = JSON.parse(text);
                if (data && data.message) {
                    message = data.message;
                }
            } catch (error) {
                message = text;
            }
            const error = new Error(message || "Request failed.");
            if (response.status === 401) {
                error.code = "UNAUTHORIZED";
            }
            throw error;
        }
        if (!text) {
            return null;
        }
        return JSON.parse(text);
    }

    async function getAvailability(date) {
        return fetchJson(`/api/machines/availability?date=${encodeURIComponent(date)}`);
    }

    async function createBooking(payload) {
        const response = await fetch("/api/machines/bookings", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
        if (response.status === 401) {
            if (unauthorizedHandler) {
                unauthorizedHandler();
            }
            return { unauthorized: true };
        }
        const data = await response.json();
        return { data };
    }

    async function getJobs() {
        return fetchJson("/api/automation/jobs");
    }

    async function createJob(payload) {
        return fetchJson("/api/automation/jobs", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
    }

    async function updateJob(jobId, status) {
        return fetchJson(`/api/automation/jobs/${jobId}`, {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ status })
        });
    }

    async function deleteJob(jobId) {
        return fetchJson(`/api/automation/jobs/${jobId}`, { method: "DELETE" });
    }

    async function getAttempts() {
        return fetchJson("/api/automation/attempts?limit=100");
    }

    async function getPendingBookings() {
        return fetchJson("/api/bookings/pending");
    }

    async function cancelBooking(bookingId) {
        return fetchJson(`/api/bookings/cancel/${bookingId}`, { method: "POST" });
    }

    async function login(payload) {
        return fetchJson("/api/auth/login", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
    }

    async function authStatus() {
        return fetchJson("/api/auth/status");
    }

    async function logout() {
        return fetchJson("/api/auth/logout", {
            method: "POST",
            headers: { "Content-Type": "application/json" }
        });
    }

    return {
        setUnauthorizedHandler,
        getAvailability,
        createBooking,
        getJobs,
        createJob,
        updateJob,
        deleteJob,
        getAttempts,
        getPendingBookings,
        cancelBooking,
        login,
        authStatus,
        logout
    };
}
