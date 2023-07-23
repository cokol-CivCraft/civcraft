package com.avrgaming.civcraft.structurevalidation;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.BuildableLayer;
import com.avrgaming.civcraft.template.TemplateStream;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CallbackInterface;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.SimpleBlock;
import org.bukkit.ChatColor;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class StructureValidator implements Runnable {

    private static String playerName = null;
    private static Buildable buildable = null;
    private static String templateFilepath = null;
    private static BlockCoord cornerLoc = null;
    private static CallbackInterface callback = null;


    /* Only validate a single structure at a time. */
    private static final ReentrantLock validationLock = new ReentrantLock();
    private static TemplateStream tplStream = null;


    /* Private tasks we'll reuse. */
    private static final SyncLoadSnapshotsFromLayer layerLoadTask = new SyncLoadSnapshotsFromLayer();


    private static final HashMap<ChunkCoord, ChunkSnapshot> chunks = new HashMap<>();

    /*
     * Validate an already existing buildable.
     */

    /* Instance variables that wait to be set while validator is running. */
    private String iPlayerName = null;
    private Buildable iBuildable = null;
    private String iTemplateName = null;
    private BlockCoord iCornerLoc = null;
    private CallbackInterface iCallback = null;
    private final boolean isWork = CivCraft.getIsValidate();

    public static boolean isEnabled() {
        String enabledStr;
        try {
            enabledStr = CivSettings.getString(CivSettings.civConfig, "global.structure_validation");
        } catch (InvalidConfiguration e) {
            e.printStackTrace();
            return false;
        }

        return enabledStr.equalsIgnoreCase("true");
    }

    public StructureValidator(Player player, Buildable bld) {
        if (player != null) {
            this.iPlayerName = player.getName();
        }
        this.iBuildable = bld;
    }

    public StructureValidator(Player player, String templateName, Location cornerLoc, CallbackInterface callback) {
        if (player != null) {
            this.iPlayerName = player.getName();
        }

        this.iTemplateName = templateName;
        this.iCornerLoc = new BlockCoord(cornerLoc);
        this.iCallback = callback;
    }

    private static class SyncLoadSnapshotsFromLayer implements Runnable {
        public List<SimpleBlock> bottomLayer;
        public StructureValidator notifyTask;

        public SyncLoadSnapshotsFromLayer() {
        }

        @Override
        public void run() {
            BlockCoord corner = new BlockCoord(cornerLoc);
            /*
             * Grab all of the chunk snapshots and go into async mode.
             */
            chunks.clear();

            for (SimpleBlock sb : bottomLayer) {
                Block next = corner.getBlock().getRelative(sb.x, corner.getY(), sb.z);
                ChunkCoord coord = new ChunkCoord(next.getLocation());

                if (chunks.containsKey(coord)) {
                    continue;
                }

                chunks.put(coord, next.getChunk().getChunkSnapshot());
            }

            synchronized (notifyTask) {
                notifyTask.notify();
            }
        }
    }


    public void finishValidate(HashMap<ChunkCoord, ChunkSnapshot> chunks, List<SimpleBlock> bottomLayer) {
        Player player = null;
        try {
            if (playerName != null) {
                player = CivGlobal.getPlayer(playerName);
            }
        } catch (CivException ignored) {
        }

        int checkedLevelCount = 0;
        boolean valid = true;
        String message = "";

        if (buildable == null) {
            return;
        }
        if (!isWork) {
            return;
        }

        for (int y = cornerLoc.getY() - 1; y > 0; y--) {
            checkedLevelCount++;
            double totalBlocks = 0;
            double reinforcementValue = 0;

            for (SimpleBlock sb : bottomLayer) {
                /* We only want the bottom layer of a template to be checked. */
                if (sb.getType() == Material.AIR) {
                    continue;
                }
                try {
                    int absX;
                    int absZ;
                    absX = cornerLoc.getX() + sb.x;
                    absZ = cornerLoc.getZ() + sb.z;

                    Material type = Buildable.getBlockIDFromSnapshotMap(chunks, absX, y, absZ, cornerLoc.getWorldname());
                    totalBlocks++;
                    reinforcementValue += Buildable.getReinforcementValue(type);
                } catch (CivException e) {
                    e.printStackTrace();
                    break;
                }
            }

            double percentValid = reinforcementValue / totalBlocks;
            buildable.layerValidPercentages.put(y, new BuildableLayer((int) reinforcementValue, (int) totalBlocks));

            if (valid) {
                if (percentValid < Buildable.getReinforcementRequirementForLevel(checkedLevelCount)) {
                    DecimalFormat df = new DecimalFormat();
                    message = CivSettings.localize.localizedString("var_structureValidator_layerInvalid", y, df.format(percentValid * 100), (reinforcementValue + "/" + totalBlocks), df.format(Buildable.validPercentRequirement * 100));
                    valid = false;
                }
            }
        }

        buildable.validated = true;
        buildable.invalidLayerMessage = message;
        buildable.setValid(valid);

        if (player != null) {
            CivMessage.sendError(player, message);
            if (player.isOp()) {
                CivMessage.send(player, ChatColor.GRAY + CivSettings.localize.localizedString("structureValidator_isOP"));
                valid = true;
            }

            if (valid) {
                CivMessage.send(player, ChatColor.GREEN + CivSettings.localize.localizedString("structureValidator_isValid"));
                buildable.setValid(true);
                buildable.invalidLayerMessage = "";
            }
        }

        if (callback != null) {
            if (valid || (player != null && player.isOp())) {
                callback.execute(playerName);
            }
        }
    }

    private static void cleanup() {
        playerName = null;
        buildable = null;
        cornerLoc = null;
        templateFilepath = null;
        callback = null;
        validationLock.unlock();
    }


    @Override
    public void run() {
        if (!isEnabled()) {
            iBuildable.validated = true;
            iBuildable.setValid(true);
            return;
        }

        /* Wait for validation lock to open. */
        validationLock.lock();

        try {
            /* Copy over instance variables to static variables. */
            playerName = iPlayerName;
            if (iBuildable != null) {
                if (iBuildable.isIgnoreFloating()) {
                    iBuildable.validated = true;
                    iBuildable.setValid(true);
                    return;
                }
                buildable = iBuildable;
                cornerLoc = this.iBuildable.getCorner();
                templateFilepath = this.iBuildable.getSavedTemplatePath();
            } else {
                cornerLoc = this.iCornerLoc;
                templateFilepath = this.iTemplateName;
            }
            callback = this.iCallback;

            List<SimpleBlock> bottomLayer;

            /* Load the template stream. */
            if (tplStream == null) {
                tplStream = new TemplateStream(templateFilepath);
            } else {
                tplStream.setSource(templateFilepath);
            }

            bottomLayer = tplStream.getBlocksForLayer(0);

            /* Launch sync layer load task. */
            layerLoadTask.bottomLayer = bottomLayer;
            layerLoadTask.notifyTask = this;
            TaskMaster.syncTask(layerLoadTask);

            /* Wait for sync task to notify us to continue. */
            synchronized (this) {
                this.wait();
            }

            this.finishValidate(chunks, bottomLayer);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }
}
