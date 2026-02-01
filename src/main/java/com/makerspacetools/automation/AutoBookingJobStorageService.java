package com.makerspacetools.automation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * JSON file-backed store for auto-booking jobs.
 */
@Service
class AutoBookingJobStorageService {

    private final Path filePath;
    private final ObjectMapper objectMapper;
    private final ReentrantReadWriteLock lock;
    private final List<AutoBookingJob> jobs;

    @Autowired
    AutoBookingJobStorageService(AutomationProperties properties, ObjectMapper objectMapper) {
        this.filePath = properties.jobsFile();
        this.objectMapper = objectMapper;
        this.lock = new ReentrantReadWriteLock();
        this.jobs = new ArrayList<>();
        loadFromFile();
    }

    List<AutoBookingJob> list() {
        lock.readLock().lock();
        try {
            return List.copyOf(jobs);
        } finally {
            lock.readLock().unlock();
        }
    }

    Optional<AutoBookingJob> find(UUID jobId) {
        lock.readLock().lock();
        try {
            return jobs.stream().filter(job -> job.id().equals(jobId)).findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    AutoBookingJob add(AutoBookingJob job) {
        lock.writeLock().lock();
        try {
            jobs.add(job);
            saveToFile();
            return job;
        } finally {
            lock.writeLock().unlock();
        }
    }

    Optional<AutoBookingJob> update(AutoBookingJob updated) {
        lock.writeLock().lock();
        try {
            for (int index = 0; index < jobs.size(); index++) {
                if (jobs.get(index).id().equals(updated.id())) {
                    jobs.set(index, updated);
                    saveToFile();
                    return Optional.of(updated);
                }
            }
            return Optional.empty();
        } finally {
            lock.writeLock().unlock();
        }
    }

    boolean delete(UUID jobId) {
        lock.writeLock().lock();
        try {
            boolean removed = jobs.removeIf(job -> job.id().equals(jobId));
            if (removed) {
                saveToFile();
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void loadFromFile() {
        if (Files.notExists(filePath)) {
            return;
        }
        lock.writeLock().lock();
        try {
            List<AutoBookingJob> loaded = Arrays.asList(objectMapper.readValue(filePath.toFile(), AutoBookingJob[].class));
            jobs.clear();
            jobs.addAll(loaded);
        } catch (IOException ignored) {
            jobs.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void saveToFile() {
        try {
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), jobs);
        } catch (IOException ignored) {
        }
    }
}
