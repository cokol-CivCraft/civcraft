package com.avrgaming.civcraft.provider;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMaterial;
import org.bukkit.DyeColor;
import org.bukkit.GrassSpecies;
import org.bukkit.Material;
import org.bukkit.SandstoneType;
import org.bukkit.entity.EntityType;
import org.bukkit.material.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@ParametersAreNonnullByDefault
public class DefaultMaterialProvider {
    public static final List<ConfigMaterial> entries = new ArrayList<>();
    public static final String T1_MATERIAL = "<lightgreen>Tier 1 Materials";
    public static final String T2_MATERIAL = "<lightblue>Tier 2 Materials";
    public static final String T3_MATERIAL = "<lightpurple>Tier 3 Materials";
    public static final String T4_MATERIAL = "<gold>Tier 4 Materials";
    public static final String JUNK_FISH = "<lightpurple>Fishing Junk";
    public static final String T1_FISH = "<lightgreen>Fish Tier 1";
    public static final String T2_FISH = "<lightblue>Fish Tier 2";
    public static final String T3_FISH = "<lightpurple>Fish Tier 3";
    public static final String T4_FISH = "<gold>Fish Tier 4";
    public static final ConfigMaterial CARVED_LEATHER =
            material(
                    "mat_carved_leather",
                    "T1 Carved Leather",
                    new MaterialData(Material.LEATHER)
            ).category(T1_MATERIAL).end();
    public static final ConfigMaterial REFINED_STONE =
            material(
                    "mat_refined_stone",
                    "T1 Refined Stone",
                    new MaterialData(Material.STONE)
            ).category(T1_MATERIAL).end();

    public static final ConfigMaterial REFINED_FEATHERS =
            material(
                    "mat_refined_feathers",
                    "T1 Refined Stone",
                    new MaterialData(Material.QUARTZ)
            ).category(T1_MATERIAL).end();

    public static final ConfigMaterial CRAFTED_STRING =
            material(
                    "mat_crafted_string",
                    "T1 Crafted String",
                    new MaterialData(Material.STRING)
            ).category(T1_MATERIAL).end();

    public static final ConfigMaterial CRAFTED_STICK =
            material(
                    "mat_crafted_sticks",
                    "T1 Crafted Sticks",
                    new MaterialData(Material.STICK)
            ).category(T1_MATERIAL).end();
    public static final ConfigMaterial REFINED_SULPHUR =
            material(
                    "mat_refined_sulphur",
                    "T1 Refined Sulphur",
                    new MaterialData(Material.SULPHUR)
            ).category(T1_MATERIAL).end();
    public static final ConfigMaterial COMPACTED_SAND =
            material(
                    "mat_compacted_sand",
                    "T1 Compacted Sand",
                    new Sandstone(SandstoneType.GLYPHED)
            ).category(T1_MATERIAL).end();
    public static final ConfigMaterial FORGED_CLAY =
            material(
                    "mat_forged_clay",
                    "T1 Forged Clay",
                    new MaterialData(Material.CLAY_BRICK)
            ).category(T1_MATERIAL).end();
    public static final ConfigMaterial CRAFTED_REEDS =
            material(
                    "mat_crafted_reeds",
                    "T1 Crafted Reeds",
                    new MaterialData(Material.SUGAR_CANE))
                    .tradeValue(0.5)
                    .category(T1_MATERIAL).end();
    public static final ConfigMaterial REFINED_SUGAR =
            material(
                    "mat_refined_sugar",
                    "T1 Refined Sugar",
                    new MaterialData(Material.SUGAR))
                    .category(T1_MATERIAL).end();
    public static final ConfigMaterial REFINED_WART =
            material(
                    "mat_refined_wart",
                    "T1 Refined Wart",
                    new MaterialData(Material.NETHER_STALK))
                    .category(T1_MATERIAL).end();

    public static final ConfigMaterial REFINED_WOOD =
            material(
                    "mat_refined_wood",
                    "T1 Refined Wood",
                    new MaterialData(Material.WOOD_STEP))
                    .category(T1_MATERIAL).end();
    public static final ConfigMaterial REFINED_SLIME =
            material(
                    "mat_refined_slime",
                    "T1 Refined Slime",
                    new MaterialData(Material.SLIME_BALL))
                    .category(T1_MATERIAL).end();
    public static final ConfigMaterial CHROMIUM_ORE =
            material(
                    "mat_chromium_ore",
                    "Chromium Ore",
                    new MaterialData(Material.GHAST_TEAR))
                    .tradeValue(0.1)
                    .category(T1_MATERIAL).end();

