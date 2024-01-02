/*
 * AVRGAMING LLC
 * __________________
 *
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,TICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.config;

import com.avrgaming.civcraft.camp.Camp;
import com.avrgaming.civcraft.endgame.ConfigEndCondition;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.units.Unit;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancement;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.provider.DefaultRecipeProvider;
import com.avrgaming.civcraft.randomevents.ConfigRandomEvent;
import com.avrgaming.civcraft.template.Template;
import localize.Localize;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class CivSettings {

    public static CivCraft plugin;
    /* Number of days that you can remain in debt before an action occurs. */

    //TODO make this configurable.
    public static final int GRACE_DAYS = 3;

    public static final int CIV_DEBT_GRACE_DAYS = 7;
    public static final int CIV_DEBT_SELL_DAYS = 14;
    public static final int CIV_DEBT_TOWN_SELL_DAYS = 21;
    public static final int TOWN_DEBT_GRACE_DAYS = 7;
    public static final int TOWN_DEBT_SELL_DAYS = 14;


    /* cached for faster access. */
    //public static float leather_speed;
    //public static float metal_speed;
    public static float T1_leather_speed;
    public static float T2_leather_speed;
    public static float T3_leather_speed;
    public static float T4_leather_speed;
    public static float T1_metal_speed;
    public static float T2_metal_speed;
    public static float T3_metal_speed;
    public static float T4_metal_speed;
    public static float normal_speed;

    public static FileConfiguration townConfig; /* town.yml */
    public static Map<Integer, ConfigTownLevel> townLevels = new HashMap<>();
    public static Map<String, ConfigTownUpgrade> townUpgrades = new TreeMap<>();

    public static FileConfiguration civConfig; /* civ.yml */
    public static Map<String, ConfigEndCondition> endConditions = new HashMap<>();

    public static FileConfiguration cultureConfig; /* culture.yml */
    public static Map<Integer, ConfigCultureLevel> cultureLevels = new HashMap<>();
    private static final Map<String, ConfigCultureBiomeInfo> cultureBiomes = new HashMap<>();

    public static FileConfiguration structureConfig; /* structures.yml */
    public static Map<String, ConfigBuildableInfo> structures = new HashMap<>();
    public static Map<Integer, ConfigGrocerLevel> grocerLevels = new HashMap<>();
    public static Map<Integer, ConfigCottageLevel> cottageLevels = new HashMap<>();
    public static Map<Integer, ConfigTempleLevel> templeLevels = new HashMap<>();
    public static Map<Integer, ConfigTradeShipLevel> tradeShipLevels = new HashMap<>();

    public static FileConfiguration wonderConfig; /* wonders.yml */
    public static Map<String, ConfigWonderBuff> wonderBuffs = new HashMap<>();

    public static FileConfiguration religionConfig; /*religion.yml */
    public static Map<String, ConfigReligion> religions = new HashMap<>();
    public static FileConfiguration techsConfig; /* techs.yml */
    public static Map<String, ConfigTech> techs = new HashMap<>();
    public static Map<Material, ConfigTechItem> techItems = new HashMap<>();
    public static Map<String, ConfigTechPotion> techPotions = new HashMap<>();
    public static FileConfiguration goodsConfig; /* goods.yml */
    public static Map<String, ConfigTradeGood> goods = new HashMap<>();
    public static Map<String, ConfigTradeGood> landGoods = new HashMap<>();
    public static Map<String, ConfigTradeGood> waterGoods = new HashMap<>();
    public static Map<String, ConfigHemisphere> hemispheres = new HashMap<>();

    public static FileConfiguration buffConfig;
    public static Map<String, ConfigBuff> buffs = new HashMap<>();

    public static FileConfiguration unitConfig;
    public static Map<String, ConfigUnit> units = new HashMap<>();

    public static FileConfiguration espionageConfig;
    public static Map<String, ConfigMission> missions = new HashMap<>();

    public static FileConfiguration governmentConfig; /* governments.yml */
    public static Map<String, ConfigGovernment> governments = new HashMap<>();

    public static HashSet<Material> switchItems = new HashSet<>();
    public static Map<Material, Integer> restrictedItems = new HashMap<>();
    public static Map<Material, Integer> blockPlaceExceptions = new HashMap<>();
    public static Map<EntityType, Integer> restrictedSpawns = new HashMap<>();
    public static HashSet<EntityType> playerEntityWeapons = new HashSet<>();
    public static HashSet<Material> alwaysCrumble = new HashSet<>();

    public static FileConfiguration warConfig; /* war.yml */

    public static ConfigScore scoreConfig; /* score.yml */

    public static FileConfiguration perkConfig; /* perks.yml */
    public static HashMap<String, ArrayList<ConfigTemplate>> templates = new HashMap<>();

    public static FileConfiguration enchantConfig; /* enchantments.yml */
    public static Map<String, ConfigEnchant> enchants = new HashMap<>();

    public static FileConfiguration campConfig; /* camp.yml */
    public static Map<Integer, ConfigCampLonghouseLevel> longhouseLevels = new HashMap<>();
    public static Map<String, ConfigCampUpgrade> campUpgrades = new HashMap<>();

    public static FileConfiguration marketConfig; /* market.yml */
    public static Map<Integer, ConfigMarketItem> marketItems = new HashMap<>();

    public static Set<ConfigStableItem> stableItems = new HashSet<>();
    public static HashMap<Integer, ConfigStableHorse> horses = new HashMap<>();

    public static FileConfiguration happinessConfig; /* happiness.yml */
    public static HashMap<Integer, ConfigTownHappinessLevel> townHappinessLevels = new HashMap<>();
    public static HashMap<Integer, ConfigHappinessState> happinessStates = new HashMap<>();

    public static FileConfiguration materialsConfig; /* materials.yml */
    public static HashMap<String, ConfigMaterial> materials = new HashMap<>();

    public static FileConfiguration randomEventsConfig; /* randomevents.yml */
    public static HashMap<String, ConfigRandomEvent> randomEvents = new HashMap<>();
    public static ArrayList<String> randomEventIDs = new ArrayList<>();

    public static FileConfiguration arenaConfig; /* arenas.yml */
    public static HashMap<String, ConfigArena> arenas = new HashMap<>();

    public static FileConfiguration fishingConfig; /* fishing.yml */
    public static ArrayList<ConfigFishing> fishingDrops = new ArrayList<>();

    public static double iron_rate;
    public static double gold_rate;
    public static double diamond_rate;
    public static double emerald_rate;
    public static double startingCoins;

    public static ArrayList<String> kitItems = new ArrayList<>();
    public static HashMap<Material, ConfigRemovedRecipes> removedRecipies = new HashMap<>();
    public static HashSet<Material> restrictedUndoBlocks = new HashSet<>();
    public static String CURRENCY_NAME;

    public static Localize localize;

    public static boolean hasTitleAPI = false;

    public static Material previewMaterial = Material.GLASS;
    public static Boolean showPreview = true;

    public static void init(JavaPlugin plugin) throws IOException, InvalidConfigurationException {
        CivSettings.plugin = (CivCraft) plugin;

        String languageFile = plugin.getConfig().getString("localization_file", "default_lang.yml");
        localize = new Localize(plugin, languageFile);

        CivLog.debug(localize.localizedString("welcome_string", "test", 1337, 100.50));
        CURRENCY_NAME = localize.localizedString("civ_currencyName");

        // Check for required data folder, if it's not there export it.
        CivSettings.validateFiles();

        initRestrictedItems();
        initRestrictedUndoBlocks();
        initSwitchItems();
        initRestrictedSpawns();
        initBlockPlaceExceptions();
        initPlayerEntityWeapons();

        loadConfigFiles();
        loadConfigObjects();

        Unit.init();

        CivSettings.T1_leather_speed = (float) CivSettings.unitConfig.getDouble("base.T1_leather_speed", 1.05);
        CivSettings.T2_leather_speed = (float) CivSettings.unitConfig.getDouble("base.T2_leather_speed", 1.1);
        CivSettings.T3_leather_speed = (float) CivSettings.unitConfig.getDouble("base.T3_leather_speed", 1.15);
        CivSettings.T4_leather_speed = (float) CivSettings.unitConfig.getDouble("base.T4_leather_speed", 1.2);
        CivSettings.T1_metal_speed = (float) CivSettings.unitConfig.getDouble("base.T1_metal_speed", 0.95);
        CivSettings.T2_metal_speed = (float) CivSettings.unitConfig.getDouble("base.T2_metal_speed", 0.9);
        CivSettings.T3_metal_speed = (float) CivSettings.unitConfig.getDouble("base.T3_metal_speed", 0.85);
        CivSettings.T4_metal_speed = (float) CivSettings.unitConfig.getDouble("base.T4_metal_speed", 0.8);
        CivSettings.normal_speed = 0.2f;

        for (Object obj : civConfig.getList("global.start_kit")) {
            if (obj instanceof String) {
                kitItems.add((String) obj);
            }
        }


        CivGlobal.banWords.add("fuck");
        CivGlobal.banWords.add("shit");
        CivGlobal.banWords.add("nigger");
        CivGlobal.banWords.add("faggot");
        CivGlobal.banWords.add("gay");
        CivGlobal.banWords.add("rape");
        CivGlobal.banWords.add("http");
        CivGlobal.banWords.add("cunt");

        iron_rate = civConfig.getDouble("ore_rates.iron", 20.0);
        gold_rate = civConfig.getDouble("ore_rates.gold", 200.0);
        diamond_rate = civConfig.getDouble("ore_rates.diamond", 400.0);
        emerald_rate = civConfig.getDouble("ore_rates.emerald", 800.0);
        startingCoins = civConfig.getDouble("global.starting_coins", 250.0);

        alwaysCrumble.add(Material.BEDROCK);
        alwaysCrumble.add(Material.COAL_BLOCK);
        alwaysCrumble.add(Material.EMERALD_BLOCK);
        alwaysCrumble.add(Material.LAPIS_BLOCK);
        alwaysCrumble.add(Material.SPONGE);
        alwaysCrumble.add(Material.HAY_BLOCK);
        alwaysCrumble.add(Material.GOLD_BLOCK);
        alwaysCrumble.add(Material.DIAMOND_BLOCK);
        alwaysCrumble.add(Material.IRON_BLOCK);
        alwaysCrumble.add(Material.REDSTONE_BLOCK);
        alwaysCrumble.add(Material.ENDER_CHEST);
        alwaysCrumble.add(Material.BEACON);

        LoreEnhancement.init();
        LoreCraftableMaterial.buildStaticMaterials();
        LoreCraftableMaterial.buildRecipes();
        loadRecipes();
        Template.initAttachableTypes();

        hasTitleAPI = CivSettings.plugin.hasPlugin("TitleAPI");
        if (hasTitleAPI) {
            CivLog.info("TitleAPI hooks enabled");
        } else {
            CivLog.warning("TitleAPI not found, not registering TitleAPI hooks. This is fine if you're not using TitleAPI.");
        }

        previewMaterial = Material.getMaterial(structureConfig.getString("global.previewBlock", Material.GLASS.name()));

        showPreview = structureConfig.getBoolean("shouldShowPreview", true);

    }

    private static void loadRecipes() {
        for (Map.Entry<NamespacedKey, ConfigRecipe> recipe : DefaultRecipeProvider.provide().entrySet()) {
            if (recipe.getValue() instanceof ConfigRecipeShapless config) {
                ShapelessRecipe shapeless = new ShapelessRecipe(recipe.getKey(), LoreMaterial.spawn(config.material));
                for (ItemStack itemStack : config.ingridients) {
                    shapeless.addIngredient(itemStack.getAmount(), itemStack.getData());
                }
                Bukkit.getServer().addRecipe(shapeless);
            }
        }
    }

    private static void initRestrictedUndoBlocks() {
        restrictedUndoBlocks.add(Material.CROPS);
        restrictedUndoBlocks.add(Material.CARROT);
        restrictedUndoBlocks.add(Material.POTATO);
        restrictedUndoBlocks.add(Material.REDSTONE);
        restrictedUndoBlocks.add(Material.REDSTONE_WIRE);
        restrictedUndoBlocks.add(Material.REDSTONE_TORCH_OFF);
        restrictedUndoBlocks.add(Material.REDSTONE_TORCH_ON);
        restrictedUndoBlocks.add(Material.DIODE_BLOCK_OFF);
        restrictedUndoBlocks.add(Material.DIODE_BLOCK_ON);
        restrictedUndoBlocks.add(Material.REDSTONE_COMPARATOR_OFF);
        restrictedUndoBlocks.add(Material.REDSTONE_COMPARATOR_ON);
        restrictedUndoBlocks.add(Material.REDSTONE_COMPARATOR);
        restrictedUndoBlocks.add(Material.STRING);
        restrictedUndoBlocks.add(Material.TRIPWIRE);
        restrictedUndoBlocks.add(Material.SUGAR_CANE_BLOCK);
        restrictedUndoBlocks.add(Material.BEETROOT_SEEDS);
        restrictedUndoBlocks.add(Material.LONG_GRASS);
        restrictedUndoBlocks.add(Material.RED_ROSE);
        restrictedUndoBlocks.add(Material.RED_MUSHROOM);
        restrictedUndoBlocks.add(Material.DOUBLE_PLANT);
        restrictedUndoBlocks.add(Material.CAKE_BLOCK);
        restrictedUndoBlocks.add(Material.CACTUS);
        restrictedUndoBlocks.add(Material.PISTON_BASE);
        restrictedUndoBlocks.add(Material.PISTON_EXTENSION);
        restrictedUndoBlocks.add(Material.PISTON_MOVING_PIECE);
        restrictedUndoBlocks.add(Material.PISTON_STICKY_BASE);
        restrictedUndoBlocks.add(Material.TRIPWIRE_HOOK);
        restrictedUndoBlocks.add(Material.SAPLING);
        restrictedUndoBlocks.add(Material.PUMPKIN_STEM);
        restrictedUndoBlocks.add(Material.MELON_STEM);

    }

    private static void initPlayerEntityWeapons() {
        playerEntityWeapons.add(EntityType.PLAYER);
        playerEntityWeapons.add(EntityType.ARROW);
        playerEntityWeapons.add(EntityType.SPECTRAL_ARROW);
        playerEntityWeapons.add(EntityType.TIPPED_ARROW);
        playerEntityWeapons.add(EntityType.EGG);
        playerEntityWeapons.add(EntityType.SNOWBALL);
        playerEntityWeapons.add(EntityType.SPLASH_POTION);
        playerEntityWeapons.add(EntityType.LINGERING_POTION);
        playerEntityWeapons.add(EntityType.FISHING_HOOK);
    }

    public static void validateFiles() {
//		if (plugin == null) {
//			CivLog.debug("null plugin");
//		}
//		
//		if (plugin.getDataFolder() == null) {
//			CivLog.debug("null data folder");
//		}
//		
//		if (plugin.getDataFolder().getPath() == null) {
//			CivLog.debug("path null");
//		}
        File data = new File(plugin.getDataFolder().getPath() + "/data");
        if (!data.exists()) {
            data.mkdirs();
        }
//		
    }

    public static void streamResourceToDisk(String filepath) throws IOException {
        URL inputUrl = plugin.getClass().getResource(filepath);
        File dest = new File(plugin.getDataFolder().getPath() + filepath);
        if (inputUrl == null) {
            CivLog.error("Destination is null: " + filepath);
            return;
        }
        FileUtils.copyURLToFile(inputUrl, dest);
    }

    public static FileConfiguration loadCivConfig(String filepath) throws IOException, InvalidConfigurationException {
        File file = getConfigFileFromName(filepath);
        CivLog.warning(file.getPath());
        if (!file.exists()) {
            CivLog.warning("Configuration file:" + filepath + " was missing. Streaming to disk from Jar.");
            streamResourceToDisk("/data/" + filepath);
        }

        CivLog.info("Loading Configuration file:" + filepath);
        // read the config.yml into memory
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.load(file);
        return cfg;
    }

    public static File getConfigFileFromName(String name) {
        return new File(plugin.getDataFolder().getPath() + "/data/" + name);
    }

    public static void reloadGovConfigFiles() throws IOException, InvalidConfigurationException {
        CivSettings.governments.clear();
        governmentConfig = loadCivConfig("governments.yml");
        ConfigGovernment.loadConfig(governmentConfig, governments);
    }

    private static void loadConfigFiles() throws IOException, InvalidConfigurationException {
        townConfig = loadCivConfig("town.yml");
        civConfig = loadCivConfig("civ.yml");
        cultureConfig = loadCivConfig("culture.yml");
        structureConfig = loadCivConfig("structures.yml");
        techsConfig = loadCivConfig("techs.yml");
        religionConfig = loadCivConfig("religion.yml");
        goodsConfig = loadCivConfig("goods.yml");
        buffConfig = loadCivConfig("buffs.yml");
        governmentConfig = loadCivConfig("governments.yml");
        warConfig = loadCivConfig("war.yml");
        wonderConfig = loadCivConfig("wonders.yml");
        unitConfig = loadCivConfig("units.yml");
        espionageConfig = loadCivConfig("espionage.yml");
        scoreConfig = ConfigScore.fromFile(getConfigFileFromName("score.yml"));
        perkConfig = loadCivConfig("perks.yml");
        enchantConfig = loadCivConfig("enchantments.yml");
        campConfig = loadCivConfig("camp.yml");
        marketConfig = loadCivConfig("market.yml");
        happinessConfig = loadCivConfig("happiness.yml");
        materialsConfig = loadCivConfig("materials.yml");
        randomEventsConfig = loadCivConfig("randomevents.yml");
        arenaConfig = loadCivConfig("arena.yml");
        fishingConfig = loadCivConfig("fishing.yml");
    }

    private static void loadConfigObjects() {
        ConfigTownLevel.loadConfig(townConfig, townLevels);
        ConfigTownUpgrade.loadConfig(townConfig, townUpgrades);
        ConfigCultureLevel.loadConfig(cultureConfig, cultureLevels);
        ConfigBuildableInfo.loadConfig(structureConfig, wonderConfig, structures);
        ConfigTech.loadConfig(techsConfig, techs);
        ConfigReligion.loadConfig(religionConfig, religions);
        ConfigTechItem.loadConfig(techsConfig, techItems);
        ConfigTechPotion.loadConfig(techsConfig, techPotions);
        ConfigHemisphere.loadConfig(goodsConfig, hemispheres);
        ConfigBuff.loadConfig(buffConfig, buffs);
        ConfigWonderBuff.loadConfig(wonderConfig, wonderBuffs);
        ConfigTradeGood.loadConfig(goodsConfig, goods, landGoods, waterGoods);
        ConfigGrocerLevel.loadConfig(structureConfig, grocerLevels);
        ConfigCottageLevel.loadConfig(structureConfig, cottageLevels);
        ConfigTempleLevel.loadConfig(structureConfig, templeLevels);
        ConfigGovernment.loadConfig(governmentConfig, governments);
        ConfigEnchant.loadConfig(enchantConfig, enchants);
        ConfigUnit.loadConfig(unitConfig, units);
        ConfigMission.loadConfig(espionageConfig, missions);
        ConfigTemplate.loadConfig(perkConfig, templates);
        ConfigCampLonghouseLevel.loadConfig(campConfig, longhouseLevels);
        ConfigCampUpgrade.loadConfig(campConfig, campUpgrades);
        ConfigMarketItem.loadConfig(marketConfig, marketItems);
        ConfigStableItem.loadConfig(structureConfig, stableItems);
        ConfigStableHorse.loadConfig(structureConfig, horses);
        ConfigTownHappinessLevel.loadConfig(happinessConfig, townHappinessLevels);
        ConfigHappinessState.loadConfig(happinessConfig, happinessStates);
        ConfigCultureBiomeInfo.loadConfig(cultureConfig, cultureBiomes);
        ConfigMaterial.loadConfig(materialsConfig, materials);
        ConfigRandomEvent.loadConfig(randomEventsConfig, randomEvents, randomEventIDs);
        ConfigEndCondition.loadConfig(civConfig, endConditions);
        ConfigArena.loadConfig(arenaConfig, arenas);
        ConfigFishing.loadConfig(fishingConfig, fishingDrops);
        ConfigTradeShipLevel.loadConfig(structureConfig, tradeShipLevels);

        ConfigRemovedRecipes.removeRecipes(materialsConfig, removedRecipies);
        CivGlobal.tradeGoodPreGenerator.preGenerate();
//        Wall.init_settings();
//        FortifiedWall.init_settings();
    }

    private static void initRestrictedSpawns() {
        restrictedSpawns.put(EntityType.BLAZE, 0);
        restrictedSpawns.put(EntityType.CAVE_SPIDER, 0);
        restrictedSpawns.put(EntityType.CREEPER, 0);
        restrictedSpawns.put(EntityType.ENDER_DRAGON, 0);
        restrictedSpawns.put(EntityType.ENDERMAN, 0);
        restrictedSpawns.put(EntityType.GHAST, 0);
        restrictedSpawns.put(EntityType.GIANT, 0);
        restrictedSpawns.put(EntityType.PIG_ZOMBIE, 0);
        restrictedSpawns.put(EntityType.SILVERFISH, 0);
        restrictedSpawns.put(EntityType.SKELETON, 0);
        restrictedSpawns.put(EntityType.SLIME, 0);
        restrictedSpawns.put(EntityType.SPIDER, 0);
        restrictedSpawns.put(EntityType.WITCH, 0);
        restrictedSpawns.put(EntityType.WITHER, 0);
        restrictedSpawns.put(EntityType.ZOMBIE, 0);
        restrictedSpawns.put(EntityType.BAT, 0);
        restrictedSpawns.put(EntityType.ENDERMITE, 0);
        restrictedSpawns.put(EntityType.GUARDIAN, 0);
        restrictedSpawns.put(EntityType.HUSK, 0);
        restrictedSpawns.put(EntityType.STRAY, 0);
        restrictedSpawns.put(EntityType.ZOMBIE_VILLAGER, 0);
    }

    private static void initRestrictedItems() {
        // TODO make this configurable?
        restrictedItems.put(Material.FLINT_AND_STEEL, 0);
        restrictedItems.put(Material.BUCKET, 0);
        restrictedItems.put(Material.WATER_BUCKET, 0);
        restrictedItems.put(Material.LAVA_BUCKET, 0);
        restrictedItems.put(Material.CAKE_BLOCK, 0);
        restrictedItems.put(Material.CAULDRON, 0);
        restrictedItems.put(Material.DIODE, 0);
        restrictedItems.put(Material.INK_SACK, 0);
        restrictedItems.put(Material.ITEM_FRAME, 0);
        restrictedItems.put(Material.PAINTING, 0);
        restrictedItems.put(Material.SHEARS, 0);
        restrictedItems.put(Material.STATIONARY_LAVA, 0);
        restrictedItems.put(Material.STATIONARY_WATER, 0);
        restrictedItems.put(Material.TNT, 0);
    }

    private static void initSwitchItems() {
        //TODO make this configurable?
        switchItems.add(Material.ANVIL);
        switchItems.add(Material.BEACON);
        switchItems.add(Material.BREWING_STAND);
        switchItems.add(Material.BURNING_FURNACE);
        switchItems.add(Material.CAKE_BLOCK);
        switchItems.add(Material.CAULDRON);
        switchItems.add(Material.CHEST);
        switchItems.add(Material.TRAPPED_CHEST);
        switchItems.add(Material.COMMAND);
        switchItems.add(Material.DIODE);
        switchItems.add(Material.DIODE_BLOCK_OFF);
        switchItems.add(Material.DIODE_BLOCK_ON);
        switchItems.add(Material.DISPENSER);
        switchItems.add(Material.FENCE_GATE);
        switchItems.add(Material.FURNACE);
        switchItems.add(Material.JUKEBOX);
        switchItems.add(Material.LEVER);
        //	switchItems.add(Material.LOCKED_CHEST);
        switchItems.add(Material.STONE_BUTTON);
        switchItems.add(Material.STONE_PLATE);
        switchItems.add(Material.IRON_DOOR);
        switchItems.add(Material.TNT);
        switchItems.add(Material.TRAP_DOOR);
        switchItems.add(Material.WOOD_DOOR);
        switchItems.add(Material.WOODEN_DOOR);
        switchItems.add(Material.WOOD_PLATE);
        //switchItems.put(Material.WOOD_BUTTON, 0); //intentionally left out

        // 1.5 additions.
        switchItems.add(Material.HOPPER);
        switchItems.add(Material.HOPPER_MINECART);
        switchItems.add(Material.DROPPER);
        switchItems.add(Material.REDSTONE_COMPARATOR);
        switchItems.add(Material.REDSTONE_COMPARATOR_ON);
        switchItems.add(Material.REDSTONE_COMPARATOR_OFF);
        switchItems.add(Material.GOLD_PLATE);
        switchItems.add(Material.IRON_PLATE);
        switchItems.add(Material.IRON_TRAPDOOR);

        // 1.6 additions.
        switchItems.add(Material.SPRUCE_DOOR);
        switchItems.add(Material.BIRCH_DOOR);
        switchItems.add(Material.JUNGLE_DOOR);
        switchItems.add(Material.ACACIA_DOOR);
        switchItems.add(Material.DARK_OAK_DOOR);

        // 1.7 additions
        switchItems.add(Material.ACACIA_FENCE_GATE);
        switchItems.add(Material.BIRCH_FENCE_GATE);
        switchItems.add(Material.DARK_OAK_FENCE_GATE);
        switchItems.add(Material.SPRUCE_FENCE_GATE);
        switchItems.add(Material.JUNGLE_FENCE_GATE);
    }

    private static void initBlockPlaceExceptions() {
        /* These blocks can be placed regardless of permissions.
         * this is currently used only for blocks that are generated
         * by specific events such as portal or fire creation.
         */
        blockPlaceExceptions.put(Material.FIRE, 0);
        blockPlaceExceptions.put(Material.PORTAL, 0);
    }

    public static ConfigTownUpgrade getUpgradeByName(String name) {
        for (ConfigTownUpgrade upgrade : townUpgrades.values()) {
            if (upgrade.name.equalsIgnoreCase(name)) {
                return upgrade;
            }
        }
        return null;
    }

    public static ConfigHappinessState getHappinessState(double amount) {
        ConfigHappinessState closestState = happinessStates.get(0);

        for (int i = 0; i < happinessStates.size(); i++) {
            ConfigHappinessState state = happinessStates.get(i);
            if ((double) Math.round(amount * 100) / 100 >= state.amount()) {
                closestState = state;
            }
        }

        return closestState;
    }

    public static ConfigTownUpgrade getUpgradeByNameRegex(Town town, String name) throws CivException {
        ConfigTownUpgrade returnUpgrade = null;
        for (ConfigTownUpgrade upgrade : townUpgrades.values()) {
            if (!upgrade.isAvailable(town)) {
                continue;
            }

            if (name.equalsIgnoreCase(upgrade.name)) {
                return upgrade;
            }

            String loweredUpgradeName = upgrade.name.toLowerCase();
            String loweredName = name.toLowerCase();

            if (loweredUpgradeName.contains(loweredName)) {
                if (returnUpgrade == null) {
                    returnUpgrade = upgrade;
                } else {
                    throw new CivException(CivSettings.localize.localizedString("var_cmd_notSpecificUpgrade", name));
                }
            }
        }
        return returnUpgrade;
    }

    public static ConfigCampUpgrade getCampUpgradeByNameRegex(Camp camp, String name) throws CivException {
        ConfigCampUpgrade returnUpgrade = null;
        for (ConfigCampUpgrade upgrade : campUpgrades.values()) {
            if (!upgrade.isAvailable(camp)) {
                continue;
            }

            if (name.equalsIgnoreCase(upgrade.name())) {
                return upgrade;
            }

            String loweredUpgradeName = upgrade.name().toLowerCase();
            String loweredName = name.toLowerCase();

            if (loweredUpgradeName.contains(loweredName)) {
                if (returnUpgrade == null) {
                    returnUpgrade = upgrade;
                } else {
                    throw new CivException(CivSettings.localize.localizedString("var_cmd_notSpecificUpgrade", name));
                }
            }
        }
        return returnUpgrade;
    }

    public static ConfigBuildableInfo getBuildableInfoByName(String fullArgs) {
        for (ConfigBuildableInfo sinfo : structures.values()) {
            if (sinfo.displayName.equalsIgnoreCase(fullArgs)) {
                return sinfo;
            }
        }
        return null;
    }

    public static ConfigTech getTechByName(String techname) {
        for (ConfigTech tech : techs.values()) {
            if (tech.name().equalsIgnoreCase(techname)) {
                return tech;
            }
        }
        return null;
    }

    public static int getCottageMaxLevel() {
        return cottageLevels.keySet().stream().mapToInt(level -> level).filter(level -> level >= 0).max().orElse(0);
        /*
        3:18:56 Логарифм по основанию три скольки? То есть, логарифм по основанию три трёх это один, а логарифм по основанию три нискольки( не ноль ибо ноль тоже нужно уточнять) это абсолютная ошибка, но я буду учитывать это как логарифм по основанию три трёх.  Десятичная однёрка? Реально, так никто не говорит, ты наверное хотел сказать одна десятая, хотя в твою голову мне не залезть, так что буду думать что ты имелл ввиду 0,(9) потому что именно так выглядит "десятичная однёрка в периоде", но только тогда это не одна десятая, а одна целая.
И так закончили с перидсловием и давайте разбирёмся сколько это будет и того log3(3)*cos2π*300/10+1.(9). log3(3)= 1;cos2π=1; 1.(9)=2; 10+2=12; 300/12=25; 1*1*25=25. Но ты утверждал, что должно было выйти 3. И того 25≠3. На этом у меня всё.  Во всём остально класный видос.
         */
    }

    public static int getTempleMaxLevel() {
        return templeLevels.keySet().stream().mapToInt(level -> level).filter(level -> level >= 0).max().orElse(0);
        //3 дні тому
        //аы
    }

    public static int getMaxCultureLevel() {
        return cultureLevels.keySet().stream().mapToInt(level -> level).filter(level -> level >= 0).max().orElse(0);
        // "Охуеть спасибо дедушка" - вежливо отвечает бобр
    }

    public static ConfigCultureBiomeInfo getCultureBiome(String name) {
        return Optional.ofNullable(cultureBiomes.get(name)).orElseGet(() -> cultureBiomes.get("UNKNOWN"));
    }
}
