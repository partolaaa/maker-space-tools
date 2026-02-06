import {
    addHours,
    addMonths,
    formatDate,
    isAfterMonth,
    isBeforeMonth,
    isSameDay,
    startOfDay,
    startOfMonth
} from "../utils/dates.js";

export function createCalendar({ config, onDateSelected, getIsAutoMode }) {
    const calendarGrid = document.getElementById("calendarGrid");
    const monthLabel = document.getElementById("monthLabel");
    const selectedDateLabel = document.getElementById("selectedDateLabel");
    const prevMonthButton = document.getElementById("prevMonth");
    const nextMonthButton = document.getElementById("nextMonth");

    const today = startOfDay(new Date());
    const maxDate = addHours(new Date(), config.maxBookingHours);

    let visibleMonth = startOfMonth(today);
    let selectedDate = null;
    let bookedDates = new Set();

    if (prevMonthButton) {
        prevMonthButton.addEventListener("click", () => {
            visibleMonth = addMonths(visibleMonth, -1);
            render();
        });
    }

    if (nextMonthButton) {
        nextMonthButton.addEventListener("click", () => {
            visibleMonth = addMonths(visibleMonth, 1);
            render();
        });
    }

    function render() {
        calendarGrid.innerHTML = "";
        monthLabel.textContent = visibleMonth.toLocaleString("en-US", { month: "long", year: "numeric" });

        const firstDayOfMonth = new Date(visibleMonth);
        const startOffset = (firstDayOfMonth.getDay() + 6) % 7;
        const daysInMonth = new Date(visibleMonth.getFullYear(), visibleMonth.getMonth() + 1, 0).getDate();

        for (let index = 0; index < startOffset; index++) {
            const placeholder = document.createElement("div");
            placeholder.className = "calendar-day is-disabled";
            calendarGrid.appendChild(placeholder);
        }

        for (let day = 1; day <= daysInMonth; day++) {
            const date = new Date(visibleMonth.getFullYear(), visibleMonth.getMonth(), day);
            const button = document.createElement("button");
            button.type = "button";
            button.className = "calendar-day";
            button.textContent = day;

            if (selectedDate && isSameDay(date, selectedDate)) {
                button.classList.add("is-selected");
            }

            const dateKey = formatDate(date);
            if (bookedDates.has(dateKey)) {
                button.classList.add("has-booking");
                button.title = "You have a booking on this day.";
            }

            if (!isSelectableDate(date)) {
                button.classList.add("is-disabled");
                button.disabled = true;
            } else {
                button.addEventListener("click", () => selectDate(date));
            }

            calendarGrid.appendChild(button);
        }

        if (prevMonthButton) {
            prevMonthButton.disabled = isBeforeMonth(addMonths(visibleMonth, -1), today);
        }
        if (nextMonthButton) {
            if (getIsAutoMode && getIsAutoMode()) {
                nextMonthButton.disabled = false;
            } else {
                nextMonthButton.disabled = isAfterMonth(addMonths(visibleMonth, 1), maxDate);
            }
        }
    }

    function selectDate(date) {
        selectedDate = date;
        if (selectedDateLabel) {
            selectedDateLabel.textContent = formatDate(date);
        }
        render();
        if (onDateSelected) {
            onDateSelected(date);
        }
    }

    function isSelectableDate(date) {
        const day = startOfDay(date);
        if (getIsAutoMode && getIsAutoMode()) {
            return day >= today;
        }
        return day >= today && day <= startOfDay(maxDate);
    }

    function getSelectedDate() {
        return selectedDate;
    }

    function setBookedDates(dates) {
        bookedDates = new Set(dates || []);
        render();
    }

    return {
        render,
        getSelectedDate,
        setBookedDates
    };
}