    public static final ConfigMaterial FORGED_CHROMIUM =
            material(
                    "mat_forged_chromium",
                    "T1 Forged Chromium",
                    new MaterialData(Material.IRON_INGOT))
                    .category(T1_MATERIAL).end();

    public static final ConfigMaterial TUNGSTEN_ORE =
            material(
                    "mat_tungsten_ore",
                    "Tungsten Ore",
                    new MaterialData(Material.FIREBALL))
                    .tradeValue(0.5)
                    .noRightClick().category(T1_MATERIAL).end();

    public static final ConfigMaterial FORGED_TUNGSTEN =
            material(
                    "mat_forged_tungsten",
                    "T1 Forged Tungsten",
                    new MaterialData(Material.EMERALD_BLOCK))
                    .noRightClick()
                    .category(T1_MATERIAL).end();
    public static final ConfigMaterial POND_SCUM =
            material(
                    "mat_pond_scum",
                    "Pond Scum",
                    new Dye(DyeColor.MAGENTA))
                    .tradeValue(0.1)
                    .noRightClick().category(JUNK_FISH).end();

    public static final ConfigMaterial TANGLED_STRING =
            material(
                    "mat_tangled_string",
                    "Tangled String",
                    new MaterialData(Material.WEB))
                    .tradeValue(0.1)
                    .noRightClick().category(JUNK_FISH).end();

    public static final ConfigMaterial SEAWEED =
            material(
                    "mat_seaweed",
                    "Seaweed",
                    new LongGrass(GrassSpecies.NORMAL))
                    .tradeValue(0.1)
                    .noRightClick().category(JUNK_FISH).end();

    public static final ConfigMaterial MINNOWS =
            material(
                    "mat_minnows",
                    "Minnows",
                    new MaterialData(Material.MELON_SEEDS))
                    .tradeValue(0.5)
                    .noRightClick().category(JUNK_FISH).end();

    public static final ConfigMaterial TADPOLE =
            material(
                    "mat_tadpole",
                    "Tadpole",
                    new MaterialData(Material.SPIDER_EYE))
                    .tradeValue(0.2)
                    .noRightClick().category(JUNK_FISH).end();
    public static final ConfigMaterial MERCURY =
            material(
                    "mat_mercury",
                    "Mercury",
                    new MaterialData(Material.MELON_SEEDS))
                    .noRightClick()
                    .category(T1_MATERIAL).end();

    // T2
    public static final ConfigMaterial MERCURY_BATH =
            material(
                    "mat_mercury_bath",
                    "T2 Mercury Bath",
                    new MaterialData(Material.ENDER_PEARL))
                    .noRightClick()
                    .category(T2_MATERIAL).end();
    public static final ConfigMaterial INDUSTRIAL_DIAMOND =
            material(
                    "mat_industrial_diamond",
                    "T2 Industrial Diamond",
                    new MaterialData(Material.DIAMOND))
                    .category(T2_MATERIAL).end();

    public static final ConfigMaterial CRAFTED_LEATHER =
            material(
                    "mat_crafted_leather",
                    "T2 Industrial Diamond",
                    new MaterialData(Material.LONG_GRASS))
                    .tradeValue(1)
                    .category(T2_MATERIAL).end();
    public static final ConfigMaterial DECORATIVE_JEWELS =
            material(
                    "mat_decorative_jewels",
                    "T2 Decorative Jewels",
                    new MaterialData(Material.NETHER_STALK))
                    .category(T2_MATERIAL).end();

    public static final ConfigMaterial CRUSHED_STONE =
            material(
                    "mat_crushed_stone",
                    "T2 Crushed Stone",
                    new MaterialData(Material.STEP))
                    .category(T2_MATERIAL).end();
    public static final ConfigMaterial JEWELRY_GRADE_GOLD =
            material(
                    "mat_jewelry_grade_gold",
                    "T2 Jewelry Grade Gold",
                    new MaterialData(Material.GOLDEN_APPLE))
                    .tradeValue(1400)
                    .category(T2_MATERIAL).end();
    public static final ConfigMaterial MILLED_LUMBER =
            material(
                    "mat_milled_lumber",
                    "T2 Milled Lumber",
                    new MaterialData(Material.WOOD_STAIRS))
                    .tradeValue(1)
                    .category(T2_MATERIAL).end();

