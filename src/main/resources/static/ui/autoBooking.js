import {
    formatDate,
    formatDateTime,
    formatDayOfWeek,
    formatDuration
} from "../utils/dates.js";
import { isUnauthorizedError } from "../api.js";

export function createAutoBooking({
    api,
    showToast,
    setStatus,
    getSelection,
    buildAutoBookingSelection,
    validateAutoBookingSelection
}) {
    const autoModeToggle = document.getElementById("autoModeToggle");
    const autoSummary = document.getElementById("autoSummary");
    const autoStartDateLabel = document.getElementById("autoStartDateLabel");
    const autoWeekdayLabel = document.getElementById("autoWeekdayLabel");
    const autoTimeLabel = document.getElementById("autoTimeLabel");
    const autoDurationLabel = document.getElementById("autoDurationLabel");
    const autoSelectionStatus = document.getElementById("autoSelectionStatus");
    const jobList = document.getElementById("jobList");
    const attemptFeed = document.getElementById("attemptFeed");
    const bookButton = document.getElementById("bookButton");

    let automationJobs = [];
    let attemptEntries = [];
    let attemptPoller = null;
    let isAutoMode = false;

    function init({ onModeChange }) {
        if (autoModeToggle) {
            autoModeToggle.addEventListener("change", () => {
                isAutoMode = autoModeToggle.checked;
                updateModeUI();
                if (onModeChange) {
                    onModeChange(isAutoMode);
                }
            });
        }
        isAutoMode = autoModeToggle ? autoModeToggle.checked : false;
    }

    function isAutoModeEnabled() {
        return isAutoMode;
    }

    function updateModeUI() {
        if (bookButton) {
            bookButton.textContent = isAutoMode ? "Create auto booking" : "Book Range";
        }
        if (autoSummary) {
            autoSummary.classList.toggle("is-hidden", !isAutoMode);
        }
        if (setStatus) {
            if (isAutoMode) {
                setStatus("Auto mode: select any time range (busy allowed).", true);
            } else {
                setStatus("Select a day to view slots.", true);
            }
        }
    }

    async function submitAutoBookingForm() {
        const selection = buildAutoBookingSelection ? buildAutoBookingSelection() : null;
        const validation = validateAutoBookingSelection ? validateAutoBookingSelection(selection) : { valid: false, message: "Select a date and time range first." };
        if (!validation.valid) {
            showToast(validation.message, "error");
            setAutoSelectionStatus(validation.message, false);
            return;
        }
        try {
            await api.createJob({
                startDate: selection.startDate,
                startTime: selection.startTime,
                endTime: selection.endTime,
                status: "ACTIVE"
            });
            showToast("Auto-booking created.", "success");
            setAutoSelectionStatus("Auto-booking scheduled.", true);
            refreshJobs();
        } catch (error) {
            if (isUnauthorizedError(error)) {
                return;
            }
            showToast(error.message || "Unable to create auto-booking.", "error");
            setAutoSelectionStatus(error.message || "Unable to create auto-booking.", false);
        }
    }

    async function refreshJobs() {
        try {
            automationJobs = await api.getJobs();
            renderJobs(automationJobs);
        } catch (error) {
            if (isUnauthorizedError(error)) {
                renderJobs([]);
                return;
            }
            renderJobs([]);
            showToast(error.message || "Unable to load jobs.", "error");
        }
    }

    async function refreshAttempts() {
        try {
            attemptEntries = await api.getAttempts();
            renderAttempts(attemptEntries);
        } catch (error) {
            if (isUnauthorizedError(error)) {
                renderAttempts([]);
                return;
            }
            renderAttempts([]);
        }
    }

    function renderJobs(jobs) {
        if (!jobList) {
            return;
        }
        jobList.innerHTML = "";
        if (!jobs || jobs.length === 0) {
            const empty = document.createElement("div");
            empty.className = "status-message";
            empty.textContent = "No auto-booking jobs yet.";
            jobList.appendChild(empty);
            return;
        }
        jobs.forEach((job) => {
            const card = document.createElement("div");
            card.className = "job-card";

            const row = document.createElement("div");
            row.className = "job-row";

            const meta = document.createElement("div");
            meta.className = "job-meta";
            const startDateLabel = job.startDate ? `Starts ${job.startDate}` : "Start date not set";
            meta.textContent = `${formatDayOfWeek(job.dayOfWeek)} · ${job.startTime} - ${job.endTime} · ${startDateLabel}`;

            const status = document.createElement("div");
            status.className = "job-status";
            const isActive = job.status === "ACTIVE";
            status.textContent = isActive ? "Active" : "Inactive";
            if (!isActive) {
                status.classList.add("is-disabled");
            }

            row.appendChild(meta);
            row.appendChild(status);

            const details = document.createElement("div");
            details.className = "attempt-meta";
            const lastBooked = job.lastBookedDate ? `Last booked: ${job.lastBookedDate}` : "Last booked: -";
            const lastAttempt = job.lastAttemptAt ? `Last attempt: ${formatDateTime(job.lastAttemptAt)}` : "Last attempt: -";
            details.textContent = `${lastBooked} · ${lastAttempt}`;

            const actions = document.createElement("div");
            actions.className = "job-actions";

            const toggleButton = document.createElement("button");
            toggleButton.type = "button";
            toggleButton.className = "secondary-button";
            toggleButton.textContent = isActive ? "Deactivate" : "Activate";
            toggleButton.addEventListener("click", () => toggleJob(job));

            const deleteButton = document.createElement("button");
            deleteButton.type = "button";
            deleteButton.className = "secondary-button is-danger";
            deleteButton.textContent = "Delete";
            deleteButton.addEventListener("click", () => deleteJob(job));

            actions.appendChild(toggleButton);
            actions.appendChild(deleteButton);

            card.appendChild(row);
            card.appendChild(details);
            card.appendChild(actions);
            jobList.appendChild(card);
        });
    }

    function renderAttempts(attempts) {
        if (!attemptFeed) {
            return;
        }
        attemptFeed.innerHTML = "";
        if (!attempts || attempts.length === 0) {
            const empty = document.createElement("div");
            empty.className = "status-message";
            empty.textContent = "No booking attempts yet.";
            attemptFeed.appendChild(empty);
            return;
        }
        attempts.forEach((attempt) => {
            const card = document.createElement("div");
            card.className = "attempt-card";

            const title = document.createElement("div");
            title.className = "attempt-title";
            title.textContent = `${attempt.targetDate} · ${attempt.startTime} - ${attempt.endTime}`;

            const meta = document.createElement("div");
            meta.className = "attempt-meta";
            meta.textContent = formatDateTime(attempt.occurredAt);

            const status = document.createElement("div");
            status.className = "attempt-status";
            status.textContent = attempt.success ? "Success" : "Failed";
            if (!attempt.success) {
                status.classList.add("is-failed");
            }

            const message = document.createElement("div");
            message.className = "attempt-meta";
            message.textContent = attempt.message || "";

            card.appendChild(title);
            card.appendChild(meta);
            card.appendChild(status);
            card.appendChild(message);
            attemptFeed.appendChild(card);
        });
    }

    async function toggleJob(job) {
        try {
            const isActive = job.status === "ACTIVE";
            const nextStatus = isActive ? "INACTIVE" : "ACTIVE";
            await api.updateJob(job.id, nextStatus);
            showToast(isActive ? "Auto-booking deactivated." : "Auto-booking activated.", "success");
            refreshJobs();
        } catch (error) {
            if (isUnauthorizedError(error)) {
                return;
            }
            showToast(error.message || "Unable to update job.", "error");
        }
    }

    async function deleteJob(job) {
        try {
            await api.deleteJob(job.id);
            showToast("Auto-booking deleted.", "success");
            refreshJobs();
        } catch (error) {
            if (isUnauthorizedError(error)) {
                return;
            }
            showToast(error.message || "Unable to delete job.", "error");
        }
    }

    function updateSelectionSummary(selectionInput) {
        if (!autoStartDateLabel || !autoWeekdayLabel || !autoTimeLabel || !autoDurationLabel) {
            return;
        }
        const selection = selectionInput || (getSelection ? getSelection() : {});
        const startDateValue = selection.selectedDate ? formatDate(selection.selectedDate) : "--";
        const weekdayValue = selection.selectedDate
            ? selection.selectedDate.toLocaleDateString("en-US", { weekday: "long" })
            : "--";
        const timeValue = selection.selectedStartTime && selection.selectedEndTime
            ? `${selection.selectedStartTime} - ${selection.selectedEndTime}`
            : "--";
        const durationMinutes = selection.durationMinutes || 0;
        autoStartDateLabel.textContent = startDateValue;
        autoWeekdayLabel.textContent = weekdayValue;
        autoTimeLabel.textContent = timeValue;
        autoDurationLabel.textContent = durationMinutes > 0 ? formatDuration(durationMinutes) : "--";
        if (autoSelectionStatus) {
            const hasSelection = Boolean(selection.selectedDate && selection.selectedStartTime && selection.selectedEndTime);
            autoSelectionStatus.textContent = hasSelection ? "" : "Select a date and time range in the booking form.";
            autoSelectionStatus.classList.remove("is-success", "is-error");
        }
    }

    function setAutoSelectionStatus(message, success) {
        if (!autoSelectionStatus) {
            return;
        }
        autoSelectionStatus.textContent = message;
        autoSelectionStatus.classList.toggle("is-success", success);
        autoSelectionStatus.classList.toggle("is-error", !success);
    }

    function startAttemptPolling() {
        if (attemptPoller) {
            return;
        }
        attemptPoller = setInterval(() => refreshAttempts(), 30000);
    }

    function stopAttemptPolling() {
        if (!attemptPoller) {
            return;
        }
        clearInterval(attemptPoller);
        attemptPoller = null;
    }

    function enterAutoPage() {
        refreshJobs();
        refreshAttempts();
        startAttemptPolling();
    }

    function leaveAutoPage() {
        stopAttemptPolling();
    }

    return {
        init,
        isAutoModeEnabled,
        updateModeUI,
        submitAutoBookingForm,
        updateSelectionSummary,
        enterAutoPage,
        leaveAutoPage
    };
}
