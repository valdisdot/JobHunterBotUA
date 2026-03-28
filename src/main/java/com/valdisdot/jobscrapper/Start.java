package com.valdisdot.jobscrapper;

import com.valdisdot.jobscrapper.consumer.TelegramBotJobPositionConsumer;
import com.valdisdot.jobscrapper.exclusion.InFileExclusionService;
import com.valdisdot.jobscrapper.processed.InFileProcessedUrlService;
import com.valdisdot.jobscrapper.scraper.WorkUaJobScraper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Main entry point for the Job Hunter Bot application.
 * Initializes and manages job scraping from work.ua,
 * Telegram bot integration, and persistence services.
 */
public class Start {
    /**
     * Main method - entry point of the application.
     * Reads configuration from application.properties and
     * starts scheduled job scraping and Telegram bot.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        Logger logger = LogManager.getLogger(Start.class);
        try (FileInputStream applicationProperties = new FileInputStream("application.properties")) {
            Properties properties = new Properties();
            properties.load(applicationProperties);

            List<ScheduledExecutorService> scheduledExecutorServices = new LinkedList<>();
            ScheduledExecutorService mainScheduler = Executors.newScheduledThreadPool(properties.getProperty("work_ua_vacancies_paths").split(",").length + 1);
            scheduledExecutorServices.add(mainScheduler);
            InFileExclusionService exclusionService = new InFileExclusionService(properties.getProperty("exclusion_path", "exclusion.txt"));
            InFileProcessedUrlService processedUrlService = new InFileProcessedUrlService(properties.getProperty("processed_path", "processed.txt"));
            TelegramBotJobPositionConsumer telegramBotJobPositionConsumer = new TelegramBotJobPositionConsumer(
                    properties.getProperty("bot_token"),
                    () -> {
                        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
                        scheduledExecutorServices.add(scheduledExecutorService);
                        return scheduledExecutorService;
                    },
                    exclusionService,
                    processedUrlService,
                    Arrays.stream(properties.getProperty("allowed_users").split(",")).map(String::trim).toList()
            );

            Collection<WorkUaJobScraper> workUaJobScrapers = new LinkedList<>();
            for (String workUaVacancyPath : properties.getProperty("work_ua_vacancies_paths").split(",")) {
                String tag = workUaVacancyPath.replaceAll("[^0-9a-zA-Zа-яА-ЯїЇіІєЄёЁ]", "_").trim();
                tag = tag.isBlank() ? "#work_ua_job_positions" : "#" + tag;
                WorkUaJobScraper workUaJobScraper = new WorkUaJobScraper(
                        workUaVacancyPath,
                        tag,
                        Integer.parseInt(properties.getProperty("scraping_pages", "10")),
                        telegramBotJobPositionConsumer,
                        exclusionService,
                        processedUrlService
                );
                mainScheduler.scheduleAtFixedRate(
                        workUaJobScraper,
                        0,
                        Integer.parseInt(properties.getProperty("scraping_interval", "15")),
                        TimeUnit.MINUTES
                );
                workUaJobScrapers.add(workUaJobScraper);
                telegramBotJobPositionConsumer.addJobPositionFinder(workUaJobScraper);
            }

            mainScheduler.scheduleAtFixedRate(
                    () -> {
                        exclusionService.persist();
                        processedUrlService.persist();
                    },
                    5,
                    60,
                     TimeUnit.MINUTES
            );

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.warn("Shutting down...");
                workUaJobScrapers.forEach(WorkUaJobScraper::close);
                telegramBotJobPositionConsumer.stop();
                scheduledExecutorServices.forEach(ExecutorService::shutdownNow);
                exclusionService.persist();
                processedUrlService.persist();
            }));
        } catch (IOException e1) {
            logger.error("Error reading application properties", e1);
        } catch (Exception e2) {
            logger.error("Unexpected error", e2);
        }
    }
}