    public static final ConfigMaterial WOVEN_MESH_PATCH =
            material(
                    "mat_woven_mesh_patch",
                    "T2 Woven Mesh Patch",
                    new MaterialData(Material.MAP))
                    .tradeValue(1)
                    .category(T2_MATERIAL).end();

    public static final ConfigMaterial STEEL_INGOT =
            material(
                    "mat_steel_ingot",
                    "T2 Steel Ingot",
                    new MaterialData(Material.IRON_INGOT))
                    .tradeValue(400)
                    .category(T2_MATERIAL).end();

    public static final ConfigMaterial VARNISH =
            material(
                    "mat_varnish",
                    "T2 Varnish",
                    new MaterialData(Material.EXP_BOTTLE))
                    .noRightClick()
                    .tradeValue(1)
                    .category(T2_MATERIAL).end();
    public static final ConfigMaterial STICKY_RESIN =
            material(
                    "mat_sticky_resin",
                    "T2 Sticky Resin",
                    new MaterialData(Material.GLASS_BOTTLE))
                    .tradeValue(1)
                    .category(T2_MATERIAL).end();
    public static final ConfigMaterial CLAY_MOLDING =
            material(
                    "mat_clay_molding",
                    "T2 Clay Molding",
                    new MaterialData(Material.BRICK))
                    .tradeValue(1)
                    .category(T2_MATERIAL).end();
    public static final ConfigMaterial CHROMIUM_INGOT =
            material(
                    "mat_chromium_ingot",
                    "T2 Chromium Ingot",
                    new MaterialData(Material.NETHER_STAR))
                    .tradeValue(2)
                    .category(T2_MATERIAL).end();
    public static final ConfigMaterial AGED_WOOD_STAVE =
            material(
                    "mat_aged_wood_stave",
                    "T2 Aged Wood Stave",
                    new MaterialData(Material.STICK))
                    .tradeValue(1)
                    .category(T2_MATERIAL).end();
    public static final ConfigMaterial WOVEN_THREADING =
            material(
                    "mat_woven_threading",
                    "T2 Woven Threading",
                    new MaterialData(Material.LEASH))
                    .tradeValue(1)
                    .noRightClick()
                    .category(T2_MATERIAL).end();
    public static final ConfigMaterial STEEL_SWORD_BLADE =
            material(
                    "mat_steel_sword_blade",
                    "T2 Steel Sword Blade",
                    new MaterialData(Material.IRON_INGOT))
                    .category(T2_MATERIAL).end();
    public static final ConfigMaterial STEEL_SWORD_HILT =
            material(
                    "mat_steel_sword_hilt",
                    "T2 Steel Sword Hilt",
                    new MaterialData(Material.STICK))
                    .tradeValue(1)
                    .category(T2_MATERIAL).end();
    public static final ConfigMaterial STEEL_PLATE =
            material(
                    "mat_steel_plate",
                    "T2 Steel Plate",
                    new MaterialData(Material.IRON_PLATE))
                    .tradeValue(400)
                    .category(T2_MATERIAL).end();


    // fish
    public static final ConfigMaterial FISH_FISH =
            material(
                    "mat_fish_fish",
                    "Fish",
                    new MaterialData(Material.RAW_FISH))
                    .tradeValue(5.0)
                    .noRightClick().category(T1_FISH).end();

    public static final ConfigMaterial FISH_SALMON =
            material(
                    "mat_fish_salmon",
                    "Salmon",
                    new MaterialData(Material.RAW_FISH, (byte) 1))
                    .tradeValue(5.0)
                    .noRightClick().category(T1_FISH).end();

    public static final ConfigMaterial FISH_CLOWNFISH =
            material(
                    "mat_fish_clownfish",
                    "Clownfish",
                    new MaterialData(Material.RAW_FISH, (byte) 2))
                    .tradeValue(5.0)
                    .noRightClick().category(T1_FISH).end();

    public static final ConfigMaterial FISH_PUFFERFISH =
            material(
                    "mat_fish_pufferfish",
                    "Pufferfish",
                    new MaterialData(Material.RAW_FISH, (byte) 3))
                    .tradeValue(5.0)
                    .noRightClick().category(T1_FISH).end();

    public static final ConfigMaterial FISH_BROWN_TROUT =
            material(
                    "mat_fish_brown_trout",
                    "Brown Trout",
                    new MaterialData(Material.RAW_FISH))
                    .tradeValue(5.0)
                    .shiny()
                    .noRightClick().category(T1_FISH).end();

