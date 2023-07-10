package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.util.BlockCoord;

import java.util.List;

public interface RespawnLocationHolder {

    String getRespawnName();

    List<BlockCoord> getRespawnPoints();

    BlockCoord getRandomRevivePoint();

}
