package com.makerspacetools.automation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * In-memory log of booking attempts.
 */
@Service
public class BookingAttemptLogger {

    private final Deque<BookingAttempt> attempts;
    private final int maxEntries;

    @Autowired
    BookingAttemptLogger(AutomationProperties properties) {
        this.attempts = new ArrayDeque<>();
        this.maxEntries = properties.feedSize();
    }

    /**
     * Adds an attempt to the log.
     *
     * @param attempt attempt to add
     */
    public synchronized void add(BookingAttempt attempt) {
        attempts.addFirst(attempt);
        while (attempts.size() > maxEntries) {
            attempts.removeLast();
        }
    }

    /**
     * Returns a snapshot of attempts.
     *
     * @param limit maximum number of entries
     * @return list of attempts
     */
    public synchronized List<BookingAttempt> list(int limit) {
        return attempts.stream().limit(limit).toList();
    }
}
