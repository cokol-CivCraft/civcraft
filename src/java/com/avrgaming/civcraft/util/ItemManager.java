package com.avrgaming.civcraft.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;


/*
 * The ItemManager class is going to be used to wrap itemstack operations that have now
 * been deprecated by Bukkit. If bukkit ever actually takes these methods away from us,
 * we'll just have to use NMS or be a little creative. Doing it on spot (here) will be
 * better than having fragile code scattered everywhere.
 *
 * Additionally it gives us an opportunity to unit test certain item operations that we
 * want to use with our new custom item stacks.
 */

public class ItemManager {

    @SuppressWarnings("deprecation")
    public static int getId(Enchantment e) {
        return e.getId();
    }

    public static short getData(ItemStack stack) {
        return stack.getDurability();
    }

    @SuppressWarnings("deprecation")
    public static byte getData(BlockState state) {
        return state.getRawData();
    }

    @SuppressWarnings("deprecation")
    public static void sendBlockChange(Player player, Location loc, Material type, int data) {
        player.sendBlockChange(loc, type, (byte) data);
    }

    @SuppressWarnings("deprecation")
    public static void setData(MaterialData data, byte chestData) {
        data.setData(chestData);
    }

    @SuppressWarnings("deprecation")
    public static ItemStack spawnPlayerHead(String playerName, String itemDisplayName) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM.getId(), 1, (short) 3);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwner(playerName);
        meta.setDisplayName(itemDisplayName);
        skull.setItemMeta(meta);
        return skull;
    }

    public static boolean removeItemFromPlayer(Player player, Material mat, int amount) {
        ItemStack m = new ItemStack(mat, amount);
        if (player.getInventory().contains(mat)) {
            player.getInventory().removeItem(m);
            return true;
        }
        return false;
    }

}
