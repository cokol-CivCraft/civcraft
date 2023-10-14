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
package com.avrgaming.civcraft.threading.timers;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import org.bukkit.ChatColor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AnnouncementTimer implements Runnable {

    private final List<String> announcements = new ArrayList<>();


    public AnnouncementTimer(String filename) {
        try {
            File file = new File(CivSettings.plugin.getDataFolder().getPath() + "/data/" + filename);
            if (!file.exists()) {
                CivLog.warning("Configuration file: " + filename + " was missing. Streaming to disk from Jar.");
                CivSettings.streamResourceToDisk("/data/" + filename);
            }

            CivLog.info("Loading Configuration file: " + filename);

            if (!file.exists()) {
                CivLog.warning("No " + filename + " to run announcements on.");
                return;
            }
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                announcements.add(line);
            }

            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        Random random = new Random();

        String str = announcements.get(random.nextInt(0, announcements.size()));

        CivMessage.sendAll(ChatColor.GOLD + CivSettings.localize.localizedString("TipHeading") + " " + ChatColor.WHITE + str);
    }

}
