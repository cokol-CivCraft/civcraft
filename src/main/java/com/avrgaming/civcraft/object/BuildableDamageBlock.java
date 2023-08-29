package com.avrgaming.civcraft.object;

import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.util.BlockCoord;
import org.bukkit.entity.Player;

public interface BuildableDamageBlock {
    Buildable getOwner();

    void setOwner(Buildable owner);

    Town getTown();

    Civilization getCiv();

    BlockCoord getCoord();

    void setCoord(BlockCoord coord);

    int getX();

    int getY();

    int getZ();

    String getWorldname();

    boolean isDamageable();

    void setDamageable(boolean damageable);

    boolean canDestroyOnlyDuringWar();

    boolean allowDamageNow(Player player);
}
