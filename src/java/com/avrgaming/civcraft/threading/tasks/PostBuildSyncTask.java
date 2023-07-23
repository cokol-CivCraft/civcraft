/*
 * AVRGAMING LLC
 * __________________
 *
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.threading.tasks;


import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.StructureChest;
import com.avrgaming.civcraft.object.StructureSign;
import com.avrgaming.civcraft.structure.*;
import com.avrgaming.civcraft.structure.wonders.GrandShipIngermanland;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.SimpleBlock;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Sign;

import java.util.Optional;

public class PostBuildSyncTask implements Runnable {

    /*
     * Search the template for special command blocks and handle them *after* the structure
     * has finished building.
     */

    Template tpl;
    Buildable buildable;

    public PostBuildSyncTask(Template tpl, Buildable buildable) {
        this.tpl = tpl;
        this.buildable = buildable;
    }

    public static void validate(Template tpl, Buildable buildable) {

        /*
         * Use the location's of the command blocks in the template and the buildable corner
         * to find their real positions. Then perform any special building we may want to do
         * at those locations.
         */
        /* These block coords do not point to a location in the world, just a location in the template. */
        for (BlockCoord relativeCoord : tpl.commandBlockRelativeLocations) {
            SimpleBlock sb = tpl.blocks[relativeCoord.getX()][relativeCoord.getY()][relativeCoord.getZ()];
            StructureSign structSign;
            Block block;
            BlockCoord absCoord = new BlockCoord(buildable.getCorner().getBlock().getRelative(relativeCoord.getX(), relativeCoord.getY(), relativeCoord.getZ()));

            if (!(sb.getMaterialData() instanceof Sign)) {
                buildable.onPostBuild(absCoord, sb);
                continue;
            }
            /* Signs and chests should already be handled, look for more exotic things. */
            switch (sb.command) {
                case "/tradeoutpost":
                    /* Builds the trade outpost tower at this location. */
                    if (buildable instanceof TradeOutpost) {
                        TradeOutpost outpost = (TradeOutpost) buildable;
                        outpost.setTradeOutpostTower(absCoord);
                        try {
                            outpost.build_trade_outpost_tower();
                        } catch (CivException e) {
                            e.printStackTrace();
                        }

                    }
                    break;
                case "/techbar":
                    if (buildable instanceof TownHall) {
                        TownHall townhall = (TownHall) buildable;

                        int index = Integer.parseInt(sb.keyvalues.get("id"));
                        townhall.addTechBarBlock(absCoord, index);

                    }
                    break;
                case "/techname":
                    if (buildable instanceof TownHall) {
                        TownHall townhall = (TownHall) buildable;

                        townhall.setTechnameSign(absCoord);
                        townhall.setTechnameSignData((byte) sb.getData());

                    }
                    break;
                case "/techdata":
                    if (buildable instanceof TownHall) {
                        TownHall townhall = (TownHall) buildable;

                        townhall.setTechdataSign(absCoord);
                        townhall.setTechdataSignData((byte) sb.getData());

                    }
                    break;
                case "/itemframe":
                    String strvalue = sb.keyvalues.get("id");
                    if (strvalue != null) {
                        int index = Integer.parseInt(strvalue);

                        if (buildable instanceof TownHall) {
                            TownHall townhall = (TownHall) buildable;
                            townhall.createGoodieItemFrame(absCoord, index, sb.getData());
                            townhall.addStructureBlock(absCoord, false);
                        }
                    }
                    break;
                case "/respawn":
                    if (buildable instanceof TownHall) {
                        TownHall townhall = (TownHall) buildable;

                        townhall.setRespawnPoint(absCoord);
                    }
                    break;
                case "/revive":
                    if (buildable instanceof TownHall) {
                        TownHall townhall = (TownHall) buildable;

                        townhall.setRevivePoint(absCoord);
                    }
                    break;
                case "/towerfire":
                    if (buildable instanceof ArrowShip) {
                        ArrowShip arrowship = (ArrowShip) buildable;
                        arrowship.setTurretLocation(absCoord);
                    } else if (buildable instanceof ArrowTower) {
                        ArrowTower arrowtower = (ArrowTower) buildable;
                        arrowtower.setTurretLocation(absCoord);
                    } else if (buildable instanceof CannonShip) {
                        CannonShip cannonship = (CannonShip) buildable;
                        cannonship.setTurretLocation(absCoord);
                    } else if (buildable instanceof CannonTower) {
                        CannonTower cannontower = (CannonTower) buildable;
                        cannontower.setTurretLocation(absCoord);
                    } else if (buildable instanceof TeslaTower) {
                        TeslaTower teslaTower = (TeslaTower) buildable;
                        teslaTower.setTurretLocation(absCoord);
                    }

                    break;
                case "/arrowfire":
                    if (buildable instanceof GrandShipIngermanland) {
                        GrandShipIngermanland arrowtower = (GrandShipIngermanland) buildable;
                        arrowtower.setArrowLocation(absCoord);
                    }
                    break;
                case "/cannonfire":
                    if (buildable instanceof GrandShipIngermanland) {
                        GrandShipIngermanland cannontower = (GrandShipIngermanland) buildable;
                        cannontower.setCannonLocation(absCoord);
                    }
                    break;
                case "/sign":
                    structSign = CivGlobal.getStructureSign(absCoord);
                    if (structSign == null) {
                        structSign = new StructureSign(absCoord, buildable);
                    }
                    block = absCoord.getBlock();
                    block.setType(sb.getType());
                    block.setData((byte) sb.getData());

                    structSign.setDirection(block.getState().getRawData());
                    for (String key : sb.keyvalues.keySet()) {
                        structSign.setType(key);
                        structSign.setAction(sb.keyvalues.get(key));
                        break;
                    }

                    structSign.setOwner(buildable);
                    buildable.addStructureSign(structSign);
                    CivGlobal.addStructureSign(structSign);

                    break;
                case "/chest":
                    StructureChest structChest = CivGlobal.getStructureChest(absCoord);
                    if (structChest == null) {
                        structChest = new StructureChest(absCoord, buildable);
                    }
                    structChest.setChestId(Integer.parseInt(sb.keyvalues.get("id")));
                    buildable.addStructureChest(structChest);
                    CivGlobal.addStructureChest(structChest);

                    /* Convert sign data to chest data.*/
                    block = absCoord.getBlock();
                    if (block.getType() != Material.CHEST) {
                        block.getState().setData(new org.bukkit.material.Chest(((Sign) sb.getMaterialData()).getFacing()));
                    }

                    Chest chest = (Chest) block.getState();
                    MaterialData data = chest.getData();
//					ItemManager.setData(data, chestData);
                    chest.setData(data);
                    chest.update();
//				}

                    break;
            }

            buildable.onPostBuild(absCoord, sb);
        }

        //	if (buildable instanceof Structure) {
        buildable.updateSignText();
        //}
    }

    public static void start(Template tpl, Buildable buildable) {
        for (BlockCoord relativeCoord : tpl.doorRelativeLocations) {
            SimpleBlock sb = tpl.blocks[relativeCoord.getX()][relativeCoord.getY()][relativeCoord.getZ()];
            BlockCoord absCoord = new BlockCoord(buildable.getCorner().getBlock().getRelative(relativeCoord.getX(), relativeCoord.getY(), relativeCoord.getZ()));

            Block block = absCoord.getBlock();
            if (block.getType() == sb.getType()) {
                continue;
            }
            if (buildable.getCiv().isAdminCiv()) {
                block.getState().setData(new MaterialData(Material.AIR));
            } else {
                block.getState().setData(sb.getMaterialData());
            }
        }

        for (BlockCoord relativeCoord : tpl.attachableLocations) {
            SimpleBlock sb = tpl.blocks[relativeCoord.getX()][relativeCoord.getY()][relativeCoord.getZ()];
            BlockCoord absCoord = new BlockCoord(buildable.getCorner().getBlock().getRelative(relativeCoord.getX(), relativeCoord.getY(), relativeCoord.getZ()));

            Block block = absCoord.getBlock();
            if (block.getType() != sb.getType()) {
                block.getState().setData(sb.getMaterialData());
            }
        }

        /*
         * Use the location's of the command blocks in the template and the buildable's corner
         * to find their real positions. Then perform any special building we may want to do
         * at those locations.
         */
        /* These block coords do not point to a location in the world, just a location in the template. */
        for (BlockCoord relativeCoord : tpl.commandBlockRelativeLocations) {
            SimpleBlock sb = tpl.blocks[relativeCoord.getX()][relativeCoord.getY()][relativeCoord.getZ()];
            Block block;
            BlockCoord absCoord = new BlockCoord(buildable.getCorner().getBlock().getRelative(relativeCoord.getX(), relativeCoord.getY(), relativeCoord.getZ()));
            if (!(sb.getMaterialData() instanceof org.bukkit.material.Sign)) {
                buildable.onPostBuild(absCoord, sb);
                continue;
            }
            /* Signs and chests should already be handled, look for more exotic things. */
            switch (sb.command) {
                case "/tradeoutpost":
                    /* Builds the trade outpost tower at this location. */
                    if (buildable instanceof TradeOutpost) {
                        TradeOutpost outpost = (TradeOutpost) buildable;
                        outpost.setTradeOutpostTower(absCoord);
                        try {
                            outpost.build_trade_outpost_tower();
                        } catch (CivException e) {
                            e.printStackTrace();
                        }

                    }
                    break;

                case "/techbar":
                    if (buildable instanceof TownHall) {
                        TownHall townhall = (TownHall) buildable;

                        int index = Integer.parseInt(sb.keyvalues.get("id"));
                        townhall.addTechBarBlock(absCoord, index);

                    }
                    break;
                case "/techname":
                    if (buildable instanceof TownHall) {
                        TownHall townhall = (TownHall) buildable;

                        townhall.setTechnameSign(absCoord);
                        townhall.setTechnameSignData((byte) sb.getData());

                    }
                    break;
                case "/techdata":
                    if (buildable instanceof TownHall) {
                        TownHall townhall = (TownHall) buildable;

                        townhall.setTechdataSign(absCoord);
                        townhall.setTechdataSignData((byte) sb.getData());

                    }
                    break;
                case "/itemframe":
                    String strvalue = sb.keyvalues.get("id");
                    if (strvalue != null) {
                        int index = Integer.parseInt(strvalue);

                        if (buildable instanceof TownHall) {
                            TownHall townhall = (TownHall) buildable;
                            townhall.createGoodieItemFrame(absCoord, index, sb.getData());
                            townhall.addStructureBlock(absCoord, false);
                        }
                    }
                    break;
                case "/respawn":
                    if (buildable instanceof TownHall) {
                        TownHall townhall = (TownHall) buildable;

                        townhall.setRespawnPoint(absCoord);
                    }
                    break;
                case "/revive":
                    if (buildable instanceof TownHall) {
                        TownHall townhall = (TownHall) buildable;

                        townhall.setRevivePoint(absCoord);
                    }
                    break;
                case "/control":
                    if (buildable instanceof TownHall) {
                        TownHall townhall = (TownHall) buildable;
                        townhall.createControlPoint(absCoord);
                    }
                    break;
                case "/towerfire":
                    if (buildable instanceof ArrowShip) {
                        ArrowShip arrowship = (ArrowShip) buildable;
                        arrowship.setTurretLocation(absCoord);
                    } else if (buildable instanceof ArrowTower) {
                        ArrowTower arrowtower = (ArrowTower) buildable;
                        arrowtower.setTurretLocation(absCoord);
                    } else if (buildable instanceof CannonShip) {
                        CannonShip cannonship = (CannonShip) buildable;
                        cannonship.setTurretLocation(absCoord);
                    } else if (buildable instanceof CannonTower) {
                        CannonTower cannontower = (CannonTower) buildable;
                        cannontower.setTurretLocation(absCoord);
                    } else if (buildable instanceof TeslaTower) {
                        TeslaTower teslaTower = (TeslaTower) buildable;
                        teslaTower.setTurretLocation(absCoord);
                    }
                    break;
                case "/arrowfire":
                    if (buildable instanceof GrandShipIngermanland) {
                        GrandShipIngermanland arrowtower = (GrandShipIngermanland) buildable;
                        arrowtower.setArrowLocation(absCoord);
                    }
                    break;
                case "/cannonfire":
                    if (buildable instanceof GrandShipIngermanland) {
                        GrandShipIngermanland cannontower = (GrandShipIngermanland) buildable;
                        cannontower.setCannonLocation(absCoord);
                    }
                    break;
                case "/sign":
                    StructureSign structSign = Optional.ofNullable(CivGlobal.getStructureSign(absCoord)).orElseGet(() -> new StructureSign(absCoord, buildable));
                    block = absCoord.getBlock();
                    block.setType(sb.getType());
                    block.setData((byte) sb.getData());

                    structSign.setDirection(block.getState().getRawData());
                    for (String key : sb.keyvalues.keySet()) {
                        structSign.setType(key);
                        structSign.setAction(sb.keyvalues.get(key));
                        break;
                    }

                    structSign.setOwner(buildable);
                    buildable.addStructureSign(structSign);
                    CivGlobal.addStructureSign(structSign);

                    break;
                case "/chest":
                    StructureChest structChest = Optional.ofNullable(CivGlobal.getStructureChest(absCoord)).orElseGet(() -> new StructureChest(absCoord, buildable));
                    structChest.setChestId(Integer.parseInt(sb.keyvalues.get("id")));
                    buildable.addStructureChest(structChest);
                    CivGlobal.addStructureChest(structChest);

                    /* Convert sign data to chest data.*/
                    block = absCoord.getBlock();
                    if (block.getType() != Material.CHEST) {
                        block.getState().setData(new org.bukkit.material.Chest(((Sign) sb.getMaterialData()).getFacing()));
                    }

                    Chest chest = (Chest) block.getState();
                    MaterialData data = chest.getData();
//					ItemManager.setData(data, chestData);
                    chest.setData(data);
                    chest.update();
//				}

                    break;
            }

            buildable.onPostBuild(absCoord, sb);
        }
        /* Run the tech bar task now in order to protect the blocks */
        if (buildable instanceof TownHall) {
            UpdateTechBar techbartask = new UpdateTechBar(buildable.getCiv());
            techbartask.run();
        }

        //	if (buildable instanceof Structure) {
        buildable.updateSignText();
        //}
    }

    @Override
    public void run() {
        PostBuildSyncTask.start(tpl, buildable);
    }

}
