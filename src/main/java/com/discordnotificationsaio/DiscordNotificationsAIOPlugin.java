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
package com.discordnotificationsaio;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.input.KeyManager;
import net.runelite.client.party.PartyMember;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import net.runelite.http.api.loottracker.LootRecordType;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.runelite.api.widgets.WidgetID.QUEST_COMPLETED_GROUP_ID;
import static net.runelite.http.api.RuneLiteAPI.GSON;

@Slf4j
@PluginDescriptor(name = "Discord Notifications/Split Tracker")
public class DiscordNotificationsAIOPlugin extends Plugin {
	private static final String COLLECTION_LOG_TEXT = "New item added to your collection log: ";
	
	private static final Pattern KC_PATTERN = Pattern.compile(
			"Your (?<pre>completion count for |subdued |completed )?(?<boss>.+?) (?<post>(?:(?:kill|harvest|lap|completion) )?(?:count )?)is: <col=ff0000>(?<kc>\\d+)</col>");
	// private Map<String, String> lastValuableDropItems;
	private static final Map<String, String> KILLCOUNT_RENAMES = ImmutableMap.of("Barrows chest", "Barrows Chests");
	// private static final Pattern VALUABLE_DROP_PATTERN =
	// Pattern.compile(".*Valuable drop: ([^<>]+?\\(((?:\\d+,?)+)
	// coins\\))(?:</col>)?");
	private static final ImmutableList<String> PET_MESSAGES = ImmutableList.of(
			"You have a funny feeling like you're being followed",
			"You feel something weird sneaking into your backpack",
			"You have a funny feeling like you would have been followed");
	private static final String COX_DUST_MESSAGE_TEXT = "Dust recipients: ";
	private static final String COX_KIT_MESSAGE_TEXT = "Twisted Kit recipients: ";
	private static final Pattern TOB_UNIQUE_MESSAGE_PATTERN = Pattern.compile("(.+) found something special: (.+)");

	private static final Pattern QUEST_PATTERN_1 = Pattern.compile(".+?ve\\.*? (?<verb>been|rebuilt|.+?ed)? ?(?:the )?'?(?<quest>.+?)'?(?: [Qq]uest)?[!.]?$");
	
	private static final Pattern QUEST_PATTERN_2 = Pattern.compile("'?(?<quest>.+?)'?(?: [Qq]uest)? (?<verb>[a-z]\\w+?ed)?(?: f.*?)?[!.]?$");
	
	private static final ImmutableList<String> RFD_TAGS = ImmutableList.of("Another Cook", "freed", "defeated", "saved");
	
	private static final ImmutableList<String> WORD_QUEST_IN_NAME_TAGS = ImmutableList.of("Another Cook", "Doric", "Heroes", "Legends", "Observatory", "Olaf", "Waterfall");

	public String playerName;
	public String boss;
	public String itemName;
	public Integer itemKc;
	public String bossName;
	public String itemValue;
	public String notificationType;
	@Inject
	public Client client;
	@Inject
	public DiscordNotificationsAIOConfig config;
	@Inject
	public OkHttpClient okHttpClient;
	@Inject
	public KeyManager keyManager;

	// TODO: Include kc for the other notification types too
	// - Collection log entries
	// - Pets
	@Inject
	public DrawManager drawManager;
	@Inject
	public ConfigManager configManager;
	
	private ClientThread clientThread;

@Provides
	DiscordNotificationsAIOConfig provideConfig( ConfigManager configManager) {
	return configManager.getConfig( DiscordNotificationsAIOConfig.class);
	}

	public PartyMember partyMember;
	private String lastBossKill;
	private int lastBossKC = -1;

	private Hashtable<String, Integer> currentLevels;
	
	private ArrayList<String> leveledSkills;
	
	private boolean shouldSendLevelMessage = false;
	
	private boolean shouldSendQuestMessage = false;
	
	private boolean shouldSendClueMessage = false;

	private int ticksWaited = 0;
	private boolean shouldSendLootMessage;
	private boolean notificationStarted;
	@Getter(AccessLevel.PACKAGE)
	@Setter(AccessLevel.PACKAGE)
	private DiscordNotificationsAIOPanel discordNotificationsAIOPanel;
	@Inject
	private ItemManager itemManager;

	@Inject
	private ClientToolbar clientToolbar;

	private String playerIconUrl = "";

	private int colorCode = 0;
	private NavigationButton navButton;

