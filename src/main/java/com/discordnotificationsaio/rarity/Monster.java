package com.discordnotificationsaio.rarity;
import java.util.ArrayList;

public class Monster
{
	String id;
	ArrayList<Drop> drops;

	public Monster(String id)
	{
		this.id = id;
		drops = new ArrayList<>();
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public ArrayList<Drop> getDrops()
	{
		return drops;
	}

	public void addDrops( Drop drops)
	{
		this.drops.add(drops);
	}
}
