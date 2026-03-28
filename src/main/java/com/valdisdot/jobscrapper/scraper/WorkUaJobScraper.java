package com.valdisdot.jobscrapper.scraper;

import com.valdisdot.jobscrapper.JobPosition;
import com.valdisdot.jobscrapper.exclusion.ExclusionService;
import com.valdisdot.jobscrapper.processed.ProcessedUrlService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import java.util.function.Consumer;

/**
 * Scraper for job listings from work.ua website.
 * Scrapes job positions from specified vacancy paths,
 * filters them based on exclusions, and sends them
 * to the configured consumer.
 */
public class WorkUaJobScraper implements Runnable {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final String vacanciesUrl;
    private final int maxPages;
    private final Consumer<JobPosition> jobPositionConsumer;
    private final ExclusionService exclusionService;
    private final ProcessedUrlService processedUrlService;
    private final String tag;

    private final WebDriver driver;

    /**
     * Creates a new WorkUaJobScraper.
     *
     * @param vacanciesUrl the base path for vacancies (e.g., "java")
     * @param tag the tag for categorizing scraped jobs
     * @param maxPages maximum number of pages to scrape
     * @param jobPositionConsumer consumer for processed.txt job positions
     * @param exclusionService service for filtering excluded jobs
     * @param processedUrlService service for tracking processed.txt URLs
     */
    public WorkUaJobScraper(String vacanciesUrl, String tag, int maxPages, Consumer<JobPosition> jobPositionConsumer, ExclusionService exclusionService, ProcessedUrlService processedUrlService) {
        this.vacanciesUrl = "https://www.work.ua/" + vacanciesUrl;
        this.maxPages = maxPages;
        this.jobPositionConsumer = jobPositionConsumer;
        this.exclusionService = exclusionService;
        this.processedUrlService = processedUrlService;
        this.tag = tag;

        ChromeOptions options = new ChromeOptions();

        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-gpu");
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);

        driver = new ChromeDriver(options);

        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        driver.manage().window().setSize(new Dimension(1920, 1080));
    }

    /**
     * Scrapes job listings from work.ua website.
     * Iterates through pages from maxPages down to 1,
     * extracts job information, filters by exclusions,
     * and sends results to the consumer.
     *
     * @throws InterruptedException if the thread is interrupted
     */
    @Override
    public void run() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        logger.info("Pulling jobs...");
        for (int page = maxPages; page >= 1; --page) {
            logger.info("Scrapping jobs from {}/?page={}", vacanciesUrl, page);
            try {
                String pageUrl = vacanciesUrl + "/?page=" + page;
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
                                    jobPosition.setTag(this.tag);
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
                                    jobPositionConsumer.accept(jobPosition);
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
                return;
            } catch (Exception e) {
                logger.error("Error during processing job listing page {}", page, e);
            }
        }
    }

    public void close() {
        driver.manage().deleteAllCookies();
        driver.quit();
    }
}
