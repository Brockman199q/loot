package com.discordnotificationsaio;

import com.discordnotificationsaio.rarity.Drop;
import com.discordnotificationsaio.rarity.Monster;
import com.discordnotificationsaio.wiki.WikiItem;
import com.discordnotificationsaio.wiseoldman.Groups;

import com.google.gson.Gson;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

public class ApiTools {

	@Inject
	private static OkHttpClient client;

	@Inject
	private static Gson gson;

public static String getWikiIcon ( String itemName ) throws IOException, InterruptedException
	{
	String sURL =
			"https://oldschool.runescape.wiki/api.php?action=query&format=json&formatversion=2&prop=pageimages&titles=" +
			itemName.replace( " ", "_" ).replace( "%20", "_" );
	Request request = new Request.Builder()
			.url( sURL )
			.header("User-Agent", "skyhawkgaming/better-discord-loot-logger")
			.build();
	CompletableFuture<String> icon = new CompletableFuture<>();
	CompletableFuture.supplyAsync( () ->
		{
		client.newCall( request ).enqueue( new Callback() {
			@Override
			public void onFailure ( @NotNull Call call, @NotNull IOException e )
				{
				
				icon.completeExceptionally( e );
				}
			
			@Override
			public void onResponse ( @NotNull Call call, @NotNull Response response ) throws IOException
				{
				String responseBody = Objects.requireNonNull( response.body() ).string();
				
					if ( responseBody.contains( "source" ) )
						{
						WikiItem wikiItem = gson.fromJson( responseBody, WikiItem.class );
//						System.out.println(wikiItem.getQuery().getPages().get( 0 ).getThumbnail().getSource());
						String wikiIcon = wikiItem.getQuery().getPages().get( 0 ).getThumbnail().getSource();
						icon.complete( wikiIcon );
						response.close();
						}
					}
				
		} );
		// System.out.println( icon.getNow( "failed https://oldschool.runescape.wiki/images/Coins_10000.png" ) );
		return icon;
		});
	try
		{
		return icon.get();
		}
	catch (ExecutionException e)
		{
		throw new RuntimeException( e );
		}
	}


public static Object[] getWomGroupIds ( String playerName ) throws IOException, InterruptedException
	{
	String compUrl =
			"https://api.wiseoldman.net/players/username/" + playerName.replace( " ", "_" ).replace( "%20", "_" ) +
			"/competitions";
	Request request = new Request.Builder().url( compUrl ).build();
	String responseBody = Objects.requireNonNull( client.newCall( request ).execute().body() ).string();
	if ( ! responseBody.contains( "groupId" ) )
		{
		return null;
		}
	JSONArray jsonArray = new JSONArray( responseBody );
//        System.out.println(responseBody);
//        System.out.println(Arrays.toString(IntStream.range(0, jsonArray.length())
//                .mapToObj(index -> ((JSONObject) jsonArray.get(index)).optString("groupId")).distinct().sorted().toArray()));
	return (IntStream.range( 0, jsonArray.length() )
	                 .mapToObj( index -> ((JSONObject) jsonArray.get( index )).optString( "groupId" ) ).distinct()
	                 .sorted()).toArray();
	}

public static Object[] getGroupMembers ( int groupId ) throws IOException, InterruptedException
	{
	String groupUrl = "https://api.wiseoldman.net/groups/" + groupId + "/members";
	Request request = new Request.Builder().url( groupUrl ).build();
	String responseBody = Objects.requireNonNull( client.newCall( request ).execute().body() ).string();
	if ( ! responseBody.contains( "username" ) )
		{
		return null;
		}
	JSONArray jsonArray = new JSONArray( responseBody );
//        System.out.println(responseBody);
//        System.out.println(Arrays.toString((IntStream.range(0, jsonArray.length())
//                .mapToObj(index -> ((JSONObject) jsonArray.get(index)).optString("displayName")).sorted()).toArray()));
	//	System.out.println( displayNames );
	return (IntStream.range( 0, jsonArray.length() )
	                 .mapToObj( index -> ((JSONObject) jsonArray.get( index )).optString( "displayName" ) )
	                 .sorted( String.CASE_INSENSITIVE_ORDER )).toArray( String[]::new );
	}

public static String getClanName ( int groupId ) throws IOException, InterruptedException
	{
	String groupUrl = String.format( "https://api.wiseoldman.net/groups/%d", groupId );
	Request request = new Request.Builder().url( groupUrl ).build();
	String responseBody = Objects.requireNonNull( client.newCall( request ).execute().body() ).string();
	if ( ! responseBody.contains( "name" ) )
		{
		return null;
		}
//        System.out.println(responseBody);
	Groups resJson = gson.fromJson( responseBody, Groups.class );
//        System.out.println(resJson.getName());
	return resJson.getName();
	}
	

public static String getItemRarity (ArrayList<Monster> mobs, String npcName, String itemName) throws IOException
	{
	
	final String[] rarity = new String[1];
	Optional<Monster> killed = mobs.stream().filter( monster -> monster.getId().equals(npcName)).findFirst();
	killed.ifPresent(k ->
		{
		Optional<Drop> dropped = k.getDrops().stream().filter( drop -> drop.getName().equals(itemName)).findFirst();
//		dropped.ifPresent(drop -> System.out.println("\n\nRarity of " + drop.getName() + " from " + npcName + " is " + drop.getRarity()));
		dropped.ifPresent( drop -> rarity[0] = (drop.getRarity()));
		if( ! Objects.equals( rarity[0], "Always" ) ) rarity[0]=rarity[0].split("/")[1];
		});
	if (rarity[0] == null) return "";
	return rarity[0];
	}

}
