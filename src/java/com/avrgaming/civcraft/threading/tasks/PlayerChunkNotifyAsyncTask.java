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
package com.avrgaming.civcraft.threading.tasks;

import com.avrgaming.civcraft.camp.Camp;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.*;
import com.avrgaming.civcraft.util.AsciiMap;
import com.avrgaming.civcraft.util.ChunkCoord;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Date;
import java.util.HashMap;

public class PlayerChunkNotifyAsyncTask implements Runnable {

    Location from;
    Location to;
    String playerName;

    public static int BORDER_SPAM_TIMEOUT = 30000; //30 second border spam protection.
    public static HashMap<String, Date> cultureEnterTimes = new HashMap<>();

    public PlayerChunkNotifyAsyncTask(Location from, Location to, String playerName) {
        this.from = from;
        this.to = to;
        this.playerName = playerName;
    }

    public static String getNotifyColor(CultureChunk toCc, Relation.Status status, Player player) {

        String color = String.valueOf(ChatColor.WHITE);
        switch (status) {
            case NEUTRAL -> {
                if (toCc.getTown().isOutlaw(player.getName())) {
                    color = String.valueOf(ChatColor.YELLOW);
                }
            }
            case HOSTILE -> color = String.valueOf(ChatColor.YELLOW);
            case WAR -> color = String.valueOf(ChatColor.RED);
            case PEACE -> color = String.valueOf(ChatColor.AQUA);
            case ALLY -> color = String.valueOf(ChatColor.DARK_GREEN);
        }

        return color;
    }

    private String getToWildMessage() {
        return ChatColor.GRAY + CivSettings.localize.localizedString("playerChunkNotify_enterWilderness") + " " + ChatColor.RED + "[PvP]";
    }

    private String getToTownMessage(Town town, TownChunk tc) {
        Player player;
        try {
            player = CivGlobal.getPlayer(playerName);
        } catch (CivException e) {
            return "";
        }

        if (town.getBuffManager().hasBuff("buff_hanging_gardens_regen")) {
            Resident resident = CivGlobal.getResident(player);
            if (resident != null && resident.getTown() == town) {
                CivMessage.send(player, String.valueOf(ChatColor.DARK_GREEN) + ChatColor.ITALIC + "You feel invigorated by the glorious hanging gardens.");
            }
        }

        if (!tc.isOutpost()) {
            return ChatColor.GRAY + CivSettings.localize.localizedString("var_playerChunkNotify_EnterTown", ChatColor.WHITE + town.getName(), town.getPvpString());
        } else {
            return ChatColor.GRAY + CivSettings.localize.localizedString("var_playerChunkNotify_EnterOutpost", ChatColor.WHITE + town.getName(), town.getPvpString());
        }
    }

