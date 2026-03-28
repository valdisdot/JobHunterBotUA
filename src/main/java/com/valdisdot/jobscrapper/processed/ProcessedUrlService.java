package com.valdisdot.jobscrapper.processed;

/**
 * Interface for tracking processed.txt job listing URLs to prevent
 * duplicate processing.
 */
public interface ProcessedUrlService {

    /**
     * Checks if a URL has already been processed.txt.
     *
     * @param url the URL to check
     * @return true if the URL has been processed.txt, false otherwise
     */
    boolean isProcessed(String url);

    /**
     * Marks a URL as processed.txt.
     *
     * @param url the URL to mark as processed.txt
     * @return true if the URL was newly added (not already processed.txt), false otherwise
     */
    boolean markAsProcessed(String url);

    /**
     * Saves all processed.txt URLs to persistent storage.
     */
    void persist();
}
