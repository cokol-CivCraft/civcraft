package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.components.ProjectileArrowComponent;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.SimpleBlock;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

public class ArrowShip extends WaterStructure {

    ProjectileArrowComponent arrowComponent;
    private final HashMap<Integer, ProjectileArrowComponent> arrowTowers = new HashMap<>();


    protected ArrowShip(Location center, String id, Town town)
            throws CivException {
        super(center, id, town);
    }

    public ArrowShip(UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(uuid, nbt);
    }

    @Override
    public void loadSettings() {
        super.loadSettings();
        arrowComponent = new ProjectileArrowComponent(this, this.getCenterLocation().getLocation());
        arrowComponent.createComponent(this);
    }

    public void onPostBuild(BlockCoord absCoord, SimpleBlock commandBlock) {
        if (commandBlock.command.equals("/towerfire")) {
            String id = commandBlock.keyvalues.get("id");
            Integer towerID = Integer.valueOf(id);

            if (!arrowTowers.containsKey(towerID)) {

                ProjectileArrowComponent arrowTower = new ProjectileArrowComponent(this, absCoord.getLocation());
                arrowTower.createComponent(this);
                arrowTower.setTurretLocation(absCoord);

                arrowTowers.put(towerID, arrowTower);
            }
        }
    }

    /**
     * @return the damage
     */
    public int getDamage() {
        double rate = 1 + this.getTown().getBuffManager().getEffectiveDouble(Buff.FIRE_BOMB);
        return (int) (arrowComponent.getDamage() * rate);
    }

    /**
     * @param power the power to set
     */
    public void setPower(double power) {
        arrowComponent.setPower(power);
    }

    public void setTurretLocation(BlockCoord absCoord) {
        arrowComponent.setTurretLocation(absCoord);
    }

}
