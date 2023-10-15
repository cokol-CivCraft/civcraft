package com.avrgaming.civcraft.command;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import java.util.Set;

public class Sender implements CommandSender {
    private final CommandSender baseSender;

    public Sender(CommandSender baseSender) {
        this.baseSender = baseSender;
    }

    public Player getPlayer() {
        if (!(baseSender instanceof Player)) {
            throw new CommandProblemException(ChatColor.RED + CivSettings.localize.localizedString("cmd_MustBePlayer"));
        }
        return (Player) baseSender;
    }

    public Resident getResident() {
        return CivGlobal.getResident(getPlayer());
    }

    public Town getTown() {
        Resident resident = getResident();
        if (!resident.hasTown()) {
            throw new CommandProblemException(ChatColor.RED + CivSettings.localize.localizedString("cmd_notPartOfTown"));
        }
        return resident.getTown();
    }

    public Civilization getCiv() {
        return getTown().getCiv();
    }

    @Override
    public void sendMessage(String message) {
        baseSender.sendMessage(message);
    }

    @Override
    public void sendMessage(String[] messages) {
        baseSender.sendMessage(messages);
    }

    @Override
    public Server getServer() {
        return baseSender.getServer();
    }

    @Override
    public String getName() {
        return baseSender.getName();
    }

    @Override
    public Spigot spigot() {
        return baseSender.spigot();
    }

    @Override
    public boolean isPermissionSet(String name) {
        return baseSender.isPermissionSet(name);
    }

    @Override
    public boolean isPermissionSet(Permission perm) {
        return baseSender.isPermissionSet(perm);
    }

    @Override
    public boolean hasPermission(String name) {
        return baseSender.hasPermission(name);
    }

    @Override
    public boolean hasPermission(Permission perm) {
        return baseSender.hasPermission(perm);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
        return baseSender.addAttachment(plugin, name, value);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
        return baseSender.addAttachment(plugin);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
        return baseSender.addAttachment(plugin, name, value, ticks);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
        return baseSender.addAttachment(plugin, ticks);
    }

    @Override
    public void removeAttachment(PermissionAttachment attachment) {
        baseSender.removeAttachment(attachment);
    }

    @Override
    public void recalculatePermissions() {
        baseSender.recalculatePermissions();
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return baseSender.getEffectivePermissions();
    }

    @Override
    public boolean isOp() {
        return baseSender.isOp();
    }

    @Override
    public void setOp(boolean value) {
        baseSender.setOp(value);
    }

    public CommandSender getBaseSender() {
        return baseSender;
    }
}