    public static final ConfigMaterial FISH_BROOK_TROUT =
            material(
                    "mat_fish_brook_trout",
                    "Brook Trout",
                    new MaterialData(Material.RAW_FISH, (byte) 1))
                    .tradeValue(10.0)
                    .shiny()
                    .noRightClick().category(T2_FISH).end();

    public static final ConfigMaterial FISH_CUTTHROAT_TROUT =
            material(
                    "mat_fish_cutthroat_trout",
                    "Cutthroat Trout",
                    new MaterialData(Material.RAW_FISH, (byte) 1))
                    .tradeValue(250.0)
                    .shiny()
                    .noRightClick().category(T3_FISH).end();

    public static final ConfigMaterial FISH_RAINBOW_TROUT =
            material(
                    "mat_fish_rainbow_trout",
                    "Rainbow Trout",
                    new MaterialData(Material.RAW_FISH, (byte) 1))
                    .tradeValue(2500.0)
                    .shiny()
                    .noRightClick().category(T4_FISH).end();

    public static final ConfigMaterial FISH_ATLANTIC_STRIPED_BASS =
            material(
                    "mat_fish_atlantic_striped_bass",
                    "Atlantic Striped Bass",
                    new MaterialData(Material.COOKED_FISH))
                    .tradeValue(5.0)
                    .shiny()
                    .noRightClick().category(T1_FISH).end();

    public static final ConfigMaterial FISH_PACIFIC_OCEAN_PERCH =
            material(
                    "mat_fish_pacific_ocean_perch",
                    "Pacific Ocean Perch",
                    new MaterialData(Material.RAW_FISH, (byte) 1))
                    .tradeValue(10.0)
                    .shiny()
                    .noRightClick().category(T2_FISH).end();

    public static final ConfigMaterial FISH_ACADIAN_REDFISH =
            material(
                    "mat_fish_acadian_redfish",
                    "Acadian Redfish",
                    new MaterialData(Material.RAW_FISH, (byte) 2))
                    .tradeValue(250.0)
                    .shiny()
                    .noRightClick().category(T3_FISH).end();

    public static final ConfigMaterial FISH_WIDOW_ROCKFISH =
            material(
                    "mat_fish_widow_rockfish",
                    "Widow Rockfish",
                    new MaterialData(Material.RAW_FISH))
                    .tradeValue(2500.0)
                    .shiny()
                    .noRightClick().category(T4_FISH).end();

    public static final ConfigMaterial FISH_ATLANTIC_SURFCLAM =
            material(
                    "mat_fish_atlantic_surfclam",
                    "Atlantic Surf Clam",
                    new MaterialData(Material.SUGAR))
                    .tradeValue(5.0)
                    .shiny()
                    .noRightClick().category(T1_FISH).end();

