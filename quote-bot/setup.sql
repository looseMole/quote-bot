CREATE TABLE Quote  (
    Quote_ID INT NOT NULL,
    Quote_Source VARCHAR(255) NOT NULL,
    Quote_Text VARCHAR(255) NOT NULL,
    Pre_Meta VARCHAR(255) NOT NULL,
    Mid_Meta VARCHAR(255) NOT NULL,
    Post_Meta VARCHAR(255) NOT NULL,
    PRIMARY KEY (Quote_ID)
);

CREATE TABLE Reminder (
    Id serial PRIMARY KEY,
    Time timestamp NOT NULL,
    Text VARCHAR(255) NOT NULL,
    org_msg_id VARCHAR(255) NOT NULL,
    user_to_remind VARCHAR(255) NOT NULL,
    is_sent BOOLEAN NOT NULL
);