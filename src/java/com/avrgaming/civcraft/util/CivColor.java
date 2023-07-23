package com.avrgaming.civcraft.util;

import org.bukkit.ChatColor;

public class CivColor {
    /*
     * Takes an input from a yaml and converts 'Essentials' style color codes into
     * in game color codes.
     * XXX this is slow, so try not to do this at runtime. Just when configs load.
     */
    public static String colorize(String input) {
        String output = input;

        output = output.replaceAll("<red>", String.valueOf(ChatColor.DARK_RED));
        output = output.replaceAll("<rose>", String.valueOf(ChatColor.RED));
        output = output.replaceAll("<gold>", String.valueOf(ChatColor.GOLD));
        output = output.replaceAll("<yellow>", String.valueOf(ChatColor.YELLOW));
        output = output.replaceAll("<green>", String.valueOf(ChatColor.DARK_GREEN));
        output = output.replaceAll("<lightgreen>", String.valueOf(ChatColor.GREEN));
        output = output.replaceAll("<lightblue>", String.valueOf(ChatColor.AQUA));
        output = output.replaceAll("<blue>", String.valueOf(ChatColor.DARK_AQUA));
        output = output.replaceAll("<navy>", String.valueOf(ChatColor.DARK_BLUE));
        output = output.replaceAll("<darkpurple>", String.valueOf(ChatColor.BLUE));
        output = output.replaceAll("<lightpurple>", String.valueOf(ChatColor.LIGHT_PURPLE));
        output = output.replaceAll("<purple>", String.valueOf(ChatColor.DARK_PURPLE));
        output = output.replaceAll("<white>", String.valueOf(ChatColor.WHITE));
        output = output.replaceAll("<lightgray>", String.valueOf(ChatColor.GRAY));
        output = output.replaceAll("<gray>", String.valueOf(ChatColor.DARK_GRAY));
        output = output.replaceAll("<black>", String.valueOf(ChatColor.BLACK));
        output = output.replaceAll("<b>", String.valueOf(ChatColor.BOLD));
        output = output.replaceAll("<u>", String.valueOf(ChatColor.UNDERLINE));
        output = output.replaceAll("<i>", String.valueOf(ChatColor.ITALIC));
        output = output.replaceAll("<magic>", String.valueOf(ChatColor.MAGIC));
        output = output.replaceAll("<s>", String.valueOf(ChatColor.STRIKETHROUGH));
        output = output.replaceAll("<r>", String.valueOf(ChatColor.RESET));

        return output;
    }

    public static String valueOf(String color) {
        switch (color.toLowerCase()) {
            case "black":
                return String.valueOf(ChatColor.BLACK);
            case "navy":
                return String.valueOf(ChatColor.DARK_BLUE);
            case "green":
                return String.valueOf(ChatColor.DARK_GREEN);
            case "blue":
                return String.valueOf(ChatColor.DARK_AQUA);
            case "red":
                return String.valueOf(ChatColor.DARK_RED);
            case "purple":
                return String.valueOf(ChatColor.DARK_PURPLE);
            case "gold":
                return String.valueOf(ChatColor.GOLD);
            case "lightgray":
                return String.valueOf(ChatColor.GRAY);
            case "gray":
                return String.valueOf(ChatColor.DARK_GRAY);
            case "darkpurple":
                return String.valueOf(ChatColor.BLUE);
            case "lightgreen":
                return String.valueOf(ChatColor.GREEN);
            case "lightblue":
                return String.valueOf(ChatColor.AQUA);
            case "rose":
                return String.valueOf(ChatColor.RED);
            case "lightpurple":
                return String.valueOf(ChatColor.LIGHT_PURPLE);
            case "yellow":
                return String.valueOf(ChatColor.YELLOW);
            default:
                return String.valueOf(ChatColor.WHITE);
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
