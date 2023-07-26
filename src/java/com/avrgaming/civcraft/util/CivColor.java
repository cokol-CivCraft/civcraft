package com.avrgaming.civcraft.util;

import org.bukkit.ChatColor;

public class CivColor {

    public static final String Black = "§0";
    public static final String Navy = "§1";
    public static final String Green = "§2";
    public static final String Blue = "§3";
    public static final String Red = "§4";
    public static final String Purple = "§5";
    public static final String Gold = "§6";
    public static final String LightGray = "§7";
    public static final String Gray = "§8";
    public static final String DarkPurple = "§9";
    public static final String LightGreen = "§a";
    public static final String LightBlue = "§b";
    public static final String Rose = "§c";
    public static final String LightPurple = "§d";
    public static final String Yellow = "§e";
    public static final String White = "§f";
    public static final String BOLD = String.valueOf(ChatColor.BOLD);
    public static final String ITALIC = String.valueOf(ChatColor.ITALIC);
    public static final String MAGIC = String.valueOf(ChatColor.MAGIC);
    public static final String STRIKETHROUGH = String.valueOf(ChatColor.STRIKETHROUGH);
    public static final String RESET = String.valueOf(ChatColor.RESET);
    public static final String UNDERLINE = String.valueOf(ChatColor.UNDERLINE);


    /*
     * Takes an input from a yaml and converts 'Essentials' style color codes into
     * in game color codes.
     * XXX this is slow, so try not to do this at runtime. Just when configs load.
     */
    public static String colorize(String input) {
        String output = input;
		
		output = output.replaceAll("<red>", Red);
		output = output.replaceAll("<rose>", Rose);
		output = output.replaceAll("<gold>", Gold);
		output = output.replaceAll("<yellow>", Yellow);
		output = output.replaceAll("<green>", Green);
		output = output.replaceAll("<lightgreen>", LightGreen);
        output = output.replaceAll("<lightblue>", LightBlue);
        output = output.replaceAll("<blue>", Blue);
        output = output.replaceAll("<navy>", Navy);
        output = output.replaceAll("<darkpurple>", DarkPurple);
        output = output.replaceAll("<lightpurple>", LightPurple);
        output = output.replaceAll("<purple>", Purple);
        output = output.replaceAll("<white>", White);
        output = output.replaceAll("<lightgray>", LightGray);
        output = output.replaceAll("<gray>", Gray);
        output = output.replaceAll("<black>", Black);
        output = output.replaceAll("<b>", String.valueOf(ChatColor.BOLD));
        output = output.replaceAll("<u>", String.valueOf(ChatColor.UNDERLINE));
        output = output.replaceAll("<i>", String.valueOf(ChatColor.ITALIC));
        output = output.replaceAll("<magic>", String.valueOf(ChatColor.MAGIC));
        output = output.replaceAll("<s>", String.valueOf(ChatColor.STRIKETHROUGH));
        output = output.replaceAll("<r>", String.valueOf(ChatColor.RESET));

        return output;
    }
	
	public static String strip(String line) {

		for (ChatColor cc : ChatColor.values())
			line.replaceAll(cc.toString(), "");
		return line;
	}

	public static String valueOf(String color) {
		switch (color.toLowerCase()) {
		case "black":
			return Black;
		case "navy":
			return Navy;
		case "green":
			return Green;
		case "blue":
			return Blue;
		case "red":
			return Red;
		case "purple":
			return Purple;
		case "gold":
			return Gold;
		case "lightgray":
			return LightGray;
		case "gray":
			return Gray;
		case "darkpurple":
			return DarkPurple;
		case "lightgreen":
			return LightGreen;
		case "lightblue":
			return LightBlue;
		case "rose":
			return Rose;
		case "lightpurple":
			return LightPurple;
		case "yellow":
			return Yellow;
		case "white":
			return White;
		default:
			return White;
		}		
	}

	public static String stripTags(String input) {
		String output = input;
		
		output = output.replaceAll("<red>", "");
		output = output.replaceAll("<rose>", "");
		output = output.replaceAll("<gold>", "");
		output = output.replaceAll("<yellow>", "");
		output = output.replaceAll("<green>", "");
		output = output.replaceAll("<lightgreen>", "");
		output = output.replaceAll("<lightblue>", "");
		output = output.replaceAll("<blue>", "");
		output = output.replaceAll("<navy>", "");
		output = output.replaceAll("<darkpurple>", "");
		output = output.replaceAll("<lightpurple>", "");
		output = output.replaceAll("<purple>", "");
		output = output.replaceAll("<white>", "");
		output = output.replaceAll("<lightgray>", "");
		output = output.replaceAll("<gray>", "");
		output = output.replaceAll("<black>", "");
		output = output.replaceAll("<b>", "");
		output = output.replaceAll("<u>", "");
		output = output.replaceAll("<i>", "");
		output = output.replaceAll("<magic>", "");
		output = output.replaceAll("<s>", "");
		output = output.replaceAll("<r>", "");
		
		return output;
	}
	
}
