package com.valdisdot.jobscrapper.service;

import com.valdisdot.jobscrapper.consumer.JobPositionConsumer;
import com.valdisdot.jobscrapper.source.JobPositionSource;

import java.util.Collection;

public class JobPositionPuller implements Runnable {
    private final JobPositionConsumer jobPositionConsumer;
    private final Collection<JobPositionSource> jobPositionSources;

    public JobPositionPuller(JobPositionConsumer jobPositionConsumer, Collection<JobPositionSource> jobPositionSources) {
        this.jobPositionConsumer = jobPositionConsumer;
        this.jobPositionSources = jobPositionSources;
    }

    @Override
    public void run() {
        jobPositionSources.stream().map(JobPositionSource::iterateJobPositions).forEach(iterator -> {
            while (iterator.hasNext()) {
                iterator.next().forEach(jobPositionConsumer);
            }
        });
    }
}
