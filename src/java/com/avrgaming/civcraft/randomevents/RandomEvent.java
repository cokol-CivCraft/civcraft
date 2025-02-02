package com.avrgaming.civcraft.randomevents;


import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.database.SQLController;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.SQLObject;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.randomevents.components.HammerRate;
import com.avrgaming.civcraft.randomevents.components.Happiness;
import com.avrgaming.civcraft.randomevents.components.Unhappiness;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.mysql.jdbc.StringUtils;
import org.bukkit.ChatColor;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class RandomEvent extends SQLObject {

    public ConfigRandomEvent configRandomEvent;

    public HashMap<String, RandomEventComponent> actions = new HashMap<>();
    public HashMap<String, RandomEventComponent> requirements = new HashMap<>();
    public HashMap<String, RandomEventComponent> success = new HashMap<>();
    public HashMap<String, RandomEventComponent> failure = new HashMap<>();

    private Town town = null;
    private Date startDate = null;
    private boolean active = false;

    /*
     * Components can communicate with each other by saving variables in this hashmap.
     */
    public HashMap<String, String> componentVars = new HashMap<>();
    public LinkedList<String> savedMessages = new LinkedList<>();

    public static final String TABLE_NAME = "RANDOMEVENTS";

    public static void init() throws SQLException {
        if (!SQLController.hasTable(TABLE_NAME)) {
            String table_create = "CREATE TABLE " + SQLController.tb_prefix + TABLE_NAME + " (" +
                    "`uuid` VARCHAR(36) NOT NULL," +
                    "`config_id` mediumtext," +
                    "`town_uuid` VARCHAR(36)," +
                    "`start_date` long NOT NULL," +
                    "`active` boolean DEFAULT false," +
                    "`component_vars` mediumtext," +
                    "`saved_messages` mediumtext," +
                    "PRIMARY KEY (`uuid`)" + ")";

            SQLController.makeTable(table_create);
            CivLog.info("Created " + TABLE_NAME + " table");
        } else {
            SQLController.makeCol("active", "boolean", TABLE_NAME);
        }
    }

    @Override
    public void load(ResultSet rs) throws SQLException, CivException {
        this.setUUID(UUID.fromString(rs.getString("uuid")));
        this.configRandomEvent = CivSettings.randomEvents.get(rs.getString("config_id"));
        if (this.configRandomEvent == null) {
            /* Delete the random event. */
            this.delete();
            throw new CivException("Couldn't find random event config id:" + rs.getString("config_id"));
        }

        this.town = Town.getTownFromUUID(UUID.fromString(rs.getString("town_uuid")));
        if (this.town == null) {
            this.delete();
            throw new CivException("Couldn't find town id:" + rs.getInt("town_id") + " while loading random event.");
        }

        this.startDate = new Date(rs.getLong("start_date"));
        this.active = rs.getBoolean("active");

        loadComponentVars(rs.getString("component_vars"));
        loadSavedMessages(rs.getString("saved_messages"));

        /* Re-run the on start to re-enable any listeners. */
        /* Loop through all components for onStart() */
        buildComponents();
        for (RandomEventComponent comp : this.actions.values()) {
            comp.onStart();
        }
        for (RandomEventComponent comp : this.requirements.values()) {
            comp.onStart();
        }
        for (RandomEventComponent comp : this.success.values()) {
            comp.onStart();
        }
        for (RandomEventComponent comp : this.failure.values()) {
            comp.onStart();
        }

        RandomEventSweeper.register(this);
    }

    private void loadComponentVars(String input) {
        if (input == null || input.isEmpty()) {
            return;
        }
        String[] keyValues = input.split(",");

        for (String kvs : keyValues) {
            String[] split = kvs.split(":");
            String keyEncoded = split[0];
            String valueEncoded = split[1];

            String key = StringUtils.toAsciiString(Base64Coder.decode(keyEncoded));
            String value = StringUtils.toAsciiString(Base64Coder.decode(valueEncoded));

            this.componentVars.put(key, value);
        }
    }

    private void loadSavedMessages(String input) {
        String[] messages = input.split(",");

        for (String encodedMessage : messages) {
            String message = StringUtils.toAsciiString(Base64Coder.decode(encodedMessage));
            this.savedMessages.add(message);
        }
    }

    @Override
    public void save() {
        SQLUpdate.add(this);
    }


    @Override
    public void saveNow() throws SQLException {
        HashMap<String, Object> hashmap = new HashMap<>();

        hashmap.put("config_id", this.configRandomEvent.id);
        hashmap.put("town_uuid", this.getTown().getUUID().toString());
        hashmap.put("start_date", this.startDate.getTime());
        hashmap.put("component_vars", this.getComponentVarsSaveString());
        hashmap.put("saved_messages", this.getSavedMessagesSaveString());
        hashmap.put("active", this.active);

        SQLController.updateNamedObject(this, hashmap, TABLE_NAME);
    }

    private String getComponentVarsSaveString() {
        StringBuilder out = new StringBuilder();

        for (String key : this.componentVars.keySet()) {
            String value = this.componentVars.get(key);

            String keyEncoded = new String(Base64Coder.encode(key.getBytes()));
            String valueEncoded = new String(Base64Coder.encode(value.getBytes()));

            out.append(keyEncoded).append(":").append(valueEncoded).append(",");

        }

        return out.toString();
    }

    private String getSavedMessagesSaveString() {
        StringBuilder out = new StringBuilder();

        for (String message : this.savedMessages) {

            String msgEncoded = new String(Base64Coder.encode(message.getBytes()));
            out.append(msgEncoded).append(",");
        }

        return out.toString();
    }


    @Override
    public void delete() throws SQLException {
        SQLController.deleteNamedObject(this, TABLE_NAME);
    }

    public RandomEvent(ConfigRandomEvent config) {
        this.configRandomEvent = config;
        buildComponents();
    }

    public RandomEvent(ResultSet rs) throws SQLException, CivException {
        this.load(rs);

        /* Place ourselves back in the town we just loaded. */
        this.town.setActiveEvent(this);
    }

    public void buildComponents() {
        buildComponents("com.avrgaming.civcraft.randomevents.components.", configRandomEvent.actions, actions);
        buildComponents("com.avrgaming.civcraft.randomevents.components.", configRandomEvent.requirements, requirements);
        buildComponents("com.avrgaming.civcraft.randomevents.components.", configRandomEvent.success, success);
        buildComponents("com.avrgaming.civcraft.randomevents.components.", configRandomEvent.failure, failure);
    }

    public void buildComponents(String classPath, List<HashMap<String, String>> compInfoList, HashMap<String, RandomEventComponent> components) {
        if (compInfoList != null) {
            for (HashMap<String, String> compInfo : compInfoList) {
                String className = classPath + compInfo.get("name");
                Class<?> someClass;

                try {
                    someClass = Class.forName(className);
                    RandomEventComponent perkCompClass;
                    perkCompClass = (RandomEventComponent) someClass.newInstance();
                    perkCompClass.setName(compInfo.get("name"));

                    for (String key : compInfo.keySet()) {
                        perkCompClass.setAttribute(key, compInfo.get(key));
                    }

                    perkCompClass.createComponent(this);
                    components.put(perkCompClass.getName(), perkCompClass);
                } catch (InstantiationException | ClassNotFoundException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /*
     * Private for now, since we only allow random events on towns atm.
     */
    private void start() {

        /* Loop through all components for onStart() */
        for (RandomEventComponent comp : this.actions.values()) {
            comp.onStart();
        }
        for (RandomEventComponent comp : this.requirements.values()) {
            comp.onStart();
        }
        for (RandomEventComponent comp : this.success.values()) {
            comp.onStart();
        }
        for (RandomEventComponent comp : this.failure.values()) {
            comp.onStart();
        }

        /* Start by processing all of the action components. */
        boolean requireActivation = false;
        for (RandomEventComponent comp : this.actions.values()) {
            if (!comp.requiresActivation()) {
                comp.process();
            } else {
                requireActivation = true;
                CivMessage.sendTown(this.town, ChatColor.YELLOW + CivSettings.localize.localizedString("re_activationRequired"));
            }
        }

        if (!requireActivation) {
            this.active = true;
        }

        /* Register this random event with the sweeper until complete. */
        RandomEventSweeper.register(this);

        /* Setup start date. */
        this.startDate = new Date();

        this.save();
    }

    public Town getTown() {
        return town;
    }

    public void setTown(Town town) {
        this.town = town;
    }

    public void cleanup() {
        /* Loop through all components for cleanup */
        for (RandomEventComponent comp : this.actions.values()) {
            comp.onCleanup();
        }
        for (RandomEventComponent comp : this.requirements.values()) {
            comp.onCleanup();
        }
        for (RandomEventComponent comp : this.success.values()) {
            comp.onCleanup();
        }
        for (RandomEventComponent comp : this.failure.values()) {
            comp.onCleanup();
        }

        town.setActiveEvent(null);
        try {
            this.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public int getLength() {
        return this.configRandomEvent.length;
    }

    public void start(Town town) {
        this.town = town;

        /* Show message to town */
        CivMessage.sendTownHeading(town, "Event: " + this.configRandomEvent.name);
        for (String str : this.configRandomEvent.message) {
            CivMessage.sendTown(town, str);
            savedMessages.add(str);
        }


        town.setActiveEvent(this);
        this.start();
    }

    public static double getUnhappiness(Town town) {
        //	CivGlobal.getSessionDB().add("randomevent:unhappiness", unhappiness+":"+duration, this.getParentTown().getCiv().getId(), this.getParentTown().getId(), 0);

        ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(Unhappiness.getKey(town));
        double unhappy = 0.0;

        ArrayList<SessionEntry> removed = new ArrayList<>();
        for (SessionEntry entry : entries) {
            String[] split = entry.value.split(":");
            int unhappiness = Integer.parseInt(split[0]);
            int duration = Integer.parseInt(split[1]);


            Date start = new Date(entry.time);
            Date now = new Date();

            if (now.getTime() > (start.getTime() + ((long) duration * RandomEventSweeper.MILLISECONDS_PER_HOUR))) {
                /* Entry is expired, delete it and continue. */
                removed.add(entry);
                continue;
            }

            unhappy += unhappiness;
        }

        /* Remove any expired entries */
        for (SessionEntry entry : removed) {
            CivGlobal.getSessionDB().delete(entry.request_id, entry.key);
        }

        return unhappy;
    }

    public static double getHappiness(Town town) {
        ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(Happiness.getKey(town));
        double happy = 0.0;

        ArrayList<SessionEntry> removed = new ArrayList<>();
        for (SessionEntry entry : entries) {
            String[] split = entry.value.split(":");
            int happiness = Integer.parseInt(split[0]);
            int duration = Integer.parseInt(split[1]);


            Date start = new Date(entry.time);
            Date now = new Date();

            if (now.getTime() > (start.getTime() + ((long) duration * RandomEventSweeper.MILLISECONDS_PER_HOUR))) {
                /* Entry is expired, delete it and continue. */
                removed.add(entry);
                continue;
            }

            happy += happiness;
        }

        /* Remove any expired entries */
        for (SessionEntry entry : removed) {
            CivGlobal.getSessionDB().delete(entry.request_id, entry.key);
        }

        return happy;
    }

    public static double getHammerRate(Town town) {
        ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(HammerRate.getKey(town));
        double hammerrate = 1.0;

        ArrayList<SessionEntry> removed = new ArrayList<>();
        for (SessionEntry entry : entries) {
            String[] split = entry.value.split(":");
            double rate = Double.parseDouble(split[0]);
            int duration = Integer.parseInt(split[1]);


            Date start = new Date(entry.time);
            Date now = new Date();

            if (now.getTime() > (start.getTime() + ((long) duration * RandomEventSweeper.MILLISECONDS_PER_HOUR))) {
                /* Entry is expired, delete it and continue. */
                removed.add(entry);
                continue;
            }

            hammerrate *= rate;
        }

        /* Remove any expired entries */
        for (SessionEntry entry : removed) {
            CivGlobal.getSessionDB().delete(entry.request_id, entry.key);
        }

        return hammerrate;
    }

    public List<String> getMessages() {
        return savedMessages;
    }

    public Date getEndDate() {
        return new Date(this.startDate.getTime() + ((long) this.configRandomEvent.length * RandomEventSweeper.MILLISECONDS_PER_HOUR));
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void activate() throws CivException {
        if (this.active) {
            throw new CivException(CivSettings.localize.localizedString("re_alreadyActive"));
        }

        this.active = true;
        /* Start by processing all of the action components. */
        for (RandomEventComponent comp : this.actions.values()) {
            comp.process();
        }

        this.save();
    }
}
