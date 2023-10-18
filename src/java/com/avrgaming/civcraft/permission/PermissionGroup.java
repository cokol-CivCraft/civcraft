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
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.SQLObject;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.INBTSerializable;
import net.minecraft.server.v1_12_R1.NBTTagCompound;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionGroup extends SQLObject implements INBTSerializable {

    private final Map<String, Resident> members = new ConcurrentHashMap<>();
    /* Only cache towns as the 'civ' can change when a town gets conquered or gifted/moved. */
    private Town cacheTown = null;

    private UUID civUUID;
    private UUID townUUID;

    public PermissionGroup(Civilization civ, String name) throws InvalidNameException {
        this.civUUID = civ.getUUID();
        this.townUUID = NULL_UUID;
        this.setUUID(UUID.randomUUID());
        this.setName(name);
    }

    public PermissionGroup(Civilization civ, NBTTagCompound nbt) throws InvalidNameException {
        this.civUUID = civ.getUUID();
        this.townUUID = NULL_UUID;
        this.loadFromNBT(nbt);
    }

    public PermissionGroup(Town town, String name) throws InvalidNameException {
        this.townUUID = town.getUUID();
        this.civUUID = NULL_UUID;
        this.cacheTown = town;
        this.setUUID(UUID.randomUUID());
        this.setName(name);
    }

    public PermissionGroup(Town town, NBTTagCompound nbt) throws InvalidNameException {
        this.townUUID = town.getUUID();
        this.civUUID = NULL_UUID;
        this.cacheTown = town;
        this.loadFromNBT(nbt);
    }

    public void addMember(Resident res) {
        members.put(res.getUUIDString(), res);
    }

    public void removeMember(Resident res) {
        members.remove(res.getUUIDString());
    }

    public boolean hasMember(Resident res) {
        return members.containsKey(res.getUUIDString());
    }

    public static final String TABLE_NAME = "GROUPS";

    public static void init() throws SQLException {
        if (!SQLController.hasTable(TABLE_NAME)) {
            String table_create = "CREATE TABLE " + SQLController.tb_prefix + TABLE_NAME + " (" +
                    "`id` int(11) unsigned NOT NULL auto_increment," +
                    "`uuid` VARCHAR(36) NOT NULL," +
                    "`name` VARCHAR(64) NOT NULL," +
                    "`town_uuid` VARCHAR(36)," +
                    "`civ_uuid` VARCHAR(36)," +
                    "`members` mediumtext," +
                    //"FOREIGN KEY (town_id) REFERENCES "+SQLController.tb_prefix+"TOWN(id),"+
                    //"FOREIGN KEY (civ_id) REFERENCES "+SQLController.tb_prefix+"CIVILIZATIONS(id),"+
                    "PRIMARY KEY (`id`)" + ")";

            SQLController.makeTable(table_create);
            CivLog.info("Created " + TABLE_NAME + " table");
        } else {
            CivLog.info(TABLE_NAME + " table OK!");
        }
    }

    @Override
    public void load(ResultSet rs) throws SQLException, InvalidNameException {
        this.setId(rs.getInt("id"));
        this.setUUID(UUID.fromString(rs.getString("uuid")));
        this.setName(rs.getString("name"));
        this.setTownUUID(UUID.fromString(rs.getString("town_uuid")));
        this.setCivUUID(UUID.fromString(rs.getString("civ_uuid")));
        loadMembersFromSaveString(rs.getString("members"));

        if (!this.getTownUUID().equals(NULL_UUID)) {
            this.cacheTown = Town.getTownFromUUID(this.getTownUUID());
            this.getTown().addGroup(this);
        } else {
            Civilization civ = Civilization.getCivFromUUID(this.getCivUUID());
            if (civ == null) {
                civ = CivGlobal.getConqueredCivFromUUID(this.getCivUUID());
                if (civ == null) {
                    CivLog.warning("COUlD NOT FIND CIV ID:" + this.getCivUUID() + " for group: " + this.getName() + " to load.");
                    return;
                }
            }

            civ.addGroup(this);
        }
    }

    @Override
    public void save() {

    }

    @Override
    public void saveNow() throws SQLException {
        HashMap<String, Object> hashmap = new HashMap<>();

        hashmap.put("name", this.getName());
        hashmap.put("members", this.getMembersSaveString());
        hashmap.put("town_uuid", this.getTownUUID().toString());
        hashmap.put("civ_uuid", this.getCivUUID().toString());

        SQLController.updateNamedObject(this, hashmap, TABLE_NAME);
    }

    @Override
    public void delete() throws SQLException {
        SQLController.deleteNamedObject(this, TABLE_NAME);
    }

    private String getMembersSaveString() {
        StringBuilder ret = new StringBuilder();

        for (Resident resident : members.values()) {
            ret.append(resident.getUUID()).append(",");
        }

        return ret.toString();
    }

    private void loadMembersFromSaveString(String src) {
        for (String n : src.split(",")) {
            Resident res;

            if (!n.isEmpty()) {
                res = CivGlobal.getResidentViaUUID(UUID.fromString(n));

                if (res != null) {
                    members.put(n, res);
                }
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
        return members.values();
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

        for (String uuid : members.keySet()) {
            Resident res = CivGlobal.getResidentViaUUID(UUID.fromString(uuid));
            out.append(res.getName()).append(", ");
        }
        return out.toString();
    }

    public UUID getCivUUID() {
        return civUUID;
    }

    public void setCivUUID(UUID civUUID) {
        this.civUUID = civUUID;
    }

    public UUID getTownUUID() {
        return townUUID;
    }

    public void setTownUUID(UUID townId) {
        this.townUUID = townId;
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
