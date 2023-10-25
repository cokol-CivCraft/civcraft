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

import com.avrgaming.civcraft.database.SQLController;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.NamedObject;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.INBTSerializable;
import net.minecraft.server.v1_12_R1.NBTTagCompound;

import java.sql.SQLException;
import java.util.*;

public class PermissionGroup extends NamedObject implements INBTSerializable {

    private final Set<UUID> members = new HashSet<>();
    /* Only cache towns as the 'civ' can change when a town gets conquered or gifted/moved. */
    private Town cacheTown = null;

    public PermissionGroup(Civilization civ, String name) throws InvalidNameException {
        this.setUUID(UUID.randomUUID());
        this.setName(name);
    }

    public PermissionGroup(Civilization civ, NBTTagCompound nbt) throws InvalidNameException {
        this.loadFromNBT(nbt);
    }

    public PermissionGroup(Town town, String name) throws InvalidNameException {
        this.cacheTown = town;
        this.setUUID(UUID.randomUUID());
        this.setName(name);
    }

    public PermissionGroup(Town town, NBTTagCompound nbt) throws InvalidNameException {
        this.cacheTown = town;
        this.loadFromNBT(nbt);
    }

    public void addMember(Resident res) {
        members.add(res.getUUID());
    }

    public void removeMember(Resident res) {
        members.remove(res.getUUID());
    }

    public boolean hasMember(Resident res) {
        return members.contains(res.getUUID());
    }

    public static final String TABLE_NAME = "GROUPS";

    public static void init() throws SQLException {
        if (!SQLController.hasTable(TABLE_NAME)) {
            String table_create = "CREATE TABLE " + SQLController.tb_prefix + TABLE_NAME + " (" +
                    "`uuid` VARCHAR(36) NOT NULL," +
                    "`name` VARCHAR(64) NOT NULL," +
                    "`town_uuid` VARCHAR(36)," +
                    "`civ_uuid` VARCHAR(36)," +
                    "`members` mediumtext," +
                    //"FOREIGN KEY (town_id) REFERENCES "+SQLController.tb_prefix+"TOWN(id),"+
                    //"FOREIGN KEY (civ_id) REFERENCES "+SQLController.tb_prefix+"CIVILIZATIONS(id),"+
                    "PRIMARY KEY (`uuid`)" + ")";

            SQLController.makeTable(table_create);
            CivLog.info("Created " + TABLE_NAME + " table");
        } else {
            CivLog.info(TABLE_NAME + " table OK!");
        }
    }

    private String getMembersSaveString() {
        StringBuilder ret = new StringBuilder();

        for (UUID uuid : members) {
            ret.append(uuid).append(",");
        }

        return ret.toString();
    }

    private void loadMembersFromSaveString(String src) {
        for (String n : src.split(",")) {
            if (!n.isEmpty()) {
                members.add(UUID.fromString(n));
            }
        }
    }

    public Town getTown() {
        return cacheTown;
    }

    public void setTown(Town town) {
        this.cacheTown = town;
    }

    public int getMemberCount() {
        return members.size();
    }

    public Collection<Resident> getMemberList() {
        ArrayList<Resident> list = new ArrayList<>();
        for (UUID uuid : members) {
            Resident res = CivGlobal.getResidentViaUUID(uuid);
            if (res != null) {
                list.add(res);
            }
        }
        return list;
    }

    public Civilization getCiv() {
        if (cacheTown == null) {
            return null;
        }

        return cacheTown.getCiv();
    }

    public boolean isProtectedGroup() {
        return isTownProtectedGroup(this.getName()) || isCivProtectedGroup(this.getName());
    }

    public static boolean isProtectedGroupName(String name) {
        return isTownProtectedGroup(name) || isCivProtectedGroup(name);
    }

    public boolean isTownProtectedGroup() {
        return isTownProtectedGroup(this.getName());
    }

    public boolean isCivProtectedGroup() {
        return isCivProtectedGroup(this.getName());
    }

    private static boolean isTownProtectedGroup(String name) {
        return switch (name.toLowerCase()) {
            case "mayors", "assistants", "residents" -> true;
            default -> false;
        };
    }

    private static boolean isCivProtectedGroup(String name) {
        return switch (name.toLowerCase()) {
            case "leaders", "advisers" -> true;
            default -> false;
        };
    }

    public String getMembersString() {
        StringBuilder out = new StringBuilder();

        for (Resident res : getMemberList()) {
            out.append(res.getName()).append(", ");
        }
        return out.toString();
    }

    @Override
    public void saveToNBT(NBTTagCompound nbt) {
        nbt.setString("uuid", this.getUUID().toString());
        nbt.setString("name", this.getName());
        nbt.setString("members", this.getMembersSaveString());
    }

    @Override
    public void loadFromNBT(NBTTagCompound nbt) {
        this.setUUID(UUID.fromString(nbt.getString("uuid")));
        try {
            this.setName(nbt.getString("name"));
        } catch (InvalidNameException e) {
            throw new RuntimeException(e);
        }
        loadMembersFromSaveString(nbt.getString("members"));
    }

}
