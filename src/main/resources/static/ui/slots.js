import {
    addHours,
    formatBookingDateTime,
    formatDate,
    formatDuration,
    minutesToTime,
    resolveIntervalMinutes,
    startOfDay,
    timeToMinutes
} from "../utils/dates.js";
import { isUnauthorizedError } from "../api.js";

export function createSlots({ api, config, showToast, onSelectionChange }) {
    const slotsGrid = document.getElementById("slotsGrid");
    const resourceName = document.getElementById("resourceName");
    const statusMessage = document.getElementById("statusMessage");
    const startTimeLabel = document.getElementById("startTimeLabel");
    const endTimeLabel = document.getElementById("endTimeLabel");
    const durationLabel = document.getElementById("durationLabel");
    const loadingOverlay = document.getElementById("loadingOverlay");
    const loadingText = document.getElementById("loadingText");
    const bookButton = document.getElementById("bookButton");
    const pendingBookingsList = document.getElementById("pendingBookingsList");
    const refreshBookings = document.getElementById("refreshBookings");

    const maxDate = addHours(new Date(), config.maxBookingHours);

    let selectedDate = null;
    let selectedStartTime = null;
    let selectedEndTime = null;
    let currentSlots = [];
    let intervalMinutes = 30;
    let slotMap = new Map();
    let isAutoMode = false;

    function init() {
        if (refreshBookings) {
            refreshBookings.addEventListener("click", () => loadPendingBookings());
        }
    }

    function setAutoMode(value) {
        isAutoMode = value;
        if (bookButton) {
            bookButton.textContent = isAutoMode ? "Create auto booking" : "Book Range";
        }
    }

    function getSelectedDate() {
        return selectedDate;
    }

    function handleDateSelected(date) {
        selectedDate = date;
        resetSelection();
        setStatus("Loading availability...", true);
        loadAvailability(date);
    }

    function resetSelection() {
        selectedStartTime = null;
        selectedEndTime = null;
        updateSelectionLabels();
        if (bookButton) {
            bookButton.disabled = true;
        }
    }

    async function loadAvailability(date) {
        slotsGrid.innerHTML = "";
        resetSelection();
        setLoading(true, "Loading availability...");
        if (isAutoMode && isBeyondBookingWindow(date)) {
            currentSlots = buildDefaultSlots();
            slotMap = new Map(currentSlots.map((slot) => [slot.time, slot]));
            intervalMinutes = config.autoSlotMinutes;
            renderSlots();
            setStatus("Availability not available yet. Select any time range.", true);
            setLoading(false, "");
            return;
        }
        try {
            const payload = await api.getAvailability(formatDate(date));
            resourceName.textContent = payload.resourceName || "Machine";
            const rawSlots = Array.isArray(payload.slots) ? payload.slots : [];
            currentSlots = normalizeSlots(rawSlots);
            currentSlots.sort((a, b) => a.time.localeCompare(b.time));
            slotMap = new Map(currentSlots.map((slot) => [slot.time, slot]));
            const intervalSlots = currentSlots.filter((slot) => !slot.boundary);
            intervalMinutes = resolveIntervalMinutes(intervalSlots);
            renderSlots();
            const message = isAutoMode
                ? "Auto mode: select a date and time range (busy allowed)."
                : "Select start and end time between 09:00 and 17:00 (max 4h).";
            setStatus(message, true);
        } catch (error) {
            currentSlots = [];
            slotMap = new Map();
            slotsGrid.innerHTML = "";
            setStatus("Unable to load availability.", false);
        } finally {
            setLoading(false, "");
        }
    }

    function renderSlots() {
        slotsGrid.innerHTML = "";
        if (currentSlots.length === 0) {
            const empty = document.createElement("div");
            empty.className = "status-message";
            empty.textContent = "No slots available for this date.";
            slotsGrid.appendChild(empty);
            return;
        }

        const startMinutes = selectedStartTime ? timeToMinutes(selectedStartTime) : null;
        const endMinutes = selectedEndTime ? timeToMinutes(selectedEndTime) : null;
        const hasRange = startMinutes !== null && endMinutes !== null && endMinutes > startMinutes;

        currentSlots.forEach((slot) => {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "slot";
            button.textContent = slot.time;
            const booked = isSlotBooked(slot);
            const isStart = slot.time === selectedStartTime;
            const isEnd = slot.time === selectedEndTime;
            button.classList.add(booked ? "slot--busy" : "slot--free");

            const slotMinutes = timeToMinutes(slot.time);
            if (hasRange && !slot.boundary && !booked && !isStart && !isEnd && slotMinutes > startMinutes && slotMinutes < endMinutes) {
                button.classList.add("slot--range");
            }
            if (isStart) {
                button.classList.add("slot--selected", "slot--start");
            }
            if (isEnd) {
                button.classList.add("slot--selected", "slot--end");
            }
            if (slot.boundary && !isStart && !isEnd && !booked) {
                button.classList.add("slot--boundary");
            }

            button.addEventListener("click", () => selectSlot(slot.time));
            slotsGrid.appendChild(button);
        });
    }

    function selectSlot(time) {
        const slot = slotMap.get(time);
        if (!selectedStartTime || selectedEndTime) {
            if (!isStartTimeAllowed(time, slot)) {
                setStatus(isAutoMode ? "Select a start time between 09:00 and 17:00." : "Select a free start time between 09:00 and 17:00.", false);
                return;
            }
            selectedStartTime = time;
            selectedEndTime = null;
            updateSelectionLabels();
            renderSlots();
            setStatus("Select an end time within 4 hours, up to 17:00.", true);
            updateBookButton();
            return;
        }

        const startMinutes = timeToMinutes(selectedStartTime);
        const endMinutes = timeToMinutes(time);
        if (endMinutes <= startMinutes) {
            if (!isStartTimeAllowed(time, slot)) {
                setStatus(isAutoMode ? "Select a start time between 09:00 and 17:00." : "Select a free start time between 09:00 and 17:00.", false);
                return;
            }
            selectedStartTime = time;
            selectedEndTime = null;
            updateSelectionLabels();
            renderSlots();
            setStatus("Select an end time within 4 hours, up to 17:00.", true);
            updateBookButton();
            return;
        }

        selectedEndTime = time;
        updateSelectionLabels();
        renderSlots();
        updateBookButton();
    }

    function updateSelectionLabels() {
        startTimeLabel.textContent = selectedStartTime || "--";
        endTimeLabel.textContent = selectedEndTime || "--";
        const durationMinutes = calculateDurationMinutes();
        durationLabel.textContent = durationMinutes > 0 ? formatDuration(durationMinutes) : "--";
        if (onSelectionChange) {
            onSelectionChange(getSelection());
        }
    }

    function updateBookButton() {
        if (bookButton) {
            bookButton.disabled = true;
        }
        const validation = isAutoMode
            ? validateAutoBookingSelection(buildAutoBookingSelection())
            : validateSelection();
        if (!validation.valid) {
            if (selectedStartTime && selectedEndTime) {
                setStatus(validation.message, false);
            }
            return;
        }
        if (bookButton) {
            bookButton.disabled = false;
        }
        const label = isAutoMode ? "Ready to schedule" : "Ready to book";
        setStatus(`${label} ${formatDuration(validation.durationMinutes)}.`, true);
    }

    function validateSelection() {
        if (!selectedStartTime || !selectedEndTime) {
            return { valid: false, message: "Select a start and end time." };
        }
        const startMinutes = timeToMinutes(selectedStartTime);
        const endMinutes = timeToMinutes(selectedEndTime);
        if (startMinutes < config.workingStartMinutes || startMinutes >= config.workingEndMinutes) {
            return { valid: false, message: "Start time must be between 09:00 and 17:00." };
        }
        if (endMinutes > config.workingEndMinutes) {
            return { valid: false, message: "End time must be no later than 17:00." };
        }
        const durationMinutes = calculateDurationMinutes();
        if (durationMinutes <= 0) {
            return { valid: false, message: "End time must be after start time." };
        }
        if (durationMinutes > config.maxBookingDurationMinutes) {
            return { valid: false, message: "Maximum booking duration is 4 hours." };
        }
        if (durationMinutes % intervalMinutes !== 0) {
            return { valid: false, message: "Selection must align with slot intervals." };
        }
        if (!isAutoMode && !isRangeAvailable(selectedStartTime, selectedEndTime)) {
            return { valid: false, message: "Selected range includes busy slots." };
        }
        return { valid: true, durationMinutes };
    }

    function calculateDurationMinutes() {
        if (!selectedStartTime || !selectedEndTime) {
            return 0;
        }
        return timeToMinutes(selectedEndTime) - timeToMinutes(selectedStartTime);
    }

    function normalizeSlots(slots) {
        let filtered = slots.filter((slot) => {
            const minutes = timeToMinutes(slot.time);
            return minutes >= config.workingStartMinutes && minutes < config.workingEndMinutes;
        });
        if (filtered.length === 0) {
            return [];
        }
        const boundaryTime = minutesToTime(config.workingEndMinutes);
        let hasBoundary = false;
        filtered = filtered.map((slot) => {
            if (slot.time === boundaryTime) {
                hasBoundary = true;
                return { ...slot, available: true, booked: false, boundary: true };
            }
            return slot;
        });
        if (!hasBoundary) {
            filtered.push({ time: boundaryTime, available: true, booked: false, boundary: true });
        }
        return filtered;
    }

    function isRangeAvailable(startTime, endTime) {
        if (isAutoMode) {
            return true;
        }
        const startMinutes = timeToMinutes(startTime);
        const endMinutes = timeToMinutes(endTime);
        for (let minutes = startMinutes; minutes < endMinutes; minutes += intervalMinutes) {
            const time = minutesToTime(minutes);
            const slot = slotMap.get(time);
            if (!slot || isSlotBlocked(slot)) {
                return false;
            }
        }
        return true;
    }

    function isSlotBooked(slot) {
        return Boolean(slot.booked) && !slot.boundary;
    }

    function isSlotBlocked(slot) {
        return isSlotBooked(slot) || slot.available === false;
    }

    function isStartTimeAllowed(time, slot) {
        const minutes = timeToMinutes(time);
        if (minutes < config.workingStartMinutes || minutes >= config.workingEndMinutes) {
            return false;
        }
        if (!slot) {
            return false;
        }
        if (isAutoMode) {
            return true;
        }
        return !isSlotBlocked(slot);
    }

    function isBeyondBookingWindow(date) {
        const day = startOfDay(date);
        return day > startOfDay(maxDate);
    }

    function buildDefaultSlots() {
        const slots = [];
        for (let minutes = config.workingStartMinutes; minutes < config.workingEndMinutes; minutes += config.autoSlotMinutes) {
            slots.push({
                time: minutesToTime(minutes),
                available: true,
                booked: false
            });
        }
        return slots;
    }

    function setLoading(isLoading, message) {
        if (!loadingOverlay) {
            return;
        }
        loadingOverlay.classList.toggle("is-hidden", !isLoading);
        if (loadingText && message) {
            loadingText.textContent = message;
        }
    }

    function setStatus(message, success) {
        statusMessage.textContent = message;
        statusMessage.classList.toggle("is-success", success);
        statusMessage.classList.toggle("is-error", !success);
    }

    function buildAutoBookingSelection() {
        return {
            startDate: selectedDate ? formatDate(selectedDate) : null,
            startTime: selectedStartTime,
            endTime: selectedEndTime
        };
    }

    function validateAutoBookingSelection(selection) {
        if (!selection.startDate || !selection.startTime || !selection.endTime) {
            return { valid: false, message: "Select a date and time range first." };
        }
        const startMinutes = timeToMinutes(selection.startTime);
        const endMinutes = timeToMinutes(selection.endTime);
        if (startMinutes >= endMinutes) {
            return { valid: false, message: "End time must be after start time." };
        }
        if (startMinutes < config.workingStartMinutes || endMinutes > config.workingEndMinutes) {
            return { valid: false, message: "Times must be within 09:00 and 17:00." };
        }
        const durationMinutes = endMinutes - startMinutes;
        if (durationMinutes > config.maxBookingDurationMinutes) {
            return { valid: false, message: "Maximum booking duration is 4 hours." };
        }
        if (durationMinutes % config.autoSlotMinutes !== 0) {
            return { valid: false, message: "Times must align to slot intervals." };
        }
        return { valid: true, durationMinutes };
    }

    function getSelection() {
        return {
            selectedDate,
            selectedStartTime,
            selectedEndTime,
            durationMinutes: calculateDurationMinutes()
        };
    }

    async function submitBooking() {
        if (!selectedDate) {
            return;
        }
        const validation = validateSelection();
        if (!validation.valid) {
            setStatus(validation.message, false);
            updateBookButton();
            return;
        }
        setStatus("Booking...", false);
        if (bookButton) {
            bookButton.disabled = true;
        }
        try {
            const result = await api.createBooking({
                date: formatDate(selectedDate),
                startTime: selectedStartTime,
                durationMinutes: validation.durationMinutes
            });
            if (result && result.unauthorized) {
                return;
            }
            const payload = result ? result.data : null;
            if (payload && payload.success) {
                setStatus(payload.message || "Booking confirmed.", true);
                await loadAvailability(selectedDate);
                loadPendingBookings();
            } else {
                const details = payload && Array.isArray(payload.errors) && payload.errors.length > 0
                    ? payload.errors.join(" ")
                    : "";
                const message = payload && payload.message ? payload.message : "Booking failed.";
                setStatus(`${message} ${details}`.trim(), false);
            }
        } catch (error) {
            setStatus("Booking failed. Please try again.", false);
        } finally {
            updateBookButton();
        }
    }

    async function loadPendingBookings() {
        if (!pendingBookingsList) {
            return;
        }
        try {
            const response = await api.getPendingBookings();
            renderPendingBookings(response && response.bookings ? response.bookings : []);
        } catch (error) {
            if (isUnauthorizedError(error)) {
                renderPendingBookings(null);
                return;
            }
            renderPendingBookings([]);
        }
    }

    function renderPendingBookings(bookings) {
        if (!pendingBookingsList) {
            return;
        }
        pendingBookingsList.innerHTML = "";
        if (bookings === null) {
            const empty = document.createElement("div");
            empty.className = "status-message";
            empty.textContent = "Sign in to view bookings.";
            pendingBookingsList.appendChild(empty);
            return;
        }
        if (!bookings || bookings.length === 0) {
            const empty = document.createElement("div");
            empty.className = "status-message";
            empty.textContent = "No pending bookings.";
            pendingBookingsList.appendChild(empty);
            return;
        }
        bookings.forEach((booking) => {
            const card = document.createElement("div");
            card.className = "booking-item";

            const header = document.createElement("div");
            header.className = "booking-header";

            const title = document.createElement("div");
            title.className = "booking-title";
            title.textContent = `Booking #${booking.bookingNumber || booking.id}`;

            const cancelButton = document.createElement("button");
            cancelButton.type = "button";
            cancelButton.className = "booking-cancel";
            cancelButton.setAttribute("aria-label", "Cancel booking");
            cancelButton.innerHTML = "&times;";
            cancelButton.addEventListener("click", () => confirmCancelBooking(booking));

            header.appendChild(title);
            header.appendChild(cancelButton);

            const range = document.createElement("div");
            range.className = "booking-meta";
            range.textContent = `${formatBookingDateTime(booking.fromTime)} - ${formatBookingDateTime(booking.toTime)}`;

            const created = document.createElement("div");
            created.className = "booking-meta";
            created.textContent = `Created: ${formatBookingDateTime(booking.createdOn)}`;

            card.appendChild(header);
            card.appendChild(range);
            card.appendChild(created);
            pendingBookingsList.appendChild(card);
        });
    }

    async function confirmCancelBooking(booking) {
        const bookingLabel = booking.bookingNumber ? `#${booking.bookingNumber}` : `#${booking.id}`;
        const confirmed = window.confirm(`Are you 100% sure you want to cancel booking ${bookingLabel}?`);
        if (!confirmed) {
            return;
        }
        try {
            await api.cancelBooking(booking.id);
            showToast(`Booking ${bookingLabel} cancelled.`, "success");
            loadPendingBookings();
        } catch (error) {
            if (isUnauthorizedError(error)) {
                return;
            }
            showToast(error.message || "Unable to cancel booking.", "error");
        }
    }

    return {
        init,
        setAutoMode,
        handleDateSelected,
        loadAvailability,
        renderSlots,
        updateBookButton,
        setStatus,
        getSelection,
        buildAutoBookingSelection,
        validateAutoBookingSelection,
        loadPendingBookings,
        renderPendingBookings,
        submitBooking,
        getSelectedDate
    };
}
