package com.loosemole.quotebot.listeners;

import java.io.Serializable;

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
        if (!this.hasMeta()) {
            return this.quote + " - " + this.source;
        } else { // If the quote has meta text, add it in the right order:
            StringBuilder sb = new StringBuilder(this.getQuote());
            if (!this.getPreMeta().equals("")) {
                sb = new StringBuilder(this.getPreMeta())
                        .append(this.getQuote());
            }
            if (!this.getMidMeta().equals("")) {
                sb.append(this.getMidMeta());
            }
            if (!this.getPostMeta().equals("")) {
                sb.append(this.getPostMeta());
            }
            return sb.toString();
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
