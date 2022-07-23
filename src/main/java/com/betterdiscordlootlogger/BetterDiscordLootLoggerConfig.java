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
package com.betterdiscordlootlogger;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;


@ConfigGroup("discordsplittracker")
public interface BetterDiscordLootLoggerConfig extends Config {
    @ConfigSection(
            name = "Choose what to send",
            description = "Choose from the options below which events you would like to send",
            position = 30
    )
    String whatToSendSection = "what to send";

    @ConfigSection(
            name = "Advanced",
            description = "Advanced/Experimental Options",
            position = 40
    )
    String advancedSection = "advanced";

//    @ConfigItem(
//            keyName = "keybind",
//            name = "Screenshot Keybind",
//            description = "Add keybind to manually take a screenshot and send a message of your rare drop",
//            position = 4
//    )
//    default Keybind keybind()
//    {
//        return Keybind.NOT_SET;
//    }

    @ConfigItem(
            keyName = "webhook",
            name = "Default WebHook",
            description = "The webhook used to send messages to Discord.",
            position = 1
    )
    String webhook();

    @ConfigItem(
            keyName = "autoLog",
            name = "Automatic loot logging",
            description = "Log loot to Discord WebHook automatically?",
            position = 2
    )
    default boolean autoLog() {
        return true;
    }

    @ConfigItem(
            keyName = "autoWebHookToggle",
            name = "Use Separate WebHook?",
            description = "Use a secondary WebHook for Automatic Loot Logging, separate from your default one.",
            position = 3
    )
    default boolean autoWebHookToggle() {
        return false;
    }

    @ConfigItem(
            keyName = "autoWebHook",
            name = "Automatic WebHook",
            description = "Secondary WebHook for Automatic Loot Logging.",
            position = 4
    )
    String autoWebHook();

    @ConfigItem(
            keyName = "sendScreenshot",
            name = "Send Screenshot?",
            description = "Include a screenshot in the discord message?",
            position = 5
    )
    default boolean sendScreenshot() {
        return true;
    }

    @ConfigItem(
            keyName = "bingo",
            name = "Include Bingo String",
            description = "Add a bingo string to your screenshot's message",
            position = 6
    )
    default boolean includeBingo() {
        return false;
    }

    @ConfigItem(
            keyName = "bingoString",
            name = "Custom Bingo String",
            description = "Insert your custom Bingo String here.",
            position = 7
    )
    default String bingoString() {
        return "#ABC123";
    }

    @ConfigItem(
            keyName = "pets",
            name = "Include Pets",
            description = "Configures whether new pets will be automatically sent to discord",
            position = 8,
            section = whatToSendSection
    )
    default boolean includePets() {
        return true;
    }

    @ConfigItem(
            keyName = "valuableDrop",
            name = "Include Valuable drops",
            description = "Configures whether valuable drops will be automatically sent to discord.",
            position = 9,
            section = whatToSendSection
    )
    default boolean includeValuableDrops() {
        return true;
    }

    @ConfigItem(
            keyName = "valuableDropThreshold",
            name = "Valuable Drop Threshold",
            description = "The minimum value of a drop for it to send a discord message.",
            position = 10,
            section = whatToSendSection
    )
    default int valuableDropThreshold() {
        return 100000;
    }

    @ConfigItem(
            keyName = "collectionLogItem",
            name = "Include collection log items",
            description = "Configures whether a message will be automatically sent to discord when you obtain a new collection log item.",
            position = 11,
            section = whatToSendSection
    )
    default boolean includeCollectionLogItems() {
        return true;
    }

    @ConfigItem(
            keyName = "customField",
            name = "Custom Field Title",
            description = "",
            position = 38
    )
    default String customField() {
        return "";
    }

    @ConfigItem(
            keyName = "customValue",
            name = "Custom Field Value",
            description = "",
            position = 39
    )
    default String customValue() {
        return "";
    }

    @ConfigItem(
            keyName = "whiteListedRSNs",
            name = "Whitelisted RSNs",
            description = "(optional) Comma-separated list of RSNs which are allowed to post to the webhook, can prevent drops being posted from all your accounts",
            section = advancedSection,
            position = 60
    )
    default String whiteListedRSNs() {
        return "";
    }
    @ConfigItem(
            keyName = "raidLoot",
            name = "Include raid loot (Experimental)",
            description = "Configures whether a message will be automatically sent to discord when you obtain a raid unique.",
            position = 62,
            section = advancedSection
    )
    default boolean includeRaidLoot() {
        return true;
    }

//    @ConfigItem(
//            keyName = "partyNames",
//            name = "Experimental Party Integration",
//            description = "Get current party members in the Split Members Section of the Side Panel",
//            section = advancedSection,
//            position = 70
//    )
//    default boolean partyNames() {
//        return false;
//    }

}
