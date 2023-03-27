package com.loosemole.quotebot;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

public class QuoteBot {
    private final Dotenv config;

    public QuoteBot() {
        this.config = Dotenv.configure().load();
        String token = config.get("TOKEN");

        JDA api = JDABuilder.createDefault(token).build(); // Actually Run the bot.
        api.getPresence().setActivity(Activity.listening("Interesting Quotes"));
    }

    public static void main(String[] args) {
        QuoteBot qb = new QuoteBot();
    }

    public Dotenv getConfig() {
        return config;
    }
}
