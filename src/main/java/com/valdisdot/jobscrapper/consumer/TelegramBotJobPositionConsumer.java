package com.valdisdot.jobscrapper.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valdisdot.jobscrapper.JobPosition;
import com.valdisdot.jobscrapper.exclusion.ExclusionService;
import com.valdisdot.jobscrapper.processed.ProcessedUrlService;
import okhttp3.OkHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.util.TelegramOkHttpClientFactory;
import org.telegram.telegrambots.meta.TelegramUrl;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.GetUpdates;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Telegram bot consumer that handles job position notifications and bot commands.
 * Implements both {@link JobPositionConsumer} for receiving job positions and
 * {@link LongPollingUpdateConsumer} for processing bot commands.
 *
 * <p>Supported commands:</p>
 * <ul>
 *   <li><code>exclude: word1, word2</code> - Add words to exclusion.txt list</li>
 *   <li><code>unexclude: word1, word2</code> - Remove words from exclusion.txt list</li>
 *   <li><code>pull</code> - Trigger immediate job scraping</li>
 *   <li><code>persist</code> - Save exclusions and processed.txt URLs</li>
 *   <li><code>help</code> - Display available commands</li>
 * </ul>
 */
public class TelegramBotJobPositionConsumer implements JobPositionConsumer, LongPollingUpdateConsumer {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final TelegramClient client;
    private final TelegramBotsLongPollingApplication application;
    private final ExclusionService exclusionService;
    private final ProcessedUrlService processedUrlService;
    private final Collection<String> allowedUsers;

    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<JobPosition>> queue;

    private final List<Runnable> jobPositionFinders;
    private final ScheduledExecutorService internalExecutorService;
    private final String helpBody;

    public TelegramBotJobPositionConsumer(
            String token,
            Supplier<ScheduledExecutorService> scheduledExecutorServiceSupplier,
            ExclusionService exclusionService,
            ProcessedUrlService processedUrlService,
            Collection<String> allowedUsers
    ) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            client = new OkHttpTelegramClient(
                    objectMapper,
                    new OkHttpClient.Builder().build(),
                    token,
                    TelegramUrl.DEFAULT_URL
            );
            application = new TelegramBotsLongPollingApplication(
                    () -> objectMapper,
                    new TelegramOkHttpClientFactory.DefaultOkHttpClientCreator(),
                    scheduledExecutorServiceSupplier
            );
            application.registerBot(
                    token,
                    () -> TelegramUrl.DEFAULT_URL,
                    lastReceivedUpdate -> GetUpdates.builder()
                            .limit(50)
                            .timeout(60)
                            .offset(lastReceivedUpdate + 1)
                            .allowedUpdates(List.of())
                            .build(),
                    this
            );
            this.exclusionService = exclusionService;
            this.processedUrlService = processedUrlService;
            this.allowedUsers = allowedUsers;
            this.queue = new ConcurrentHashMap<>();
            for (String allowedUser : allowedUsers) this.queue.put(allowedUser, new ConcurrentLinkedDeque<>());
            this.jobPositionFinders = new LinkedList<>();
            this.internalExecutorService = scheduledExecutorServiceSupplier.get();
            this.internalExecutorService.scheduleAtFixedRate(this::iterateMessageQueue, 2, 1, TimeUnit.SECONDS);

