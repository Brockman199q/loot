package com.betterdiscordlootlogger;

import com.google.gson.Gson;
import com.betterdiscordlootlogger.wiki.WikiItem;
import com.betterdiscordlootlogger.wiseoldman.Groups;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.IntStream;

public class ApiTools {

public static String getWikiIcon ( String itemName ) throws IOException, InterruptedException
	{
	String sURL =
			"https://oldschool.runescape.wiki/api.php?action=query&format=json&formatversion=2&prop=pageimages&titles=" +
			itemName.replace( " ", "_" ).replace( "%20", "_" );
	OkHttpClient client = new OkHttpClient();
	Request request = new Request.Builder().url( sURL ).build();
	String responseBody = Objects.requireNonNull( client.newCall( request ).execute().body() ).string();
	Gson g = new Gson();
	if ( ! responseBody.contains( "source" ) )
		{
		return "https://oldschool.runescape.wiki/images/Coins_10000.png";
		}
	WikiItem wikiItem = g.fromJson( responseBody, WikiItem.class );
//        System.out.println(wikiItem.getQuery().getPages().get(0).getThumbnail().getSource());
	return wikiItem.getQuery().getPages().get( 0 ).getThumbnail().getSource();
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
	if ( ! responseBody.contains( "displayName" ) )
		{
		return null;
		}
	JSONArray jsonArray = new JSONArray( responseBody );
//        System.out.println(responseBody);
//        System.out.println(Arrays.toString((IntStream.range(0, jsonArray.length())
//                .mapToObj(index -> ((JSONObject) jsonArray.get(index)).optString("displayName")).sorted()).toArray()));
	return (IntStream.range( 0, jsonArray.length() )
	                 .mapToObj( index -> ((JSONObject) jsonArray.get( index )).optString( "displayName" ) )
	                 .sorted()).toArray();
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