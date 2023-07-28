package com.avrgaming.civcraft.fishing;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigFishing;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Random;

public class FishingListener implements Listener {

    public ArrayList<ConfigFishing> getRandomDrops() {
        Random rand = new Random();
        ArrayList<ConfigFishing> dropped = new ArrayList<>();

        for (ConfigFishing d : CivSettings.fishingDrops) {
            if (rand.nextInt(10000) < (d.drop_chance * 10000)) {
                dropped.add(d);
            }

        }
        return dropped;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        CivLog.debug("NOT cancelling player fish event...");
        Player player = event.getPlayer();

        ArrayList<ConfigFishing> dropped = getRandomDrops();
        event.getCaught().remove();

        if (dropped.isEmpty()) {
            for (ItemStack is : player.getInventory().addItem(new ItemStack(Material.RAW_FISH, 1, (short) 0)).values()) {
                player.getWorld().dropItem(player.getLocation(), is);
            }
            CivMessage.send(event.getPlayer(), ChatColor.GREEN + CivSettings.localize.localizedString("var_fishing_success", ChatColor.LIGHT_PURPLE + CivSettings.localize.localizedString("fishing_rawFish")));
            return;
        }
        dropped.forEach(d -> drop(event, player, d));

    }

    private static void drop(PlayerFishEvent event, Player player, ConfigFishing d) {
        ItemStack stack;
        if (d.craftMatId == null) {
            stack = new ItemStack(d.type_id, 1, (short) 0);
            CivMessage.send(event.getPlayer(), ChatColor.GREEN + CivSettings.localize.localizedString("var_fishing_success", ChatColor.LIGHT_PURPLE + stack.getType().name().replace("_", " ").toLowerCase()));
        } else {
            LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterialFromId(d.craftMatId);
            if (craftMat != null) {
                stack = LoreCraftableMaterial.spawn(craftMat);
                CivMessage.send(event.getPlayer(), ChatColor.GREEN + CivSettings.localize.localizedString("var_fishing_success", ChatColor.LIGHT_PURPLE + craftMat.getName()));
            } else {
                stack = null;
                return;
            }
        }
        for (ItemStack is : player.getInventory().addItem(stack).values()) {
            player.getWorld().dropItem(player.getLocation(), is);
        }
    }
}