            this.helpBody = new TelegramHTMLMessageBuilder()
                    .append("Available commands:").ln()
                    .bold().append("exclude: [list]").end().append(" - excludes positions with certain words in job title and description.").ln()
                    .code().append("exclude: sale, smm, ppc").end().ln().ln()
                    .bold().append("unexclude: [list]").end().append(" - opposite to exclude, removes exclusions.").ln()
                    .code().append("unexclude: smm, ppc").end().ln().ln()
                    .bold().append("pull").end().append(" - pull job positions out the schedule.").ln()
                    .code().append("pull").end().ln().ln()
                    .bold().append("persist").end().append(" - saves exclusions and processed.txt job positions out the schedule.").ln()
                    .code().append("persist").end()
                    .toString();
        } catch (TelegramApiException e) {
            logger.error("Error occurred while creating Telegram client logic", e);
            throw new RuntimeException(e);
        }
    }

    private static void sleep(long nanos) {
        try {
            Thread.sleep(Duration.ofNanos(nanos));
        } catch (InterruptedException ignored) {}
    }

    //1 message to 1 chat is allowed so call it each 1 second (for wost scenario where there is only one allowedChat)
    public void iterateMessageQueue() {
        long startTime = System.nanoTime();
        int messagesSent = 0;

        for (String userChatId : allowedUsers) {
            JobPosition jobPosition = queue.get(userChatId).poll();
            if (jobPosition != null) {
                try {
                    client.execute(SendMessage.builder()
                            .parseMode("HTML")
                            .text(TelegramHTMLMessageBuilder.format(jobPosition))
                            .chatId(userChatId)
                            .build()
                    );
                } catch (TelegramApiException e) {
                    logger.warn("Cannot send the message with job position", e);
                    queue.get(userChatId).addFirst(jobPosition);
                }
                //increment the counter in both normal flow and error
                ++messagesSent;

                //made 30 messages
                if (messagesSent % 30 == 0) {
                    //sleep 1 second - spent time
                    sleep(1000000000 + startTime - System.nanoTime());
                }
            }
        }
    }

    @Override
    public void accept(JobPosition jobPosition) {
        for (String userChatId : allowedUsers) {
            this.queue.get(userChatId).addLast(jobPosition);
        }
    }

    /**
     * Processes a single update from Telegram.
     *
     * @param update the update to process
     */
    private void consume(Update update) {
        if (update.hasMessage() && allowedUsers.contains(update.getMessage().getChatId().toString()) && update.getMessage().hasText()) {
            String chatId = update.getMessage().getChatId().toString();
            String[] tokens = update.getMessage().getText().split(":");
            try {
                if ("exclude".equals(tokens[0])) {
                    String[] excludeTokens = tokens[1].split(",");
                    if (excludeTokens.length == 0)
                        client.execute(new SendMessage(chatId, "No positions to exclude were provided."));
                    else {
                        for (String excludeToken : excludeTokens) exclusionService.exclude(excludeToken.trim());
                        client.execute(new SendMessage(chatId, String.format("Added to exclusions %d elements.", excludeTokens.length)));
                    }
                } else if ("unexclude".equalsIgnoreCase(tokens[0])) {
                    String[] unexcludeTokens = tokens[1].split(",");
                    if (unexcludeTokens.length == 0)
                        client.execute(new SendMessage(chatId, "No positions to unexclude were provided."));
                    else {
                        for (String excludeToken : unexcludeTokens) exclusionService.unexclude(excludeToken.trim());
                        client.execute(new SendMessage(chatId, String.format("Removed from exclusions %d elements.", unexcludeTokens.length)));
                    }
                } else if ("persist".equalsIgnoreCase(tokens[0])) {
                    exclusionService.persist();
                    processedUrlService.persist();
                    client.execute(new SendMessage(chatId, "Successfully persisted exclusions and processed.txt job positions."));
                } else if ("pull".equalsIgnoreCase(tokens[0])) {
                    for (Runnable runnable : jobPositionFinders) internalExecutorService.schedule(runnable, 1, TimeUnit.MICROSECONDS);
                    client.execute(new SendMessage(chatId, "Started pulling job positions."));
                } else if ("help".equalsIgnoreCase(tokens[0])) {
                    client.execute(SendMessage.builder().chatId(chatId).text(helpBody).parseMode("HTML").build());
                } else {
                    client.execute(new SendMessage(chatId, "Unknown command."));
                    client.execute(SendMessage.builder().chatId(chatId).text(helpBody).parseMode("HTML").build());
                }
            } catch (TelegramApiException e) {
                logger.error("Error occurred while sending message response", e);
            }
        }
    }

    @Override
    public void consume(List<Update> updates) {
        for (Update update : updates) consume(update);
    }

    public void addJobPositionFinder(Runnable runnable) {
        if (runnable != null && !jobPositionFinders.contains(runnable)) this.jobPositionFinders.add(runnable);
    }

    public void stop() {
        if (application.isRunning()) {
            try {
                application.stop();
            } catch (TelegramApiException e) {
                logger.error("Error occurred while stopping Telegram client logic", e);
            }
        }
    }
}
