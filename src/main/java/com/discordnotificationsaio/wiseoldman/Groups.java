package com.discordnotificationsaio.wiseoldman;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Groups{

	@SerializedName("score")
	@Expose
	private int score;

	@SerializedName("createdAt")
	@Expose
	private String createdAt;

	@SerializedName("homeworld")
	@Expose
	private Object homeworld;

	@SerializedName("clanChat")
	@Expose
	private String clanChat;

	@SerializedName("memberCount")
	@Expose
	private int memberCount;

	@SerializedName("name")
	@Expose
	private String name;

	@SerializedName("verified")
	@Expose
	private boolean verified;

	@SerializedName("description")
	@Expose
	private Object description;

	@SerializedName("id")
	@Expose
	private int id;

	@SerializedName("updatedAt")
	@Expose
	private String updatedAt;

	public void setScore(int score){
		this.score = score;
	}

	public int getScore(){
		return score;
	}

	public void setCreatedAt(String createdAt){
		this.createdAt = createdAt;
	}

	public String getCreatedAt(){
		return createdAt;
	}

	public void setHomeworld(Object homeworld){
		this.homeworld = homeworld;
	}

	public Object getHomeworld(){
		return homeworld;
	}

	public void setClanChat(String clanChat){
		this.clanChat = clanChat;
	}

	public String getClanChat(){
		return clanChat;
	}

	public void setMemberCount(int memberCount){
		this.memberCount = memberCount;
	}

	public int getMemberCount(){
		return memberCount;
	}

	public void setName(String name){
		this.name = name;
	}

	public String getName(){
		return name;
	}

	public void setVerified(boolean verified){
		this.verified = verified;
	}

	public boolean isVerified(){
		return verified;
	}

	public void setDescription(Object description){
		this.description = description;
	}

	public Object getDescription(){
		return description;
	}

	public void setId(int id){
		this.id = id;
	}

	public int getId(){
		return id;
	}

	public void setUpdatedAt(String updatedAt){
		this.updatedAt = updatedAt;
	}

	public String getUpdatedAt(){
		return updatedAt;
	}

	@Override
 	public String toString(){
		return 
			"Groups{" + 
			"score = '" + score + '\'' + 
			",createdAt = '" + createdAt + '\'' + 
			",homeworld = '" + homeworld + '\'' + 
			",clanChat = '" + clanChat + '\'' + 
			",memberCount = '" + memberCount + '\'' + 
			",name = '" + name + '\'' + 
			",verified = '" + verified + '\'' + 
			",description = '" + description + '\'' + 
			",id = '" + id + '\'' + 
			",updatedAt = '" + updatedAt + '\'' + 
			"}";
		}
}