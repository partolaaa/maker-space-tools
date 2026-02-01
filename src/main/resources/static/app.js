(() => {
    const bookingConfig = {
        maxBookingHours: 360,
        maxBookingDurationMinutes: 240,
        workingStartMinutes: 9 * 60,
        workingEndMinutes: 17 * 60,
        autoSlotMinutes: 30
    };

    const toastContainer = document.getElementById("toastContainer");

    function showToast(message, type) {
        if (!toastContainer) {
            return;
        }
        const toast = document.createElement("div");
        toast.className = "toast";
        if (type === "success") {
            toast.classList.add("is-success");
        }
        if (type === "error") {
            toast.classList.add("is-error");
        }
        toast.textContent = message;
        toastContainer.appendChild(toast);
        setTimeout(() => {
            toast.remove();
        }, 3500);
    }

    Promise.all([
        import("./api.js"),
        import("./ui/calendar.js"),
        import("./ui/slots.js"),
        import("./ui/auth.js"),
        import("./ui/autoBooking.js")
    ])
        .then(([apiModule, calendarModule, slotsModule, authModule, autoModule]) => {
            const api = apiModule.createApi();
            let autoBooking = null;

            const slots = slotsModule.createSlots({
                api,
                config: bookingConfig,
                showToast,
                onSelectionChange: (selection) => {
                    if (autoBooking) {
                        autoBooking.updateSelectionSummary(selection);
                    }
                }
            });

            const auth = authModule.createAuth({
                api,
                showToast,
                onAuthenticated: () => slots.loadPendingBookings(),
                onUnauthenticated: () => slots.renderPendingBookings(null)
            });

            api.setUnauthorizedHandler(auth.handleUnauthorized);

            autoBooking = autoModule.createAutoBooking({
                api,
                showToast,
                setStatus: slots.setStatus,
                getSelection: slots.getSelection,
                buildAutoBookingSelection: slots.buildAutoBookingSelection,
                validateAutoBookingSelection: slots.validateAutoBookingSelection
            });

            const calendar = calendarModule.createCalendar({
                config: bookingConfig,
                onDateSelected: (date) => slots.handleDateSelected(date),
                getIsAutoMode: () => autoBooking.isAutoModeEnabled()
            });

            slots.init();
            auth.init();
            autoBooking.init({
                onModeChange: (isAutoMode) => {
                    slots.setAutoMode(isAutoMode);
                    calendar.render();
                    slots.updateBookButton();
                    if (slots.getSelectedDate()) {
                        slots.loadAvailability(slots.getSelectedDate());
                    } else {
                        slots.renderSlots();
                    }
                }
            });
            slots.setAutoMode(autoBooking.isAutoModeEnabled());

            const bookButton = document.getElementById("bookButton");
            if (bookButton) {
                bookButton.addEventListener("click", async () => {
                    if (autoBooking.isAutoModeEnabled()) {
                        await autoBooking.submitAutoBookingForm();
                        return;
                    }
                    await slots.submitBooking();
                });
            }

            const navBooking = document.getElementById("navBooking");
            const navAuto = document.getElementById("navAuto");
            const bookingPage = document.getElementById("bookingPage");
            const autoPage = document.getElementById("autoPage");

            function setActivePage(page) {
                if (page === "auto") {
                    if (bookingPage) {
                        bookingPage.classList.add("is-hidden");
                    }
                    if (autoPage) {
                        autoPage.classList.remove("is-hidden");
                    }
                    if (navBooking) {
                        navBooking.classList.remove("is-active");
                    }
                    if (navAuto) {
                        navAuto.classList.add("is-active");
                    }
                    autoBooking.enterAutoPage();
                } else {
                    if (autoPage) {
                        autoPage.classList.add("is-hidden");
                    }
                    if (bookingPage) {
                        bookingPage.classList.remove("is-hidden");
                    }
                    if (navAuto) {
                        navAuto.classList.remove("is-active");
                    }
                    if (navBooking) {
                        navBooking.classList.add("is-active");
                    }
                    autoBooking.leaveAutoPage();
                }
            }

            if (navBooking) {
                navBooking.addEventListener("click", () => setActivePage("booking"));
            }
            if (navAuto) {
                navAuto.addEventListener("click", () => setActivePage("auto"));
            }

            calendar.render();
            auth.refreshAuthStatus();
            setActivePage("booking");
            autoBooking.updateModeUI();
        })
        .catch((error) => {
            console.error(error);
        });
})();