    public static final ConfigMaterial FISH_OCEAN_QUAHOG =
            material(
                    "mat_fish_ocean_quahog",
                    "Ocean Quahog",
                    new MaterialData(Material.SULPHUR))
                    .tradeValue(10.0)
                    .shiny()
                    .noRightClick().category(T2_FISH).end();
    public static final ConfigMaterial FISH_NORTHERN_QUAHOG =
            material(
                    "mat_fish_northern_quahog",
                    "Northern Quahog",
                    new MaterialData(Material.CLAY_BALL))
                    .tradeValue(250.0)
                    .shiny()
                    .noRightClick().category(T3_FISH).end();
    public static final ConfigMaterial FISH_GEODUCK =
            material(
                    "mat_fish_geoduck",
                    "Geoduck",
                    new MaterialData(Material.RABBIT_FOOT))
                    .tradeValue(2500.0)
                    .shiny()
                    .noRightClick().category(T4_FISH).end();
    public static final ConfigMaterial FISH_ATLANTIC_COD =
            material(
                    "mat_fish_atlantic_cod",
                    "Atlantic Cod",
                    new MaterialData(Material.COOKED_FISH))
                    .tradeValue(5.0)
                    .shiny()
                    .noRightClick().category(T1_FISH).end();
    public static final ConfigMaterial FISH_PACIFIC_COD =
            material(
                    "mat_fish_pacific_cod",
                    "Pacific Cod",
                    new MaterialData(Material.RAW_FISH, (byte) 1))
                    .tradeValue(10.0)
                    .shiny()
                    .noRightClick().category(T2_FISH).end();
    public static final ConfigMaterial FISH_LINGCOD =
            material(
                    "mat_fish_lingcod",
                    "Lingcod",
                    new MaterialData(Material.COOKED_FISH, (byte) 1))
                    .tradeValue(250.0)
                    .shiny()
                    .noRightClick().category(T3_FISH).end();
    public static final ConfigMaterial FISH_SABLEFISH =
            material(
                    "mat_fish_sablefish",
                    "Sablefish",
                    new MaterialData(Material.RAW_FISH))
                    .tradeValue(2500.0)
                    .shiny()
                    .noRightClick().category(T4_FISH).end();
    public static final ConfigMaterial FISH_ARROWTOOTH_FLOUNDER =
            material(
                    "mat_fish_arrowtooth_flounder",
                    "Arrowtooth Flounder",
                    new MaterialData(Material.RAW_FISH, (byte) 3))
                    .tradeValue(5.0)
                    .shiny()
                    .noRightClick().category(T1_FISH).end();
    public static final ConfigMaterial FISH_SUMMER_FLOUNDER =
            material(
                    "mat_fish_summer_flounder",
                    "Summer Flounder",
                    new MaterialData(Material.RAW_FISH, (byte) 3))
                    .tradeValue(10.0)
                    .shiny()
                    .noRightClick().category(T2_FISH).end();
    public static final ConfigMaterial FISH_WINTER_FLOUNDER =
            material(
                    "mat_fish_winter_flounder",
                    "Winter Flounder",
                    new MaterialData(Material.RAW_FISH, (byte) 3))
                    .tradeValue(250.0)
                    .shiny()
                    .noRightClick().category(T3_FISH).end();
    public static final ConfigMaterial FISH_YELLOWTAIL_FLOUNDER =
            material(
                    "mat_fish_yellowtail_flounder",
                    "Yellowtail Flounder",
                    new MaterialData(Material.RAW_FISH, (byte) 3))
                    .tradeValue(2500.0)
                    .shiny()
                    .noRightClick().category(T4_FISH).end();
    public static final ConfigMaterial FISH_GAG_GROUPER =
            material(
                    "mat_fish_gag_grouper",
                    "Gag Grouper",
                    new MaterialData(Material.RAW_FISH, (byte) 1))
                    .tradeValue(5.0)
                    .shiny()
                    .noRightClick().category(T1_FISH).end();
    public static final ConfigMaterial FISH_RED_GROUPER =
            material(
                    "mat_fish_red_grouper",
                    "Red Grouper",
                    new MaterialData(Material.RAW_FISH, (byte) 2))
                    .tradeValue(10.0)
                    .shiny()
                    .noRightClick().category(T2_FISH).end();
    public static final ConfigMaterial FISH_BLACK_SEA_BASS =
            material(
                    "mat_fish_black_sea_bass",
                    "Black Sea Bass",
                    new MaterialData(Material.RAW_FISH))
                    .tradeValue(250.0)
                    .shiny()
                    .noRightClick().category(T3_FISH).end();
    public static final ConfigMaterial FISH_WRECKFISH =
            material(
                    "mat_fish_wreckfish",
                    "Wreckfish",
                    new MaterialData(Material.COOKED_FISH))
                    .tradeValue(2500.0)
                    .shiny()
                    .noRightClick().category(T4_FISH).end();
    public static final ConfigMaterial CREEPER_EGG = eggHostile1("mat_creeper_egg", "Creeper Egg", EntityType.CREEPER).end();
    public static final ConfigMaterial CREEPER_EGG_2 = eggHostile2("mat_creeper_egg_2", "Creeper Egg 2", EntityType.CREEPER).end();
    public static final ConfigMaterial CREEPER_EGG_3 = eggHostile3("mat_creeper_egg_3", "Creeper Egg 3", EntityType.CREEPER).end();
    public static final ConfigMaterial CREEPER_EGG_4 = eggHostile4("mat_creeper_egg_4", "Creeper Egg 4", EntityType.CREEPER).end();


    public static final ConfigMaterial SKELETON_EGG = eggHostile1("mat_skeleton_egg", "Skeleton Egg", EntityType.SKELETON).end();
    public static final ConfigMaterial SKELETON_EGG_2 = eggHostile2("mat_skeleton_egg_2", "Skeleton Egg 2", EntityType.SKELETON).end();
    public static final ConfigMaterial SKELETON_EGG_3 = eggHostile3("mat_skeleton_egg_3", "Skeleton Egg 3", EntityType.SKELETON).end();
    public static final ConfigMaterial SKELETON_EGG_4 = eggHostile4("mat_skeleton_egg_4", "Skeleton Egg 4", EntityType.SKELETON).end();


