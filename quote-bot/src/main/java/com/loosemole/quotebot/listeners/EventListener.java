package com.loosemole.quotebot.listeners;

import com.loosemole.quotebot.exceptions.IncorrectQuoteFormatException;
import com.loosemole.quotebot.exceptions.NoTextInMessageException;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.github.cdimascio.dotenv.Dotenv;

public class EventListener extends ListenerAdapter implements Serializable {
    private HashMap<String, ArrayList<Quote>> quoteListMap = new HashMap<>(); // Storing all currently loaded quote-lists w. Guild ID as key.
    private Connection conn;
    private final Dotenv config;

    public EventListener() {
        // Create connection to DB
        this.config = Dotenv.configure().load();
        try {
            DriverManager.registerDriver(new org.postgresql.Driver());
            conn = DriverManager.getConnection(
                    config.get("SQL_CONNECTION_URL"),
                    config.get("SQL_USERNAME"),
                    config.get("SQL_PASSWORD"));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // When ready on a particular server.
    @Override
    public void onGuildReady(GuildReadyEvent event) {
        Guild guild = event.getGuild();
        List<GuildChannel> channelList = guild.getChannels();
        TextChannel quotesChannel = null;
        ArrayList<Quote> quotes;

        // Check if server has the correctly named channel to scrape from
        for (GuildChannel ch : channelList) {
            if (ch.getName().equals("cool-quotes-by-us")) { // The name of the channel to scrape for messages.
                quotesChannel = (TextChannel) ch;
                break;
            }
        }

        if (quotesChannel == null) {
            System.out.println("Guild: " + guild.getName() + " had no appropriate Quote channel.");
            return;
        }

        quotes = this.load_quotes(guild);

        if (quotes == null) {
            // Get all previously sent messages of the Quotes Channel
            quotes = this.get_all_sent_quotes(quotesChannel);
        } else {
            // Update list of quotes with the ones sent since last saved quote.
            String latestQuoteId = quotes.get(quotes.size() - 1).getMessageId();
            quotes.addAll(this.get_quotes_since(quotesChannel, latestQuoteId));
        }

        /*
            Keep updating list of quotes until getHistory requests returns no new messages.
        */
        int amountOfQuotes = quotes.size();
        int updatedAmountOfQuotes = -1;
        int loopCounter = 0;
        while (amountOfQuotes > 0 && (amountOfQuotes != updatedAmountOfQuotes && loopCounter < 30)) {
            if (updatedAmountOfQuotes > amountOfQuotes) {
                amountOfQuotes = updatedAmountOfQuotes;
            }

            String latestQuoteId = quotes.get(quotes.size() - 1).getMessageId();
            quotes.addAll(this.get_quotes_since(quotesChannel, latestQuoteId));

            updatedAmountOfQuotes = quotes.size();
            loopCounter++;

            System.out.println("get_quotes_since has been called for " + guild.getName() + ", " + loopCounter + " times during this startup...");
        }

        this.quoteListMap.put(guild.getId(), quotes);
        this.save_quotes(guild, quotes);
    }

    // TODO: Override the below methods.
    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        super.onMessageDelete(event);
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        super.onMessageUpdate(event);
    }

    @Override // Where the command-action happens.
    public void onMessageReceived(MessageReceivedEvent event) {
        // Safeguards:
        if (!event.isFromGuild()) return;
        if (event.getAuthor().isBot()) return;

        String prefix = "!";  // The prefix for this bot's commands.
        Message triggerMessage = event.getMessage();
        String mContent = triggerMessage.getContentDisplay();

        if (!mContent.startsWith(prefix)) {
            if (!event.getChannel().getName().equals("cool-quotes-by-us")) {
                return;
            }
            try {
                this.quoteListMap.get(event.getGuild().getId()).addAll(this.identifyQuotes(event.getMessage()));
            } catch (IncorrectQuoteFormatException | NoTextInMessageException e) {
                System.out.println(event.getMessage().getContentDisplay() + " is not formatted correctly for a quote."); // TODO Handle incorrectly formatted quotes better.
            }
            return;
        }

        mContent = mContent.replaceFirst(prefix, "");  // Remove prefix.
        String[] mWords = mContent.split(" ");
        TextChannel cChannel = (TextChannel) event.getChannel();
        String guildId = event.getGuild().getId();

        // All valid commands are under here:
        switch (mWords[0].toLowerCase()) {
            case "stats" ->
                    cChannel.sendMessage((this.quotesStats(guildId))).queue(); // Send stats to the events origin channel.
            case "random" -> {
                if (mWords.length >= 2) {
                    try {
                        cChannel.sendMessage(this.getQuoteByName(guildId, mWords[1]).toString()).queue();
                    } catch (NullPointerException e) {
                        cChannel.sendMessage("No quotes attributed to \"" + mWords[1] + "\" found.").queue();
                    }
                } else {
                    cChannel.sendMessage(this.getRandomQuote(guildId).toString()).queue();
                }
            }
            case "guess" -> {  // TODO: Find a more creative way to reveal answer.
                Quote randomQuote = this.getRandomQuote(guildId);

                if (!randomQuote.hasMeta()) {
                    cChannel.sendMessage("Who said: " + randomQuote.getQuote() + "? Answer: ||" + randomQuote.getSource() + "||").queue();
                } else /*(If The Quote has meta text, the meta can be treated as a hint)*/ {
                    StringBuilder sb = new StringBuilder(randomQuote.getQuote());
                    if (!randomQuote.getPreMeta().equals("")) {
                        sb = new StringBuilder(randomQuote.getPreMeta())
                                .append("||" + randomQuote.getQuote() + "||");
                    }
                    if (!randomQuote.getMidMeta().equals("")) {
                        sb.append("||" + randomQuote.getMidMeta() + "||");
                    }
                    if (!randomQuote.getPostMeta().equals("")) {
                        sb.append(" - <source> ")
                                .append("||" + randomQuote.getPostMeta() + "||");
                    }
                    cChannel.sendMessage("Who said: " + sb + "? Answer: ||" + randomQuote.getSource() + "||").queue();
                }
            }
            case "remindme" -> {
                cChannel.sendMessage("The functionality for: `" + mContent + "` is not quite done yet.").queue();

                // TODO: Create time-management system for checking when there is a given reminder due.
                String errorMessage = "Unknown date: `" + mContent + "` try again, with a `yyyy-mm-dd hh:mm` format.";
                if (mWords.length <= 2) { // Check size of message.
                    cChannel.sendMessage(errorMessage).queue();
//                    System.out.println("Message too short.");
                    return;
                }

                Timestamp plannedTime;
                try { // Check if the date is valid.
                    plannedTime = Timestamp.valueOf(mWords[1] + " " + mWords[2] + ":00");
                } catch (IllegalArgumentException e) {
                    cChannel.sendMessage(errorMessage).queue();
                    return;
                }

                Message referencedMessage = triggerMessage.getMessageReference().resolve().complete();

                // TODO: Insert into database here.
                try {
                    PreparedStatement insertStatement = conn.prepareStatement("INSERT INTO reminder (time, text, org_msg_id, user_to_remind) VALUES(?,?,?,?)");
                    insertStatement.setTimestamp(1, plannedTime);
                    insertStatement.setString(2, referencedMessage.getContentDisplay());
                    insertStatement.setString(3, referencedMessage.getId());
                    insertStatement.setString(4, triggerMessage.getAuthor().getId());

                    insertStatement.execute();
                } catch (SQLException e) {
                    System.out.println(e);
                }

                System.out.println(plannedTime);
                cChannel.addReactionById(triggerMessage.getId(), Emoji.fromUnicode("U+1F44D")).queue(); // Confirm interaction.
            }
            default -> cChannel.sendMessage("Unknown command: `" + mContent + "`").queue();
        }
    }

    /*
     * "Picks" a random number between 0 and the amount of quotes associated with the server, then returns the quote
     * that is "sitting" on that index, in the server's quotes ArrayList.
     */
    public Quote getRandomQuote(String guildId) {
        ArrayList<Quote> quotes = quoteListMap.get(guildId); // The quotes for this server.
        int index = (int) (Math.random() * quotes.size()); // Random number between 0 and quotes-size.
        return quotes.get(index);
    }

    /*
     * Runs through the ArrayList of quotes for the server, adding any quotes by an author with a matching name to a new
     * temporary ArrayList. - Then performs the same operation as getRandomQuote on this, smaller list.
     * As both the server-wide amount of quotes, and the amount of quotes associated with the same name can be very large,
     * this is not optimally memory-efficient.
     */
    public Quote getQuoteByName(String guildId, String quoteAuthor) throws NullPointerException {
        ArrayList<Quote> quotes = quoteListMap.get(guildId); // The quotes for this server.
        ArrayList<Quote> namedQuotes = new ArrayList<>();

        // Compose list of all quotes attributed to the same person
        for (Quote q : quotes) {
            if (q.getSource().equals(quoteAuthor)) {
                namedQuotes.add(q);
            }
        }
        if (namedQuotes.size() == 0) {
            throw new NullPointerException();
        }

        int index = (int) (Math.random() * namedQuotes.size()); // Random number between 0 and quotes-size.
        return namedQuotes.get(index);
    }

    private String quotesStats(String guildId) {
        ArrayList<Quote> quotes = quoteListMap.get(guildId);
        StringBuilder statBuilder = new StringBuilder();
        HashMap<String, Integer> authorMap = new HashMap<>();

        // Count how many quotes each saved "Source" has to their name.
        statBuilder.append(quotes.size() + " quotes loaded.\n");
        for (int i = 0; i < quotes.size(); i++) {
            Quote q = quotes.get(i);
            String qAuthor = q.getSource();
            if (!authorMap.containsKey(qAuthor)) {
                authorMap.put(qAuthor, 1);
            } else {
                authorMap.put(qAuthor, authorMap.get(qAuthor) + 1); // Increment the number related to the author.
            }
        }

        // NGL: This sorting is stolen from StackOverflow
        Map<String, Integer> sortedMap =
                authorMap.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                        .collect(Collectors.toMap(
                                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        // Build stats-string.
        for (String key : sortedMap.keySet()) {
            statBuilder.append(key + ": " + sortedMap.get(key) + " Quotes.\n");
        }

        String stats = statBuilder.toString();
        return stats;
    }

    // Return an ArrayList containing all quotes send since the list was last updated.
    private ArrayList<Quote> get_all_sent_quotes(TextChannel quotesChannel) {
        return get_quotes_since(quotesChannel, null);
    }

    private ArrayList<Quote> get_quotes_since(TextChannel quotesChannel, String latestMessageId) {
        ArrayList<Quote> quotes = new ArrayList<>();
        MessageHistory messageHistory;

        if (latestMessageId == null) {
            messageHistory = MessageHistory.getHistoryFromBeginning(quotesChannel).complete();
        } else {
            Message latestMessage;
            try {
                latestMessage = quotesChannel.retrieveMessageById(latestMessageId).complete();
            } catch (ErrorResponseException e) {
                System.out.println("The latest saved quote has probably been deleted.");
                return null; // TODO: Handle MessageNotFoundError more gracefully, potentially by trying with the next-oldest saved quote.
            }

            messageHistory = MessageHistory.getHistoryAfter(quotesChannel, latestMessage.getId()).complete();
        }

        List<Message> reverseMessages = messageHistory.getRetrievedHistory(); // Returns list of retrieved messages, sorted from newest to oldest.

        // Reverse the sorting of the list.
        ArrayList<Message> messages = new ArrayList<>();
        for (Message m : reverseMessages) {
            messages.add(m);
        }
        Collections.reverse(messages);

        // Identify "Correctly formatted messages"
        LinkedList<Integer> inCorrectMessages = new LinkedList<>(); // TODO: Actually use inCorrectMessages for something.

        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getAuthor().isBot()) continue; // Bots are not quoteable.

            LinkedList<Quote> convoQs;

            try {
                convoQs = new LinkedList<>(this.identifyQuotes(messages.get(i)));
            } catch (NoTextInMessageException | IncorrectQuoteFormatException e) {
                inCorrectMessages.add(i);
                continue;
            }

            if (convoQs.isEmpty()) {
                inCorrectMessages.add(i);
                continue;
            }

            quotes.addAll(convoQs);
        }

        return quotes;
    }

    private ArrayList<Quote> identifyQuotes(Message inputMessage) throws NoTextInMessageException, IncorrectQuoteFormatException {
        String message = inputMessage.getContentDisplay();
        String messageId = inputMessage.getId();

        if (message.isEmpty()) {
            throw new NoTextInMessageException();
        }

        String m; // Message
        String s; // Message source / quoted author

        ArrayList<Quote> convoQs = new ArrayList<>();

        Pattern pattern = Pattern.compile("^(?<premeta>.*?)(?:\\|?[\"'“](?<quote>.+)[\"'”]\\|?) ?(?: ?(?<midmeta>.*) ?(?=[-/]))?[-/]? ?(?<name>\\w+)[,]? ?(?<postmeta>.*)"); // Props to MidnightRocket for this RegEx work.
        Matcher matcher = pattern.matcher(message);

        while (matcher.find()) {
            if (!(matcher.group("quote") == null | matcher.group("quote").isEmpty() | matcher.group("name") == null | matcher.group("name").isEmpty())) {
                m = "\"" + matcher.group("quote") + "\"";
                s = matcher.group("name").toLowerCase();
                s = s.substring(0, 1).toUpperCase() + s.substring(1);
                convoQs.add(new Quote(m, s, messageId));
            } else {
                throw new IncorrectQuoteFormatException();
            }
        }
        return convoQs;
    }

    private boolean save_quotes(Guild guild, ArrayList<Quote> quotes) {
        Quote[] quoteArray = new Quote[1];
        quoteArray = quotes.toArray(quoteArray);

        File f = new File(guild.getId() + "Quotes.txt");

        // Should do nothing if file already exists.
        try {
            Files.createFile(Path.of(f.toURI()));
        } catch (IOException e) {
            System.out.println("Error while creating file: " + e);
        }

        try (FileOutputStream fos = new FileOutputStream(f.getAbsolutePath())) {

            try (ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                oos.writeObject(quoteArray);
            }
        } catch (IOException e) {
            System.out.println(e);
            return false;
        }
        return true;
    }

    private ArrayList<Quote> load_quotes(Guild guild) {
        ArrayList<Quote> quotes;
        Quote[] quoteArray;
        try {
            FileInputStream fis = new FileInputStream(guild.getId() + "Quotes.txt");
            try (ObjectInputStream ois = new ObjectInputStream(fis)) {
                quoteArray = (Quote[]) ois.readObject();
            } catch (FileNotFoundException e) {
                System.out.println("No previously saved quotes found.");
                return null;
            } catch (ClassNotFoundException | IOException e) {
                System.out.println(e);
                return null;
            }
        } catch (FileNotFoundException e) {
            System.out.println(e);
            return null;
        }

        quotes = new ArrayList<>(List.of(quoteArray));
        return quotes;
    }
}
