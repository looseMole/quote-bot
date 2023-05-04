package com.loosemole.quotebot.listeners;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.HashMap;

public class Quote implements Serializable {
    private String quote;
    private String source;
    private String messageId;
    private String preMeta = "";
    private String midMeta = "";
    private String postMeta = "";

    public Quote(String quote, String source, String messageId) {
        this.quote = quote;
        this.source = source;
        this.messageId = messageId;
    }

    public boolean hasMeta() {
        return (!preMeta.isEmpty() | !midMeta.isEmpty() | !postMeta.isEmpty());
    }

    @Override
    public String toString() {
        return this.quote + " - " + this.source;
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

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getPreMeta() {
        return preMeta;
    }

    public void setPreMeta(String preMeta) {
        this.preMeta = preMeta;
    }

    public String getMidMeta() {
        return midMeta;
    }

    public void setMidMeta(String midMeta) {
        this.midMeta = midMeta;
    }

    public String getPostMeta() {
        return postMeta;
    }

    public void setPostMeta(String postMeta) {
        this.postMeta = postMeta;
    }
}