    public static final ConfigMaterial SPIDER_EGG = eggHostile1("mat_spider_egg", "Spider Egg", EntityType.SPIDER).end();
    public static final ConfigMaterial SPIDER_EGG_2 = eggHostile2("mat_spider_egg_2", "Spider Egg 2", EntityType.SPIDER).end();
    public static final ConfigMaterial SPIDER_EGG_3 = eggHostile3("mat_spider_egg_3", "Spider Egg 3", EntityType.SPIDER).end();
    public static final ConfigMaterial SPIDER_EGG_4 = eggHostile4("mat_spider_egg_4", "Spider Egg 4", EntityType.SPIDER).end();


    public static final ConfigMaterial ZOMBIE_EGG = eggHostile1("mat_zombie_egg", "Zombie Egg", EntityType.ZOMBIE).end();
    public static final ConfigMaterial ZOMBIE_EGG_2 = eggHostile2("mat_zombie_egg_2", "Zombie Egg 2", EntityType.ZOMBIE).end();
    public static final ConfigMaterial ZOMBIE_EGG_3 = eggHostile3("mat_zombie_egg_3", "Zombie Egg 3", EntityType.ZOMBIE).end();
    public static final ConfigMaterial ZOMBIE_EGG_4 = eggHostile4("mat_zombie_egg_4", "Zombie Egg 4", EntityType.ZOMBIE).end();


    public static final ConfigMaterial SLIME_EGG = eggHostile1("mat_slime_egg", "Slime Egg", EntityType.SLIME).end();
    public static final ConfigMaterial SLIME_EGG_2 = eggHostile2("mat_slime_egg_2", "Slime Egg 2", EntityType.SLIME).end();
    public static final ConfigMaterial SLIME_EGG_3 = eggHostile3("mat_slime_egg_3", "Slime Egg 3", EntityType.SLIME).end();
    public static final ConfigMaterial SLIME_EGG_4 = eggHostile4("mat_slime_egg_4", "Slime Egg 4", EntityType.SLIME).end();


    public static final ConfigMaterial ENDERMAN_EGG = eggHostile1("mat_enderman_egg", "Enderman Egg", EntityType.ENDERMAN).end();
    public static final ConfigMaterial ENDERMAN_EGG_2 = eggHostile2("mat_enderman_egg_2", "Enderman Egg 2", EntityType.ENDERMAN).end();
    public static final ConfigMaterial ENDERMAN_EGG_3 = eggHostile3("mat_enderman_egg_3", "Enderman Egg 3", EntityType.ENDERMAN).end();
    public static final ConfigMaterial ENDERMAN_EGG_4 = eggHostile4("mat_enderman_egg_4", "Enderman Egg 4", EntityType.ENDERMAN).end();


    public static final ConfigMaterial PIG_EGG = eggPassive1("mat_pig_egg", "Pig Egg", EntityType.PIG).end();
    public static final ConfigMaterial PIG_EGG_2 = eggPassive2("mat_pig_egg_2", "Pig Egg 2", EntityType.PIG).end();
    public static final ConfigMaterial PIG_EGG_3 = eggPassive3("mat_pig_egg_3", "Pig Egg 3", EntityType.PIG).end();
    public static final ConfigMaterial PIG_EGG_4 = eggPassive4("mat_pig_egg_4", "Pig Egg 4", EntityType.PIG).end();


    public static final ConfigMaterial COW_EGG = eggPassive1("mat_cow_egg", "Cow Egg", EntityType.COW).end();
    public static final ConfigMaterial COW_EGG_2 = eggPassive2("mat_cow_egg_2", "Cow Egg 2", EntityType.COW).end();
    public static final ConfigMaterial COW_EGG_3 = eggPassive3("mat_cow_egg_3", "Cow Egg 3", EntityType.COW).end();
    public static final ConfigMaterial COW_EGG_4 = eggPassive4("mat_cow_egg_4", "Cow Egg 4", EntityType.COW).end();


