package com.loosemole.quotebot.listeners;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EventListener extends ListenerAdapter {

    @Override
    public void onGuildReady(GuildReadyEvent event) { // When ready on a particular server.
        // Check if server has the correctly named channel to scrape from
        Guild guild = event.getGuild();
        List<GuildChannel> channelList = guild.getChannels();
        TextChannel quotesChannel = null;

        for(GuildChannel ch : channelList) {
            if(ch.getName().equals("cool-quotes-by-us")) { // The name of the server to scrape for messages.
                quotesChannel = (TextChannel) ch;
                break;
            }
        }

        if(quotesChannel == null) {
            System.out.println("Guild: " + guild.getName() + " had no appropriate Quote channel.");
            return;
        }

        // Get all previously sent messages of the Quotes Channel
        MessageHistory messageHistory = MessageHistory.getHistoryFromBeginning(quotesChannel).complete();
        List<Message> messages = messageHistory.getRetrievedHistory();

        // Identify "Correctly formatted messages"
        String message;
        char[] messageLetters;
        ArrayList<Integer> inCorrectMessages = new ArrayList<>();
        ArrayList<Quote> quotes = new ArrayList<>();

        int quoteStartIndex;
        int quoteEndIndex;
        int sourceStartIndex;
        int sourceEndIndex;
        int descStartIndex;

        for(int i = 0; i < messages.size(); i++) {
            quoteStartIndex = 0;
            quoteEndIndex = 0;
            sourceStartIndex = 0;
            sourceEndIndex = 0;

            message = messages.get(i).getContentDisplay();
            messageLetters = message.toCharArray();

            if(message.isEmpty()) {
                continue;
            }

            if(messageLetters[0] != '"') { // If message does not start with a '"', it is not on "correct" form.
                if(messageLetters[0] == '|' && messageLetters[2] == '"') {
                    quoteStartIndex = 2;
                } else {
                    inCorrectMessages.add(i);
                    System.out.println(message + " Does not start wih a '\"'.");
                    continue;
                }
            }

            for(int j = quoteStartIndex + 1; j < messageLetters.length; j++) {
                if(messageLetters[j] == '"') {
                    if(messageLetters[j + 1] == '|' && messageLetters[j + 2] == '|') {
                        quoteEndIndex = j + 3;
                        break;
                    } else {
                        quoteEndIndex = j + 1;
                        break;
                    }
                }
            }

            if(quoteEndIndex == 0) { // If endofquote or start of source has not been found.
                inCorrectMessages.add(i);
                System.out.println(message + " Does not have a end of quote.");
                continue;
            }

            for(int j = quoteEndIndex; j < messageLetters.length; j++) {
                if (messageLetters[j] != ' ' && messageLetters[j] != '-') {
                    sourceStartIndex = j;
                    break;
                }
            }

            if(sourceStartIndex == 0) { // If start of source has not been found.
                inCorrectMessages.add(i);
                System.out.println(message + " Does not have a source start.");
                continue;
            }

            for(int j = sourceStartIndex; j < messageLetters.length; j++) {
                if(messageLetters[j] == ' ') {
                    sourceEndIndex = j;
                    break;
                }
            }

            if(sourceEndIndex == 0) { // In that case, there is no description.
                sourceEndIndex = messageLetters.length;
            }

            String m = new String(Arrays.copyOfRange(messageLetters, 0, quoteEndIndex));
            String s = new String(Arrays.copyOfRange(messageLetters, sourceStartIndex, sourceEndIndex));

            if(sourceEndIndex != messageLetters.length) { // Because then there *is* a description.
                descStartIndex = sourceEndIndex + 1;
                String d =  new String(Arrays.copyOfRange(messageLetters, descStartIndex, messageLetters.length));

                Quote q = new Quote(m, s, d.trim());
                quotes.add(q);
            } else {
                Quote q = new Quote(m, s);
                quotes.add(q);
            }
        }

        // Debug:
        for(Quote q : quotes) {
            System.out.println(q);
        }
//        quotesChannel.sendMessage(quotes.size() + " quotes ||loaded||!").queue();
    }



            }
        }


        }

        }

    }
}

