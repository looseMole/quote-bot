# quote-bot
A Discord bot, which will scrape quotes on the standard form of our server’s #cool-quotes-by-us channel. These can then be presented in various humorous ways. 

**Initial Idea**  
On one of the Discord servers I am a member of, there is a long-running "game" where every server-member try their best to listen for *Quotes* - both "real" and taken out of 
context that make the "author" of the quote seem as deranged or stupid as possible, and then write them down for the entire server to remember them by.  
This "game" has prompted a channel on our Discord server named "cool-quotes-by-us", full of these quotes, which is where the inspiration came from.  
I wanted a command-driven bot which could process all of these quotes, and then make a game of them, in which the members of the server try to guess who was the original
"author" of a certain quote.  
  
The "Standard formula" for these quotes, are: `[premeta] "<Quote" [midmeta] - <Name> [postmeta]` - the fields marked with square brackets ([]) being optional.

**Initially proposed features**
1. “Random quote”
2. “Random quote by name”
3. “Who said?”: A form of quiz game where the bot presents a quote, and possibly some options, and then the server members have to guess who said that quote.
4. Stats like which names have been attributed the most quotes.

**Known problems**
1. Some quotes do not follow the standard format. The scraping of the channel has to take this into consideration.  
  a. Some quotes have been recorded on image form, some of which contain the quoted persons name/online alias. These would probably have to be manually handled.  
  b. The current filter is case sensitive regarding the authors of quotes, resulting in “Jon123” and “jon123” being regarded as two different people.  
2. Currently the bot only loads already-sent, quotes when it “boots up” and does thus not handle any additional quotes that might be created while the bot is active.  

**Roadmap**  
Have a bot ✅  
Scrape all quotes on the normal, or close-to-normal formats ✅  
Feature 1 ✅  
Feature 2 ✅  
Feature 4 ✅  
Feature 3 ✅  
Problem 1.b ✅  
Problem 1.a ✅  
Problem 2 ✅  
