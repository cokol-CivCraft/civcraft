package com.avrgaming.civcraft.structure;

import java.util.List;

import com.avrgaming.civcraft.util.BlockCoord;

public interface RespawnLocationHolder {

    String getRespawnName();

    List<BlockCoord> getRespawnPoints();

    BlockCoord getRandomRevivePoint();

}
