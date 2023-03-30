package com.loosemole.quotebot.listeners;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.HashMap;

public class Quote implements Serializable {
    private String quote;
    private String source;
    private String messageId;
    private String description = "";

    public Quote(String quote, String source, String messageId) {
        this.quote = quote;
        this.source = source;
        this.messageId = messageId;
    }

    public Quote(String quote, String source, String messageId, String description) {
        this(quote, source, messageId);
        this.description = description;
    }

    @Override
    public String toString() {
        if(this.description == ""){
            return this.quote + " - " + this.source;
        } else {
            return this.quote + " - " + this.source + " " + this.description;
        }
    }

    // Getter/Setters:
    public String getQuote() {
        return quote;
    }

    public void setQuote(String quote) {
        this.quote = quote;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}
