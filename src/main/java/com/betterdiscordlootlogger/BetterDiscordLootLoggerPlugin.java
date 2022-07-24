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
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.input.KeyManager;
import net.runelite.client.party.PartyMember;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@PluginDescriptor(name = "Discord Split Tracker")
public class BetterDiscordLootLoggerPlugin extends Plugin {
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
	public BetterDiscordLootLoggerConfig config;
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

	public PartyMember partyMember;
	private String lastBossKill;
	private int lastBossKC = -1;
	private boolean shouldSendMessage;
	private boolean notificationStarted;
	@Getter(AccessLevel.PACKAGE)
	@Setter(AccessLevel.PACKAGE)
	private BetterDiscordLootLoggerPanel betterDiscordLootLoggerPanel;
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
		betterDiscordLootLoggerPanel = new BetterDiscordLootLoggerPanel(this, client);

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "balance.png");

		navButton = NavigationButton.builder().tooltip("Split Tracker").icon(icon).priority(5)
				.panel(betterDiscordLootLoggerPanel).build();

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
			shouldSendMessage = true;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		if (!client.getGameState().equals(GameState.LOGGED_IN)) {
			return;
		}
		if (Objects.equals(playerIconUrl, "")) {
			playerIconUrl = getPlayerIconUrl();
			colorCode = getColorCode();
			CompletableFuture.runAsync(() -> betterDiscordLootLoggerPanel.buildWomPanel());
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
					BetterDiscordLootLoggerPlugin.this.sendMessage(itemName,
							lastBossKC == -1 ? null
									: BetterDiscordLootLoggerPlugin.this.getKc(playerName, lastBossKill),
							npcName, Integer.toString(value), "NPC Loot", thumbnailUrl.get(), "", config.autoLog());
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
	public void onChatMessage(ChatMessage event) {
		if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM &&
				event.getType() != ChatMessageType.TRADE
				&& event.getType() != ChatMessageType.FRIENDSCHATNOTIFICATION) {
			return;
		}

		String message = event.getMessage();
		Matcher kcmatcher = KC_PATTERN.matcher(message);

		if (config.includePets() && PET_MESSAGES.stream().anyMatch(message::contains)) {
			sendMessage("", 0, "", "", "pet", "", "", config.autoLog());
		}
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
		//// sendMessage(valuableDropName, getKc(playerName,lastBossKill), "",
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
		// sendMessage(valuableDropName, null, "", valuableDropValueString, "Boss
		// Loot");
		// }
		// }

		if (config.includeCollectionLogItems() && message.startsWith(COLLECTION_LOG_TEXT) &&
				client.getVarbitValue(Varbits.COLLECTION_LOG_NOTIFICATION) == 1) {
			String entry = Text.removeTags(message).substring(COLLECTION_LOG_TEXT.length());
			sendMessage(entry, 0, "", "", "Collection Log", "", "", config.autoLog());
		}

		if (config.includeRaidLoot()) {
			if (message.startsWith(COX_DUST_MESSAGE_TEXT)) {
				final String dustRecipient = Text.removeTags(message).substring(COX_DUST_MESSAGE_TEXT.length());
				final String dropName = "Metamorphic dust";

				if (dustRecipient.equals(Text.sanitize(Objects.requireNonNull(client.getLocalPlayer().getName())))) {
					itemName = dropName;
				}
			}
			if (message.startsWith(COX_KIT_MESSAGE_TEXT)) {
				final String dustRecipient = Text.removeTags(message).substring(COX_KIT_MESSAGE_TEXT.length());
				final String dropName = "Twisted ancestral colour kit";

				if (dustRecipient.equals(Text.sanitize(Objects.requireNonNull(client.getLocalPlayer().getName())))) {
					itemName = dropName;
				}
			}

			Matcher tobUniqueMessage = TOB_UNIQUE_MESSAGE_PATTERN.matcher(message);
			if (tobUniqueMessage.matches()) {
				final String lootRecipient = Text.sanitize(tobUniqueMessage.group(1)).trim();
				final String dropName = tobUniqueMessage.group(2).trim();

				if (lootRecipient.equals(Text.sanitize(Objects.requireNonNull(client.getLocalPlayer().getName())))) {
					itemName = dropName;
				}
			}
		}
	}

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
	public void onScriptPreFired(ScriptPreFired scriptPreFired) {
		switch (scriptPreFired.getScriptId()) {
			case ScriptID.NOTIFICATION_START:
				notificationStarted = true;
				break;
			case ScriptID.NOTIFICATION_DELAY:
				if (!notificationStarted) {
					return;
				}
				String notificationTopText = client.getVarcStrValue(VarClientStr.NOTIFICATION_TOP_TEXT);
				String notificationBottomText = client.getVarcStrValue(VarClientStr.NOTIFICATION_BOTTOM_TEXT);
				if (notificationTopText.equalsIgnoreCase("Collection log") && config.includeCollectionLogItems()) {
					String entry = Text.removeTags(notificationBottomText).substring("New item:".length());
					sendMessage(entry, 0, "", "", "Collection Log", "", "", config.autoLog());
				}
				notificationStarted = false;
				break;
		}
	}

	@Provides
	BetterDiscordLootLoggerConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(BetterDiscordLootLoggerConfig.class);
	}

	public void sendMessage(String itemName, Integer bossKC, String npcName, String itemValue, String notificationType,
			String itemImageURL, String splitMembers, boolean send) {
		this.itemName = itemName;
		this.itemKc = bossKC;
		this.bossName = npcName;
		this.itemValue = itemValue;
		this.notificationType = notificationType;
		String codeBlocks = "";

		if (config.codeBlocks()) {
			codeBlocks = "`";
		} else
			codeBlocks = "";

		if (!shouldSendMessage) {
			return;
		}

		switch (notificationType) {
			case "Split Loot":
				break;
			case "Pet":
				itemName = "a new pet!";
				break;
			// case "valuable drop":
			// itemName = "a valuable drop: **" + itemName + "**!";
			// break;
			case "Collection Log":
				itemName = "a new collection log item: " + itemName + "!";
				break;
			case "NPC Loot":
				itemName = "a rare drop from " + npcName + ": " + itemName + "!";
				break;
			default:
				notificationType = "Manual Upload";
				itemName = "a screenshot";
				break;
		}

		String screenshotString = client.getLocalPlayer().getName();

		String valueMessage = null;
		if (!itemValue.isEmpty()) {
			screenshotString += " just received " + itemName;
			valueMessage = itemValue + " gp";
		} else if (!itemName.isEmpty()) {
			screenshotString += " just posted " + itemName;
		} else {
			screenshotString += " just received " + itemName;
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
				embedsObject.put("description", codeBlocks + "Value: " + valueMessage + codeBlocks);
			} else {
				embedsObject.put("description", codeBlocks + "Split Value: " + valueMessage + codeBlocks);
			}
		}

		JSONObject customField = new JSONObject();
		JSONObject bingoField = new JSONObject();
		JSONObject splitField = new JSONObject();

		if (config.includeBingo() && !Objects.equals(config.bingoString(), "")) {
			String bingoString = config.bingoString();

			bingoField.put("name", "Bingo String").put("value", bingoString).put("inline", true);
			fieldsArray.put(bingoField);
		}

		if (!config.customValue().equals("")) {
			customField.put("name", config.customField()).put("value", config.customValue()).put("inline", true);
			fieldsArray.put(customField);
		}

		if (!Objects.equals(splitMembers, "")) {
			splitField.put("name", "Split With").put("value", splitMembers).put("inline", true);
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
		CompletableFuture.runAsync(() -> sendWebhook(webhookObject.toString(), send));
		// System.out.println( webhookObject );
	}

	public void sendWebhook(String embedsObject, boolean send) {
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
				if (!send && betterDiscordLootLoggerPanel.before != null) {
					CompletableFuture.runAsync(
							() -> sendWebhookWithBuffer(url, requestBodyBuilder, betterDiscordLootLoggerPanel.before));
				} else {
					CompletableFuture.runAsync(() -> sendWebhookWithScreenshot(url, requestBodyBuilder));
				}
			} else {
				CompletableFuture.runAsync(() -> buildRequestAndSend(url, requestBodyBuilder));
			}
		}
	}

	public void sendWebhookWithScreenshot(HttpUrl url, MultipartBody.Builder requestBodyBuilder) {
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

	public void sendWebhookWithBuffer(HttpUrl url, MultipartBody.Builder requestBodyBuilder, BufferedImage screenshot) {

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
				image -> betterDiscordLootLoggerPanel.panelOverride(bossName, itemName, (BufferedImage) image));
	}

	private void resetState() {
		shouldSendMessage = false;
	}

}
