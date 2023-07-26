package com.avrgaming.civcraft.config;

import com.avrgaming.civcraft.util.BlockCoord;

import java.util.LinkedList;

public class ConfigArenaTeam {
	public Integer number;
	public String name;
	public LinkedList<BlockCoord> controlPoints;
	public LinkedList<BlockCoord> revivePoints;
	public LinkedList<BlockCoord> respawnPoints;
	public LinkedList<BlockCoord> chests;
	public BlockCoord respawnSign;
}
