package com.avrgaming.civcraft.config;


import org.bukkit.Material;

public class ConfigIngredient {

	//public static HashMap<String, ConfigIngredient> ingredientMap = new HashMap<String, ConfigIngredient>();
	
	public Material type_id;
	public int data;
	
	/* optional */
	public String custom_id;
	public int count = 1;
	public String letter;
	public boolean ignore_data;
	
}