	private static byte[] convertImageToByteArray(BufferedImage bufferedImage) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
		return byteArrayOutputStream.toByteArray();
	}

	private void setKc(String boss, int killcount) {
		configManager.setRSProfileConfiguration("killcount", boss.toLowerCase(), killcount);
	}

	private void unsetKc(String boss) {
		configManager.unsetRSProfileConfiguration("killcount", boss.toLowerCase());
	}

	public int getKc(String playerName, String boss) {
		this.playerName = playerName;
		this.boss = boss;
		Integer killCount = configManager.getRSProfileConfiguration("killcount", boss.toLowerCase(), int.class);
		return killCount == null ? 0 : killCount;
	}

	@Override
	protected void startUp() throws Exception {
	discordNotificationsAIOPanel = new DiscordNotificationsAIOPanel(this, client);

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "balance.png");
	
		currentLevels = new Hashtable<String, Integer>();
		
		leveledSkills = new ArrayList<String>();
		
		navButton = NavigationButton.builder().tooltip("Split Tracker").icon(icon).priority(5)
		                            .panel( discordNotificationsAIOPanel ).build();

		clientToolbar.addNavigation(navButton);

	}

	@Override
	protected void shutDown() throws Exception {
		notificationStarted = false;
		clientToolbar.removeNavigation(navButton);
	}

	@Subscribe
	public void onUsernameChanged(UsernameChanged usernameChanged) {
		resetState();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event) {
		if (event.getGameState().equals(GameState.LOGIN_SCREEN)) {
			resetState();
		} else {
			switch (client.getAccountType()) {
				case IRONMAN:
					playerIconUrl = "https://oldschool.runescape.wiki/images/0/09/Ironman_chat_badge.png";
				case HARDCORE_IRONMAN:
					playerIconUrl = "https://oldschool.runescape.wiki/images/b/b8/Hardcore_ironman_chat_badge.png";
				case ULTIMATE_IRONMAN:
					playerIconUrl = "https://oldschool.runescape.wiki/images/0/02/Ultimate_ironman_chat_badge.png";
				case GROUP_IRONMAN:
					playerIconUrl = "https://oldschool.runescape.wiki/images/Group_ironman_chat_badge.png";
				case HARDCORE_GROUP_IRONMAN:
					playerIconUrl = "https://oldschool.runescape.wiki/images/Hardcore_group_ironman_chat_badge.png";
				case NORMAL:
					playerIconUrl = "https://oldschool.runescape.wiki/images/thumb/Grand_Exchange_logo.png/225px-Grand_Exchange_logo.png?88cff";
				default:
					playerIconUrl = "";
			}
		shouldSendLootMessage = true;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		boolean didCompleteClue = client.getWidget( WidgetInfo.CLUE_SCROLL_REWARD_ITEM_CONTAINER) != null;
	
		if (!client.getGameState().equals(GameState.LOGGED_IN)) {
		return;
		}
		if (Objects.equals(playerIconUrl, "")) {
		playerIconUrl = getPlayerIconUrl();
		colorCode = getColorCode();
		CompletableFuture.runAsync(() -> discordNotificationsAIOPanel.buildWomPanel());
		}
		
		if (shouldSendClueMessage && didCompleteClue && config.includeClue())
			{
			shouldSendClueMessage = false;
			sendClueMessage();
			}
		if (
				shouldSendQuestMessage
				&& config.includeQuestComplete()
				&& client.getWidget(WidgetInfo.QUEST_COMPLETED_NAME_TEXT) != null
		) {
		shouldSendQuestMessage = false;
		String text = client.getWidget(WidgetInfo.QUEST_COMPLETED_NAME_TEXT).getText();
		String questName = parseQuestCompletedWidget(text);
		sendQuestMessage(questName);
		}
		if (!shouldSendLevelMessage)
			{
			return;
			}
		
		if (ticksWaited < 2)
			{
			ticksWaited++;
			return;
			}
		
		shouldSendLevelMessage = false;
		ticksWaited = 0;
		sendLevelMessage();

	}

	@Subscribe
	public void onStatChanged(net.runelite.api.events.StatChanged statChanged)
		{
		if (!config.includeLevelling())
			{
			return;
			}
		
		String skillName = statChanged.getSkill().getName();
		int newLevel = statChanged.getLevel();
		
		// .contains wasn't behaving so I went with == null
		Integer previousLevel = currentLevels.get(skillName);
		if (previousLevel == null || previousLevel == 0)
			{
			currentLevels.put(skillName, newLevel);
			return;
			}
		
		if (previousLevel != newLevel)
			{
			currentLevels.put(skillName, newLevel);
			
			// Certain activities can multilevel, check if any of the levels are valid for the message.
			for (int level = previousLevel + 1; level <= newLevel; level++)
				{
				if (shouldSendForThisLevel(level))
					{
					leveledSkills.add(skillName);
					shouldSendLevelMessage = true;
					break;
					}
				}
			}
		}



// private final List<PartyMember> members = new ArrayList<>();
	//
	// public PartyMember getMemberById(final long id) {
	// for (PartyMember member : members) {
	// if (id == member.getMemberId()) {
	// return member;
	// }
	// }
	//
	// return null;
	// }
	//
	// @Subscribe
	// public void onUserJoin(UserJoin message) {
	// PartyMember partyMember = getMemberById(message.getMemberId());
	// if (partyMember == null) {
	// partyMember = new PartyMember(message.getMemberId());
	// members.add(partyMember);
	// log.info("User {} joins party, {} members", partyMember.getMemberId(),
	// members.size());
	// }
	//
	// }

	@Subscribe
	public void onNpcLootReceived(NpcLootReceived event) throws IOException, InterruptedException,
			InvocationTargetException {
		if (isPlayerIgnored())
			return;
		if (!config.autoLog())
			return;
		NPC npc = event.getNpc();
		Collection<ItemStack> items = event.getItems();

		if (items.isEmpty() || npc == null) {
			return;
		}

		String npcName = npc.getName();

		items.forEach(itemStack -> {
			int itemId = itemStack.getId();
			int value = itemManager.getItemPrice(itemId) * itemStack.getQuantity();
			if (value >= config.valuableDropThreshold()) {
				String itemName = itemManager.getItemComposition(itemId).getName();
				dataToPanel(npcName, itemName);
				AtomicReference<String> thumbnailUrl = new AtomicReference<>("");
				CompletableFuture.runAsync(() -> {
					try {
						thumbnailUrl.set(ApiTools.getWikiIcon(itemName));
					} catch (IOException | InterruptedException e) {
						throw new RuntimeException(e);
					}
					sendLootMessage(itemName,
							lastBossKC == -1 ? null
									: getKc(playerName, lastBossKill),
							npcName, Integer.toString(value), "Loot Received", thumbnailUrl.get(), "", config.autoLog());
				});
			}
		});
		
		

		// lastValuableDropItems.forEach((name, value) ->
		// {
		// items.forEach(itemStack ->
		// {
		// String itemName =
		// itemManager.getItemComposition(itemStack.getId()).getName();
		// if (itemName == name)
		// {
		// itemStack.getId()
		// }
		// });
		// });
	}

	@Subscribe
	public void onLootReceived(LootReceived lootReceived)
		{
		if(isPlayerIgnored()) return;
		
		// Only process EVENTS such as Barrows, CoX etc. and PICKPOCKET
		// For NPCs onNpcLootReceived receives more information and is used instead.
		if (lootReceived.getType() == LootRecordType.NPC)
			{
			return;
			}
		String npcName = lootReceived.getName();
		
		Collection<ItemStack> items = lootReceived.getItems();
		List<CompletableFuture<Boolean>> futures = new ArrayList<>();
		
		items.forEach(itemStack -> {
		int itemId = itemStack.getId();
		int value = itemManager.getItemPrice(itemId) * itemStack.getQuantity();
		if (value >= config.valuableDropThreshold()) {
		String itemName = itemManager.getItemComposition(itemId).getName();
		dataToPanel(npcName, itemName);
		AtomicReference<String> thumbnailUrl = new AtomicReference<>("");
		CompletableFuture.runAsync(() -> {
		try {
		thumbnailUrl.set(ApiTools.getWikiIcon(itemName));
		} catch (IOException | InterruptedException e) {
		throw new RuntimeException(e);
		}
		sendLootMessage(itemName,
				lastBossKC == -1 ? null
				                 : getKc(playerName, lastBossKill),
				npcName, Integer.toString(value), "Loot Received", thumbnailUrl.get(), "", config.autoLog());
		});
		}
		});
		}
	
//	private CompletableFuture<ItemData> getLootReceivedItemData(String eventName, LootRecordType lootRecordType, int itemId){
//	CompletableFuture<ItemData> result = new CompletableFuture<>();
//
//	ItemData itemData = lootRecordType == LootRecordType.PICKPOCKET ?
//	                    rarityChecker.CheckRarityPickpocket(eventName, EnrichItem(itemId), itemManager) :
//	                    rarityChecker.CheckRarityEvent(eventName, EnrichItem(itemId), itemManager);
//
//	result.complete(itemData);
//	return result;
//	}
//
//	private CompletableFuture<ItemData> getNPCLootReceivedItemData(int npcId, int itemId, int quantity)
//		{
//		ItemData incomplete = EnrichItem(itemId);
//		return rarityChecker.CheckRarityNPC(npcId, incomplete, itemManager, quantity);
//		}


@Subscribe
	public void onChatMessage(ChatMessage event) {
		{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
			{
			return;
			}
		
		String chatMessage = event.getMessage();
		if (config.setPets() && PET_MESSAGES.stream().anyMatch(chatMessage::contains))
			{
			sendPetMessage();
			}
		if (config.setCollectionLogs() && chatMessage.startsWith(COLLECTION_LOG_TEXT) && client.getVarbitValue(Varbits.COLLECTION_LOG_NOTIFICATION) == 1)
			{
			String itemName = Text.removeTags(chatMessage).substring(COLLECTION_LOG_TEXT.length());
			sendCollectionLogMessage(itemName);
			}
		}

		String message = event.getMessage();
		Matcher kcmatcher = KC_PATTERN.matcher(message);
		
		final String playerName = client.getLocalPlayer().getName();

		if (config.includeValuableDrops()) {
			if (kcmatcher.find()) {
				lastBossKC = -1;

				final String boss = kcmatcher.group("boss");
				final int kc = Integer.parseInt(kcmatcher.group("kc"));
				final String pre = kcmatcher.group("pre");
				final String post = kcmatcher.group("post");

				if (Strings.isNullOrEmpty(pre) && Strings.isNullOrEmpty(post)) {
					unsetKc(boss);
				}

				String renamedBoss = KILLCOUNT_RENAMES.getOrDefault(boss, boss)
						// The config service doesn't support keys with colons in them
						.replace(":", "");
				if (!Objects.equals(boss, renamedBoss)) {
					// Unset old TOB kc
					unsetKc(boss);
					unsetKc(boss.replace(":", "."));
					// Unset old story mode
					unsetKc("Theatre of Blood Story Mode");
				}

				setKc(renamedBoss, kc);
				lastBossKill = renamedBoss;
				lastBossKC = kc;
			}
		}
		// {
		// Matcher matcher = VALUABLE_DROP_PATTERN.matcher(message);
		// if (matcher.matches())
		// {
		// int valuableDropValue = Integer.parseInt(matcher.group(2).replaceAll(",",
		// ""));
		// if (valuableDropValue >= config.valuableDropThreshold())
		// {
		// String[] valuableDrop = matcher.group(1).split(" \\(");
		// String valuableDropName = (String) Array.get(valuableDrop, 0);
		// String valuableDropValueString = matcher.group(2);
		//
		// itemManager.search(valuableDropName);
		//
		// lastValuableDropItems.put(valuableDropName, valuableDropValueString);
		//// sendLootMessage(valuableDropName, getKc(playerName,lastBossKill), "",
		// valuableDropValueString, "Boss Loot");
		// }
		// }
		// }
		// } else {
		// Matcher matcher = VALUABLE_DROP_PATTERN.matcher(message);
		// if (matcher.matches())
		// {
		// int valuableDropValue = Integer.parseInt(matcher.group(2).replaceAll(",",
		// ""));
		// if (valuableDropValue >= config.valuableDropThreshold())
		// {
		// String[] valuableDrop = matcher.group(1).split(" \\(");
		// String valuableDropName = (String) Array.get(valuableDrop, 0);
		// String valuableDropValueString = matcher.group(2);
		// sendLootMessage(valuableDropName, null, "", valuableDropValueString, "Boss
		// Loot");
		// }
		// }
	

		if (config.includeRaidLoot()) {
			if (message.startsWith(COX_DUST_MESSAGE_TEXT)) {
				final String dustRecipient = Text.removeTags(message).substring(COX_DUST_MESSAGE_TEXT.length());
				final String dropName = "Metamorphic dust";

				if (dustRecipient.equals(Text.sanitize(Objects.requireNonNull(client.getLocalPlayer().getName())))) {
					itemName = dropName;
					sendLootMessage( itemName, getKc( playerName, "cox cm" ), "Chambers of Xeric: Challenge Mode", "", "Loot Received", "https://oldschool.runescape.wiki/images/thumb/Metamorphic_dust_detail.png/150px-Metamorphic_dust_detail.png", "", true);
				}
			}
			if (message.startsWith(COX_KIT_MESSAGE_TEXT)) {
				final String dustRecipient = Text.removeTags(message).substring(COX_KIT_MESSAGE_TEXT.length());
				final String dropName = "Twisted ancestral colour kit";

				if (dustRecipient.equals(Text.sanitize(Objects.requireNonNull(client.getLocalPlayer().getName())))) {
					itemName = dropName;
					sendLootMessage( itemName, getKc( playerName, "Chambers of Xeric Challenge Mode" ), "Chambers of Xeric: Challenge Mode", "", "Loot Received", "https://oldschool.runescape.wiki/images/thumb/Metamorphic_dust_detail.png/150px-Metamorphic_dust_detail.png", "", true);
				}
			}

			Matcher tobUniqueMessage = TOB_UNIQUE_MESSAGE_PATTERN.matcher(message);
			if (tobUniqueMessage.matches()) {
				final String lootRecipient = Text.sanitize(tobUniqueMessage.group(1)).trim();
				final String dropName = tobUniqueMessage.group(2).trim();

				if (lootRecipient.equals(Text.sanitize(Objects.requireNonNull(client.getLocalPlayer().getName())))) {
					itemName = dropName;
				sendLootMessage( itemName, getKc( playerName, "Theatre of Blood" ), "Theatre of Blood", "", "Loot Received", "", "", true);
				}
			}
		}
	}

//	private ItemRarity EnrichItem( int itemId)
//		{
//		ItemRarity r = new ItemRarity();
//		r.ItemId = itemId;
//
//		if(log.isDebugEnabled()){
//		log.debug( MessageFormat.format("Item {0} prices HA{1}, GE{2}", itemId));
//		}
//
//		return r;
//		}
	
//	private CompletableFuture<Boolean> processNpcNotification(NPC npc, int itemId, int quantity, float rarity)
//		{
//		int npcId = npc.getId();
//		int npcCombatLevel = npc.getCombatLevel();
//		String npcName = npc.getName();
//
//		CompletableFuture<Boolean> f = new CompletableFuture<>();
//		clientThread.invokeLater(() -> {
//		sendLootMessage( itemManager.getItemComposition(itemId).getName(),getKc(playerName,
//				Objects.requireNonNull( npcName ) ),npcName,itemValue,"Loot Received","","",true );
//			});
//		return f;
//		}

	private boolean isPlayerIgnored() {
		if (config.whiteListedRSNs().trim().length() > 0) {
			String playerName = getPlayerName().toLowerCase();
			List<String> whiteListedRSNs = Arrays.asList(config.whiteListedRSNs().split(","));

			return whiteListedRSNs.stream().noneMatch(rsn -> rsn.length() > 0 && playerName.equals(rsn.toLowerCase()));
		}

		return false;
	}

	private String getPlayerIconUrl() {
		switch (client.getAccountType()) {
			case IRONMAN:
				return "https://oldschool.runescape.wiki/images/0/09/Ironman_chat_badge.png";
			case HARDCORE_IRONMAN:
				return "https://oldschool.runescape.wiki/images/b/b8/Hardcore_ironman_chat_badge.png";
			case ULTIMATE_IRONMAN:
				return "https://oldschool.runescape.wiki/images/0/02/Ultimate_ironman_chat_badge.png";
			case GROUP_IRONMAN:
				return "https://oldschool.runescape.wiki/images/Group_ironman_chat_badge.png";
			case HARDCORE_GROUP_IRONMAN:
				return "https://oldschool.runescape.wiki/images/Hardcore_group_ironman_chat_badge.png";
			case NORMAL:
				return "https://oldschool.runescape.wiki/images/thumb/Grand_Exchange_logo.png/225px-Grand_Exchange_logo.png?88cff";
			default:
				return "";
		}
	}

	private int getColorCode() {
		switch (client.getAccountType()) {
			case IRONMAN:
				return 3881787;
			case HARDCORE_IRONMAN:
				return 5832704;
			case ULTIMATE_IRONMAN:
				return 9342606;
			case GROUP_IRONMAN:
				return 6579;
			case HARDCORE_GROUP_IRONMAN:
				return 8454144;
			default:
				return 8817417;
		}
	}

	private String getPlayerName() {
		return client.getLocalPlayer().getName();
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired scriptPreFired)
		{
		switch (scriptPreFired.getScriptId())
			{
			case ScriptID.NOTIFICATION_START:
				notificationStarted = true;
				break;
			case ScriptID.NOTIFICATION_DELAY:
				if (!notificationStarted)
					{
					return;
					}
				String notificationTopText = client.getVarcStrValue(VarClientStr.NOTIFICATION_TOP_TEXT);
				String notificationBottomText = client.getVarcStrValue(VarClientStr.NOTIFICATION_BOTTOM_TEXT);
				if (notificationTopText.equalsIgnoreCase("Collection log") && config.setCollectionLogs())
					{
					String itemName = "**" + Text.removeTags(notificationBottomText).substring("New item:".length()) + "**";
					sendCollectionLogMessage(itemName);
					}
				notificationStarted = false;
				break;
			}
		}

	@Subscribe
	public void onActorDeath(ActorDeath actorDeath)
		{
		if (!config.includeDeath()) {
		return;
		}
		
		Actor actor = actorDeath.getActor();
		if (actor instanceof Player)
			{
			Player player = (Player) actor;
			if (player == client.getLocalPlayer())
				{
				sendDeathMessage();
				}
			}
		}

	private boolean shouldSendForThisLevel(int level)
		{
		return level >= config.minLevel()
		       && levelMeetsIntervalRequirement(level);
		}
	
	private boolean levelMeetsIntervalRequirement(int level) {
	int levelInterval = config.levelInterval();
	
	if (config.linearLevelMax() > 0) {
	levelInterval = (int) Math.max(Math.ceil(-.1*level + config.linearLevelMax()), 1);
	}
	
	return levelInterval <= 1
	       || level == 99
	       || level % levelInterval == 0;
	}
	
	private void sendQuestMessage(String questName)
		{
		String codeBlocks = "";
		String bold = "**";
		if (config.codeBlocks()) {
		codeBlocks = "`";
		bold = "";
		} else
			codeBlocks = "";
		
		String localName = bold + client.getLocalPlayer().getName() + bold;
		
		String questMessageString = config.questMessage().replaceAll("\\$name", codeBlocks + localName + codeBlocks)
		                                  .replaceAll("\\$quest", codeBlocks + questName + codeBlocks);
		
		NonLootWebhookBody nonLootWebhookBody = new NonLootWebhookBody();
		nonLootWebhookBody.setContent(questMessageString);
		sendNonLootWebhook( nonLootWebhookBody, config.sendQuestingScreenshot());
		}
	
	private void sendDeathMessage()
		{
		String codeBlocks = "";
		String bold = "**";
		if (config.codeBlocks()) {
		codeBlocks = "`";
		bold = "";
		} else
			codeBlocks = "";
		
		String localName = bold + client.getLocalPlayer().getName() + bold;
		
		String deathMessageString = config.deathMessage().replaceAll("\\$name", codeBlocks + localName + codeBlocks);
		
		NonLootWebhookBody nonLootWebhookBody = new NonLootWebhookBody();
		nonLootWebhookBody.setContent(deathMessageString);
		sendNonLootWebhook( nonLootWebhookBody, config.sendDeathScreenshot());
		}
	
	private void sendClueMessage()
		{
		String codeBlocks = "";
		String bold = "**";
		if (config.codeBlocks()) {
		codeBlocks = "`";
		bold = "";
		} else
			codeBlocks = "";
		
		String localName = bold + client.getLocalPlayer().getName() + bold;
		
		String clueMessage = config.clueMessage().replaceAll("\\$name", codeBlocks + localName + codeBlocks);
		
		NonLootWebhookBody nonLootWebhookBody = new NonLootWebhookBody();
		nonLootWebhookBody.setContent(clueMessage);
		sendNonLootWebhook( nonLootWebhookBody, config.sendClueScreenshot());
		}
	
	private void sendLevelMessage()
		{
		String codeBlocks = "";
		String bold = "**";
		if (config.codeBlocks()) {
		codeBlocks = "`";
		bold = "";
		} else
			codeBlocks = "";
		
		String localName = bold + client.getLocalPlayer().getName() + bold;
		
		String levelUpString = config.levelMessage().replaceAll("\\$name", codeBlocks + localName + codeBlocks);
		
		String[] skills = new String[leveledSkills.size()];
		skills = leveledSkills.toArray(skills);
		leveledSkills.clear();
		
		for (int i = 0; i < skills.length; i++)
			{
			if(i != 0) {
			levelUpString += config.andLevelMessage();
			}
			
			String fixed = levelUpString
					               .replaceAll("\\$skill", codeBlocks + skills[i] + codeBlocks)
					               .replaceAll("\\$level", codeBlocks + currentLevels.get(skills[i]).toString() + codeBlocks);
			
			levelUpString = fixed;
			}
		
		NonLootWebhookBody nonLootWebhookBody = new NonLootWebhookBody();
		nonLootWebhookBody.setContent(levelUpString);
		sendNonLootWebhook( nonLootWebhookBody, config.sendLevellingScreenshot());
		}
	
	private void sendPetMessage()
		{
		String codeBlocks = "";
		String bold = "**";
		if (config.codeBlocks()) {
		codeBlocks = "`";
		bold = "";
		} else
			codeBlocks = "";
		
		String localName = bold + client.getLocalPlayer().getName() + bold;
		
		String petMessageString = config.petMessage().replaceAll("\\$name", codeBlocks + localName + codeBlocks);
		
		NonLootWebhookBody nonLootWebhookBody = new NonLootWebhookBody();
		nonLootWebhookBody.setContent(petMessageString);
		sendNonLootWebhook( nonLootWebhookBody, config.sendPetScreenshot());
		}
	
	private void sendCollectionLogMessage(String itemName)
		{
		String codeBlocks = "";
		String bold = "**";
		if (config.codeBlocks()) {
		codeBlocks = "`";
		bold = "";
		} else
			codeBlocks = "";
		
		String localName = bold + client.getLocalPlayer().getName() + bold;
		
		String collectionLogMessageString = config.collectionLogMessage()
		                                          .replaceAll("\\$name", codeBlocks + localName + codeBlocks)
		                                          .replaceAll("\\$itemName", codeBlocks + itemName + codeBlocks);
		
		NonLootWebhookBody nonLootWebhookBody = new NonLootWebhookBody();
		nonLootWebhookBody.setContent(collectionLogMessageString);
		sendNonLootWebhook( nonLootWebhookBody, config.sendCollectionLogScreenshot());
		}

	public void sendLootMessage(String itemName, Integer bossKC, String npcName, String itemValue, String notificationType,
			String itemImageURL, String splitMembers, boolean send) {
		this.itemName = itemName;
		this.itemKc = bossKC;
		this.bossName = npcName;
		this.itemValue = itemValue;
		this.notificationType = notificationType;
	String codeBlocksGreenStart = "";
	String codeBlocksYellowStart = "";
	String codeBlocksOrangeStart = "";
	String codeBlocksRedStart = "";
	String codeBlocksEnd = "";
	String bold = "**";
	if (config.codeBlocks()) {
	codeBlocksGreenStart = "```glsl\n";
	codeBlocksYellowStart = "```fix\n";
	codeBlocksOrangeStart = "```elm\n";
	codeBlocksRedStart = "```diff\n";
	codeBlocksEnd = "\n```";
	bold = "";
	} else
		{
		codeBlocksGreenStart = "";
		codeBlocksYellowStart = "";
		codeBlocksOrangeStart = "";
		codeBlocksRedStart = "";
		codeBlocksEnd = "";
		}
	
	if (! shouldSendLootMessage ) {
	return;
	}
	
	switch (notificationType) {
	case "Split Loot":
		break;
	case "Loot Received":
		itemName = "a rare drop from " + npcName + ": " + itemName;
		break;
	default:
		notificationType = "Manual Upload";
		itemName = "a screenshot";
		break;
	}
	
	String screenshotString = client.getLocalPlayer().getName();
	
	String valueMessage = null;
	if (!itemValue.isEmpty()) {
	screenshotString += " just received " + itemName + "!";
	valueMessage = itemValue + " gp";
	} else if (!itemName.isEmpty()) {
	screenshotString += " just posted " + itemName + "!";
	} else {
	screenshotString += " just received " + itemName + "!";
	}
	SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
	String playerName = client.getLocalPlayer().getName();
	
	JSONObject webhookObject = new JSONObject();
	JSONArray embedsArray = new JSONArray();
	
	JSONObject embedsObject = new JSONObject();
	JSONObject authorObject = new JSONObject();
	authorObject.put("icon_url", playerIconUrl);
	embedsObject.put("author", authorObject);
	authorObject.put("name", notificationType + " - " + playerName);
	embedsObject.put("title", screenshotString);
	
	JSONObject thumbnailObject = new JSONObject();
	if (!Objects.equals(itemImageURL, "")) {
	thumbnailObject.put("url", itemImageURL);
	embedsObject.put("thumbnail", thumbnailObject);
	}
	embedsArray.put(embedsObject);
	webhookObject.put("embeds", embedsArray);
	JSONArray fieldsArray = new JSONArray();
	embedsObject.putOnce("fields", fieldsArray);
	embedsObject.putOnce("color", colorCode);
	JSONObject footerObject = new JSONObject();
	StringBuilder footerString = new StringBuilder(String.format("Date: %s", sdfDate.format(new Date())));
	
	if (!itemValue.isEmpty()) {
	if (!notificationType.equals("Split Loot")) {
	String descriptionText = codeBlocksGreenStart + bold + "Value: " + bold + valueMessage + codeBlocksEnd;
//	if (!rarity.equals(0.0f)) descriptionText = descriptionText + codeBlocksGreenStart + rarity + codeBlocksEnd;
	embedsObject.put("description", descriptionText);
	} else {
	String descriptionText = codeBlocksGreenStart + bold +"Split Value: " + bold + valueMessage + codeBlocksEnd;
//	if (!rarity.equals(0.0f)) descriptionText = descriptionText + codeBlocksGreenStart + rarity + codeBlocksEnd;
	embedsObject.put("description", descriptionText);
	}
	}
	
	JSONObject customField = new JSONObject();
	JSONObject bingoField = new JSONObject();
	JSONObject splitField = new JSONObject();
	
	if (config.includeBingo() && !Objects.equals(config.bingoString(), "")) {
	String bingoString = config.bingoString();
	
	bingoField.put("name", "Event String").put("value", codeBlocksGreenStart + "#" + bingoString + codeBlocksEnd).put("inline", true);
	fieldsArray.put(bingoField);
	}
	
	if (!config.customValue().equals("")) {
	customField.put("name", config.customField()).put("value", codeBlocksYellowStart + config.customValue() + codeBlocksEnd).put("inline", true);
	fieldsArray.put(customField);
	}
	
	if (!Objects.equals(splitMembers, "")) {
	splitField.put("name", "Split With").put("value", codeBlocksYellowStart + splitMembers + codeBlocksEnd).put("inline", true);
	fieldsArray.put(splitField);
	}

		if (!Objects.equals(npcName, "")) {
			try {
				String npcIconUrl = ApiTools.getWikiIcon((npcName));
				if (!(npcName.equals("")))
					footerString.append(" - ").append(npcName);
				if (!npcIconUrl.equals(""))
					footerObject.put("icon_url", npcIconUrl);
			} catch (IOException | InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		footerString.append(bossKC == null ? "" : " Kill Count: " + bossKC);

		footerObject.put("text", footerString);
		embedsObject.put("footer", footerObject);
		CompletableFuture.runAsync(() -> sendLootWebhook(webhookObject.toString(), send));
		// System.out.println( webhookObject );
	}

	public void sendLootWebhook(String embedsObject, boolean send) {
		String configUrl;
		if (send && config.autoLog() && config.autoWebHookToggle()) {
			configUrl = config.autoWebHook();
		} else if (!send && config.autoWebHookToggle()) {
			configUrl = config.webhook();
		} else if (Objects.equals(config.autoWebHook(), "")) {
			configUrl = config.webhook();
		} else {
			configUrl = config.webhook();
		}

		if (Strings.isNullOrEmpty(configUrl)) {
		} else {

			HttpUrl url = HttpUrl.parse(configUrl);
			MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM)
					.addFormDataPart("payload_json",
							embedsObject);

			if (config.sendScreenshot()) {
				if ( !send && discordNotificationsAIOPanel.before != null) {
					CompletableFuture.runAsync(
							() -> sendLootWebhookWithBuffer(url, requestBodyBuilder, discordNotificationsAIOPanel.before));
				} else {
					CompletableFuture.runAsync(() -> sendLootWebhookWithScreenshot(url, requestBodyBuilder));
				}
			} else {
				CompletableFuture.runAsync(() -> buildRequestAndSend(url, requestBodyBuilder));
			}
		}
	}

	public void sendLootWebhookWithScreenshot(HttpUrl url, MultipartBody.Builder requestBodyBuilder) {
		drawManager.requestNextFrameListener(image -> {
			BufferedImage bufferedImage = (BufferedImage) image;
			byte[] imageBytes;
			try {
				imageBytes = convertImageToByteArray(bufferedImage);
			} catch (IOException e) {
				log.warn("Error converting image to byte array", e);
				return;
			}

			requestBodyBuilder.addFormDataPart("file", "image.png",
					RequestBody.create(MediaType.parse("image/png"), imageBytes));
			CompletableFuture.runAsync(() -> buildRequestAndSend(url, requestBodyBuilder));

		});
	}

	public void sendLootWebhookWithBuffer(HttpUrl url, MultipartBody.Builder requestBodyBuilder, BufferedImage screenshot) {

		byte[] imageBytes;
		try {
			imageBytes = convertImageToByteArray(screenshot);
		} catch (IOException e) {
			log.warn("Error converting image to byte array", e);
			return;
		}

		requestBodyBuilder.addFormDataPart("file", "image.png",
				RequestBody.create(MediaType.parse("image/png"), imageBytes));
		buildRequestAndSend(url, requestBodyBuilder);
	}

	private void sendNonLootWebhook( NonLootWebhookBody discordWebhookBody, boolean sendScreenshot)
		{
		String configUrl;
		if (config.autoLog() && config.autoWebHookToggle()) {
		configUrl = config.autoWebHook();
		} else if (config.autoWebHookToggle()) {
		configUrl = config.webhook();
		} else if (Objects.equals(config.autoWebHook(), "")) {
		configUrl = config.webhook();
		} else {
		configUrl = config.webhook();
		}
		
		if (Strings.isNullOrEmpty(configUrl)) {
		} else {
		
		HttpUrl url = HttpUrl.parse(configUrl);
		MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
				                                           .setType(MultipartBody.FORM)
				                                           .addFormDataPart("payload_json", GSON.toJson(discordWebhookBody));
		
		if (sendScreenshot)
			{
			sendNonLootWebhookWithScreenshot(url, requestBodyBuilder);
			}
		else
			{
			buildRequestAndSend(url, requestBodyBuilder);
			}
		}}
	
	private void sendNonLootWebhookWithScreenshot(HttpUrl url, MultipartBody.Builder requestBodyBuilder)
		{
		drawManager.requestNextFrameListener(image ->
			{
			BufferedImage bufferedImage = (BufferedImage) image;
			byte[] imageBytes;
			try
				{
				imageBytes = convertImageToByteArray(bufferedImage);
				}
			catch (IOException e)
				{
				log.warn("Error converting image to byte array", e);
				return;
				}
			
			requestBodyBuilder.addFormDataPart("file", "image.png",
					RequestBody.create(MediaType.parse("image/png"), imageBytes));
			buildRequestAndSend(url, requestBodyBuilder);
			});
		}

	private void buildRequestAndSend(HttpUrl url, MultipartBody.Builder requestBodyBuilder) {
		RequestBody requestBody = requestBodyBuilder.build();
		Request request = new Request.Builder().url(url).post(requestBody).build();
		sendRequest(request);
		// System.out.println(request);
	}

	private void sendRequest(Request request) {
		okHttpClient.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(@NotNull Call call, @NotNull IOException e) {
				log.debug("Error submitting webhook", e);
			}

			@Override
			public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
				response.close();
			}
		});
	}

	public void dataToPanel(String bossName, String itemName) {
		drawManager.requestNextFrameListener(
				image -> discordNotificationsAIOPanel.panelOverride(bossName, itemName, (BufferedImage) image));
	}

	private void resetState() {
	currentLevels.clear();
	leveledSkills.clear();
	shouldSendLevelMessage = false;
	shouldSendQuestMessage = false;
	shouldSendClueMessage = false;
	shouldSendLootMessage = false;
	ticksWaited = 0;
	}

	static String parseQuestCompletedWidget(final String text)
		{
		// "You have completed The Corsair Curse!"
		final Matcher questMatch1 = QUEST_PATTERN_1.matcher(text);
		// "'One Small Favour' completed!"
		final Matcher questMatch2 = QUEST_PATTERN_2.matcher(text);
		final Matcher questMatchFinal = questMatch1.matches() ? questMatch1 : questMatch2;
		if (!questMatchFinal.matches())
			{
			return "Unable to find quest name!";
			}
		
		String quest = questMatchFinal.group("quest");
		String verb = questMatchFinal.group("verb") != null ? questMatchFinal.group("verb") : "";
		
		if (verb.contains("kind of"))
			{
			quest += " partial completion";
			}
		else if (verb.contains("completely"))
			{
			quest += " II";
			}
		
		if (RFD_TAGS.stream().anyMatch((quest + verb)::contains))
			{
			quest = "Recipe for Disaster - " + quest;
			}
		
		if (WORD_QUEST_IN_NAME_TAGS.stream().anyMatch(quest::contains))
			{
			quest += " Quest";
			}
		
		return quest;
		}
	
	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
		{
		int groupId = event.getGroupId();
		
		if (groupId == QUEST_COMPLETED_GROUP_ID) {
		shouldSendQuestMessage = true;
		}
		
		if ( groupId == WidgetID.CLUE_SCROLL_REWARD_GROUP_ID) {
		shouldSendClueMessage = true;
		}
		}

}
