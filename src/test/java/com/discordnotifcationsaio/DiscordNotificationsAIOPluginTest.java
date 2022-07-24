package com.discordnotifcationsaio;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class DiscordNotificationsAIOPluginTest {
	public static void main(String[] args) throws Exception {
		ExternalPluginManager.loadBuiltin( DiscordNotificationsAIOPlugin.class);
		RuneLite.main(args);
	}
}