package com.loosemole.quotebot;

import com.loosemole.quotebot.listeners.EventListener;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class QuoteBot {
    private final Dotenv config;

    public QuoteBot() {
        this.config = Dotenv.configure().load();
        String token = config.get("TOKEN");

        JDA api = JDABuilder.createDefault(token).enableIntents(GatewayIntent.MESSAGE_CONTENT).addEventListeners(new EventListener()).build(); // Actually run the bot.
        api.getPresence().setActivity(Activity.listening("Interesting Quotes"));
    }

    public static void main(String[] args) {
        QuoteBot qb = new QuoteBot();
    }

    public Dotenv getConfig() {
        return config;
    }
}