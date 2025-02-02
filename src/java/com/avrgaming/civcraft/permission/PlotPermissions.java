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
package com.avrgaming.civcraft.permission;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;

import java.util.ArrayList;
import java.util.UUID;

public class PlotPermissions {

    public enum Type {
        BUILD,
        DESTROY,
        INTERACT,
        ITEMUSE
    }

    public PermissionNode build = new PermissionNode("build");
    public PermissionNode destroy = new PermissionNode("destroy");
    public PermissionNode interact = new PermissionNode("interact");
    public PermissionNode itemUse = new PermissionNode("itemUse");
    private boolean fire, mobs;

    /*
     * Owner of this permission node.
     */
    private Resident owner;

    /*
     * Group for this permission node.
     */
    //private PermissionGroup group;
    private final ArrayList<PermissionGroup> groups = new ArrayList<>();

    public String getSaveString() {

        String ownerString = "";
        if (owner != null) {
            ownerString = owner.getName();
        }

        StringBuilder groupString = new StringBuilder("0:");
        if (!groups.isEmpty()) {
            for (PermissionGroup grp : groups) {
                groupString.append(grp.getUUID().toString()).append(":");
            }
        }

        return build.getSaveString() + "," + destroy.getSaveString() + "," + interact.getSaveString() + "," + itemUse.getSaveString() + "," + ownerString + "," + groupString + "," + fire + "," + mobs;
    }

    public void loadFromSaveString(Town town, String src) {
        String[] split = src.split(",");

        build.loadFromString(split[0]);
        destroy.loadFromString(split[1]);
        interact.loadFromString(split[2]);
        itemUse.loadFromString(split[3]);

        setOwner(CivGlobal.getResident(split[4]));
        String[] grpString = split[5].split(":");

        for (String gstr : grpString) {
            gstr = gstr.trim();
            if (gstr.equals("0") || gstr.isEmpty()) {
                continue;
            }
            PermissionGroup group = town.getGroupFromUUID(UUID.fromString(gstr));
            addGroup(group);
        }

        if (split.length > 7) {
            fire = Boolean.parseBoolean(split[6]);
            mobs = Boolean.parseBoolean(split[7]);
        }

        //	group = CivGlobal.getPermissionGroup(town, Integer.valueOf(split[5]));

    }

    public boolean isFire() {
        return fire;
    }

    public void setFire(boolean fire) {
        this.fire = fire;
    }

    public boolean isMobs() {
        return mobs;
    }

    public void setMobs(boolean mobs) {
        this.mobs = mobs;
    }

    public String getBuildString() {
        return build.getString();
    }

    public String getDestroyString() {
        return destroy.getString();
    }

    public String getInteractString() {
        return interact.getString();
    }

    public String getItemUseString() {
        return itemUse.getString();
    }

    public Resident getOwner() {
        return owner;
    }

    public void setOwner(Resident owner) {
        this.owner = owner;
    }

    private boolean checkPermissionNode(PermissionNode node, Resident resident) {
        if (node != null) {
            if (owner == resident && node.isPermitOwner())
                return true;

            if (owner != null && owner.isFriend(resident) && node.isPermitOwner())
                return true;


            if (!groups.isEmpty() && node.isPermitGroup()) {
                for (PermissionGroup group : groups) {
                    if (group.hasMember(resident)) {
                        return true;
                    }
                }
            }

            return node.isPermitOthers();
        }
        return false;
    }

    public boolean hasPermission(Type type, Resident resident) {
        if (resident.isPermOverride()) {
            return true;
        }

        switch (type) {
            case BUILD -> {
                return checkPermissionNode(this.build, resident);
            }
            case DESTROY -> {
                return checkPermissionNode(this.destroy, resident);
            }
            case INTERACT -> {
                return checkPermissionNode(this.interact, resident);
            }
            case ITEMUSE -> {
                return checkPermissionNode(this.itemUse, resident);
            }
            default -> {
            }
        }

        return false;
    }

    public void addGroup(PermissionGroup grp) {
        if (grp == null) {
            return;
        }

        if (!groups.contains(grp)) {
            groups.add(grp);
        }
    }

    public void removeGroup(PermissionGroup grp) {
        groups.remove(grp);
    }

    public ArrayList<PermissionGroup> getGroups() {
        return this.groups;
    }

    public void resetPerms() {
        build.setPermitOwner(true);
        build.setPermitGroup(true);
        build.setPermitOthers(false);

        destroy.setPermitOwner(true);
        destroy.setPermitGroup(true);
        destroy.setPermitOthers(false);

        interact.setPermitOwner(true);
        interact.setPermitGroup(true);
        interact.setPermitOthers(false);

        itemUse.setPermitOwner(true);
        itemUse.setPermitGroup(true);
        itemUse.setPermitOthers(false);
    }

    public String getGroupString() {
        StringBuilder out = new StringBuilder();

        for (PermissionGroup grp : groups) {
            out.append(grp.getName()).append(", ");
        }
        return out.toString();
    }

    public void clearGroups() {
        this.groups.clear();
    }

    public void replaceGroups(PermissionGroup defaultGroup) {
        this.groups.clear();
        this.addGroup(defaultGroup);
    }

}
