package com.valdisdot.jobscrapper.exclusion;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Interface for managing exclusion.txt rules in the job scraper.
 * Allows filtering out job positions based on keywords found in
 * job titles and descriptions.
 */
public interface ExclusionService {

    /**
     * Checks if the given text contains any exclusion.txt keywords.
     *
     * @param text the text to check against exclusion.txt rules
     * @return true if any exclusion.txt keyword is found in the text, false otherwise
     */
    default boolean containsExclusion(String text) {
        Set<String> words = Arrays.stream(text.split(" "))
                .map(w -> w.replaceAll("[^0-9a-zA-Zа-яА-ЯїЇіІєЄёЁ-]", "").trim().toLowerCase())
                .filter(Predicate.not(String::isBlank))
                .collect(Collectors.toSet());
        if (!words.isEmpty()) {
            Iterator<String> iterator = exclusions();
            String exclusion;
            while (iterator.hasNext()) {
                exclusion = iterator.next();
                for (String word : words)
                    if (word.contains(exclusion))
                        return true;
            }
        }
        return false;
    }

    /**
     * Returns an iterator over all currently configured exclusion.txt keywords.
     *
     * @return Iterator over exclusion.txt keywords
     */
    Iterator<String> exclusions();

    /**
     * Adds a new exclusion.txt keyword to the exclusion.txt list.
     *
     * @param text the keyword to exclude
     */
    void exclude(String text);

    /**
     * Removes an exclusion.txt keyword from the exclusion.txt list.
     *
     * @param text the keyword to remove from exclusions
     */
    void unexclude(String text);

    /**
     * Saves all current exclusion.txt keywords to persistent storage.
     */
    void persist();
}

