/*
 * AVRGAMING LLC
 * __________________
 *
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.main;

import com.avrgaming.civcraft.arena.Arena;
import com.avrgaming.civcraft.arena.ArenaTeam;
import com.avrgaming.civcraft.camp.Camp;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.Reflection;
import com.connorlinfoot.titleapi.TitleAPI;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CivMessage {

    /* Stores the player name and the hash code of the last message sent to prevent error spamming the player. */
    private static final HashMap<String, Integer> lastMessageHashCode = new HashMap<>();

    /* Indexed off of town names, contains a list of extra people who listen to town chats.(mostly for admins to listen to towns) */
    private static final Map<String, ArrayList<String>> extraTownChatListeners = new ConcurrentHashMap<>();

    /* Indexed off of civ names, contains a list of extra people who listen to civ chats. (mostly for admins to list to civs) */
    private static final Map<String, ArrayList<String>> extraCivChatListeners = new ConcurrentHashMap<>();

    public static void sendErrorNoRepeat(Object sender, String line) {
        if (sender instanceof Player player) {

            Integer hashcode = lastMessageHashCode.get(player.getName());
            if (hashcode != null && hashcode == line.hashCode()) {
                return;
            }

            lastMessageHashCode.put(player.getName(), line.hashCode());
        }

        send(sender, ChatColor.RED + line);
    }

    public static void sendError(Object sender, String line) {
        send(sender, ChatColor.RED + line);
    }

    /*
     * Sends message to playerName(if online) AND console.
     */
    public static void console(String playerName, String line) {
        try {
            Player player = CivGlobal.getPlayer(playerName);
            send(player, line);
        } catch (CivException ignored) {
        }
        CivLog.info(line);
    }

    public static void sendActionBar(Player p, String actionbar) {
        if (CivSettings.hasTitleAPI) {
            if (p != null) {
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(actionbar));
            }
        }
        // send(sender, actionbar); (отправляет хуйню)
    }

    public static void sendTitle(Object sender, int fadeIn, int show, int fadeOut, String title, String subTitle) {
        if (CivSettings.hasTitleAPI) {
            Player player = null;
            Resident resident = null;
            if ((sender instanceof Player)) {
                player = (Player) sender;
                resident = CivGlobal.getResident(player);
            } else if (sender instanceof Resident) {
                try {
                    resident = (Resident) sender;
                    player = CivGlobal.getPlayer(resident);
                } catch (CivException e) {
                    // No player online
                }
            }
            if (player != null && resident != null && resident.isTitleAPI()) {
                TitleAPI.sendTitle(player, fadeIn, show, fadeOut, title, subTitle);
            }
        }
        send(sender, title);
        if (subTitle != "") {
            send(sender, subTitle);
        }
    }


    public static void sendTitle(Object sender, String title, String subTitle) {
        sendTitle(sender, 10, 40, 5, title, subTitle);
    }

    public static void send(Object sender, String line) {
        if ((sender instanceof Player)) {
            ((Player) sender).sendMessage(line);
        } else if (sender instanceof CommandSender) {
            ((CommandSender) sender).sendMessage(line);
        } else if (sender instanceof Resident) {
            try {
                CivGlobal.getPlayer(((Resident) sender)).sendMessage(line);
            } catch (CivException e) {
                // No player online
            }
        }
    }

    public static String itemTooltip(ItemStack itemStack) {
        try {
            Object nmsItem = Reflection.getMethod(Reflection.getOBCClass("inventory.CraftItemStack"), "asNMSCopy", new Class[]{ItemStack.class}).invoke(null, itemStack);
            return (Reflection.getMethod(Reflection.getNMSClass("ItemStack"), "save", new Class[]{Reflection.getNMSClass("NBTTagCompound")}).invoke(nmsItem, Reflection.getNMSClass("NBTTagCompound").newInstance()).toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void send(Object sender, String line, ItemStack item) {
        if ((sender instanceof Player p)) {
            TextComponent msg = new TextComponent(line);
            msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new ComponentBuilder(itemTooltip(item)).create()));

            p.spigot().sendMessage(msg);
        } else if (sender instanceof CommandSender) {

            ((CommandSender) sender).sendMessage(line);
        } else if (sender instanceof Resident) {
            try {
                Player p = CivGlobal.getPlayer(((Resident) sender));
                TextComponent msg = new TextComponent(line);
                msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new ComponentBuilder(itemTooltip(item)).create()));

                p.spigot().sendMessage(msg);
            } catch (CivException e) {
                // No player online
            }
        }
    }

    public static void send(Object sender, String[] lines) {
        boolean isPlayer = sender instanceof Player;

        for (String line : lines) {
            if (isPlayer) {
                ((Player) sender).sendMessage(line);
            } else {
                ((CommandSender) sender).sendMessage(line);
            }
        }
    }

    public static String buildTitle(String title) {
        String line = "-------------------------------------------------";
        String titleBracket = "[ " + ChatColor.YELLOW + title + ChatColor.AQUA + " ]";

        if (titleBracket.length() > line.length()) {
            return ChatColor.AQUA + "-" + titleBracket + "-";
        }

        int min = (line.length() / 2) - titleBracket.length() / 2;
        int max = (line.length() / 2) + titleBracket.length() / 2;

        String out = ChatColor.AQUA + line.substring(0, Math.max(0, min));
        out += titleBracket + line.substring(max);

        return out;
    }

    public static String buildSmallTitle(String title) {
        String line = ChatColor.AQUA + "------------------------------";

        String titleBracket = "[ " + title + " ]";

        int min = (line.length() / 2) - titleBracket.length() / 2;
        int max = (line.length() / 2) + titleBracket.length() / 2;

        String out = ChatColor.AQUA + line.substring(0, Math.max(0, min));
        out += titleBracket + line.substring(max);

        return out;
    }

    public static void sendSubHeading(CommandSender sender, String title) {
        send(sender, buildSmallTitle(title));
    }

    public static void sendHeading(Resident resident, String title) {
        Player player;
        try {
            player = CivGlobal.getPlayer(resident);
            sendHeading(player, title);
        } catch (CivException ignored) {
        }
    }

    public static void sendHeading(CommandSender sender, String title) {
        send(sender, buildTitle(title));
    }

    public static void sendSuccess(CommandSender sender, String message) {
        send(sender, ChatColor.GREEN + message);
    }

    public static void global(String string) {
        CivLog.info("[Global] " + string);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(ChatColor.AQUA + CivSettings.localize.localizedString("civMsg_Globalprefix") + " " + ChatColor.WHITE + string);
        }
    }

    public static void globalTitle(String title, String subTitle) {
        CivLog.info("[GlobalTitle] " + title + " - " + subTitle);
        for (Player player : Bukkit.getOnlinePlayers()) {
            Resident resident = CivGlobal.getResident(player);
            if (CivSettings.hasTitleAPI && resident.isTitleAPI()) {
                CivMessage.sendTitle(player, 10, 60, 10, title, subTitle);
            } else {
                send(player, buildTitle(title));
                if (!subTitle.isEmpty()) {
                    send(player, subTitle);
                }
            }
        }
    }

    public static void globalHeading(String string) {
        CivLog.info("[GlobalHeading] " + string);
        for (Player player : Bukkit.getOnlinePlayers()) {
            send(player, buildTitle(string));
        }
    }

    public static void sendScout(Civilization civ, String string) {
        CivLog.info("[Scout:" + civ.getName() + "] " + string);
        for (Town t : civ.getTowns()) {
            for (Resident resident : t.getResidents()) {
                if (!resident.isShowScout()) {
                    continue;
                }

                Player player;
                try {
                    player = CivGlobal.getPlayer(resident);
                    CivMessage.send(player, ChatColor.DARK_PURPLE + CivSettings.localize.localizedString("civMsg_ScoutPrefix") + " " + ChatColor.WHITE + string);
                } catch (CivException ignored) {
                }
            }

        }
    }

    public static void sendTown(Town town, String string) {
        CivLog.info("[Town:" + town.getName() + "] " + string);

        for (Resident resident : town.getResidents()) {
            if (!resident.isShowTown()) {
                continue;
            }

            Player player;
            try {
                player = CivGlobal.getPlayer(resident);
                CivMessage.send(player, ChatColor.GOLD + CivSettings.localize.localizedString("civMsg_Townprefix") + " " + ChatColor.WHITE + string);
            } catch (CivException ignored) {
            }
        }
    }

    public static void sendCiv(Civilization civ, String string) {
        CivLog.info("[Civ:" + civ.getName() + "] " + string);
        for (Town t : civ.getTowns()) {
            for (Resident resident : t.getResidents()) {
                if (!resident.isShowCiv()) {
                    continue;
                }

                Player player;
                try {
                    player = CivGlobal.getPlayer(resident);
                    CivMessage.send(player, ChatColor.LIGHT_PURPLE + CivSettings.localize.localizedString("civMsg_Civprefix") + " " + ChatColor.WHITE + string);
                } catch (CivException ignored) {
                }
            }

        }
    }


    public static void send(CommandSender sender, List<String> outs) {
        for (String str : outs) {
            send(sender, str);
        }
    }


    public static void sendTownChat(Town town, Resident resident, String format, String message) {
        if (town == null) {
            try {
                Player player = CivGlobal.getPlayer(resident);
                player.sendMessage(ChatColor.RED + CivSettings.localize.localizedString("civMsg_tcNotInTown"));

            } catch (CivException ignored) {
            }
            return;
        }

        CivLog.info("[TC:" + town.getName() + "] " + resident.getName() + ": " + message);

        for (Resident r : town.getResidents()) {
            try {
                Player player = CivGlobal.getPlayer(r);
                String msg = ChatColor.AQUA + CivSettings.localize.localizedString("civMsg_tcPrefix") + ChatColor.WHITE + String.format(format, resident.getName(), message);
                player.sendMessage(msg);
            } catch (CivException e) {
                /* player not online. */
            }
        }

        for (String name : getExtraTownChatListeners(town)) {
            try {
                Player player = CivGlobal.getPlayer(name);
                String msg = ChatColor.AQUA + CivSettings.localize.localizedString("civMsg_tcPrefix2") + town.getName() + "]" + ChatColor.WHITE + String.format(format, resident.getName(), message);
                player.sendMessage(msg);
            } catch (CivException e) {
                /* player not online. */
            }
        }
    }


    public static void sendCivChat(Civilization civ, Resident resident, String format, String message) {
        if (civ == null) {
            try {
                Player player = CivGlobal.getPlayer(resident);
                player.sendMessage(ChatColor.RED + CivSettings.localize.localizedString("civMsg_ccNotInCiv"));

            } catch (CivException ignored) {
            }
            return;
        }

        String townName = "";
        if (resident.getTown() != null) {
            townName = resident.getTown().getName();
        }

        for (Town t : civ.getTowns()) {
            for (Resident r : t.getResidents()) {
                try {
                    Player player = CivGlobal.getPlayer(r);


                    String msg = ChatColor.GOLD + CivSettings.localize.localizedString("civMsg_ccPrefix1") + " " + townName + "]" + ChatColor.WHITE + String.format(format, resident.getName(), message);
                    player.sendMessage(msg);
                } catch (CivException e) {
                    /* player not online. */
                }
            }
        }

        for (String name : getExtraCivChatListeners(civ)) {
            try {
                Player player = CivGlobal.getPlayer(name);
                String msg = ChatColor.GOLD + CivSettings.localize.localizedString("civMsg_ccPrefix2") + civ.getName() + " " + townName + "]" + ChatColor.WHITE + String.format(format, resident.getName(), message);
                player.sendMessage(msg);
            } catch (CivException e) {
                /* player not online. */
            }
        }

    }

    public static void sendChat(Resident resident, String format, String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String msg = String.format(format, resident.getName(), message);
            player.sendMessage(msg);
        }
    }

    public static void addExtraTownChatListener(Town town, String name) {

        ArrayList<String> names = extraTownChatListeners.get(town.getName().toLowerCase());
        if (names == null) {
            names = new ArrayList<>();
        }

        for (String str : names) {
            if (str.equals(name)) {
                return;
            }
        }

        names.add(name);
        extraTownChatListeners.put(town.getName().toLowerCase(), names);
    }

    public static void removeExtraTownChatListener(Town town, String name) {
        ArrayList<String> names = extraTownChatListeners.get(town.getName().toLowerCase());
        if (names == null) {
            return;
        }

        for (String str : names) {
            if (str.equals(name)) {
                names.remove(str);
                break;
            }
        }

        extraTownChatListeners.put(town.getName().toLowerCase(), names);
    }

    public static ArrayList<String> getExtraTownChatListeners(Town town) {
        ArrayList<String> names = extraTownChatListeners.get(town.getName().toLowerCase());
        if (names == null) {
            return new ArrayList<>();
        }
        return names;
    }

    public static void addExtraCivChatListener(Civilization civ, String name) {

        ArrayList<String> names = extraCivChatListeners.get(civ.getName().toLowerCase());
        if (names == null) {
            names = new ArrayList<>();
        }

        for (String str : names) {
            if (str.equals(name)) {
                return;
            }
        }

        names.add(name);

        extraCivChatListeners.put(civ.getName().toLowerCase(), names);
    }

    public static void removeExtraCivChatListener(Civilization civ, String name) {
        ArrayList<String> names = extraCivChatListeners.get(civ.getName().toLowerCase());
        if (names == null) {
            return;
        }

        for (String str : names) {
            if (str.equals(name)) {
                names.remove(str);
                break;
            }
        }

        extraCivChatListeners.put(civ.getName().toLowerCase(), names);
    }

    public static ArrayList<String> getExtraCivChatListeners(Civilization civ) {
        ArrayList<String> names = extraCivChatListeners.get(civ.getName().toLowerCase());
        if (names == null) {
            return new ArrayList<>();
        }
        return names;
    }

    public static void sendTownSound(Town town, Sound sound, float f, float g) {
        for (Resident resident : town.getResidents()) {
            Player player;
            try {
                player = CivGlobal.getPlayer(resident);

                player.playSound(player.getLocation(), sound, f, g);
            } catch (CivException e) {
                //player not online.
            }
        }

    }

    public static void sendAll(String str) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(str);
        }
    }

    public static void sendCamp(Camp camp, String message) {
        for (Resident resident : camp.getMembers()) {
            try {
                Player player = CivGlobal.getPlayer(resident);
                player.sendMessage(ChatColor.YELLOW + "[Camp] " + ChatColor.YELLOW + message);
                CivLog.info("[Camp:" + camp.getName() + "] " + message);

            } catch (CivException e) {
                //player not online.
            }
        }
    }

    public static void sendTownHeading(Town town, String string) {
        CivLog.info("[Town:" + town.getName() + "] " + string);
        for (Resident resident : town.getResidents()) {
            if (!resident.isShowTown()) {
                continue;
            }

            Player player;
            try {
                player = CivGlobal.getPlayer(resident);
                CivMessage.sendHeading(player, string);
            } catch (CivException ignored) {
            }
        }
    }

    public static void sendSuccess(Resident resident, String message) {
        try {
            sendSuccess(CivGlobal.getPlayer(resident), message);
        } catch (CivException e) {
            e.printStackTrace();
        }
    }

    public static void sendTeam(ArenaTeam team, String message) {
        for (Resident resident : team.teamMembers) {
            CivMessage.send(resident, ChatColor.DARK_AQUA + CivSettings.localize.localizedString("civMsg_teamchatPrefix") + " (" + team.getName() + ")] " + ChatColor.RESET + message);
        }
    }

    public static void sendTeamHeading(ArenaTeam team, String message) {
        for (Resident resident : team.teamMembers) {
            CivMessage.sendHeading(resident, message);
        }
    }

    public static void sendArena(Arena arena, String message) {
        CivLog.info("[Arena] " + message);
        for (ArenaTeam team : arena.getTeams()) {
            for (Resident resident : team.teamMembers) {
                CivMessage.send(resident, ChatColor.AQUA + CivSettings.localize.localizedString("civMsg_arenaPrefix") + " " + ChatColor.RESET + message);
            }
        }
    }

}
