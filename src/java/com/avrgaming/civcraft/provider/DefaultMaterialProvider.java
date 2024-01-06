package com.avrgaming.civcraft.provider;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMaterial;
import org.bukkit.DyeColor;
import org.bukkit.GrassSpecies;
import org.bukkit.Material;
import org.bukkit.SandstoneType;
import org.bukkit.material.Dye;
import org.bukkit.material.LongGrass;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Sandstone;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@ParametersAreNonnullByDefault
public class DefaultMaterialProvider {
    public static final List<ConfigMaterial> entries = new ArrayList<>();
    public static final ConfigMaterial CARVED_LEATHER =
            material(
                    "mat_carved_leather",
                    "T1 Carved Leather",
                    new MaterialData(Material.LEATHER)
            ).category("<lightgreen>Tier 1 Materials").end();
    public static final ConfigMaterial REFINED_STONE =
            material(
                    "mat_refined_stone",
                    "T1 Refined Stone",
                    new MaterialData(Material.STONE)
            ).category("<lightgreen>Tier 1 Materials").end();

    public static final ConfigMaterial REFINED_FEATHERS =
            material(
                    "mat_refined_feathers",
                    "T1 Refined Stone",
                    new MaterialData(Material.QUARTZ)
            ).category("<lightgreen>Tier 1 Materials").end();

    public static final ConfigMaterial CRAFTED_STRING =
            material(
                    "mat_crafted_string",
                    "T1 Crafted String",
                    new MaterialData(Material.STRING)
            ).category("<lightgreen>Tier 1 Materials").end();

    public static final ConfigMaterial CRAFTED_STICK =
            material(
                    "mat_crafted_sticks",
                    "T1 Crafted Sticks",
                    new MaterialData(Material.STICK)
            ).category("<lightgreen>Tier 1 Materials").end();
    public static final ConfigMaterial REFINED_SULPHUR =
            material(
                    "mat_refined_sulphur",
                    "T1 Refined Sulphur",
                    new MaterialData(Material.SULPHUR)
            ).category("<lightgreen>Tier 1 Materials").end();
    public static final ConfigMaterial COMPACTED_SAND =
            material(
                    "mat_compacted_sand",
                    "T1 Compacted Sand",
                    new Sandstone(SandstoneType.GLYPHED)
            ).category("<lightgreen>Tier 1 Materials").end();
    public static final ConfigMaterial FORGED_CLAY =
            material(
                    "mat_forged_clay",
                    "T1 Forged Clay",
                    new MaterialData(Material.CLAY_BRICK)
            ).category("<lightgreen>Tier 1 Materials").end();
    public static final ConfigMaterial CRAFTED_REEDS =
            material(
                    "mat_crafted_reeds",
                    "T1 Crafted Reeds",
                    new MaterialData(Material.SUGAR_CANE)
            )
                    .tradeValue(0.5)
                    .category("<lightgreen>Tier 1 Materials").end();
    public static final ConfigMaterial CHROMIUM_ORE =
            material(
                    "mat_chromium_ore",
                    "Chromium Ore",
                    new MaterialData(Material.GHAST_TEAR)
            )
                    .tradeValue(0.1)
                    .category("<lightgreen>Tier 1 Materials").end();

    public static final ConfigMaterial TUNGSTEN_ORE =
            material(
                    "mat_tungsten_ore",
                    "Tungsten Ore",
                    new MaterialData(Material.FIREBALL))
                    .tradeValue(0.5)
                    .noRightClick().category("<lightgreen>Tier 1 Materials").end();

    public static final ConfigMaterial POND_SCUM =
            material(
                    "mat_pond_scum",
                    "Pond Scum",
                    new Dye(DyeColor.MAGENTA))
                    .tradeValue(0.1)
                    .noRightClick().category("<lightpurple>Fishing Junk").end();

    public static final ConfigMaterial TANGLED_STRING =
            material(
                    "mat_tangled_string",
                    "Tangled String",
                    new MaterialData(Material.WEB))
                    .tradeValue(0.1)
                    .noRightClick().category("<lightpurple>Fishing Junk").end();

    public static final ConfigMaterial SEAWEED =
            material(
                    "mat_seaweed",
                    "Seaweed",
                    new LongGrass(GrassSpecies.NORMAL))
                    .tradeValue(0.1)
                    .noRightClick().category("<lightpurple>Fishing Junk").end();

    public static final ConfigMaterial MINNOWS =
            material(
                    "mat_minnows",
                    "Minnows",
                    new MaterialData(Material.MELON_SEEDS))
                    .tradeValue(0.5)
                    .noRightClick().category("<lightpurple>Fishing Junk").end();

    public static final ConfigMaterial TADPOLE =
            material(
                    "mat_tadpole",
                    "Tadpole",
                    new MaterialData(Material.SPIDER_EYE))
                    .tradeValue(0.2)
                    .noRightClick().category("<lightpurple>Fishing Junk").end();