    public static final ConfigMaterial SHEEP_EGG = eggPassive1("mat_sheep_egg", "Sheep Egg", EntityType.SHEEP).end();
    public static final ConfigMaterial SHEEP_EGG_2 = eggPassive2("mat_sheep_egg_2", "Sheep Egg 2", EntityType.SHEEP).end();
    public static final ConfigMaterial SHEEP_EGG_3 = eggPassive3("mat_sheep_egg_3", "Sheep Egg 3", EntityType.SHEEP).end();
    public static final ConfigMaterial SHEEP_EGG_4 = eggPassive4("mat_sheep_egg_4", "Sheep Egg 4", EntityType.SHEEP).end();


    public static final ConfigMaterial CHICKEN_EGG = eggPassive1("mat_chicken_egg", "Chicken Egg", EntityType.CHICKEN).end();
    public static final ConfigMaterial CHICKEN_EGG_2 = eggPassive2("mat_chicken_egg_2", "Chicken Egg 2", EntityType.CHICKEN).end();
    public static final ConfigMaterial CHICKEN_EGG_3 = eggPassive3("mat_chicken_egg_3", "Chicken Egg 3", EntityType.CHICKEN).end();
    public static final ConfigMaterial CHICKEN_EGG_4 = eggPassive4("mat_chicken_egg_4", "Chicken Egg 4", EntityType.CHICKEN).end();


    public static final ConfigMaterial RABBIT_EGG = eggPassive1("mat_rabbit_egg", "Rabbit Egg", EntityType.RABBIT).end();
    public static final ConfigMaterial RABBIT_EGG_2 = eggPassive2("mat_rabbit_egg_2", "Rabbit Egg 2", EntityType.RABBIT).end();
    public static final ConfigMaterial RABBIT_EGG_3 = eggPassive3("mat_rabbit_egg_3", "Rabbit Egg 3", EntityType.RABBIT).end();
    public static final ConfigMaterial RABBIT_EGG_4 = eggPassive4("mat_rabbit_egg_4", "Rabbit Egg 4", EntityType.RABBIT).end();

    public static final ConfigMaterial METALLIC_CRYSTAL_FRAGMENT_1 = material(
            "mat_metallic_crystal_fragment_1",
            "Common Metallic Crystal Fragment",
            new MaterialData(Material.GHAST_TEAR))
            .category(T1_MATERIAL).end();
    public static final ConfigMaterial METALLIC_CRYSTAL_1 = material(
            "mat_metallic_crystal_1",
            "T1 Common Metallic Crystal",
            new MaterialData(Material.NETHER_STAR))
            .category(T1_MATERIAL).end();
    public static final ConfigMaterial METALLIC_CRYSTAL_FRAGMENT_2 = material(
            "mat_metallic_crystal_fragment_2",
            "Uncommon Metallic Crystal Fragment",
            new MaterialData(Material.GHAST_TEAR))
            .category(T2_MATERIAL).end();
    public static final ConfigMaterial METALLIC_CRYSTAL_2 = material(
            "mat_metallic_crystal_2",
            "T2 Uncommon Metallic Crystal",
            new MaterialData(Material.NETHER_STAR))
            .category(T2_MATERIAL).end();
    public static final ConfigMaterial METALLIC_CRYSTAL_FRAGMENT_3 = material(
            "mat_metallic_crystal_fragment_3",
            "Rare Metallic Crystal Fragment",
            new MaterialData(Material.GHAST_TEAR))
            .category(T3_MATERIAL).end();
    public static final ConfigMaterial METALLIC_CRYSTAL_3 = material(
            "mat_metallic_crystal_3",
            "T3 Rare Metallic Crystal",
            new MaterialData(Material.NETHER_STAR))
            .category(T3_MATERIAL).end();
    public static final ConfigMaterial METALLIC_CRYSTAL_FRAGMENT_4 = material(
            "mat_metallic_crystal_fragment_4",
            "Legendary Metallic Crystal Fragment",
            new MaterialData(Material.GHAST_TEAR))
            .category(T4_MATERIAL).end();
    public static final ConfigMaterial METALLIC_CRYSTAL_4 = material(
            "mat_metallic_crystal_4",
            "T4 Legendary Metallic Crystal",
            new MaterialData(Material.NETHER_STAR))
            .category(T4_MATERIAL).end();

    public static final ConfigMaterial IONIC_CRYSTAL_FRAGMENT_1 = material(
            "mat_ionic_crystal_fragment_1",
            "Common Ionic Crystal Fragment",
            new MaterialData(Material.BLAZE_POWDER))
            .category(T1_MATERIAL).end();
    public static final ConfigMaterial IONIC_CRYSTAL_1 = material(
            "mat_ionic_crystal_1",
            "T1 Common Ionic Crystal",
            new MaterialData(Material.MAGMA_CREAM))
            .category(T1_MATERIAL).end();

