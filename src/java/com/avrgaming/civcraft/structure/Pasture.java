package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.LoadPastureEntityTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class Pasture extends Structure {

    /* Global pasture chunks */
    public static Map<ChunkCoord, Pasture> pastureChunks = new ConcurrentHashMap<>();
    public static Map<UUID, Pasture> pastureEntities = new ConcurrentHashMap<>();

    /* Chunks bound to this pasture. */
    public HashSet<ChunkCoord> chunks = new HashSet<>();
    public HashSet<UUID> entities = new HashSet<>();
    public ReentrantLock lock = new ReentrantLock();

    private int pendingBreeds = 0;

    protected Pasture(Location center, String id, Town town)
            throws CivException {
        super(center, id, town);
    }

    public Pasture(ResultSet rs) throws SQLException, CivException {
        super(rs);
    }

    public int getMobCount() {
        return entities.size();
    }

    public int getMobMax() {
        return CivSettings.structureConfig.getInt("pasture.max_mobs", 30);
    }

    public boolean processMobBreed(Player player, EntityType type) {

        if (!this.isActive()) {
            CivMessage.sendError(player, CivSettings.localize.localizedString("pasture_destroyed"));
            return false;
        }

        if (this.getMobCount() >= this.getMobMax()) {
            CivMessage.sendError(player, CivSettings.localize.localizedString("pasture_isFull"));
            return false;
        }

        if ((getPendingBreeds() + this.getMobCount()) >= this.getMobMax()) {
            CivMessage.sendError(player, CivSettings.localize.localizedString("pasture_TooMuchorIsFull", CivSettings.localize.localizedString("pasture_isFull")));
            return false;
        }

        return true;
    }

    public void bindPastureChunks() {
        for (BlockCoord bcoord : this.structureBlocks.keySet()) {
            ChunkCoord coord = new ChunkCoord(bcoord);
            this.chunks.add(coord);
            pastureChunks.put(coord, this);
        }
    }

    public void unbindPastureChunks() {
        for (ChunkCoord coord : this.chunks) {
            pastureChunks.remove(coord);
        }

        this.entities.clear();
        this.chunks.clear();

        LinkedList<UUID> removeUs = new LinkedList<>();
        for (UUID id : pastureEntities.keySet()) {
            Pasture pasture = pastureEntities.get(id);
            if (pasture == this) {
                removeUs.add(id);
            }
        }

        for (UUID id : removeUs) {
            pastureEntities.remove(id);
        }

    }

    @Override
    public void onComplete() {
        bindPastureChunks();
    }

    @Override
    public void onLoad() {
        bindPastureChunks();
        loadEntities();
    }

    @Override
    public void delete() throws SQLException {
        super.delete();
        unbindPastureChunks();
        clearEntities();
    }

    public void clearEntities() {
        // TODO Clear entities bound to us?
    }

    public void onBreed(LivingEntity entity) {
        saveEntity(entity.getWorld().getName(), entity.getUniqueId());
        setPendingBreeds(getPendingBreeds() - 1);
    }

    public String getEntityKey() {
        return "pasture:" + this.getId();
    }

    public String getValue(String worldName, UUID id) {
        return worldName + ":" + id;
    }

    public void saveEntity(String worldName, UUID id) {
        TaskMaster.asyncTask(() -> {
            Pasture.this.sessionAdd(getEntityKey(), getValue(worldName, id));
            lock.lock();
            try {
                entities.add(id);
                pastureEntities.put(id, Pasture.this);
            } finally {
                lock.unlock();
            }
        }, 0);
    }

    public void loadEntities() {
        ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getEntityKey());
        Queue<SessionEntry> entriesToLoad = new LinkedList<>(entries);
        TaskMaster.syncTask(new LoadPastureEntityTask(entriesToLoad, this));
    }

    public void onEntityDeath(LivingEntity entity) {
        TaskMaster.asyncTask(() -> {
            lock.lock();
            try {
                entities.remove(entity.getUniqueId());
                pastureEntities.remove(entity.getUniqueId());
            } finally {
                lock.unlock();
            }
        }, 0);
    }

    public int getPendingBreeds() {
        return pendingBreeds;
    }

    public void setPendingBreeds(int pendingBreeds) {
        this.pendingBreeds = pendingBreeds;
    }

}
