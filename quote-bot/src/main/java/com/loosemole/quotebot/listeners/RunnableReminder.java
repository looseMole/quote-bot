package com.loosemole.quotebot.listeners;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RunnableReminder implements Runnable {
    private Connection conn;
    private JDA api;

    public RunnableReminder(JDA api, java.sql.Connection conn) {
        this.conn = conn;
        this.api = api;
    }

    @Override
    public void run() {
        try {
            PreparedStatement getStatement = conn.prepareStatement("SELECT * FROM reminder WHERE time <= now() AND is_sent = false");
            ResultSet rs = getStatement.executeQuery();

            while (rs.next()) {
                String userToRemind = rs.getString("user_to_remind");
                String reminderText = rs.getString("text");
                int reminderId = rs.getInt("id");

                User user = api.getUserById(userToRemind);

                if (user != null) {
                    user.openPrivateChannel().complete().sendMessage(reminderText).queue();
                }

                // TODO: Am here!

                PreparedStatement updateStatement = conn.prepareStatement("UPDATE reminder SET is_sent = true WHERE id = ?"); // Update is_sent to true.
                updateStatement.setInt(1, reminderId);
                updateStatement.execute();
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
    }
}