    public static final ConfigMaterial IONIC_CRYSTAL_FRAGMENT_2 = material(
            "mat_ionic_crystal_fragment_2",
            "Uncommon Ionic Crystal Fragment",
            new MaterialData(Material.BLAZE_POWDER))
            .category(T2_MATERIAL).end();
    public static final ConfigMaterial IONIC_CRYSTAL_2 = material(
            "mat_ionic_crystal_2",
            "T1 Uncommon Ionic Crystal",
            new MaterialData(Material.MAGMA_CREAM))
            .category(T2_MATERIAL).end();
    public static final ConfigMaterial IONIC_CRYSTAL_FRAGMENT_3 = material(
            "mat_ionic_crystal_fragment_3",
            "Rare Ionic Crystal Fragment",
            new MaterialData(Material.BLAZE_POWDER))
            .category(T3_MATERIAL).end();
    public static final ConfigMaterial IONIC_CRYSTAL_3 = material(
            "mat_ionic_crystal_3",
            "T3 Rare Ionic Crystal",
            new MaterialData(Material.MAGMA_CREAM))
            .category(T3_MATERIAL).end();
    public static final ConfigMaterial IONIC_CRYSTAL_FRAGMENT_4 = material(
            "mat_ionic_crystal_fragment_4",
            "Legendary Ionic Crystal Fragment",
            new MaterialData(Material.BLAZE_POWDER))
            .category(T1_MATERIAL).end();
    public static final ConfigMaterial IONIC_CRYSTAL_4 = material(
            "mat_ionic_crystal_4",
            "T4 Legendary Ionic Crystal",
            new MaterialData(Material.MAGMA_CREAM))
            .category(T4_MATERIAL).end();



    private static Builder material(String id, String name, MaterialData data) {
        return new Builder(id, name, data);
    }

    private static Builder eggHostile1(String id, String name, EntityType type) {
        return material(
                id,
                name,
                new SpawnEgg(type))
                .tradeValue(10)
                .category("<lightgreen>Eggs Tier 1");
    }

    private static Builder eggPassive1(String id, String name, EntityType type) {
        return eggHostile1(id, name, type).tradeValue(10);
    }

    private static Builder eggHostile2(String id, String name, EntityType type) {
        return material(
                id,
                name,
                new SpawnEgg(type))
                .tradeValue(500)
                .category("<lightgreen>Eggs Tier 2");
    }

    private static Builder eggPassive2(String id, String name, EntityType type) {
        return eggHostile2(id, name, type).tradeValue(50);
    }

    private static Builder eggHostile3(String id, String name, EntityType type) {
        return material(
                id,
                name,
                new SpawnEgg(type))
                .tradeValue(2500)
                .category("<lightgreen>Eggs Tier 3");
    }

    private static Builder eggPassive3(String id, String name, EntityType type) {
        return eggHostile3(id, name, type).tradeValue(500);
    }

    private static Builder eggHostile4(String id, String name, EntityType type) {
        return material(
                id,
                name,
                new SpawnEgg(type))
                .tradeValue(5000)
                .category("<lightgreen>Eggs Tier 4");
    }

    private static Builder eggPassive4(String id, String name, EntityType type) {
        return eggHostile4(id, name, type).tradeValue(1000);
    }

    private static class Builder {
        private final String id;
        private final String name;
        private final MaterialData data;
        private Double tradeValue = null;
        private String category = CivSettings.localize.localizedString("config_material_misc");


        public Builder(String id, String name, MaterialData data) {
            this.id = id;
            this.name = name;
            this.data = data;
        }

        public Builder noRightClick() {
            return this;
        }

        public Builder shiny() {
            return this;
        }
        public Builder tradeValue(double value) {
            this.tradeValue = value;
            return this;
        }
        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public ConfigMaterial end() {
            var conf = new ConfigMaterial();
            conf.name = this.name;
            conf.id = this.id;
            conf.item_id = data.getItemType();
            conf.item_data = data.getData();
            conf.category = this.category;
            if (tradeValue != null) {
                conf.tradeable = true;
                conf.tradeValue = this.tradeValue;
            }
            entries.add(conf);
            return conf;
        }
    }
}
