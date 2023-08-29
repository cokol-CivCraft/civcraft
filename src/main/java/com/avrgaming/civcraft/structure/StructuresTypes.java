package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.wonders.*;
import org.bukkit.Location;

import java.sql.ResultSet;
import java.sql.SQLException;

@SuppressWarnings("unused")
public enum StructuresTypes {
    BASE(Structure::new, Structure::new),
    THE_GREAT_PYRAMID(TheGreatPyramid::new, TheGreatPyramid::new),
    GREAT_LIBRARY(GreatLibrary::new, GreatLibrary::new),
    THE_HANGING_GARDENS(TheHangingGardens::new, TheHangingGardens::new),
    THE_COLOSSUS(TheColossus::new, TheColossus::new),
    NOTRE_DAME(NotreDame::new, NotreDame::new),
    CHICHEN_ITZA(ChichenItza::new, ChichenItza::new),
    COUNCIL_OF_EIGHT(CouncilOfEight::new, CouncilOfEight::new),
    COLOSSEUM(Colosseum::new, Colosseum::new),
    GLOBE_THEATRE(GlobeTheatre::new, GlobeTheatre::new),
    GREAT_LIGHTHOUSE(GreatLighthouse::new, GreatLighthouse::new),
    MOTHER_TREE(MotherTree::new, MotherTree::new),
    GRAND_SHIP_INGERMANLAND(GrandShipIngermanland::new, GrandShipIngermanland::new),
    BANK(Bank::new, Bank::new),
    TROMMEL(Trommel::new, Trommel::new),
    FISH_HATCHERY(FishHatchery::new, FishHatchery::new),
    TRADE_SHIP(TradeShip::new, TradeShip::new),
    QUARRY(Quarry::new, Quarry::new),
    MOB_GRINDER(MobGrinder::new, MobGrinder::new),
    STORE(Store::new, Store::new),
    STADIUM(Stadium::new, Stadium::new),
    HOSPITAL(Hospital::new, Hospital::new),
    GROCER(Grocer::new, Grocer::new),
    BROADCAST_TOWER(BroadcastTower::new, BroadcastTower::new),
    LIBRARY(Library::new, Library::new),
    UNIVERSITY(University::new, University::new),
    SCHOOL(School::new, School::new),
    RESEARCH_LAB(ResearchLab::new, ResearchLab::new),
    BLACKSMITH(Blacksmith::new, Blacksmith::new),
    GRANARY(Granary::new, Granary::new),
    COTTAGE(Cottage::new, Cottage::new),
    MONUMENT(Monument::new, Monument::new),
    TEMPLE(Temple::new, Temple::new),
    MINE(Mine::new, Mine::new),
    FARM(Farm::new, Farm::new),
    TRADE_OUTPOST(TradeOutpost::new, TradeOutpost::new),
    FISHING_BOAT(FishingBoat::new, FishingBoat::new),
    TOWN_HALL(TownHall::new, TownHall::new),
    CAPITOL(Capitol::new, Capitol::new),
    ARROW_SHIP(ArrowShip::new, ArrowShip::new),
    ARROW_TOWER(ArrowTower::new, ArrowTower::new),
    CANNON_SHIP(CannonShip::new, CannonShip::new),
    CANNON_TOWER(CannonTower::new, CannonTower::new),
    SCOUT_SHIP(ScoutShip::new, ScoutShip::new),
    SCOUT_TOWER(ScoutTower::new, ScoutTower::new),
    SHIPYARD(Shipyard::new, Shipyard::new),
    BARRACKS(Barracks::new, Barracks::new),
    WINDMILL(Windmill::new, Windmill::new),
    MUSEUM(Museum::new, Museum::new),
    MARKET(Market::new, Market::new),
    STABLE(Stable::new, Stable::new),
    PASTURE(Pasture::new, Pasture::new),
    LIGHTHOUSE(Lighthouse::new, Lighthouse::new),
    TESLA_TOWER(TeslaTower::new, TeslaTower::new),
    ;
    private final CreateRS createRS;
    private final CreateTown createTown;

    StructuresTypes(CreateRS createRS, CreateTown createTown) {
        this.createRS = createRS;
        this.createTown = createTown;
    }

    MetaStructure create(ResultSet rs) throws SQLException, CivException {
        return createRS.run(rs);
    }

    MetaStructure create(Location center, String id, Town town) throws CivException {
        return createTown.run(center, id, town);
    }

    @FunctionalInterface
    interface CreateRS {
        MetaStructure run(ResultSet rs) throws SQLException, CivException;
    }

    @FunctionalInterface
    interface CreateTown {
        MetaStructure run(Location center, String id, Town town) throws CivException;
    }
}
