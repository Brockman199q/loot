/*
 * Copyright (c) 2022, RinZ
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.discordnotifcationsaio;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("discordnotifcationsaio")
public interface DiscordNotificationsAIOConfig extends Config {
        
        // @ConfigItem(
        // keyName = "keybind",
        // name = "Screenshot Keybind",
        // description = "Add keybind to manually take a screenshot and send a message
        // of your rare drop",
        // position = 4
        // )
        // default Keybind keybind()
        // {
        // return Keybind.NOT_SET;
        // }

        @ConfigItem(keyName = "webhook", name = "Default WebHook", description = "The webhook used to send messages to Discord.", position = 1)
        String webhook();
        
        @ConfigItem(keyName = "autoLog", name = "Automatic Notifications", description = "Send Notifications to Discord on Level-up, Loot, Deaths, Quests, or Collection Log Completion", position = 2)
        default boolean autoLog() {
                return true;
        }

        @ConfigItem(keyName = "autoWebHookToggle", name = "Use Separate WebHook?", description = "Use a secondary WebHook for Automatic Notifications, separate from your Split Tracking one.", position = 3)
        default boolean autoWebHookToggle() {
                return false;
        }

        @ConfigItem(keyName = "autoWebHook", name = "Automatic WebHook", description = "Secondary WebHook for Automatic Loot Logging.", position = 4)
        
        String autoWebHook();

        @ConfigSection(name = "Valuable Loot", description = "Options for Valuable Loot Notifications", position = 5)
        String valuableLootSection = "valuable loot options";
        
        

        @ConfigItem(keyName = "sendScreenshot", name = "Send Screenshot?", description = "Include a screenshot in the discord message?", position = 6, section = valuableLootSection)
        default boolean sendScreenshot() {
                return true;
        }

        @ConfigItem(keyName = "bingo", name = "Include Bingo/Event String", description = "Add an event string to your screenshot's message", position = 7, section = valuableLootSection)
        default boolean includeBingo() {
                return false;
        }

        @ConfigItem(keyName = "bingoString", name = "Custom Bingo/Event String", description = "Insert your custom event here.", position = 8, section = valuableLootSection)
        default String bingoString() {
                return "#ABC123";
        }

        @ConfigItem(keyName = "valuableDrop", name = "Include Valuable drops", description = "Configures whether valuable drops will be automatically sent to discord.", position = 9, section = valuableLootSection)
        default boolean includeValuableDrops() {
                return true;
        }

        @ConfigItem(keyName = "valuableDropThreshold", name = "Valuable Drop Threshold", description = "The minimum value of a drop for it to send a discord message.", position = 10, section = valuableLootSection)
        default int valuableDropThreshold() {
                return 100000;
        }

        @ConfigSection(
                name = "Levelling",
                description = "The config for levelling notifications",
                position = 11,
                closedByDefault = true
        )
        String levellingConfig = "levellingConfig";
        
        @ConfigItem(
                keyName = "includeLevelling",
                name = "Send Levelling Notifications",
                description = "Send messages when you level up a skill.",
                section = levellingConfig,
                position = 12
        )
        default boolean includeLevelling() {
        return false;
        }
        
        @ConfigItem(
                keyName = "minimumLevel",
                name = "Minimum level",
                description = "Levels greater than or equal to this value will send a message.",
                section = levellingConfig,
                position = 13
        )
        default int minLevel() {
        return 0;
        }
        
        @ConfigItem(
                keyName = "levelInterval",
                name = "Send every X levels",
                description = "Only levels that are a multiple of this value are sent. Level 99 will always be sent regardless of this value.",
                section = levellingConfig,
                position = 14
        )
        default int levelInterval() {
        return 1;
        }
        
        @ConfigItem(
                keyName = "linearLevelModifier",
                name = "Linear Level Modifier",
                description = "Send every `max(-.1x + linearLevelMax, 1)` levels. Will override `Send every X levels` if set to above zero.",
                section = levellingConfig,
                position = 15
        )
        default double linearLevelMax() {
        return 0;
        }
        
        @ConfigItem(
                keyName = "levelMessage",
                name = "Level Message",
                description = "Message to send to Discord on Level",
                section = levellingConfig,
                position = 16
        )
        default String levelMessage() { return "$name leveled $skill to $level"; }
        
        @ConfigItem(
                keyName = "andLevelMessage",
                name = "Multi Skill Level Message",
                description = "Message to send to Discord when Multi Skill Level",
                section = levellingConfig,
                position = 17
        )
        default String andLevelMessage() { return ", and $skill to $level"; }
        
        @ConfigItem(
                keyName = "sendLevellingScreenshot",
                name = "Send level-up screenshots?",
                description = "Include a screenshot when leveling up.",
                section = levellingConfig,
                position = 18
        )
        default boolean sendLevellingScreenshot() {
        return false;
        }
        // End levelling config section
        
        // Questing config section
        @ConfigSection(
                name = "Questing",
                description = "The config for questing notifications",
                position = 19,
                closedByDefault = true
        )
        String questingConfig = "questingConfig";
        
        @ConfigItem(
                keyName = "includeQuests",
                name = "Send Quest Notifications",
                description = "Send messages when you complete a quest.",
                section = questingConfig
        )
        default boolean includeQuestComplete() {
        return false;
        }
        
        @ConfigItem(
                keyName = "questMessage",
                name = "Quest Message",
                description = "Message to send to Discord on Quest",
                section = questingConfig,
                position = 20
        )
        default String questMessage() { return "$name has just completed: $quest"; }
        
        @ConfigItem(
                keyName = "sendQuestingScreenshot",
                name = "Include quest screenshots",
                description = "Include a screenshot with the discord notification when leveling up.",
                section = questingConfig,
                position = 21
        )
        default boolean sendQuestingScreenshot() {
        return false;
        }
        // End questing config section
        
        // Death config section
        @ConfigSection(
                name = "Deaths",
                description = "The config for death notifications",
                position = 22,
                closedByDefault = true
        )
        String deathConfig = "deathConfig";
        
        @ConfigItem(
                keyName = "includeDeaths",
                name = "Send Death Notifications",
                description = "Send messages when you die to discord.",
                section = deathConfig,
                position = 23
        )
        default boolean includeDeath() { return false; }
        
        @ConfigItem(
                keyName = "deathMessage",
                name = "Death Message",
                description = "Message to send to Discord on Death",
                section = deathConfig,
                position = 24
        )
        default String deathMessage() { return "$name has just died!"; }
        
        @ConfigItem(
                keyName = "sendDeathScreenshot",
                name = "Include death screenshots",
                description = "Include a screenshot with the discord notification when you die.",
                section = deathConfig,
                position = 25
        )
        default boolean sendDeathScreenshot() {
        return false;
        }
        // End death config section
        
        // Clue config section
        @ConfigSection(
                name = "Clue Scrolls",
                description = "The config for clue scroll notifications",
                position = 26,
                closedByDefault = true
        )
        String clueConfig = "clueConfig";
        
        @ConfigItem(
                keyName = "includeClues",
                name = "Send Clue Notifications",
                description = "Send messages when you complete a clue scroll.",
                section = clueConfig,
                position = 27
        )
        default boolean includeClue() { return false; }
        
        @ConfigItem(
                keyName = "clueMessage",
                name = "Clue Message",
                description = "Message to send to Discord on Clue",
                section = clueConfig,
                position = 28
        )
        default String clueMessage() { return "$name has just completed a clue scroll!"; }
        
        @ConfigItem(
                keyName = "sendClueScreenshot",
                name = "Include Clue screenshots",
                description = "Include a screenshot with the discord notification when you complete a clue.",
                section = clueConfig,
                position = 29
        )
        default boolean sendClueScreenshot() {
        return false;
        }
        // End clue config section
        
        // Pet config section
        @ConfigSection(
                name = "Pets",
                description = "The config for pet notifications",
                position = 30,
                closedByDefault = true
        )
        String petConfig = "petConfig";
        
        @ConfigItem(
                keyName = "includePets",
                name = "Send Pet Notifications",
                description = "Send messages when you receive a pet.",
                section = petConfig,
                position = 31
        )
        default boolean setPets() { return false; }
        
        @ConfigItem(
                keyName = "petMessage",
                name = "Pet Message",
                description = "Message to send to Discord on Pet",
                section = petConfig,
                position = 32
        )
        default String petMessage() { return "$name has just received a pet!"; }
        
        @ConfigItem(
                keyName = "sendPetScreenshot",
                name = "Include Pet screenshots",
                description = "Include a screenshot with the discord notification when you receive a pet.",
                section = petConfig,
                position = 33
        )
        default boolean sendPetScreenshot() {
        return false;
        }
        
        // Collection Log config section
        @ConfigSection(
                name = "Collection Log",
                description = "The config for collection log notifications",
                position = 34,
                closedByDefault = true
        )
        String collectionLogConfig = "collectionLogConfig";
        
        @ConfigItem(
                keyName = "includeCollectionLogs",
                name = "Send Collection Log Notifications",
                description = "Send messages when you receive a collection log entry.",
                section = collectionLogConfig,
                position = 35
        )
        default boolean setCollectionLogs() { return false; }
        
        @ConfigItem(
                keyName = "collectionLogMessage",
                name = "Collection Log Message",
                description = "Message to send to Discord on Collection Log",
                section = collectionLogConfig,
                position = 36
        )
        default String collectionLogMessage() { return "$name just received a new collection log item: **$itemName!**"; }
        
        @ConfigItem(
                keyName = "sendCollectionLogScreenshot",
                name = "Include Collection Log screenshots",
                description = "Include a screenshot with the discord notification when you receive a collection log item.",
                section = collectionLogConfig,
                position = 37
        )
        default boolean sendCollectionLogScreenshot() {
        return false;
        }

        @ConfigSection(name = "Advanced", description = "Advanced/Experimental Options", position = 150)
        String advancedSection = "advanced";


        @ConfigItem(keyName = "customField", name = "Custom Field Title", description = "", position = 38, section = valuableLootSection)
        default String customField() {
                return "";
        }

        @ConfigItem(keyName = "customValue", name = "Custom Field Value", description = "", position = 39, section = valuableLootSection)
        default String customValue() {
                return "";
        }

        @ConfigItem(keyName = "whiteListedRSNs", name = "Whitelisted RSNs", description = "(optional) Comma-separated list of RSNs which are allowed to post to the webhook, can prevent drops being posted from all your accounts", section = advancedSection, position = 40)
        default String whiteListedRSNs() {
                return "";
        }

        @ConfigItem(keyName = "raidLoot", name = "Include raid loot (Experimental)", description = "Configures whether a message will be automatically sent to discord when you obtain a raid unique.", position = 100, section = valuableLootSection)
        default boolean includeRaidLoot() {
                return true;
        }

        @ConfigItem(keyName = "codeBlocks", name = "Show Code Blocks?", description = "Configures whether a message will have code blocks in the embeds.", position = 100, section = advancedSection)
        default boolean codeBlocks() {
                return true;
        }

        // @ConfigItem(
        // keyName = "partyNames",
        // name = "Experimental Party Integration",
        // description = "Get current party members in the Split Members Section of the
        // Side Panel",
        // section = advancedSection,
        // position = 70
        // )
        // default boolean partyNames() {
        // return false;
        // }

}
