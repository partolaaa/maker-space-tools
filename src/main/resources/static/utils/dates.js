export function formatDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
}

export function startOfDay(date) {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

export function startOfMonth(date) {
    return new Date(date.getFullYear(), date.getMonth(), 1);
}

export function addMonths(date, months) {
    return new Date(date.getFullYear(), date.getMonth() + months, 1);
}

export function addHours(date, hours) {
    return new Date(date.getTime() + hours * 60 * 60 * 1000);
}

export function isSameDay(first, second) {
    return first.getFullYear() === second.getFullYear()
        && first.getMonth() === second.getMonth()
        && first.getDate() === second.getDate();
}

export function isBeforeMonth(monthDate, boundaryDate) {
    const boundaryMonth = startOfMonth(boundaryDate);
    return monthDate < boundaryMonth;
}

export function isAfterMonth(monthDate, boundaryDate) {
    const boundaryMonth = startOfMonth(boundaryDate);
    return monthDate > boundaryMonth;
}

export function resolveIntervalMinutes(slots) {
    if (slots.length < 2) {
        return 30;
    }
    const first = timeToMinutes(slots[0].time);
    const second = timeToMinutes(slots[1].time);
    const diff = second - first;
    return diff > 0 ? diff : 30;
}

export function timeToMinutes(time) {
    const [hours, minutes] = time.split(":").map(Number);
    return hours * 60 + minutes;
}

export function minutesToTime(totalMinutes) {
    const hours = Math.floor(totalMinutes / 60) % 24;
    const minutes = totalMinutes % 60;
    return `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}`;
}

export function formatDuration(durationMinutes) {
    const hours = Math.floor(durationMinutes / 60);
    const minutes = durationMinutes % 60;
    if (hours <= 0) {
        return `${minutes}m`;
    }
    if (minutes === 0) {
        return `${hours}h`;
    }
    return `${hours}h ${minutes}m`;
}

export function formatDayOfWeek(value) {
    if (!value) {
        return "";
    }
    return value.charAt(0) + value.slice(1).toLowerCase();
}

export function formatDateTime(instantValue) {
    if (!instantValue) {
        return "";
    }
    const date = new Date(instantValue);
    return date.toLocaleString();
}

export function formatBookingDateTime(value) {
    if (!value) {
        return "-";
    }
    return value.replace("T", " ");
}
