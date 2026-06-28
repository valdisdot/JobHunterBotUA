package com.valdisdot.jobscrapper.source;

import com.valdisdot.jobscrapper.JobPosition;
import com.valdisdot.jobscrapper.exclusion.ExclusionService;
import com.valdisdot.jobscrapper.processed.ProcessedUrlService;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.*;

public class DjiniUaJobPositionSource extends JobPositionSource {
    public DjiniUaJobPositionSource(String tag, String vacanciesUrl, int maxPages, ExclusionService exclusionService, ProcessedUrlService processedUrlService) {
        super(tag, "https://djinni.co/jobs/?" + vacanciesUrl, maxPages, exclusionService, processedUrlService);
    }

    @Override
    public Iterator<Collection<JobPosition>> iterateJobPositions() {
        return new DjiniUaJobPositionIterator();
    }

    private class DjiniUaJobPositionIterator implements Iterator<Collection<JobPosition>> {
        private int currentPage = maxPages;
        private final int waitSeconds = 10;
        private final WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(this.waitSeconds));

        @Override
        public boolean hasNext() {
            return currentPage > 0;
        }

        @Override
        public Collection<JobPosition> next() {
            LinkedList<JobPosition> res = new LinkedList<>();

            logger.info("Scrapping jobs from {}&page={}", vacanciesUrl, currentPage);

            try {
                String pageUrl = vacanciesUrl + "&page=" + currentPage;
                driver.get(pageUrl);
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("main [id]")));
                Thread.sleep(2000);

                if (!pageUrl.equalsIgnoreCase(driver.getCurrentUrl())) {
                    throw new JobPositionSource.SourceException(String.format("URL '%s' does not match expected url '%s' (may be redirected), skipping", driver.getCurrentUrl(), pageUrl));
                }

                List<WebElement> jobDivs = driver.findElements(By.cssSelector("main [id]"));

                logger.info("Elements to process: {}", jobDivs.size());

                for (WebElement jobDiv : jobDivs) {
                    if (!jobDiv.findElements(By.cssSelector("h2")).isEmpty()) {
                        WebElement link;
                        try {
                            link = jobDiv.findElement(By.cssSelector(":scope > :first-child > a"));
                        } catch (Exception e) {
                            continue;
                        }

                        String href = link.getAttribute("href");

                        if (href == null) continue;

                        //djini started to add tracking rubbish that makes position url not unique
                        href = href.split("/?")[0];

                        if (processedUrlService.isProcessed(href)) continue;

                        WebElement titleElement = jobDiv.findElement(By.cssSelector("h2"));
                        String title = titleElement.getText();
                        if (exclusionService.containsExclusion(title)) continue;

                        WebElement descriptionElement;
                        try {
                            descriptionElement = jobDiv.findElement(By.cssSelector(":scope > :first-child > [id] > :first-child"));
                        } catch (Exception e) {
                            continue;
                        }

                        String description = descriptionElement.getText();
                        if (exclusionService.containsExclusion(description)) continue;

                        logger.info("Got new job position: {}", href);
                        JobPosition jobPosition = new JobPosition();
                        jobPosition.setTag(tag);
                        jobPosition.setLink(href);
                        jobPosition.setTitle(title);
                        jobPosition.setDescription(description);

                        WebElement salaryElement;
                        try {
                            salaryElement = link.findElement(By.cssSelector("header > :last-child span"));
                            jobPosition.setSalary(salaryElement.getText());
                        } catch (Exception ignored) {}

                        WebElement companyElement;
                        try {
                            companyElement = link.findElement(By.cssSelector("h2 + div > span"));
                            jobPosition.setCompany(companyElement.getText());
                        } catch (Exception ignored) {}

                        processedUrlService.markAsProcessed(href);
                        res.add(jobPosition);
                    }
                }
            } catch (InterruptedException e) {
                logger.error("Thread was interrupted, cancelling...");
                Thread.currentThread().interrupt();
                currentPage = 0;
            } catch (TimeoutException e) {
                logger.info("Page was not loaded or page content conditions was not met in {} seconds, skipping...", this.waitSeconds);
            } catch (JobPositionSource.SourceException e) {
                logger.warn(e.getMessage());
            } catch (Exception e) {
                logger.error("Error during processing job listing page '{}', skipping...", currentPage, e);
            }

            currentPage--;
            return res;
        }
    }
}
