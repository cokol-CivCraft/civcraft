package com.avrgaming.civcraft.structure.wonders;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.StructureChest;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.MultiInventory;
import org.bukkit.Location;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;

public class SpaceShuttle extends Wonder {
    private int level = 1;

    private int exp = 0;
    MultiInventory merc = new MultiInventory();
    MultiInventory fuel = new MultiInventory();
    ArrayList<StructureChest> fc = getAllChestsById(0), mc = getAllChestsById(1);

    public SpaceShuttle(ResultSet rs) throws SQLException, CivException {
        super(rs);
    }

    public SpaceShuttle(Location center, String id, Town town) throws CivException {
        super(center, id, town);
    }

    @Override
    protected void removeBuffs() {
        this.removeBuffFromCiv(this.getCiv(), Buff.SPACE);
        this.removeBuffFromCiv(this.getCiv(), Buff.TOTHEMOON);
    }

    @Override
    protected void addBuffs() {
        this.addBuffToCiv(this.getCiv(), Buff.SPACE);
        this.addBuffToCiv(this.getCiv(), Buff.TOTHEMOON);
    }

    @Override
    public void onEffectEvent() {
        merc.addChests(mc);
        fuel.addChests(fc);
        boolean starving = true;


        if (!starving) {
            Random r = new Random();
            this.addExp(r.nextInt(5));
        }
    }

    private int getExp() {
        return exp;
    }

    private void addExp(int i) {
        this.exp += i;
        this.updateLevel();
    }

    private int getLevel() {
        return level;
    }

    private void updateLevel() {
        int exp = getExp(), up = getToLVLup();
        if (exp >= up) {
            this.increaseLevel();
        }
    }

    private int getToLVLup() {
        int toLVLup = 8;
        return (int) Math.round(toLVLup * (getLevel() * getModifier()));
    }

    private void increaseLevel() {
        this.level++;
    }

    private double getModifier() {
        return 1.64;
    }

}