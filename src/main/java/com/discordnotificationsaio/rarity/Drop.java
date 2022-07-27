package com.discordnotificationsaio.rarity;

public class Drop {
String id;
String name;

String quantity;
String rarity;
String rolls;

public Drop ()
	{
	}

public String getId ()
	{
	return id;
	}

public void setId ( String id )
	{
	this.id = id;
	}

public String getQuantity ()
	{
	return quantity;
	}

public void setQuantity ( String id )
	{
	this.quantity = quantity;
	}

public String getName ()
	{
	return name;
	}

public void setName ( String name )
	{
	this.name = name;
	}

public String getRolls ()
	{
	return rolls;
	}

public void setRolls ( String rolls )
	{
	this.rolls = rolls;
	}

public String getRarity ()
	{
	return rarity;
	}

public void setRarity ( String rarity )
	{
	this.rarity = rarity;
	}
}
