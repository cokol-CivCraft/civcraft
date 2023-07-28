package com.avrgaming.civcraft.config;


import org.bukkit.Material;

public class ConfigIngredient {

    //public static HashMap<String, ConfigIngredient> ingredientMap = new HashMap<String, ConfigIngredient>();

    public final Material type_id;
    public final int data;

    /* optional */
    public final String custom_id;
    public final int count;
    public final String letter;
    public final boolean ignore_data;

    public ConfigIngredient(Material typeId, int data, String customId, int count, String letter, boolean ignoreData) {
        type_id = typeId;
        this.data = data;
        custom_id = customId;
        this.count = count;
        this.letter = letter;
        ignore_data = ignoreData;
    }
}