    public static final ConfigMaterial FISH_FISH =
            material(
                    "mat_fish_fish",
                    "Fish",
                    new MaterialData(Material.RAW_FISH))
                    .tradeValue(5.0)
                    .noRightClick().category("<lightgreen>Fish Tier 1").end();

    public static final ConfigMaterial FISH_SALMON =
            material(
                    "mat_fish_salmon",
                    "Salmon",
                    new MaterialData(Material.RAW_FISH, (byte) 1))
                    .tradeValue(5.0)
                    .noRightClick().category("<lightgreen>Fish Tier 1").end();

    public static final ConfigMaterial FISH_CLOWNFISH =
            material(
                    "mat_fish_clownfish",
                    "Clownfish",
                    new MaterialData(Material.RAW_FISH, (byte) 2))
                    .tradeValue(5.0)
                    .noRightClick().category("<lightgreen>Fish Tier 1").end();

    public static final ConfigMaterial FISH_PUFFERFISH =
            material(
                    "mat_fish_pufferfish",
                    "Pufferfish",
                    new MaterialData(Material.RAW_FISH, (byte) 3))
                    .tradeValue(5.0)
                    .noRightClick().category("<lightgreen>Fish Tier 1").end();

    public static final ConfigMaterial FISH_BROWN_TROUT =
            material(
                    "mat_fish_brown_trout",
                    "Brown Trout",
                    new MaterialData(Material.RAW_FISH))
                    .tradeValue(5.0)
                    .shiny()
                    .noRightClick().category("<lightgreen>Fish Tier 1").end();

    public static final ConfigMaterial FISH_BROOK_TROUT =
            material(
                    "mat_fish_brook_trout",
                    "Brook Trout",
                    new MaterialData(Material.RAW_FISH, (byte) 1))
                    .tradeValue(10.0)
                    .shiny()
                    .noRightClick().category("<lightblue>Fish Tier 2").end();

    public static final ConfigMaterial FISH_CUTTHROAT_TROUT =
            material(
                    "mat_fish_cutthroat_trout",
                    "Cutthroat Trout",
                    new MaterialData(Material.RAW_FISH, (byte) 1))
                    .tradeValue(250.0)
                    .shiny()
                    .noRightClick().category("<lightpurple>Fish Tier 3").end();

    public static final ConfigMaterial FISH_RAINBOW_TROUT =
            material(
                    "mat_fish_rainbow_trout",
                    "Rainbow Trout",
                    new MaterialData(Material.RAW_FISH, (byte) 1))
                    .tradeValue(2500.0)
                    .shiny()
                    .noRightClick().category("<gold>Fish Tier 4").end();

    public static final ConfigMaterial FISH_ATLANTIC_STRIPED_BASS =
            material(
                    "mat_fish_atlantic_striped_bass",
                    "Atlantic Striped Bass",
                    new MaterialData(Material.COOKED_FISH))
                    .tradeValue(5.0)
                    .shiny()
                    .noRightClick().category("<lightgreen>Fish Tier 1").end();

    public static final ConfigMaterial FISH_PACIFIC_OCEAN_PERCH =
            material(
                    "mat_fish_pacific_ocean_perch",
                    "Pacific Ocean Perch",
                    new MaterialData(Material.RAW_FISH, (byte) 1))
                    .tradeValue(10.0)
                    .shiny()
                    .noRightClick().category("<lightblue>Fish Tier 2").end();

    public static final ConfigMaterial FISH_ACADIAN_REDFISH =
            material(
                    "mat_fish_acadian_redfish",
                    "Acadian Redfish",
                    new MaterialData(Material.RAW_FISH, (byte) 2))
                    .tradeValue(250.0)
                    .shiny()
                    .noRightClick().category("<lightpurple>Fish Tier 3").end();

    public static final ConfigMaterial FISH_WIDOW_ROCKFISH =
            material(
                    "mat_fish_widow_rockfish",
                    "Widow Rockfish",
                    new MaterialData(Material.RAW_FISH))
                    .tradeValue(2500.0)
                    .shiny()
                    .noRightClick().category("<gold>Fish Tier 4").end();

    public static final ConfigMaterial FISH_ATLANTIC_SURFCLAM =
            material(
                    "mat_fish_atlantic_surfclam",
                    "Atlantic Surf Clam",
                    new MaterialData(Material.SUGAR))
                    .tradeValue(5.0)
                    .shiny()
                    .noRightClick().category("<lightgreen>Fish Tier 1").end();

