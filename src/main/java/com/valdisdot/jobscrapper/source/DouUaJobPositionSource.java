package com.valdisdot.jobscrapper.source;

import com.valdisdot.jobscrapper.JobPosition;
import com.valdisdot.jobscrapper.exclusion.ExclusionService;
import com.valdisdot.jobscrapper.processed.ProcessedUrlService;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class DouUaJobPositionSource extends JobPositionSource {
    public DouUaJobPositionSource(WebDriver driver, String tag, String urlFilter, int maxPages, ExclusionService exclusionService, ProcessedUrlService processedUrlService) {
        super(driver, tag, "https://jobs.dou.ua/vacancies/?" + urlFilter, maxPages, exclusionService, processedUrlService);
    }

    @Override
    public Iterator<Collection<JobPosition>> iterateJobPositions() {
        return new WorkUaJobPositionIterator();
    }

    private class WorkUaJobPositionIterator implements Iterator<Collection<JobPosition>> {
        private boolean isDone = false;
        private final int waitSeconds = 20;

        @Override
        public boolean hasNext() {
            return !isDone && maxPages > 0;
        }

        @Override
        public Collection<JobPosition> next() {
            LinkedList<JobPosition> res = new LinkedList<>();
            logger.info("Scrapping jobs from {}", vacanciesUrl);

            try {
                driver.get(vacanciesUrl);
                try {
                    new WebDriverWait(DouUaJobPositionSource.this.driver, Duration.ofSeconds(waitSeconds))
                            .until(
                                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("#vacancyListId div.more-btn > a"))
                            );
                } catch (TimeoutException e) {
                    logger.debug("Too little results to display 'More' button, do expect only the list");
                }

                new WebDriverWait(DouUaJobPositionSource.this.driver, Duration.ofSeconds(waitSeconds))
                        .until(
                                ExpectedConditions.presenceOfElementLocated(By.cssSelector("#vacancyListId ul > li"))
                        );
                Thread.sleep(2000);

                for (int click = 1; click < maxPages; ++click) {
                    WebElement loadMore;
                    try {
                        loadMore = driver.findElement(By.cssSelector("#vacancyListId div.more-btn > a"));
                    } catch (NoSuchElementException e) {
                        break;
                    }

                    String style = loadMore.getAttribute("style");
                    if (style != null && style.contains("display: none")) {
                        break;
                    }

                    int beforeCount = driver.findElements(By.cssSelector("#vacancyListId > ul > li")).size();
                    logger.info("Fetch more button is present, click #{} to get more job position, current count is {}", click, beforeCount);
                    loadMore.click();

                    try {
                        new WebDriverWait(driver, Duration.ofSeconds(20))
                                .until(
                                        d -> d.findElements(By.cssSelector("#vacancyListId > ul > li")).size() > beforeCount
                                );
                    } catch (TimeoutException e) {
                        break;
                    }
                }

                List<WebElement> jobListElements = driver.findElements(By.cssSelector("#vacancyListId > ul > li"));
                logger.info("Elements to process: {}", jobListElements.size());
                for (WebElement jobElement : jobListElements) {
                    WebElement titleElement;

                    try {
                        titleElement = jobElement.findElement(By.cssSelector(":scope > div:nth-of-type(2)"));

                        WebElement hrefElement = titleElement.findElement(By.cssSelector(":scope > a"));
                        String href = hrefElement.getAttribute("href");
                        String title = hrefElement.getText();

                        if (!processedUrlService.isProcessed(href)) {
                            String description = null;

                            try {
                                WebElement descriptionElement = jobElement.findElement(By.cssSelector(":scope > div:last-of-type"));
                                description = descriptionElement.getText();
                            } catch (NoSuchElementException ignored) {
                                description = "";
                            }

                            if (!(exclusionService.containsExclusion(title) || exclusionService.containsExclusion(description))) {
                                JobPosition jobPosition = new JobPosition();
                                jobPosition.setTag(tag);
                                jobPosition.setTitle(title);
                                jobPosition.setLink(href);
                                if (!description.isBlank()) jobPosition.setDescription(description);

                                String companyName = null;
                                try {
                                    companyName = titleElement.findElement(By.cssSelector(":scope > strong > a")).getText();
                                } catch (NoSuchElementException ignored) {}
                                jobPosition.setCompany(companyName);

                                List<WebElement> elements = titleElement.findElements(By.cssSelector(":scope > span"));
                                if (elements.size() > 1) {
                                    jobPosition.setSalary(elements.get(0).getText());
                                }

                                res.add(jobPosition);
                            }
                            processedUrlService.markAsProcessed(href);
                        }
                    } catch (NoSuchElementException ignored) {}
                }

            } catch (InterruptedException e) {
                logger.error("Thread was interrupted, cancelling...");
                Thread.currentThread().interrupt();
            } catch (TimeoutException e) {
                logger.info("Page was not loaded or page content conditions was not met in {} seconds, skipping...", this.waitSeconds);
            } catch (JobPositionSource.SourceException e) {
                logger.warn(e.getMessage());
            } catch (Exception e) {
                logger.error("Error during processing job page, skipping...", e);
            }

            isDone = true;
            return res;
        }
    }
}
