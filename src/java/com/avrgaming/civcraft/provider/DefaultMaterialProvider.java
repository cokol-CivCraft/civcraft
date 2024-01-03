package com.avrgaming.civcraft.provider;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMaterial;
import org.bukkit.Material;
import org.bukkit.SandstoneType;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Sandstone;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

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
            if (!tradeValue.equals(null)) {
                conf.tradeable = true;
                conf.tradeValue = this.tradeValue;
            }
            entries.add(conf);
            return conf;
        }
    }
}
