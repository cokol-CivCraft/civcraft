package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.util.BlockCoord;

public interface RespawnLocationHolder {

    String getRespawnName();

    BlockCoord getRandomRevivePoint();

}