    public static final ConfigMaterial FISH_OCEAN_QUAHOG =
            material(
                    "mat_fish_ocean_quahog",
                    "Ocean Quahog",
                    new MaterialData(Material.SULPHUR))
                    .tradeValue(10.0)
                    .shiny()
                    .noRightClick().category("<lightblue>Fish Tier 2").end();
    public static final ConfigMaterial FISH_NORTHERN_QUAHOG =
            material(
                    "mat_fish_northern_quahog",
                    "Northern Quahog",
                    new MaterialData(Material.CLAY_BALL))
                    .tradeValue(250.0)
                    .shiny()
                    .noRightClick().category("<lightpurple>Fish Tier 3").end();
    public static final ConfigMaterial FISH_GEODUCK =
            material(
                    "mat_fish_geoduck",
                    "Geoduck",
                    new MaterialData(Material.RABBIT_FOOT))
                    .tradeValue(2500.0)
                    .shiny()
                    .noRightClick().category("<gold>Fish Tier 4").end();
    public static final ConfigMaterial FISH_ATLANTIC_COD =
            material(
                    "mat_fish_atlantic_cod",
                    "Atlantic Cod",
                    new MaterialData(Material.COOKED_FISH))
                    .tradeValue(5.0)
                    .shiny()
                    .noRightClick().category("<lightgreen>Fish Tier 1").end();
    public static final ConfigMaterial FISH_PACIFIC_COD =
            material(
                    "mat_fish_pacific_cod",
                    "Pacific Cod",
                    new MaterialData(Material.RAW_FISH, (byte) 1))
                    .tradeValue(10.0)
                    .shiny()
                    .noRightClick().category("<lightblue>Fish Tier 2").end();
    public static final ConfigMaterial FISH_LINGCOD =
            material(
                    "mat_fish_lingcod",
                    "Lingcod",
                    new MaterialData(Material.COOKED_FISH, (byte) 1))
                    .tradeValue(250.0)
                    .shiny()
                    .noRightClick().category("<lightpurple>Fish Tier 3").end();
    public static final ConfigMaterial FISH_SABLEFISH =
            material(
                    "mat_fish_sablefish",
                    "Sablefish",
                    new MaterialData(Material.RAW_FISH))
                    .tradeValue(2500.0)
                    .shiny()
                    .noRightClick().category("<gold>Fish Tier 4").end();
    public static final ConfigMaterial FISH_ARROWTOOTH_FLOUNDER =
            material(
                    "mat_fish_arrowtooth_flounder",
                    "Arrowtooth Flounder",
                    new MaterialData(Material.RAW_FISH, (byte) 3))
                    .tradeValue(5.0)
                    .shiny()
                    .noRightClick().category("<lightgreen>Fish Tier 1").end();
    public static final ConfigMaterial FISH_SUMMER_FLOUNDER =
            material(
                    "mat_fish_summer_flounder",
                    "Summer Flounder",
                    new MaterialData(Material.RAW_FISH, (byte) 3))
                    .tradeValue(10.0)
                    .shiny()
                    .noRightClick().category("<lightblue>Fish Tier 2").end();
    public static final ConfigMaterial FISH_WINTER_FLOUNDER =
            material(
                    "mat_fish_winter_flounder",
                    "Winter Flounder",
                    new MaterialData(Material.RAW_FISH, (byte) 3))
                    .tradeValue(250.0)
                    .shiny()
                    .noRightClick().category("<lightpurple>Fish Tier 3").end();
    public static final ConfigMaterial FISH_YELLOWTAIL_FLOUNDER =
            material(
                    "mat_fish_yellowtail_flounder",
                    "Yellowtail Flounder",
                    new MaterialData(Material.RAW_FISH, (byte) 3))
                    .tradeValue(2500.0)
                    .shiny()
                    .noRightClick().category("<gold>Fish Tier 4").end();
    public static final ConfigMaterial FISH_GAG_GROUPER =
            material(
                    "mat_fish_gag_grouper",
                    "Gag Grouper",
                    new MaterialData(Material.RAW_FISH, (byte) 1))
                    .tradeValue(5.0)
                    .shiny()
                    .noRightClick().category("<lightgreen>Fish Tier 1").end();
    public static final ConfigMaterial FISH_RED_GROUPER =
            material(
                    "mat_fish_red_grouper",
                    "Red Grouper",
                    new MaterialData(Material.RAW_FISH, (byte) 2))
                    .tradeValue(10.0)
                    .shiny()
                    .noRightClick().category("<lightblue>Fish Tier 2").end();
    public static final ConfigMaterial FISH_BLACK_SEA_BASS =
            material(
                    "mat_fish_black_sea_bass",
                    "Black Sea Bass",
                    new MaterialData(Material.RAW_FISH))
                    .tradeValue(250.0)
                    .shiny()
                    .noRightClick().category("<lightpurple>Fish Tier 3").end();
    public static final ConfigMaterial FISH_WRECKFISH =
            material(
                    "mat_fish_wreckfish",
                    "Wreckfish",
                    new MaterialData(Material.COOKED_FISH))
                    .tradeValue(2500.0)
                    .shiny()
                    .noRightClick().category("<gold>Fish Tier 4").end();
    private static Builder material(String id, String name, MaterialData data) {
        return new Builder(id, name, data);
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
