package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.object.Town;

public class BuildTaskInstance {
    public final ConfigBuildableInfo info;
    public final Town town;

    public BuildTaskInstance(ConfigBuildableInfo info, Town town) {
        this.info = info;
        this.town = town;
    }
}
