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
package com.avrgaming.civcraft.object;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Relation.Status;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.*;

public class DiplomacyManager {

    /*
     * Manages diplomatic relationships for the object it is attached to.
     * Diplomatic relationships are stored in the SessionDB with this civ as the key.
     * There will be duplicate data that needs to be cleaned up for mutual relationships.
     * For example, if Civ A is at war with Civ B, both Civ A and Civ B will have a war relationship
     * entry.
     */

    private final Civilization ourCiv;

    /*
     * List of our relationships, hashed by civ id.
     */
    private final HashMap<UUID, Relation> relations = new HashMap<>();

    /* Number of civ's at war with us, will maintain this for fast isWar() lookups */
    private int warCount = 0;

    public DiplomacyManager(Civilization civ) {
        ourCiv = civ;
    }


    public boolean atWarWith(Civilization other) {
        if (ourCiv.getUUID().equals(other.getUUID())) {
            return false;
        }

        Relation relation = relations.get(other.getUUID());
        return relation != null && relation.getStatus() == Status.WAR;
    }

    public boolean isAtWar() {
        return (warCount != 0);
    }

    public void deleteRelation(Relation relation) {

        if (relation.getStatus() == Relation.Status.WAR &&
                relations.containsKey(relation.getOtherCiv().getUUID())) {
            warCount--;
            if (warCount < 0) {
                warCount = 0;
            }
        }
        relations.remove(relation.getOtherCiv().getUUID());

        Relation theirRelation = relation.getOtherCiv().getDiplomacyManager().getRelation(ourCiv);
        if (theirRelation != null) {
            try {
                relation.getOtherCiv().getDiplomacyManager().relations.remove(theirRelation.getOtherCiv().getUUID());
                theirRelation.delete();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        try {
            relation.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteAllRelations() {
        for (Relation relation : new LinkedList<>(relations.values())) {
            this.deleteRelation(relation);
        }

        this.relations.clear();
    }

    public void setAggressor(Civilization aggressor, Civilization otherCiv) {
        Relation relation = relations.get(otherCiv.getUUID());
        if (relation != null) {
            relation.setAggressor(aggressor);
            relation.save();
        }
    }

    public void setRelation(Civilization otherCiv, Relation.Status status, Date expires) {
        Relation relation = relations.get(otherCiv.getUUID());

        if (relation == null) {
            relations.put(otherCiv.getUUID(), new Relation(ourCiv, otherCiv, status, expires));
        } else {
            if (relation.getStatus() == status) {
                return;
            }

            if (relation.getStatus() == Relation.Status.WAR) {
                //Status was war, new status is not the same, so reduce our warcount.
                warCount--;
            }

            if (expires != null) {
                relation.setExpires(expires);
            }
            relation.setStatus(status);
//			if (status == Status.VASSAL) {
//				//End all wars with this civilization.
//				for (Relation rel : this.getRelations()) {
//					if (rel.getOtherCiv() != otherCiv) {
//						if (rel.getStatus() == Status.WAR) {
//							CivGlobal.setRelation(ourCiv, rel.getOtherCiv(), Status.NEUTRAL);
//							CivMessage.sendCiv(this.ourCiv, 
//									"Our war with "+rel.getOtherCiv().getName()+" has ended because we are now a vassal to "+otherCiv.getName());
//							CivMessage.sendCiv(rel.getOtherCiv(), 
//									"Our war with "+ourCiv.getName()+" has ended because they are now a vassal to "+otherCiv.getName());
//						}
//					}
//				}
//			}
        }

        if (status == Relation.Status.WAR) {
            warCount++;
        }
    }

    public Relation.Status getRelationStatus(Civilization otherCiv) {
        if (otherCiv.getUUID().equals(ourCiv.getUUID())) {
            return Relation.Status.ALLY;
        }

        Relation relation = relations.get(otherCiv.getUUID());
        if (relation == null) {
            return Relation.Status.NEUTRAL;
        }
        return relation.getStatus();
    }

    public Relation getRelation(Civilization otherCiv) {
        return relations.get(otherCiv.getUUID());
    }

    public void addRelation(Relation relation) {
        Relation currentRelation = relations.get(relation.getOtherCiv().getUUID());

        if (relation.getStatus() == Relation.Status.WAR) {
            if (currentRelation == null || currentRelation.getStatus() != Relation.Status.WAR) {
                warCount++;
            }
        }
        relations.put(relation.getOtherCiv().getUUID(), relation);
    }

    public Collection<Relation> getRelations() {
        return relations.values();
    }

    public int getWarCount() {
        return warCount;
    }

    public boolean atWarWith(Player attacker) {
        Resident resident = CivGlobal.getResident(attacker);
        if (resident == null) {
            return false;
        }
        if (!resident.hasTown()) {
            return false;
        }

        return atWarWith(resident.getTown().getCiv());
    }

    public ArrayList<Civilization> getAllies() {
        ArrayList<Civilization> allies = new ArrayList<>();
        for (Relation r : this.getRelations()) {
            if (r.getStatus() == Status.ALLY && r.getOtherCiv().getDiplomacyManager() != this) {
                allies.add(r.getOtherCiv());
            }
        }
        return allies;
    }

    public Relation.Status getRelationStatus(Player player) {
        Resident resident = CivGlobal.getResident(player);
        if (resident == null) {
            return Status.NEUTRAL;
        }
        if (!resident.hasTown()) {
            return Status.NEUTRAL;
        }

        return getRelationStatus(resident.getTown().getCiv());
    }

//	public Civilization getMasterCiv() {
//		for (Relation rel : this.relations.values()) {
//			if (rel.getStatus() == Status.VASSAL) {
//				return rel.getOtherCiv();
//			}
//		}
//		return null;
//	}


    public boolean isHostileWith(Resident resident) {
        return isHostileWith(resident.getCiv());
    }

    public boolean isHostileWith(Civilization civ) {
        Relation relation = this.relations.get(civ.getUUID());
        if (relation == null) {
            return false;
        }
        return switch (relation.getStatus()) {
            case WAR, HOSTILE -> true;
            default -> false;
        };
    }
}
