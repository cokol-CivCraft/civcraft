package localize;

/*
 * This plugin needs a default_lang.yml file in the jar file. This file includes the default strings.
 */

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivLog;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.IllegalFormatException;
import java.util.Optional;

public class Localize {
    private final JavaPlugin plugin;
    public String languageFile;

    public Localize(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public Localize(JavaPlugin plugin, String langFile) {
        this.plugin = plugin;
        this.setLanguageFile(langFile);
    }

    public void setLanguageFile(String langFile) {
        this.languageFile = langFile.equals("") ? "default_lang.yml" : langFile;
        this.reloadDefaultLocalizedStrings();
        this.reloadLocalizedStrings();
    }

    public String getLanguageFile() {
        return this.languageFile;
    }

    public Boolean isDefault() {
        return languageFile.equalsIgnoreCase("default_lang.yml");
    }

    public String localizedString(String pathToString) {
        if (this.isDefault()) {
            return (String) Optional.ofNullable(this.getDefaultLocalizedStrings().get(pathToString)).orElse(pathToString);
        }
        Object value = this.getLocalizedStrings().get(pathToString);
        if (value == null) {
            return (String) Optional.ofNullable(this.getDefaultLocalizedStrings().get(pathToString)).orElse(pathToString);
        }
        return (String) value;
    }

    public String localizedString(String pathToString, Object... args) {
        String localString = localizedString(pathToString);
        if (args.length < 1) {
            return localString;
        }
        if (localString.equalsIgnoreCase(pathToString)) {
            return localString;
        }
        return compounded(localString, args);
    }

    private String compounded(String string, Object... args) {
        try {
            for (int arg = 0; arg < args.length; ++arg) {
                Object replacementString = args[arg];
                string = string.replace("[%" + arg + "]", String.valueOf(replacementString));
            }
            return string;
        } catch (IllegalFormatException e1) {
            return string + " - [" + localizedString("stringFormattingError") + "]";
        }
    }

    private FileConfiguration localizedStrings = null;
    private FileConfiguration defaultLocalizedStrings = null;

    public void reloadDefaultLocalizedStrings() {
        String defaultLanguageFile = "default_lang.yml";
        File defaultLocalizedStringsFile = new File(plugin.getDataFolder().getPath() + "/localization/" + defaultLanguageFile);
        CivLog.warning("Configuration file:" + defaultLanguageFile + " in use. Updating to disk from Jar.");
        try {
            CivSettings.streamResourceToDisk("/localization/" + defaultLanguageFile);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        defaultLocalizedStrings = YamlConfiguration.loadConfiguration(defaultLocalizedStringsFile);

        CivLog.info("Loading Configuration file:" + defaultLanguageFile);
        // read the config.yml into memory
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.load(defaultLocalizedStringsFile);
        } catch (InvalidConfigurationException | IOException e1) {
            e1.printStackTrace();
        }
        defaultLocalizedStrings.setDefaults(cfg);

    }

    public void reloadLocalizedStrings() {
        File localizedStringsFile = new File(plugin.getDataFolder().getPath() + "/localization/" + languageFile);
        if (this.isDefault()) {
            if (defaultLocalizedStrings == null) {
                localizedStrings = null;
            }
            return;
        } else if (!localizedStringsFile.exists()) {

            CivLog.warning("Configuration file:" + languageFile + " was missing. You must create this file in plugins/Civcraft/localization/");
            CivLog.warning("Using default_lang.yml");
            this.setLanguageFile("");
            return;
        }
        localizedStrings = YamlConfiguration.loadConfiguration(localizedStringsFile);

        CivLog.info("Loading Configuration file:" + languageFile);
        // read the config.yml into memory
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.load(localizedStringsFile);
        } catch (InvalidConfigurationException | IOException e1) {
            e1.printStackTrace();
        }
        localizedStrings.setDefaults(cfg);

    }

    private FileConfiguration getLocalizedStrings() {
        if (localizedStrings == null) {
            reloadLocalizedStrings();
        }
        return localizedStrings;
    }

    private FileConfiguration getDefaultLocalizedStrings() {
        if (defaultLocalizedStrings == null) {
            reloadDefaultLocalizedStrings();
        }
        return defaultLocalizedStrings;
    }

}