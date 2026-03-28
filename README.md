# Job Hunter Bot UA

A Java service that scrapes popular job portals and notifies users via a Telegram bot.

## How It Works

1. **Scheduled Scraping**: The application runs scrapers at configurable intervals
2. **Job Extraction**: Scrapers extract job data from work.ua pages
3. **Filtering**: Jobs are filtered based on exclusion keywords
4. **Notification**: Valid jobs are sent to allowed Telegram users
5. **Persistence**: Processed URLs and exclusions are saved to prevent duplicates

## Features

- **Automated Job Scraping**: Scrapes job listings from work.ua website
- **Telegram Bot Notifications**: Receives job notifications directly in Telegram
- **Smart Filtering**: Exclude unwanted job positions using keywords
- **Duplicate Prevention**: Tracks processed URLs to avoid duplicates
- **Persistent Storage**: Saves exclusions and processed URLs to files
- **Scheduled Scraping**: Runs on a configurable interval
- **Manual Trigger**: Pull jobs manually via bot command

## Project Structure

```
src/main/java/com/valdisdot/jobscrapper/
├── JobPosition.java                    # Data model for job positions
├── Start.java                          # Main entry point
├── consumer/
│   ├── JobPositionConsumer.java        # Marker interface for consumers
│   ├── TelegramHTMLMessageBuilder.java # HTML message formatter
│   └── TelegramBotJobPositionConsumer.java # Telegram bot with commands
├── exclusion/
│   ├── ExclusionService.java           # Interface for exclusion rules
│   └── InFileExclusionService.java     # File-based exclusion implementation
├── processed/
│   ├── ProcessedUrlService.java        # Interface for URL tracking
│   └── InFileProcessedUrlService.java  # File-based URL tracking
└── scraper/
    └── WorkUaJobScraper.java           # work.ua scraper implementation
```

## Bot Commands

| Command | Description | Example |
|---------|-------------|---------|
| `exclude: word1, word2` | Add words to exclusion list (filters jobs with these words in title/description) | `exclude: sale, smm, ppc` |
| `unexclude: word1, word2` | Remove words from exclusion list | `unexclude: smm, ppc` |
| `pull` | Trigger immediate job scraping | `pull` |
| `persist` | Save exclusions and processed URLs to files | `persist` |
| `help` | Display available commands | `help` |

## Configuration

Create `application.properties` in `src/main/resources/`:

```properties
# Telegram Bot Configuration
bot_token=your_telegram_bot_token_here

# Job Portal Configuration (comma-separated paths)
work_ua_vacancies_paths=java,python,backend

# Allowed Telegram Users (comma-separated chat IDs)
allowed_users=123456789,987654321

# Scraping Configuration
scraping_pages=10          # Number of pages to scrape per run
scraping_interval=15       # Interval in minutes between scrapes

# File Paths
exclusion_path=exclusion.txt
processed_path=processed.txt
```

### Configuration Options

| Property | Default | Description |
|----------|---------|-------------|
| `bot_token` | - | Telegram bot token from @BotFather |
| `work_ua_vacancies_paths` | - | Comma-separated job category paths on work.ua |
| `allowed_users` | - | Comma-separated Telegram chat IDs (bot admin) |
| `scraping_pages` | 10 | Number of pages to scrape per run |
| `scraping_interval` | 15 | Minutes between automatic scrapes |
| `exclusion_path` | exclusion.txt | Path to exclusion keywords file |
| `processed_path` | processed.txt | Path to processed URLs file |

## Usage

### 1. Setup

1. Get a Telegram bot token from [@BotFather](https://t.me/BotFather)
2. Get your Telegram chat ID (use [@userinfobot](https://t.me/userinfobot))
3. Configure `application.properties` with your settings
4. Build and run the application

### 2. First Steps

1. **Add yourself as allowed user**: Add your chat ID to `allowed_users`
2. **Start receiving jobs**: The bot will automatically scrape jobs every 15 minutes
3. **Filter unwanted jobs**: Use `exclude: keyword` to filter out unwanted positions
4. **Manual scraping**: Use `pull` command to trigger immediate scraping

### 3. Managing Exclusions

```
# Exclude jobs with certain keywords
exclude: sale, internship, smm, ppc

# Remove exclusions
unexclude: smm, ppc

# Save changes to files
persist
```

## API Reference

### JobPosition

Represents a job position extracted from a job listing website.

| Field | Type | Description |
|-------|------|-------------|
| `title` | String | Job title/position name |
| `salary` | String | Salary information (may be null) |
| `company` | String | Company name |
| `description` | String | Job description text |
| `link` | String | URL to the job listing page |
| `tag` | String | Category tag for the job position |

### ExclusionService

Interface for managing exclusion rules.

| Method | Description |
|--------|-------------|
| `containsExclusion(String text)` | Check if text contains any exclusion keywords |
| `exclude(String text)` | Add a keyword to exclusions |
| `unexclude(String text)` | Remove a keyword from exclusions |
| `persist()` | Save exclusions to file |

### ProcessedUrlService

Interface for tracking processed job URLs.

| Method | Description |
|--------|-------------|
| `isProcessed(String url)` | Check if URL has been processed |
| `markAsProcessed(String url)` | Mark URL as processed |
| `persist()` | Save processed URLs to file |

### TelegramBotJobPositionConsumer

Main bot class that handles job notifications and commands.

| Method | Description |
|--------|-------------|
| `accept(JobPosition jobPosition)` | Send job notification to allowed users |
| `addJobPositionFinder(Runnable runnable)` | Add a job scraper to the bot |
| `stop()` | Stop the bot |

**Disclaimer:** This bot is not intended for production use or to promote scraping. It is designed for personal use only (job hunting).
