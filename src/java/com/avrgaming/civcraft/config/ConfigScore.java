package com.avrgaming.civcraft.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.annotation.Nonnull;
import java.io.File;

public final class ConfigScore {
    public final ConfigTownScores town_scores;
    public final double coins_per_point;

    private ConfigScore(
            ConfigTownScores town_scores,
            double coins_per_point
    ) {
        this.town_scores = town_scores;
        this.coins_per_point = coins_per_point;
    }

    @Nonnull
    public static ConfigScore fromFileConfig(FileConfiguration config) {
        return new ConfigScore(
                new ConfigTownScores(
                        config.getInt("town_scores.town_chunk", 200),
                        config.getInt("town_scores.culture_chunk", 100),
                        config.getInt("town_scores.resident", 5000),
                        config.getInt("town_scores.coins", 25)
                ),
                config.getDouble("coins_per_point", 5.0)
        );
    }

    @Nonnull
    public static ConfigScore fromFile(File file) {
        return fromFileConfig(YamlConfiguration.loadConfiguration(file));
    }


    public static final class ConfigTownScores {
        public final double town_chunk;
        public final double culture_chunk;
        public final int resident;
        public final double coins;

        private ConfigTownScores(int town_chunk, int culture_chunk, int resident, int coins) {
            this.town_chunk = town_chunk;
            this.culture_chunk = culture_chunk;
            this.resident = resident;
            this.coins = coins;
        }
    }
}