    private void showPlotMoveMessage() {

        TownChunk fromTc = CivGlobal.getTownChunk(from);
        TownChunk toTc = CivGlobal.getTownChunk(to);
        CultureChunk fromCc = CivGlobal.getCultureChunk(from);
        CultureChunk toCc = CivGlobal.getCultureChunk(to);
        Camp toCamp = CivGlobal.getCampFromChunk(new ChunkCoord(to));
        Camp fromCamp = CivGlobal.getCampFromChunk(new ChunkCoord(from));

        Player player;
        Resident resident;
        try {
            player = CivGlobal.getPlayer(this.playerName);
            resident = CivGlobal.getResident(this.playerName);
        } catch (CivException e) {
            return;
        }

        Civilization civilization = resident.getCiv();

        String title = "";
        String subTitle = "";

        //We've entered a camp.
        if (toCamp != null && toCamp != fromCamp) {
            title += ChatColor.GOLD + CivSettings.localize.localizedString("var_playerChunkNotify_enterCamp", toCamp.getName()) + " " + ChatColor.RED + "[PvP]";
        } else if (toCamp == null && fromCamp != null) {
            title += getToWildMessage();
        } else if (fromTc != null && toTc == null) {
            // From a town... to the wild
            title += getToWildMessage();
        }
        if (fromTc == null && toTc != null) {
            // To Town
            Town t = toTc.getTown();
            title += getToTownMessage(t, toTc);
            if (resident.getTown() == toTc.getTown()) {
                subTitle += CivSettings.localize.localizedString("var_civ_border_welcomeHome", player.getName());
            } else {
                if (t.isOutlaw(resident)) {
                    subTitle += ChatColor.DARK_RED + CivSettings.localize.localizedString("town_border_outlaw");
                }
            }

        }


//		// To another town(should never happen with culture...)
//		if (fromTc != null && toTc != null && fromTc.getTown() != toTc.getTown()) {
//			title += getToTownMessage(toTc.getTown(), toTc);
//		}

//		if (toTc != null) {
//			subTitle += toTc.getOnEnterString(player, fromTc);
//		}

        // Leaving culture to the wild.
        if (fromCc != null && toCc == null) {
            title += fromCc.getOnLeaveString();
        } else if (fromCc == null && toCc != null) {    // Leaving wild, entering culture.
            title += toCc.getOnEnterString();
            if (civilization != null) {
                if (civilization == toCc.getCiv()) {
                    subTitle += CivSettings.localize.localizedString("var_civ_border_welcomeBack", player.getName());
                } else {
                    String relationship = civilization.getDiplomacyManager().getRelation(toCc.getCiv()).toString();
                    if (relationship != null && !relationship.isEmpty()) {
                        subTitle = CivSettings.localize.localizedString("var_civ_border_relation", relationship);
                    }
                }
            }
            onCultureEnter(toCc);
        } else if (fromCc != null && fromCc.getCiv() != toCc.getCiv()) {
            //Leaving one civ's culture, into another.

            title += fromCc.getOnLeaveString() + " | " + toCc.getOnEnterString();
            onCultureEnter(toCc);
            if (civilization != null) {
                if (civilization == toCc.getCiv()) {
                    subTitle += CivSettings.localize.localizedString("var_civ_border_welcomeBack", player.getName());
                } else {
                    String relationship = civilization.getDiplomacyManager().getRelation(toCc.getCiv()).toString();
                    if (relationship != null && !relationship.isEmpty()) {
                        subTitle = CivSettings.localize.localizedString("var_civ_border_relation", relationship);
                    }
                }
            }
        }

        if (!title.isEmpty()) {
            //ItemMessage im = new ItemMessage(CivCraft.getPlugin());
            //im.sendMessage(player, CivColor.BOLD+out, 3);
            CivMessage.sendTitle(player, title, subTitle);
//			CivMessage.send(player, title);
        }

        if (resident.isShowInfo()) {
            CultureChunk.showInfo(player);
        }

    }

    private void onCultureEnter(CultureChunk toCc) {
        Player player;
        try {
            player = CivGlobal.getPlayer(this.playerName);
        } catch (CivException e) {
            return;
        }

        Relation.Status status = toCc.getCiv().getDiplomacyManager().getRelationStatus(player);
        String color = getNotifyColor(toCc, status, player);
        String relationName = status.name();

        if (player.isOp()) {
            return;
        }

        Resident resident = CivGlobal.getResident(player);
        if (resident != null && resident.hasTown() && resident.getCiv() == toCc.getCiv()) {
            return;
        }


        String borderSpamKey = player.getName() + ":" + toCc.getCiv().getName();
        Date lastMessageTime = cultureEnterTimes.get(borderSpamKey);

        Date now = new Date();
        if ((lastMessageTime != null) && (now.getTime() < (lastMessageTime.getTime() + BORDER_SPAM_TIMEOUT))) {
            // Preventing border spam, not issuing message.
            return;
        }
        lastMessageTime = now;

        cultureEnterTimes.put(borderSpamKey, lastMessageTime);
        CivMessage.sendCiv(toCc.getCiv(), CivSettings.localize.localizedString("var_playerChunkNotify_enteredBorderAlert", (color + player.getDisplayName() + "(" + relationName + ")")));
    }


    @Override
    public void run() {
        showPlotMoveMessage();
        showResidentMap();
    }


    private void showResidentMap() {
        Player player;
        try {
            player = CivGlobal.getPlayer(this.playerName);
        } catch (CivException e) {
            return;
        }

        Resident resident = CivGlobal.getResident(player);
        if (resident == null) {
            return;
        }

        if (resident.isShowMap()) {
            CivMessage.send(player, AsciiMap.getMapAsString(player.getLocation()));
        }
    }


}
