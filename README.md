# Discord Split Tracker
Not just another loot logger! I developed this originally with the goal of adding KC data to an existing loot logger, but decided to implement some other new features that are geared toward clans and group content. Credit and a huge thanks goes to RinzZJ for the original repo, and his work on the "better discord loot logger" plugin, as well as to MasterKenth and the developers of the "discord rare drop notificator" plugin for the inspiration to create this one.


## Retains original functionality as an automatic loot logger with some key distinctions:
- Side Panel for manual loot submissions
  - Simply set a valuable drop threshold, and open the panel when you get a drop worth sharing, it will pre-populate with item/npc information and a screenshot of the most recent valuable drop
    - Wise Old Man API Integration to list out all members in your Clan/Group and add them to the Split Submission
    - Particularly useful in clans and alongside specialized Discord bots (will update here when the partner JDA project by TheDyldozer is complete and interfaces seamlessly with this plugin)
    - Prompt for split information if applicable, which will display to track with your clan or friend group
    - Configuration available for Bingo and Event strings to prevent fraudulent activity in clan events
    - Can set a custom field such as "Clan: <clan name>", "Group", etc. to display in your Discord WebHook messages


- Automatic Discord WebHook output just like any other loot logger, but in a format that I thought was more appealing
  - First of its kind to show realtime KC information on each valuable boss drop
  - Also queries the OSRS wiki to show an item icon and an image of the npc you fought, if applicable
    - Credit to the Discord Rare Drop Notificator for the idea for this, and the playeraccounttype switch to show ironman helmets
  - Raids loot logging is still experimental, priority for the time being is the split tracking functionality

## Forked from https://github.com/RinZJ/better-discord-loot-logger
