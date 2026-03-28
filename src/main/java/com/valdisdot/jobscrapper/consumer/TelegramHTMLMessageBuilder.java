package com.valdisdot.jobscrapper.consumer;

import com.valdisdot.jobscrapper.JobPosition;

import java.util.LinkedList;

/**
 * Builder class for creating HTML-formatted messages for Telegram.
 * Provides a fluent API for building rich text messages with
 * bold, italic, links, and other Telegram-supported formatting.
 */
public class TelegramHTMLMessageBuilder {
    private final StringBuilder builder = new StringBuilder();
    private final LinkedList<String> stack = new LinkedList<>();

    /**
     * Formats a JobPosition into a Telegram HTML message.
     * The message includes: title (bold), company, salary (italic),
     * description, link, and tag.
     *
     * @param jobPosition the job position to format
     * @return the formatted HTML message
     */
    public static String format(JobPosition jobPosition) {
        return new TelegramHTMLMessageBuilder()
                .bold().append(jobPosition.getTitle()).end()
                .ln()
                .append(jobPosition.getCompany())
                .ln()
                .italic().append(jobPosition.getSalary() == null ? "-" : jobPosition.getSalary()).end()
                .ln().ln()
                .append(jobPosition.getDescription())
                .ln()
                .anchor(jobPosition.getLink()).append(jobPosition.getLink()).end()
                .ln().ln()
                .append(jobPosition.getTag())
                .toString();
    }

    /**
     * Internal method to start a formatting tag.
     *
     * @param tag the HTML tag to open
     * @return this builder for method chaining
     */
    private TelegramHTMLMessageBuilder start(String tag) {
        builder.append("<").append(tag).append(">");
        stack.add(String.format("</%s>", tag));
        return this;
    }

    /**
     * Starts a bold tag.
     *
     * @return this builder for method chaining
     */
    public TelegramHTMLMessageBuilder bold() {
        return start("b");
    }

    /**
     * Starts an italic tag.
     *
     * @return this builder for method chaining
     */
    public TelegramHTMLMessageBuilder italic() {
        return start("i");
    }

    /**
     * Starts an underline tag.
     *
     * @return this builder for method chaining
     */
    public TelegramHTMLMessageBuilder underline() {
        return start("u");
    }

    /**
     * Starts a strikethrough tag.
     *
     * @return this builder for method chaining
     */
    public TelegramHTMLMessageBuilder strikethrough() {
        return start("s");
    }

    /**
     * Starts a preformatted text tag.
     *
     * @return this builder for method chaining
     */
    public TelegramHTMLMessageBuilder preformatted() {
        return start("pre");
    }

    /**
     * Starts a code tag.
     *
     * @return this builder for method chaining
     */
    public TelegramHTMLMessageBuilder code() {
        return start("code");
    }

    /**
     * Starts a spoiler tag (Telegram-specific).
     *
     * @return this builder for method chaining
     */
    public TelegramHTMLMessageBuilder spoiler() {
        builder.append("<span class=\"tg-spoiler\">");
        stack.add("</span>");
        return this;
    }

    /**
     * Starts an anchor (link) tag.
     *
     * @param href the URL for the link
     * @return this builder for method chaining
     */
    public TelegramHTMLMessageBuilder anchor(String href) {
        if (href != null) {
            builder.append(String.format("<a href=\"%s\">", href));
            stack.add("</a>");
        }
        return this;
    }

    /**
     * Appends an object to the builder if it's not null.
     *
     * @param object the object to append
     * @return this builder for method chaining
     */
    public TelegramHTMLMessageBuilder append(Object object) {
        if (object != null) builder.append(object);
        return this;
    }

    /**
     * Appends a newline character.
     *
     * @return this builder for method chaining
     */
    public TelegramHTMLMessageBuilder ln() {
        return append("\n");
    }

    /**
     * Closes the current open tag and appends the closing tag.
     *
     * @return this builder for method chaining
     */
    public TelegramHTMLMessageBuilder end() {
        return append(stack.removeLast());
    }

    /**
     * Checks if all tags have been properly closed.
     *
     * @return true if the stack is empty (all tags closed), false otherwise
     */
    public boolean isComplete() {
        return stack.isEmpty();
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}

