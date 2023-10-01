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

import javax.annotation.Nonnull;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class StructureValidator implements Runnable {
    /* Only validate a single structure at a time. */
    private static final ReentrantLock validationLock = new ReentrantLock();

    private static final HashMap<ChunkCoord, ChunkSnapshot> chunks = new HashMap<>();

    /*
     * Validate an already existing buildable.
     */

    /* Instance variables that wait to be set while validator is running. */
    private final String iPlayerName;
    private Buildable iBuildable = null;
    private final String iTemplateName;
    private final BlockCoord iCornerLoc;
    private final CallbackInterface iCallback;
    private final boolean isWork = CivCraft.getIsValidate();

    public StructureValidator(Player player, @Nonnull Buildable bld) {
        if (player != null) {
            this.iPlayerName = player.getName();
        } else {
            this.iPlayerName = null;
        }
        this.iBuildable = bld;
        this.iCornerLoc = new BlockCoord(bld.getCorner());
        this.iCallback = (name) -> {
        };
        this.iTemplateName = bld.getSavedTemplatePath();
    }

    public StructureValidator(Player player, @Nonnull String templateName, @Nonnull Location cornerLoc, @Nonnull CallbackInterface callback) {
        if (player != null) {
            this.iPlayerName = player.getName();
        } else {
            this.iPlayerName = null;
        }

        this.iTemplateName = templateName;
        this.iCornerLoc = new BlockCoord(cornerLoc);
        this.iCallback = callback;
    }

    public static boolean isDisabled() {
        boolean enabledStr;
        try {
            enabledStr = CivSettings.getBoolean(CivSettings.civConfig, "global.structure_validation");
        } catch (InvalidConfiguration e) {
            e.printStackTrace();
            return true;
        }

        return !enabledStr;
    }

    private void finishValidate(List<SimpleBlock> bottomLayer) {
        Player player = null;
        try {
            if (iPlayerName != null) {
                player = CivGlobal.getPlayer(iPlayerName);
            }
        } catch (CivException ignored) {
        }

        int checkedLevelCount = 0;
        boolean valid = true;
        String message = "";

        if (!isWork) {
            return;
        }

        for (int y = iCornerLoc.getY() - 1; y > 0; y--) {
            checkedLevelCount++;
            double totalBlocks = 0;
            double reinforcementValue = 0;

            for (SimpleBlock sb : bottomLayer) {
                /* We only want the bottom layer of a template to be checked. */
                if (sb.getType() == Material.AIR) {
                    continue;
                }
                try {
                    int absX = iCornerLoc.getX() + sb.x;
                    int absZ = iCornerLoc.getZ() + sb.z;

                    Material type = Buildable.getBlockIDFromSnapshotMap(StructureValidator.chunks, absX, y, absZ, iCornerLoc.getWorldname());
                    totalBlocks++;
                    reinforcementValue += Buildable.getReinforcementValue(type);
                } catch (CivException e) {
                    e.printStackTrace();
                    break;
                }
            }

            double percentValid = reinforcementValue / totalBlocks;
            if (iBuildable != null) {
                iBuildable.layerValidPercentages.put(y, new BuildableLayer((int) reinforcementValue, (int) totalBlocks));
            }

            if (valid) {
                if (percentValid < Buildable.getReinforcementRequirementForLevel(checkedLevelCount)) {
                    DecimalFormat df = new DecimalFormat();
                    message = CivSettings.localize.localizedString("var_structureValidator_layerInvalid", y, df.format(percentValid * 100), (reinforcementValue + "/" + totalBlocks), df.format(Buildable.validPercentRequirement * 100));
                    valid = false;
                }
            }
        }
        if (iBuildable != null) {
            iBuildable.validated = true;
            iBuildable.invalidLayerMessage = message;
            iBuildable.setValid(valid);
        }

        if (player != null) {
            CivMessage.sendError(player, message);
            if (player.isOp()) {
                CivMessage.send(player, ChatColor.GRAY + CivSettings.localize.localizedString("structureValidator_isOP"));
                valid = true;
            }

            if (valid) {
                CivMessage.send(player, ChatColor.GREEN + CivSettings.localize.localizedString("structureValidator_isValid"));
                if (iBuildable != null) {
                    iBuildable.setValid(true);
                    iBuildable.invalidLayerMessage = "";
                }
            }
        }

        if (iCallback != null) {
            if (valid || (player != null && player.isOp())) {
                iCallback.execute(iPlayerName);
            }
        }
    }


    @Override
    public void run() {
        if (isDisabled()) {
            iBuildable.validated = true;
            iBuildable.setValid(true);
            return;
        }

        /* Wait for validation lock to open. */
        validationLock.lock();

        try {
            /* Copy over instance variables to static variables. */
            if (iBuildable != null) {
                if (iBuildable.isIgnoreFloating()) {
                    iBuildable.validated = true;
                    iBuildable.setValid(true);
                    return;
                }
            }

            /* Load the template stream. */
            TemplateStream tplStream = new TemplateStream(this.iTemplateName);

            List<SimpleBlock> bottomLayer = tplStream.getBlocksForLayer(0);

            /* Launch sync layer load task. */
            TaskMaster.syncTask(() -> {
                /*
                 * Grab all of the chunk snapshots and go into async mode.
                 */
                chunks.clear();

                for (SimpleBlock sb : bottomLayer) {
                    Block next = iCornerLoc.getBlock().getRelative(sb.x, iCornerLoc.getY(), sb.z);
                    ChunkCoord coord = new ChunkCoord(next.getLocation());

                    if (chunks.containsKey(coord)) {
                        continue;
                    }

                    chunks.put(coord, next.getChunk().getChunkSnapshot());
                }

                synchronized (StructureValidator.this) {
                    StructureValidator.this.notify();
                }
            });

            /* Wait for sync task to notify us to continue. */
            synchronized (this) {
                this.wait();
            }

            this.finishValidate(bottomLayer);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            validationLock.unlock();
        }
    }
}
