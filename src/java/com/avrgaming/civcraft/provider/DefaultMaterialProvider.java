package com.avrgaming.civcraft.provider;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMaterial;
import org.bukkit.Material;
import org.bukkit.material.MaterialData;

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

    private static Builder material(String id, String name, MaterialData data) {
        return new Builder(id, name, data);
    }

    private static class Builder {
        private final String id;
        private final String name;
        private final MaterialData data;
        private String category = CivSettings.localize.localizedString("config_material_misc");


        public Builder(String id, String name, MaterialData data) {
            this.id = id;
            this.name = name;
            this.data = data;
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
            entries.add(conf);
            return conf;
        }
    }
}
