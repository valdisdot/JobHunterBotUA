package com.valdisdot.jobscrapper.consumer;

import com.valdisdot.jobscrapper.JobPosition;

import java.util.function.Consumer;

/**
 * Marker interface for job position consumers.
 * Extends {@link Consumer} to accept {@link JobPosition} objects.
 */
public interface JobPositionConsumer extends Consumer<JobPosition> {
}
