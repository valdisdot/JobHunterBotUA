package com.valdisdot.jobscrapper.source;

import com.valdisdot.jobscrapper.JobPosition;
import com.valdisdot.jobscrapper.exclusion.ExclusionService;
import com.valdisdot.jobscrapper.processed.ProcessedUrlService;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class WorkUaJobPositionSource extends JobPositionSource {
    public WorkUaJobPositionSource(String tag, String urlFilter, int maxPages, ExclusionService exclusionService, ProcessedUrlService processedUrlService) {
        super(tag, "https://www.work.ua/" + urlFilter, maxPages, exclusionService, processedUrlService);
    }

    @Override
    public Iterator<Collection<JobPosition>> iterateJobPositions() {
        return new WorkUaJobPositionIterator();
    }

    private class WorkUaJobPositionIterator implements Iterator<Collection<JobPosition>> {
        private int currentPage = maxPages;
        private WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        @Override
        public boolean hasNext() {
            return currentPage > 0;
        }

        @Override
        public Collection<JobPosition> next() {
            LinkedList<JobPosition> res = new LinkedList<>();

            logger.info("Scrapping jobs from {}/?page={}", vacanciesUrl, currentPage);
            try {
                String pageUrl = vacanciesUrl + "/?page=" + currentPage;
                driver.get(pageUrl);
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#pjax-jobs-list div[data-id]")));
                Thread.sleep(2000);

                List<WebElement> jobDivs = driver.findElements(By.cssSelector("#pjax-jobs-list a[name] + div"));

                logger.info("Elements to process: {}", jobDivs.size());
                for (WebElement jobDiv : jobDivs) {
                    if (!jobDiv.findElements(By.cssSelector("time")).isEmpty() && !jobDiv.findElements(By.cssSelector("h2")).isEmpty()) {
                        try {
                            WebElement titleElement;
                            try {
                                titleElement = jobDiv.findElement(By.cssSelector("h2 a"));
                            } catch (Exception e) {
                                continue;
                            }

                            String href = titleElement.getAttribute("href");
                            String title = titleElement.getText();

                            if (!processedUrlService.isProcessed(href)) {
                                if (!exclusionService.containsExclusion(title)) {
                                    WebElement descriptionElement = null;
                                    try {
                                        descriptionElement = jobDiv.findElement(By.cssSelector("p"));
                                    } catch (Exception ignored) {
                                    }

                                    if (descriptionElement != null
                                            && exclusionService.containsExclusion(descriptionElement.getText())) {
                                        continue;
                                    }

                                    logger.info("Got new job position: {}", href);
                                    JobPosition jobPosition = new JobPosition();
                                    jobPosition.setTag(tag);
                                    jobPosition.setTitle(title);

                                    WebElement secondSibling = null;
                                    try {
                                        secondSibling = jobDiv.findElement(
                                                By.cssSelector("div:has(h2) + div > span")
                                        );
                                    } catch (Exception ignored) {
                                    }

                                    if (secondSibling != null) {
                                        if (secondSibling.getText().contains("грн")) {
                                            jobPosition.setSalary(secondSibling.getText());

                                            WebElement thirdSibling = null;
                                            try {
                                                thirdSibling = jobDiv.findElement(
                                                        By.cssSelector("div:has(h2) + div + div > span > span")
                                                );
                                            } catch (Exception ignored) {
                                            }

                                            if (thirdSibling != null) {
                                                jobPosition.setCompany(thirdSibling.getText());
                                            }
                                        } else {
                                            try {
                                                WebElement innerSpan = secondSibling.findElement(By.cssSelector("span"));
                                                jobPosition.setCompany(innerSpan.getText());
                                            } catch (Exception ignored) {
                                            }
                                        }
                                    }

                                    if (descriptionElement != null) {
                                        jobPosition.setDescription(descriptionElement.getText());
                                    }

                                    jobPosition.setLink(href);
                                    res.add(jobPosition);
                                }
                            }

                            processedUrlService.markAsProcessed(href);

                        } catch (Exception e) {
                            logger.error("Error during processing one vacancy block", e);
                        }
                    }
                }
            } catch (InterruptedException e) {
                logger.error("Thread was interrupted", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error("Error during processing job listing page {}", currentPage, e);
            }

            currentPage--;
            return res;
        }
    }
}
