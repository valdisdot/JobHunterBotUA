package com.valdisdot.jobscrapper.source;

import com.valdisdot.jobscrapper.JobPosition;
import com.valdisdot.jobscrapper.exclusion.ExclusionService;
import com.valdisdot.jobscrapper.processed.ProcessedUrlService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import java.util.Collection;
import java.util.Iterator;

public abstract class JobPositionSource {
    protected final Logger logger = LogManager.getLogger(this.getClass());
    protected final ExclusionService exclusionService;
    protected final ProcessedUrlService processedUrlService;
    protected final String vacanciesUrl;
    protected final int maxPages;
    protected final String tag;
    protected final WebDriver driver;

    public JobPositionSource(WebDriver driver, String tag, String vacanciesUrl, int maxPages, ExclusionService exclusionService, ProcessedUrlService processedUrlService) {
        this.tag = tag;
        this.vacanciesUrl = vacanciesUrl;
        this.maxPages = maxPages;
        this.exclusionService = exclusionService;
        this.processedUrlService = processedUrlService;
        this.driver = driver;
    }

    public void close() {
        driver.manage().deleteAllCookies();
        driver.quit();
    }

    public abstract Iterator<Collection<JobPosition>> iterateJobPositions();

    public static class SourceException extends RuntimeException {
        public SourceException(String message) {
            super(message);
        }

        public SourceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
