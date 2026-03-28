package com.valdisdot.jobscrapper.processed;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;

public class InFileProcessedUrlService implements ProcessedUrlService {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final String filePath;
    private final Set<String> processedUrls;

    public InFileProcessedUrlService(String filePath) {
        this.filePath = filePath;
        processedUrls = new HashSet<>();
        loadProcessedUrls();
    }

    public void loadProcessedUrls() {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            while (reader.ready()) processedUrls.add(reader.readLine().trim());
        } catch (Exception e) {
            logger.error("Failed to load processed.txt list from file: {}", filePath, e);
        }
    }

    @Override
    public void persist() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String url : processedUrls) writer.append(url).append("\n");
            writer.flush();
        } catch (Exception e) {
            logger.error("Failed to save processed.txt list to file: {}", filePath, e);
        }
    }

    @Override
    public boolean isProcessed(String url) {
        return processedUrls.contains(url);
    }

    @Override
    public boolean markAsProcessed(String url) {
        return processedUrls.add(url);
    }
}
