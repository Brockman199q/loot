package com.discordnotifcationsaio;

import com.discordnotifcationsaio.wiki.WikiItem;
import com.discordnotifcationsaio.wiseoldman.Groups;
import com.google.gson.Gson;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

public class ApiTools {

public static String getWikiIcon ( String itemName ) throws IOException, InterruptedException
	{
	String sURL =
			"https://oldschool.runescape.wiki/api.php?action=query&format=json&formatversion=2&prop=pageimages&titles=" +
			itemName.replace( " ", "_" ).replace( "%20", "_" );
	OkHttpClient client = new OkHttpClient();
	Request request = new Request.Builder().url( sURL ).build();
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
				Gson g = new Gson();
				
					if ( responseBody.contains( "source" ) )
						{
						WikiItem wikiItem = g.fromJson( responseBody, WikiItem.class );
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
	OkHttpClient client = new OkHttpClient();
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
	OkHttpClient client = new OkHttpClient();
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
	OkHttpClient client = new OkHttpClient();
	Request request = new Request.Builder().url( groupUrl ).build();
	String responseBody = Objects.requireNonNull( client.newCall( request ).execute().body() ).string();
	if ( ! responseBody.contains( "name" ) )
		{
		return null;
		}
//        System.out.println(responseBody);
	Gson g = new Gson();
	Groups resJson = g.fromJson( responseBody, Groups.class );
//        System.out.println(resJson.getName());
	return resJson.getName();
	}
}