package com.valdisdot.jobscrapper.exclusion;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Concrete implementation of {@link ExclusionService} that stores
 * exclusion.txt keywords in a text file.
 */
public class InFileExclusionService implements ExclusionService {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final String filePath;
    private final Set<String> exclusions;

    /**
     * Creates a new InFileExclusionService instance.
     *
     * @param filePath path to the file where exclusion.txt keywords are stored
     */
    public InFileExclusionService(String filePath) {
        this.filePath = filePath;
        exclusions = new HashSet<>();
        loadExclusions();
    }

    /**
     * Loads all exclusion.txt keywords from the file.
     * Reads each line from the file as an exclusion.txt keyword,
     * trims whitespace from each line, and logs errors if file reading fails.
     */
    public void loadExclusions() {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            while (reader.ready()) exclusions.add(reader.readLine().trim());
        } catch (Exception e) {
            logger.error("Failed to load exclusions list from file: {}", filePath, e);
        }
    }

    @Override
    public void persist() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String url : exclusions) writer.append(url).append("\n");
            writer.flush();
        } catch (Exception e) {
            logger.error("Failed to save exclusions list to file: {}", filePath, e);
        }
    }

    @Override
    public Iterator<String> exclusions() {
        return exclusions.iterator();
    }

    @Override
    public void exclude(String text) {
        exclusions.add(text);
    }

    @Override
    public void unexclude(String text) {
        exclusions.remove(text);
    }
}
