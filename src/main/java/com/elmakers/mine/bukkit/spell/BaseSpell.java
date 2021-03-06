package com.elmakers.mine.bukkit.spell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

import com.elmakers.mine.bukkit.action.CastContext;
import com.elmakers.mine.bukkit.api.data.SpellData;
import com.elmakers.mine.bukkit.api.event.CastEvent;
import com.elmakers.mine.bukkit.api.event.PreCastEvent;
import com.elmakers.mine.bukkit.api.event.SpellUpgradeEvent;
import com.elmakers.mine.bukkit.api.magic.Messages;
import com.elmakers.mine.bukkit.api.spell.CostReducer;
import com.elmakers.mine.bukkit.api.spell.MageSpell;
import com.elmakers.mine.bukkit.api.spell.Spell;
import com.elmakers.mine.bukkit.api.spell.SpellKey;
import com.elmakers.mine.bukkit.api.spell.SpellResult;
import com.elmakers.mine.bukkit.api.spell.SpellTemplate;
import com.elmakers.mine.bukkit.api.spell.TargetType;
import com.elmakers.mine.bukkit.api.wand.Wand;
import com.elmakers.mine.bukkit.api.wand.WandUpgradePath;
import com.elmakers.mine.bukkit.utility.CompatibilityUtils;
import com.elmakers.mine.bukkit.utility.InventoryUtils;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MageController;
import com.elmakers.mine.bukkit.api.spell.SpellCategory;
import com.elmakers.mine.bukkit.block.MaterialAndData;
import com.elmakers.mine.bukkit.effect.EffectPlayer;
import com.elmakers.mine.bukkit.utility.ConfigurationUtils;

public abstract class BaseSpell implements MageSpell, Cloneable {
    public static int MAX_LORE_LENGTH = 24;

    protected static final double LOOK_THRESHOLD_RADIANS = 0.9;

    // TODO: Config-drive
    protected static final int MIN_Y = 1;

    // TODO: Configurable default? this does look cool, though.
    protected final static Material DEFAULT_EFFECT_MATERIAL = Material.STATIONARY_WATER;

    public final static String[] EXAMPLE_VECTOR_COMPONENTS = {"-1", "-0.5", "0", "0.5", "1", "~-1", "~-0.5", "~0", "~0.5", "*1", "*-1", "*-0.5", "*0.5", "*1"};
    public final static String[] EXAMPLE_SIZES = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "12", "16", "32", "64"};
    public final static String[] EXAMPLE_BOOLEANS = {"true", "false"};
    public final static String[] EXAMPLE_DURATIONS = {"500", "1000", "2000", "5000", "10000", "60000", "120000"};
    public final static String[] EXAMPLE_PERCENTAGES = {"0", "0.1", "0.25", "0.5", "0.75", "1"};

    public final static String[] OTHER_PARAMETERS = {
        "transparent", "target", "target_type", "range", "duration", "player"
    };

    public final static String[] WORLD_PARAMETERS = {
        "pworld", "tworld", "otworld", "t2world"
    };

    protected final static Set<String> worldParameterMap = new HashSet<String>(Arrays.asList(WORLD_PARAMETERS));

    public final static String[] VECTOR_PARAMETERS = {
        "px", "py", "pz", "pdx", "pdy", "pdz", "tx", "ty", "tz", "otx", "oty", "otz", "t2x", "t2y", "t2z",
        "otdx", "otdy", "otdz"
    };

    protected final static Set<String> vectorParameterMap = new HashSet<String>(Arrays.asList(VECTOR_PARAMETERS));

    public final static String[] BOOLEAN_PARAMETERS = {
        "allow_max_range", "prevent_passthrough", "reverse_targeting", "passthrough", "bypass_protection",
        "bypass_build", "bypass_break", "bypass_pvp", "target_npc", "ignore_blocks", "target_self"
    };

    protected final static Set<String> booleanParameterMap = new HashSet<String>(Arrays.asList(BOOLEAN_PARAMETERS));

    public final static String[] PERCENTAGE_PARAMETERS = {
        "fizzle_chance", "backfire_chance", "cooldown_reduction"
    };

    protected final static Set<String> percentageParameterMap = new HashSet<String>(Arrays.asList(PERCENTAGE_PARAMETERS));

    public final static String[] COMMON_PARAMETERS = (String[])
        ArrayUtils.addAll(
            ArrayUtils.addAll(
                    ArrayUtils.addAll(
                            ArrayUtils.addAll(VECTOR_PARAMETERS, BOOLEAN_PARAMETERS),
                            OTHER_PARAMETERS
                    ),
                    WORLD_PARAMETERS
            ),
            PERCENTAGE_PARAMETERS
        );


    /*
     * protected members that are helpful to use
     */
    protected MageController				controller;
    protected Mage 							mage;
    protected Location    					location;
    protected CastContext currentCast;

    /*
     * Variant properties
     */
    private SpellKey spellKey;
    private String inheritKey;
    private String name;
    private String alias;
    private String description;
    private String extendedDescription;
    private String levelDescription;
    private String upgradeDescription;
    private String usage;
    private double worth;
    private int earns;
    private Color color;
    private String particle;
    private SpellCategory category;
    private Set<String> tags;
    private BaseSpell template;
    private long requiredUpgradeCasts;
    private String requiredUpgradePath;
    private MaterialAndData icon = new MaterialAndData(Material.AIR);
    private String iconURL = null;
    private List<CastingCost> costs = null;
    private List<CastingCost> activeCosts = null;

    protected double cancelOnDamage             = 0;
    protected boolean pvpRestricted           	= false;
    protected boolean disguiseRestricted        = true;
    protected boolean worldBorderRestricted     = true;
    protected boolean usesBrushSelection        = false;
    protected boolean bypassFriendlyFire    	= false;
    protected boolean bypassPvpRestriction    	= false;
    protected boolean bypassBuildRestriction    = false;
    protected boolean bypassBreakRestriction    = false;
    protected boolean bypassProtection          = false;
    protected boolean bypassConfusion             = false;
    protected boolean bypassWeakness              = false;
    protected boolean bypassPermissions           = false;
    protected boolean castOnNoTarget              = false;
    protected boolean bypassDeactivate            = false;
    protected boolean quiet                       = false;
    protected boolean loud                        = false;
    protected boolean messageTargets              = true;
    protected boolean targetSelf                  = false;
    protected boolean showUndoable              = true;
    protected boolean cancellable               = true;
    protected boolean quickCast                 = false;
    protected int                               verticalSearchDistance  = 8;

    private boolean backfired                   = false;
    private boolean hidden                      = false;
    private boolean trackCasts                  = true;

    protected ConfigurationSection parameters = null;
    protected ConfigurationSection workingParameters = null;
    protected ConfigurationSection configuration = null;

    protected static Random random            = new Random();

    /*
     * private data
     */

    private float                               cooldownReduction       = 0;
    private float                               costReduction           = 0;
    private float                               consumeReduction        = 0;

    private boolean                             bypassMageCooldown      = false;
    private int                                 mageCooldown            = 0;
    private int                                 cooldown                = 0;
    private long                                cooldownExpiration      = 0;
    private int                                 earnCooldown            = 0;
    private int                                 duration                = 0;
    private int                                 totalDuration           = -1;
    private long                                lastCast                = 0;
    private long                                lastEarn                = 0;
    private long                                lastActiveCost          = 0;
    private float                               activeCostScale         = 1;
    private long								castCount				= 0;

    private boolean								isActive				= false;

    private Map<String, Collection<EffectPlayer>>     effects				= new HashMap<String, Collection<EffectPlayer>>();

    private float								fizzleChance			= 0.0f;
    private float								backfireChance			= 0.0f;

    private long 								lastMessageSent 			= 0;
    private Set<Material>						preventPassThroughMaterials = null;
    private Set<Material>                       passthroughMaterials = null;
    private Set<Material>						unsafeMaterials = null;

    public boolean allowPassThrough(Material mat)
    {
        if (mage != null && mage.isSuperPowered()) {
            return true;
        }
        if (passthroughMaterials != null && passthroughMaterials.contains(mat)) {
            return true;
        }
        return preventPassThroughMaterials == null || !preventPassThroughMaterials.contains(mat);
    }

    public boolean isPassthrough(Material mat)
    {
        return passthroughMaterials != null && passthroughMaterials.contains(mat);
    }

    /*
     * Ground / location search and test functions
     */
    public boolean isOkToStandIn(Material mat)
    {
        if (isHalfBlock(mat)) {
            return false;
        }
        return passthroughMaterials.contains(mat) && !unsafeMaterials.contains(mat);
    }

    public boolean isWater(Material mat)
    {
        return (mat == Material.WATER || mat == Material.STATIONARY_WATER);
    }

    public boolean isOkToStandOn(Block block)
    {
        return isOkToStandOn(block.getType());
    }

    protected boolean isHalfBlock(Material mat) {

        // TODO: Data-driven half-block list
        // Don't put carpet and snow in here, acts weird. Not sure why though.
        return (mat == Material.STEP || mat == Material.WOOD_STEP);
    }

    public boolean isOkToStandOn(Material mat)
    {
        if (isHalfBlock(mat)) {
            return true;
        }
        return (mat != Material.AIR && !unsafeMaterials.contains(mat) && !passthroughMaterials.contains(mat));
    }

    public boolean isSafeLocation(Block block)
    {
        if (!block.getChunk().isLoaded()) {
            block.getChunk().load(true);
            return false;
        }

        if (block.getY() > CompatibilityUtils.getMaxHeight(block.getWorld())) {
            return false;
        }

        Block blockOneUp = block.getRelative(BlockFace.UP);
        Block blockOneDown = block.getRelative(BlockFace.DOWN);

        // Ascend to top of water
        if (isUnderwater() && (blockOneDown.getType() == Material.STATIONARY_WATER || blockOneDown.getType() == Material.WATER)
            && blockOneUp.getType() == Material.AIR && block.getType() == Material.AIR) {
            return true;
        }

        Player player = mage.getPlayer();
        return (
                (isOkToStandOn(blockOneDown) || (player != null && player.isFlying()))
                &&	isOkToStandIn(blockOneUp.getType())
                && 	isOkToStandIn(block.getType())
        );
    }

    public boolean isSafeLocation(Location loc)
    {
        return isSafeLocation(loc.getBlock());
    }

    public Location tryFindPlaceToStand(Location targetLoc)
    {
        int maxHeight = CompatibilityUtils.getMaxHeight(targetLoc.getWorld());
        return tryFindPlaceToStand(targetLoc, maxHeight, maxHeight);
    }

    public Location findPlaceToStand(Location targetLoc)
    {
        return findPlaceToStand(targetLoc, verticalSearchDistance, verticalSearchDistance);
    }

    public Location tryFindPlaceToStand(Location targetLoc, int maxDownDelta, int maxUpDelta)
    {
        Location location = findPlaceToStand(targetLoc, maxDownDelta, maxUpDelta);
        return location == null ? targetLoc : location;
    }

    public Location findPlaceToStand(Location targetLoc, int maxDownDelta, int maxUpDelta)
    {
        if (!targetLoc.getBlock().getChunk().isLoaded()) return null;
        int minY = MIN_Y;
        int maxY = CompatibilityUtils.getMaxHeight(targetLoc.getWorld());
        int targetY = targetLoc.getBlockY();
        if (targetY >= minY && targetY <= maxY && isSafeLocation(targetLoc)) {
            return checkForHalfBlock(targetLoc);
        }

        Location location = null;
        if (targetY < minY) {
            location = targetLoc.clone();
            location.setY(minY);
            location = findPlaceToStand(location, true, maxUpDelta);
        } else if (targetY > maxY) {
            location = targetLoc.clone();
            location.setY(maxY);
            location = findPlaceToStand(location, false, maxDownDelta);
        } else {
            // First look up just a little bit
            int testMaxY = Math.min(maxUpDelta, 3);
            location = findPlaceToStand(targetLoc, true, testMaxY);

            // Then  look down just a little bit
            if (location == null) {
                int testMinY = Math.min(maxDownDelta, 4);
                location = findPlaceToStand(targetLoc, false, testMinY);
            }

            // Then look all the way up
            if (location == null) {
                location = findPlaceToStand(targetLoc, true, maxUpDelta);
            }

            // Then look allll the way down.
            if (location == null) {
                location = findPlaceToStand(targetLoc, false, maxDownDelta);
            }
        }
        return location;
    }

    public Location findPlaceToStand(Location target, boolean goUp)
    {
        return findPlaceToStand(target, goUp, verticalSearchDistance);
    }

    public Location findPlaceToStand(Location target, boolean goUp, int maxDelta)
    {
        int direction = goUp ? 1 : -1;

        // search for a spot to stand
        Location targetLocation = target.clone();
        int yDelta = 0;
        int minY = MIN_Y;
        int maxY = CompatibilityUtils.getMaxHeight(targetLocation.getWorld());

        while (minY <= targetLocation.getY() && targetLocation.getY() <= maxY && yDelta < maxDelta)
        {
            Block block = targetLocation.getBlock();
            if (isSafeLocation(block))
            {
                // spot found - return location
                return checkForHalfBlock(targetLocation);
            }

            if (!allowPassThrough(block.getType())) {
                return null;
            }

            yDelta++;
            targetLocation.setY(targetLocation.getY() + direction);
        }

        // no spot found
        return null;
    }

    protected Location checkForHalfBlock(Location location) {
        // This is a hack, but data-driving would be a pain.
        boolean isHalfBlock = false;
        Block downBlock = location.getBlock().getRelative(BlockFace.DOWN);
        Material material = downBlock.getType();
        if (material == Material.STEP || material == Material.WOOD_STEP) {
            // Drop down to half-steps
            isHalfBlock = (downBlock.getData() < 8);
        } else {
            isHalfBlock = isHalfBlock(material);
        }
        if (isHalfBlock) {
            location.setY(location.getY() - 0.5);
        }

        return location;
    }

    /**
     * Get the block the player is standing on.
     *
     * @return The Block the player is standing on
     */
    public Block getPlayerBlock()
    {
        Location location = getLocation();
        if (location == null) return null;
        return location.getBlock().getRelative(BlockFace.DOWN);
    }

    /**
     * Get the direction the player is facing as a BlockFace.
     *
     * @return a BlockFace representing the direction the player is facing
     */
    public BlockFace getPlayerFacing()
    {
        return getFacing(getLocation());
    }

    public static BlockFace getFacing(Location location)
    {
        float playerRot = location.getYaw();
        while (playerRot < 0)
            playerRot += 360;
        while (playerRot > 360)
            playerRot -= 360;

        BlockFace direction = BlockFace.NORTH;
        if (playerRot <= 45 || playerRot > 315)
        {
            direction = BlockFace.SOUTH;
        }
        else if (playerRot > 45 && playerRot <= 135)
        {
            direction = BlockFace.WEST;
        }
        else if (playerRot > 135 && playerRot <= 225)
        {
            direction = BlockFace.NORTH;
        }
        else if (playerRot > 225 && playerRot <= 315)
        {
            direction = BlockFace.EAST;
        }

        return direction;
    }

    /*
     * Functions to send text to player- use these to respect "quiet" and "silent" modes.
     */

    /**
     * Send a message to a player when a spell is cast.
     *
     * @param message The message to send
     */
    @Override
    public void castMessage(String message)
    {
        Wand activeWand = mage.getActiveWand();
        // First check wand
        if (!loud && activeWand != null && !activeWand.showCastMessages()) return;

        if (!quiet && canSendMessage() && message != null && message.length() > 0)
        {
            mage.castMessage(message);
            lastMessageSent = System.currentTimeMillis();
        }
    }

    /**
     * Send a message to a player.
     *
     * Use this to send messages to the player that are important.
     *
     * @param message The message to send
     */
    @Override
    public void sendMessage(String message)
    {
        Wand activeWand = mage.getActiveWand();

        // First check wand
        if (!loud && activeWand != null && !activeWand.showMessages()) return;

        if (!quiet && message != null && message.length() > 0)
        {
            mage.sendMessage(message);
            lastMessageSent = System.currentTimeMillis();
        }
    }

    public Location getLocation()
    {
        if (location != null) return location.clone();
        if (mage != null) {
            return mage.getLocation();
        }
        return null;
    }

    public Location getWandLocation()
    {
        if (this.location != null)
        {
            return location.clone();
        }
        return mage.getWandLocation();
    }

    @Override
    public Location getEyeLocation()
    {
        if (this.location != null)
        {
            return location.clone();
        }
        return mage.getEyeLocation();
    }

    public Vector getDirection()
    {
        if (location == null) {
            return mage.getDirection();
        }
        return location.getDirection();
    }

    public boolean isLookingUp()
    {
        Vector direction = getDirection();
        if (direction == null) return false;
        return direction.getY() > LOOK_THRESHOLD_RADIANS;
    }

    public boolean isLookingDown()
    {
        Vector direction = getDirection();
        if (direction == null) return false;
        return direction.getY() < -LOOK_THRESHOLD_RADIANS;
    }

    public World getWorld()
    {
        Location location = getLocation();
        if (location != null) return location.getWorld();
        return null;
    }

    /**
     * Check to see if the player is underwater
     *
     * @return true if the player is underwater
     */
    public boolean isUnderwater()
    {
        Block playerBlock = getPlayerBlock();
        if (playerBlock == null) return false;
        playerBlock = playerBlock.getRelative(BlockFace.UP);
        return (playerBlock.getType() == Material.WATER || playerBlock.getType() == Material.STATIONARY_WATER);
    }

    protected String getBlockSkin(Material blockType) {
        String skinName = null;
        switch (blockType) {
        case CACTUS:
            skinName = "MHF_Cactus";
            break;
        case CHEST:
            skinName = "MHF_Chest";
            break;
        case MELON_BLOCK:
            skinName = "MHF_Melon";
            break;
        case TNT:
            if (random.nextDouble() > 0.5) {
                skinName = "MHF_TNT";
            } else {
                skinName = "MHF_TNT2";
            }
            break;
        case LOG:
            skinName = "MHF_OakLog";
            break;
        case PUMPKIN:
            skinName = "MHF_Pumpkin";
            break;
        default:
            // TODO .. ?
            /*
             * Blocks:
                Bonus:
                MHF_ArrowUp
                MHF_ArrowDown
                MHF_ArrowLeft
                MHF_ArrowRight
                MHF_Exclamation
                MHF_Question
             */
        }

        return skinName;
    }

    protected String getMobSkin(EntityType mobType)
    {
        String mobSkin = null;
        switch (mobType) {
            case BLAZE:
                mobSkin = "MHF_Blaze";
                break;
            case CAVE_SPIDER:
                mobSkin = "MHF_CaveSpider";
                break;
            case CHICKEN:
                mobSkin = "MHF_Chicken";
                break;
            case COW:
                mobSkin = "MHF_Cow";
                break;
            case ENDERMAN:
                mobSkin = "MHF_Enderman";
                break;
            case GHAST:
                mobSkin = "MHF_Ghast";
                break;
            case IRON_GOLEM:
                mobSkin = "MHF_Golem";
                break;
            case MAGMA_CUBE:
                mobSkin = "MHF_LavaSlime";
                break;
            case MUSHROOM_COW:
                mobSkin = "MHF_MushroomCow";
                break;
            case OCELOT:
                mobSkin = "MHF_Ocelot";
                break;
            case PIG:
                mobSkin = "MHF_Pig";
                break;
            case PIG_ZOMBIE:
                mobSkin = "MHF_PigZombie";
                break;
            case SHEEP:
                mobSkin = "MHF_Sheep";
                break;
            case SLIME:
                mobSkin = "MHF_Slime";
                break;
            case SPIDER:
                mobSkin = "MHF_Spider";
                break;
            case SQUID:
                mobSkin = "MHF_Squid";
                break;
            case VILLAGER:
                mobSkin = "MHF_Villager";
                break;
            case WOLF:
                mobSkin = "MHF_Wolf";
                break;
            case CREEPER:
                mobSkin = "MHF_Creeper";
                break;
            case ZOMBIE:
                mobSkin = "MHF_Zombie";
                break;
            case SKELETON:
                mobSkin = "MHF_Skeleton";
                break;
            case GUARDIAN:
                mobSkin = "MHF_Guardian";
                break;
            case WITCH:
                mobSkin = "MHF_Witch";
                break;
            default:
        }

        return mobSkin;
    }

    public static Collection<PotionEffect> getPotionEffects(ConfigurationSection parameters)
    {
        return getPotionEffects(parameters, null);
    }

    public static Collection<PotionEffect> getPotionEffects(ConfigurationSection parameters, Integer duration)
    {
        List<PotionEffect> effects = new ArrayList<PotionEffect>();
        PotionEffectType[] effectTypes = PotionEffectType.values();
        for (PotionEffectType effectType : effectTypes) {
            // Why is there a null entry in this list? Maybe a 1.7 bug?
            if (effectType == null) continue;

            String parameterName = "effect_" + effectType.getName().toLowerCase();
            if (parameters.contains(parameterName)) {
                String value = parameters.getString(parameterName);

                int ticks = 10;
                int power = 1;
                try {
                    if (value.contains(",")) {
                        String[] pieces = value.split(",");
                        ticks = (int)Float.parseFloat(pieces[0]);
                        power = (int)Float.parseFloat(pieces[1]);
                    } else {
                        power = (int)Float.parseFloat(value);
                        if (duration != null) {
                            ticks = duration / 50;
                        }
                    }

                } catch (Exception ex) {
                    Bukkit.getLogger().warning("Error parsing potion effect for " + effectType + ": " + value);
                }
                PotionEffect effect = new PotionEffect(effectType, ticks, power, true);
                effects.add(effect);
            }
        }
        return effects;
    }

    public boolean isInCircle(int x, int z, int R)
    {
        return ((x * x) +  (z * z) - (R * R)) <= 0;
    }

    private boolean canSendMessage()
    {
        if (lastMessageSent == 0) return true;
        int throttle = controller.getMessageThrottle();
        long now = System.currentTimeMillis();
        return (lastMessageSent < now - throttle);
    }

    protected Location getEffectLocation()
    {
        return getEyeLocation();
    }

    public boolean hasBrushOverride()
    {
        return false;
    }

    @Override
    public boolean usesBrush()
    {
        return false;
    }

    @Override
    public boolean usesBrushSelection() {
        return (usesBrushSelection || usesBrush()) && !hasBrushOverride();
    }

    @Override
    public boolean isUndoable()
    {
        return false;
    }

    public void checkActiveCosts() {
        if (activeCosts == null) return;

        long now = System.currentTimeMillis();
        activeCostScale = (float)((double)(now - lastActiveCost) / 1000);
        lastActiveCost = now;

        for (CastingCost cost : activeCosts)
        {
            if (!cost.has(this))
            {
                deactivate();
                break;
            }

            cost.use(this);
        }

        activeCostScale = 1;
    }

    public void checkActiveDuration() {
        if (duration > 0 && lastCast < System.currentTimeMillis() - duration) {
            deactivate();
        }
    }

    protected List<CastingCost> parseCosts(ConfigurationSection node) {
        if (node == null) {
            return null;
        }
        List<CastingCost> castingCosts = new ArrayList<CastingCost>();
        Set<String> costKeys = node.getKeys(false);
        for (String key : costKeys)
        {
            castingCosts.add(new CastingCost(key, node.getInt(key, 1)));
        }

        return castingCosts;
    }

    @SuppressWarnings("unchecked")
    protected void loadTemplate(ConfigurationSection node)
    {
        // Get localizations
        String baseKey = spellKey.getBaseKey();

        // Message defaults come from the messages.yml file
        name = controller.getMessages().get("spells." + baseKey + ".name", baseKey);
        description = controller.getMessages().get("spells." + baseKey + ".description", "");
        extendedDescription = controller.getMessages().get("spells." + baseKey + ".extended_description", "");
        usage = controller.getMessages().get("spells." + baseKey + ".usage", "");

        // Upgrade path information
        // The actual upgrade spell will be set externally.
        requiredUpgradePath = node.getString("upgrade_required_path");
        requiredUpgradeCasts = node.getLong("upgrade_required_casts");

        // Inheritance, currently only used to look up messages, and only goes one level deep
        inheritKey = node.getString("inherit");

        // Can be overridden by the base spell, or the variant spell
        levelDescription = controller.getMessages().get("spells." + baseKey + ".level_description", levelDescription);
        upgradeDescription = controller.getMessages().get("spells." + baseKey + ".upgrade_description", upgradeDescription);

        // Spell level variants can override
        if (spellKey.isVariant()) {
            String variantKey = spellKey.getKey();
            name = controller.getMessages().get("spells." + variantKey + ".name", name);
            description = controller.getMessages().get("spells." + variantKey + ".description", description);
            extendedDescription = controller.getMessages().get("spells." + variantKey + ".extended_description", extendedDescription);
            usage = controller.getMessages().get("spells." + variantKey + ".usage", usage);

            // Level description defaults to pre-formatted text
            levelDescription = controller.getMessages().get("spell.level_description", levelDescription);

            // Any spell may have a level description, including base spells if chosen.
            // Base spells must specify their own level in each spell config though,
            // they don't get an auto-generated one.
            levelDescription = controller.getMessages().get("spells." + variantKey + ".level_description", levelDescription);
            upgradeDescription = controller.getMessages().get("spells." + variantKey + ".upgrade_description", upgradeDescription);
        }

        // Individual spell configuration overrides all
        name = node.getString("name", name);
        alias = node.getString("alias", "");
        extendedDescription = node.getString("extended_description", extendedDescription);
        description = node.getString("description", description);
        levelDescription = node.getString("level_description", levelDescription);

        // Parameterize level description
        if (levelDescription != null && !levelDescription.isEmpty()) {
            levelDescription = levelDescription.replace("$level", Integer.toString(spellKey.getLevel()));
        }

        // Load basic properties
        icon = ConfigurationUtils.getMaterialAndData(node, "icon", icon);
        iconURL = node.getString("icon_url");
        color = ConfigurationUtils.getColor(node, "color", null);
        worth = node.getDouble("worth", 0);
        if (node.contains("worth_sp")) {
            worth = node.getDouble("worth_sp", 0) * controller.getWorthSkillPoints();
        }
        earns = node.getInt("earns_sp", 0);
        earnCooldown = node.getInt("earns_cooldown", 0);
        category = controller.getCategory(node.getString("category"));
        Collection<String> tagList = ConfigurationUtils.getStringList(node, "tags");
        if (tagList != null) {
            tags = new HashSet<String>(tagList);
        } else {
            tags = null;
        }

        costs = parseCosts(node.getConfigurationSection("costs"));
        activeCosts = parseCosts(node.getConfigurationSection("active_costs"));
        pvpRestricted = node.getBoolean("pvp_restricted", false);
        quickCast = node.getBoolean("quick_cast", false);
        disguiseRestricted = node.getBoolean("disguise_restricted", true);
        worldBorderRestricted = node.getBoolean("world_border_restricted", false);
        usesBrushSelection = node.getBoolean("brush_selection", false);
        castOnNoTarget = node.getBoolean("cast_on_no_target", castOnNoTarget);
        hidden = node.getBoolean("hidden", false);
        showUndoable = node.getBoolean("show_undoable", true);
        cancellable = node.getBoolean("cancellable", true);

        // Preload some parameters
        parameters = node.getConfigurationSection("parameters");
        if (parameters != null) {
            bypassMageCooldown = parameters.getBoolean("bypass_mage_cooldown", false);
            cooldown = parameters.getInt("cooldown", 0);
            cooldown = parameters.getInt("cool", cooldown);
            mageCooldown = parameters.getInt("cooldown_mage", 0);
            bypassPvpRestriction = parameters.getBoolean("bypass_pvp", false);
            bypassPvpRestriction = parameters.getBoolean("bp", bypassPvpRestriction);
            bypassPermissions = parameters.getBoolean("bypass_permissions", false);
            bypassBuildRestriction = parameters.getBoolean("bypass_build", false);
            bypassBuildRestriction = parameters.getBoolean("bb", bypassBuildRestriction);
            bypassBreakRestriction = parameters.getBoolean("bypass_break", false);
            bypassProtection = parameters.getBoolean("bypass_protection", false);
            bypassProtection = parameters.getBoolean("bp", bypassProtection);
            duration = parameters.getInt("duration", 0);
            totalDuration = parameters.getInt("total_duration", -1);
        }

        effects.clear();
        if (node.contains("effects")) {
            ConfigurationSection effectsNode = node.getConfigurationSection("effects");
            Collection<String> effectKeys = effectsNode.getKeys(false);
            for (String effectKey : effectKeys) {
                if (effectsNode.isString(effectKey)) {
                    String referenceKey = effectsNode.getString(effectKey);
                    if (effects.containsKey(referenceKey)) {
                        effects.put(effectKey, new ArrayList<EffectPlayer>(effects.get(referenceKey)));
                    }
                }
                else
                {
                    effects.put(effectKey, EffectPlayer.loadEffects(controller.getPlugin(), effectsNode, effectKey));
                }
            }
        }
    }

    protected void preCast()
    {

    }

    protected void reset()
    {
        Location mageLocation = mage != null ? mage.getLocation() : null;

        // Kind of a hack, but assume the default location has no direction.
        if (this.location != null && mageLocation != null) {
            this.location.setPitch(mageLocation.getPitch());
            this.location.setYaw(mageLocation.getYaw());
        }

        backfired = false;

        if (!this.isActive)
        {
            this.currentCast = null;
        }
    }

    public boolean cast(String[] extraParameters, Location defaultLocation) {
        ConfigurationSection parameters = null;
        if (extraParameters != null && extraParameters.length > 0) {
            parameters = new MemoryConfiguration();
            ConfigurationUtils.addParameters(extraParameters, parameters);
        }
        return cast(parameters, defaultLocation);
    }

    public boolean cast(ConfigurationSection parameters)
    {
        return cast(parameters, null);
    }

    public boolean cast(ConfigurationSection extraParameters, Location defaultLocation)
    {
        if (mage.getDebugLevel() > 5 && extraParameters != null) {
            Collection<String> keys = extraParameters.getKeys(false);
            if (keys.size() > 0) {
                mage.sendDebugMessage(ChatColor.BLUE + "Cast " + ChatColor.GOLD + getName() + " " + ChatColor.GREEN + ConfigurationUtils.getParameters(extraParameters));
            }
        }
        this.reset();

        Location location = mage.getLocation();
        if (location != null) {
            Set<String> overrides = controller.getSpellOverrides(mage, location);
            if (overrides != null && !overrides.isEmpty())
            {
                if (extraParameters == null) {
                    extraParameters = new MemoryConfiguration();
                }
                for (String entry : overrides) {
                    String[] pieces = StringUtils.split(entry, ' ');
                    if (pieces.length < 2) continue;

                    String fullKey = pieces[0];
                    String[] key = StringUtils.split(fullKey, ".");
                    if (key.length == 0) continue;
                    if (key.length == 2 && !key[0].equals("default") && !key[0].equals(spellKey.getBaseKey()) && !key[0].equals(spellKey.getKey())) {
                        continue;
                    }

                    fullKey = key.length == 2 ? key[1] : key[0];
                    ConfigurationUtils.set(extraParameters, fullKey, pieces[1]);
                }
            }
        }

        if (this.currentCast == null)
        {
            this.currentCast = new CastContext();
            this.currentCast.setSpell(this);
        }

        if (this.parameters == null) {
            this.parameters = new MemoryConfiguration();
        }

        this.location = defaultLocation;

        workingParameters = new MemoryConfiguration();
        ConfigurationUtils.addConfigurations(workingParameters, this.parameters);
        ConfigurationUtils.addConfigurations(workingParameters, extraParameters);
        processParameters(workingParameters);

        // Allow other plugins to cancel this cast
        PreCastEvent preCast = new PreCastEvent(mage, this);
        Bukkit.getPluginManager().callEvent(preCast);

        if (preCast.isCancelled()) {
            processResult(SpellResult.CANCELLED, workingParameters);
            sendCastMessage(SpellResult.CANCELLED, " (no cast)");
            return false;
        }

        // Don't allow casting if the player is confused or weakened
        bypassConfusion = workingParameters.getBoolean("bypass_confusion", bypassConfusion);
        bypassWeakness = workingParameters.getBoolean("bypass_weakness", bypassWeakness);
        LivingEntity livingEntity = mage.getLivingEntity();
        if (livingEntity != null && !mage.isSuperPowered()) {
            if (!bypassConfusion && livingEntity.hasPotionEffect(PotionEffectType.CONFUSION)) {
                processResult(SpellResult.CURSED, workingParameters);
                sendCastMessage(SpellResult.CURSED, " (no cast)");
                return false;
            }

            // Don't allow casting if the player is weakened
            if (!bypassWeakness && livingEntity.hasPotionEffect(PotionEffectType.WEAKNESS)) {
                processResult(SpellResult.CURSED, workingParameters);
                sendCastMessage(SpellResult.CURSED, " (no cast)");
                return false;
            }
        }

        // Don't perform permission check until after processing parameters, in case of overrides
        if (!canCast(getLocation())) {
            processResult(SpellResult.INSUFFICIENT_PERMISSION, workingParameters);
            sendCastMessage(SpellResult.INSUFFICIENT_PERMISSION, " (no cast)");
            if (mage.getDebugLevel() > 1) {
                CommandSender messageTarget = mage.getDebugger();
                if (messageTarget == null) {
                    messageTarget = mage.getCommandSender();
                }
                if (messageTarget != null) {
                    mage.debugPermissions(messageTarget, this);
                }
            }
            return false;
        }

        this.preCast();

        // PVP override settings
        bypassPvpRestriction = workingParameters.getBoolean("bypass_pvp", false);
        bypassPvpRestriction = workingParameters.getBoolean("bp", bypassPvpRestriction);
        bypassPermissions = workingParameters.getBoolean("bypass_permissions", bypassPermissions);
        bypassFriendlyFire = workingParameters.getBoolean("bypass_friendly_fire", false);

        // Check cooldowns
        cooldown = workingParameters.getInt("cooldown", cooldown);
        cooldown = workingParameters.getInt("cool", cooldown);
        mageCooldown = workingParameters.getInt("cooldown_mage", mageCooldown);

        // Color override
        color = ConfigurationUtils.getColor(workingParameters, "color", color);
        particle = workingParameters.getString("particle", null);

        long cooldownRemaining = getRemainingCooldown() / 1000;
        String timeDescription = "";
        if (cooldownRemaining > 0) {
            if (cooldownRemaining > 60 * 60 ) {
                long hours = cooldownRemaining / (60 * 60);
                if (hours == 1) {
                    timeDescription = controller.getMessages().get("cooldown.wait_hour");
                } else {
                    timeDescription = controller.getMessages().get("cooldown.wait_hours").replace("$hours", ((Long) hours).toString());
                }
            } else if (cooldownRemaining > 60) {
                long minutes = cooldownRemaining / 60;
                if (minutes == 1) {
                    timeDescription = controller.getMessages().get("cooldown.wait_minute");
                } else {
                    timeDescription = controller.getMessages().get("cooldown.wait_minutes").replace("$minutes", ((Long) minutes).toString());
                }
            } else if (cooldownRemaining > 1) {
                timeDescription = controller.getMessages().get("cooldown.wait_seconds").replace("$seconds", ((Long)cooldownRemaining).toString());
            } else {
                timeDescription = controller.getMessages().get("cooldown.wait_moment");
            }
            castMessage(getMessage("cooldown").replace("$time", timeDescription));
            processResult(SpellResult.COOLDOWN, workingParameters);
            sendCastMessage(SpellResult.COOLDOWN, " (no cast)");
            return false;
        }

        com.elmakers.mine.bukkit.api.spell.CastingCost required = getRequiredCost();
        if (required != null) {
            String baseMessage = getMessage("insufficient_resources");
            String costDescription = required.getDescription(controller.getMessages(), mage);
            // Send loud messages when items are required.
            if (required.getAmount() > 0) {
                sendMessage(baseMessage.replace("$cost", costDescription));
            } else {
                castMessage(baseMessage.replace("$cost", costDescription));
            }
            processResult(SpellResult.INSUFFICIENT_RESOURCES, workingParameters);
            sendCastMessage(SpellResult.INSUFFICIENT_RESOURCES, " (no cast)");
            return false;
        }

        return finalizeCast(workingParameters);
    }

    public boolean canCast(Location location) {
        if (location == null) return true;
        if (!hasCastPermission(mage.getCommandSender())) return false;
        Entity entity = mage.getEntity();
        if (disguiseRestricted && entity != null && entity instanceof Player && controller.isDisguised(entity)) return false;
        Boolean regionPermission = controller.getRegionCastPermission(mage.getPlayer(), this, location);
        if (regionPermission != null && regionPermission == true) return true;
        Boolean personalPermission = controller.getPersonalCastPermission(mage.getPlayer(), this, location);
        if (personalPermission != null && personalPermission == true) return true;
        if (regionPermission != null && regionPermission == false) return false;
        if (requiresBuildPermission() && !hasBuildPermission(location.getBlock())) return false;
        if (requiresBreakPermission() && !hasBreakPermission(location.getBlock())) return false;
        if (worldBorderRestricted)
        {
            WorldBorder border = location.getWorld().getWorldBorder();
            double borderSize = border.getSize() / 2 - border.getWarningDistance();
            Location offset = location.subtract(border.getCenter());
            if (offset.getX() < -borderSize || offset.getX() > borderSize || offset.getZ() < -borderSize || offset.getZ() > borderSize) return false;
        }
        return !pvpRestricted || bypassPvpRestriction || mage.isPVPAllowed(location);
    }

    @Override
    public boolean isPvpRestricted() {
        return pvpRestricted && !bypassPvpRestriction;
    }

    @Override
    public boolean isDisguiseRestricted() {
        return disguiseRestricted;
    }

    @Override
    public boolean requiresBuildPermission() {
        return false;
    }

    @Override
    public boolean requiresBreakPermission() {
        return false;
    }

    public boolean hasBreakPermission(Location location) {
        if (location == null) return true;
        return hasBreakPermission(location.getBlock());
    }

    public boolean hasBreakPermission(Block block) {
        // Cast permissions bypass
        if (bypassBreakRestriction) return true;
        Boolean castPermission = controller.getRegionCastPermission(mage.getPlayer(), this, block.getLocation());
        if (castPermission != null && castPermission == true) return true;
        if (castPermission != null && castPermission == false) return false;
        return mage.hasBreakPermission(block);
    }

    public boolean hasBuildPermission(Location location) {
        if (location == null) return true;
        return hasBuildPermission(location.getBlock());
    }

    public boolean hasBuildPermission(Block block) {
        // Cast permissions bypass
        if (bypassBuildRestriction) return true;
        Boolean castPermission = controller.getRegionCastPermission(mage.getPlayer(), this, block.getLocation());
        if (castPermission != null && castPermission == true) return true;
        if (castPermission != null && castPermission == false) return false;
        return mage.hasBuildPermission(block);
    }

    protected void onBackfire() {

    }

    protected void backfire() {
        if (!backfired) {
            onBackfire();
        }
        backfired = true;
    }

    protected boolean finalizeCast(ConfigurationSection parameters) {
        SpellResult result = null;

        // Global parameters
        controller.disablePhysics(parameters.getInt("disable_physics", 0));

        if (!mage.isSuperPowered()) {
            if (backfireChance > 0 && random.nextDouble() < backfireChance) {
                backfire();
            } else if (fizzleChance > 0 && random.nextDouble() < fizzleChance) {
                result = SpellResult.FIZZLE;
            }
        }

        if (result == null) {
            result = onCast(parameters);
        }
        if (backfired) {
            result = SpellResult.BACKFIRE;
        }
        if (result == SpellResult.CAST) {
            LivingEntity sourceEntity = mage.getLivingEntity();
            Entity targetEntity = getTargetEntity();
            if (sourceEntity == targetEntity) {
                result = SpellResult.CAST_SELF;
            }
        }
        processResult(result, parameters);

        boolean success = result.isSuccess();
        boolean requiresCost = success || (castOnNoTarget && (result == SpellResult.NO_TARGET || result == SpellResult.NO_ACTION));
        boolean free = !requiresCost && result.isFree();
        if (!free) {
            if (costs != null && !mage.isCostFree()) {
                for (CastingCost cost : costs)
                {
                    cost.use(this);
                }
            }
            updateCooldown();
        }

        sendCastMessage(result, " (" + success + ")");
        return success;
    }

    protected void updateCooldown() {
        lastCast = System.currentTimeMillis();
        if (!mage.isCooldownFree() && cooldown > 0) {
            double cooldownReduction = mage.getCooldownReduction() + this.cooldownReduction;
            if (cooldownReduction < 1) {
                int reducedCooldown = (int)Math.ceil((1.0f - cooldownReduction) * cooldown);
                cooldownExpiration = Math.max(cooldownExpiration, System.currentTimeMillis() + reducedCooldown);
            }
        }
        if (!mage.isCooldownFree() && mageCooldown > 0) {
            double cooldownReduction = mage.getCooldownReduction() + this.cooldownReduction;
            if (cooldownReduction < 1) {
                int reducedCooldown = (int)Math.ceil((1.0f - cooldownReduction) * mageCooldown);
                mage.setRemainingCooldown(reducedCooldown);
            }
        }
    }

    protected void sendCastMessage(SpellResult result, String message)
    {
        Location source = getEyeLocation();
        if (mage == null || source == null) return;

        mage.sendDebugMessage(ChatColor.WHITE + "Cast " + ChatColor.GOLD + getName() + ChatColor.WHITE + " from " +
                ChatColor.GRAY + source.getBlockX() +
                ChatColor.DARK_GRAY + ","  + ChatColor.GRAY + source.getBlockY() +
                ChatColor.DARK_GRAY + "," + ChatColor.GRAY + source.getBlockZ() +
                ChatColor.WHITE  + ": " + ChatColor.AQUA + result.name().toLowerCase() +
                ChatColor.DARK_AQUA + message);
    }

    @Override
    public String getMessage(String messageKey) {
        return getMessage(messageKey, "");
    }

    public String getMessage(String messageKey, String def) {
        String message = controller.getMessages().get("spells.default." + messageKey, def);
        if (inheritKey != null && !inheritKey.isEmpty()) {
            message = controller.getMessages().get("spells." + inheritKey + "." + messageKey, message);
        }
        message = controller.getMessages().get("spells." + spellKey.getBaseKey() + "." + messageKey, message);
        if (spellKey.isVariant()) {
            message = controller.getMessages().get("spells." + spellKey.getKey() + "." + messageKey, message);
        }
        if (message == null) message = "";
        else if (!message.isEmpty()) {
            // Escape some common parameters
            String playerName = mage.getName();
            message = message.replace("$player", playerName);

            if (message.contains("$material"))
            {
                String materialName = getDisplayMaterialName();

                // TODO: Localize "None", provide static getter
                materialName = materialName == null ? "None" : materialName;
                message = message.replace("$material", materialName);
            }
        }
        return message;
    }

    protected String getDisplayMaterialName()
    {
        return "None";
    }

    protected void processResult(SpellResult result, ConfigurationSection parameters) {
        if (mage != null) {
            mage.onCast(this, result);
        }

        // Show messaging
        String resultName = result.name().toLowerCase();
        if (!mage.isQuiet())
        {
            if (result.isSuccess()) {
                String message = null;
                if (result != SpellResult.CAST) {
                    message = getMessage("cast");
                }
                if (result.isAlternate() && result != SpellResult.ALTERNATE) {
                    message = getMessage("alternate", message);
                }
                message = getMessage(resultName, message);
                LivingEntity sourceEntity = mage.getLivingEntity();
                Entity targetEntity = getTargetEntity();
                if (targetEntity == sourceEntity) {
                    message = getMessage("cast_self", message);
                } else if (targetEntity instanceof Player) {
                    message = getMessage("cast_player", message);
                } else if (targetEntity instanceof LivingEntity) {
                    message = getMessage("cast_livingentity", message);
                } else if (targetEntity instanceof Entity) {
                    message = getMessage("cast_entity", message);
                }
                if (loud) {
                    sendMessage(message);
                } else {
                    castMessage(message);
                }
            } else
            // Special cases where messaging is handled elsewhere
            if (result != SpellResult.INSUFFICIENT_RESOURCES && result != SpellResult.COOLDOWN)
            {
                String message = null;
                if (result.isFailure() && result != SpellResult.FAIL) {
                    message = getMessage("fail");
                }

                sendMessage(getMessage(resultName, message));
            }
        }

        // Play effects
        playEffects(resultName);

        // Check for finalization
        if (currentCast != null) {
            // Legacy spells never update the final context result.
            if (isLegacy()) currentCast.addResult(result);
            // Batched spells will call finish() on completion
            if (!isBatched()) currentCast.finish();
        }
    }

    protected boolean isBatched() {
        return false;
    }

    protected boolean isLegacy() {
        return true;
    }

    public void messageTargets(String messageKey)
    {
        if (messageTargets && currentCast != null)
        {
            currentCast.messageTargets(messageKey);
        }
    }

    public void playEffects(String effectName, float scale, Block sourceBlock) {
        playEffects(effectName, getCurrentCast(), scale, sourceBlock);
    }

    public void playEffects(String effectName, float scale) {
        playEffects(effectName, getCurrentCast(), scale, null);
    }

    @Override
    public void playEffects(String effectName)
    {
        playEffects(effectName, 1);
    }

    @Override
    public void playEffects(String effectName, com.elmakers.mine.bukkit.api.action.CastContext context)
    {
        playEffects(effectName, context, 1);
    }

    public void playEffects(String effectName, com.elmakers.mine.bukkit.api.action.CastContext context, float scale, Block block) {
        context.playEffects(effectName, scale, block);
    }

    @Override
    public void playEffects(String effectName, com.elmakers.mine.bukkit.api.action.CastContext context, float scale) {
        playEffects(effectName, context, scale, null);
    }

    @Override
    public void target() {

    }

    @Override
    public Location getTargetLocation() {
        return null;
    }

    @Override
    public boolean canTarget(Entity entity) {
        if (!bypassPvpRestriction && entity instanceof Player)
        {
            Player magePlayer = mage.getPlayer();
            if (magePlayer != null)
            {
                if (!controller.isPVPAllowed(magePlayer, entity.getLocation())) return false;
                if (!controller.isPVPAllowed(magePlayer, mage.getLocation())) return false;
            }
        }
        if (!bypassProtection && !bypassFriendlyFire)
        {
            return controller.canTarget(mage.getEntity(), entity);
        }
        return true;
    }

    @Override
    public Entity getTargetEntity() {
        return null;
    }

    @Override
    public com.elmakers.mine.bukkit.api.block.MaterialAndData getEffectMaterial()
    {
        return new MaterialAndData(DEFAULT_EFFECT_MATERIAL);
    }

    public void processParameters(ConfigurationSection parameters) {
        fizzleChance = (float)parameters.getDouble("fizzle_chance", 0);
        backfireChance = (float)parameters.getDouble("backfire_chance", 0);

        Location defaultLocation = location == null ? mage.getLocation() : location;
        Location locationOverride = ConfigurationUtils.overrideLocation(parameters, "p", defaultLocation, controller.canCreateWorlds());
        if (locationOverride != null) {
            location = locationOverride;
        }
        costReduction = (float)parameters.getDouble("cost_reduction", 0);
        consumeReduction = (float)parameters.getDouble("consume_reduction", 0);
        cooldownReduction = (float)parameters.getDouble("cooldown_reduction", 0);
        bypassMageCooldown = parameters.getBoolean("bypass_mage_cooldown", false);
        cancelOnDamage = parameters.getDouble("cancel_on_damage", 0);

        if (parameters.contains("prevent_passthrough")) {
            preventPassThroughMaterials = controller.getMaterialSet(parameters.getString("prevent_passthrough"));
        } else {
            preventPassThroughMaterials = controller.getMaterialSet("indestructible");
        }

        if (parameters.contains("passthrough")) {
            passthroughMaterials = controller.getMaterialSet(parameters.getString("passthrough"));
        } else {
            passthroughMaterials = controller.getMaterialSet("passthrough");
        }

        if (parameters.contains("unsafe")) {
            unsafeMaterials = controller.getMaterialSet(parameters.getString("unsafe"));
        } else {
            unsafeMaterials = controller.getMaterialSet("unsafe");
        }

        bypassDeactivate = parameters.getBoolean("bypass_deactivate", false);
        quiet = parameters.getBoolean("quiet", false);
        loud = parameters.getBoolean("loud", false);
        targetSelf = parameters.getBoolean("target_self", false);
        messageTargets = parameters.getBoolean("message_targets", true);
        verticalSearchDistance = parameters.getInt("vertical_range", 8);
        trackCasts = parameters.getBoolean("track_casts", true);

        cooldown = parameters.getInt("cooldown", 0);
        cooldown = parameters.getInt("cool", cooldown);
        bypassPvpRestriction = parameters.getBoolean("bypass_pvp", false);
        bypassPvpRestriction = parameters.getBoolean("bp", bypassPvpRestriction);
        bypassPermissions = parameters.getBoolean("bypass_permissions", false);
        bypassBuildRestriction = parameters.getBoolean("bypass_build", false);
        bypassBuildRestriction = parameters.getBoolean("bb", bypassBuildRestriction);
        bypassBreakRestriction = parameters.getBoolean("bypass_break", false);
        bypassProtection = parameters.getBoolean("bypass_protection", false);
        bypassProtection = parameters.getBoolean("bp", bypassProtection);
        duration = parameters.getInt("duration", 0);
        totalDuration = parameters.getInt("total_duration", 0);
    }


    public String getPermissionNode()
    {
        return "Magic.cast." + spellKey.getBaseKey();
    }

    /**
     * Called when a material selection spell is cancelled mid-selection.
     */
    public boolean onCancel()
    {
        return false;
    }

    /**
     * Listener method, called on player quit for registered spells.
     *
     * @param event The player who just quit
     */
    public void onPlayerQuit(PlayerQuitEvent event)
    {

    }

    /**
     * Listener method, called on player move for registered spells.
     *
     * @param event The original entity death event
     */
    public void onPlayerDeath(EntityDeathEvent event)
    {

    }

    public void onPlayerDamage(EntityDamageEvent event)
    {

    }

    /**
     * Used internally to initialize the Spell, do not call.
     *
     * @param instance The spells instance
     */
    public void initialize(MageController instance)
    {
        this.controller = instance;
    }

    @Override
    public long getCastCount()
    {
        return castCount;
    }

    @Override
    public void setCastCount(long count) {
        castCount = count;
    }

    public void onActivate() {

    }

    public void onDeactivate() {

    }

    /**
     * Called on player data load.
     */
    public void onLoad(ConfigurationSection node)
    {

    }

    /**
     * Called on player data save.
     *
     * @param node The configuration node to load data from.
     */
    public void onSave(ConfigurationSection node)
    {

    }

    //
    // Cloneable implementation
    //

    @Override
    public Object clone()
    {
        try
        {
            return super.clone();
        }
        catch (CloneNotSupportedException ex)
        {
            return null;
        }
    }

    //
    // CostReducer Implementation
    //

    @Override
    public float getConsumeReduction()
    {
        return consumeReduction + mage.getConsumeReduction();
    }

    @Override
    public float getCostReduction()
    {
        return costReduction + mage.getCostReduction();
    }

    @Override
    public float getCostScale()
    {
        return activeCostScale;
    }

    //
    // Public API Implementation
    //

    @Override
    public com.elmakers.mine.bukkit.api.spell.Spell createSpell()
    {
        BaseSpell spell = null;
        try {
            spell = this.getClass().newInstance();
            spell.initialize(controller);
            spell.loadTemplate(spellKey.getKey(), configuration);
            spell.template = this;
        } catch (Throwable ex) {
            controller.getLogger().log(Level.WARNING, "Error creating spell " + spellKey.getKey(), ex);
        }
        return spell;
    }

    @Override
    public boolean cast()
    {
        return cast((ConfigurationSection)null, null);
    }

    @Override
    public boolean cast(String[] extraParameters)
    {
        return cast(extraParameters, null);
    }

    @Override
    public final String getKey()
    {
        return spellKey.getKey();
    }

    @Override
    public final String getName()
    {
        return name;
    }

    @Override
    public final String getAlias()
    {
        return alias;
    }

    public boolean isQuickCast() {
        return quickCast;
    }

    @Override
    public final com.elmakers.mine.bukkit.api.block.MaterialAndData getIcon()
    {
        return icon;
    }

    @Override
    public boolean hasIcon() {
        return icon != null && icon.getMaterial() != Material.AIR;
    }

    @Override
    public final String getDescription()
    {
        return description;
    }

    @Override
    public final String getExtendedDescription()
    {
        return extendedDescription;
    }

    @Override
    public final String getLevelDescription()
    {
        return levelDescription;
    }

    @Override
    public final SpellKey getSpellKey()
    {
        return spellKey;
    }

    @Override
    public final String getUsage()
    {
        return usage;
    }

    @Override
    public final double getWorth()
    {
        return worth;
    }

    @Override
    public final double getEarns()
    {
        return earns;
    }

    @Override
    public final SpellCategory getCategory()
    {
        return category;
    }

    @Override
    public boolean hasTag(String tag) {
        if (category != null && category.getKey().equals(tag)) return true;
        return tags != null && tags.contains(tag);
    }

    @Override
    public boolean hasAnyTag(Collection<String> tagSet) {
        if (category != null && tagSet.contains(category.getKey())) return true;
        return tags != null && !Collections.disjoint(tagSet, tags);
    }

    @Override
    public Collection<com.elmakers.mine.bukkit.api.effect.EffectPlayer> getEffects(SpellResult result) {
        return getEffects(result.name().toLowerCase());
    }

    public boolean hasEffects(String key) {
        Collection<EffectPlayer> effectList = effects.get(key);
        return effectList != null && effectList.size() > 0;
    }

    @Override
    public Collection<com.elmakers.mine.bukkit.api.effect.EffectPlayer> getEffects(String key) {
        Collection<EffectPlayer> effectList = effects.get(key);
        if (effectList == null) {
            return new ArrayList<com.elmakers.mine.bukkit.api.effect.EffectPlayer>();
        }
        return new ArrayList<com.elmakers.mine.bukkit.api.effect.EffectPlayer>(effectList);
    }

    @Override
    public Collection<com.elmakers.mine.bukkit.api.spell.CastingCost> getCosts() {
        if (costs == null) return null;
        List<com.elmakers.mine.bukkit.api.spell.CastingCost> copy = new ArrayList<com.elmakers.mine.bukkit.api.spell.CastingCost>();
        copy.addAll(costs);
        return copy;
    }

    @Override
    public Collection<com.elmakers.mine.bukkit.api.spell.CastingCost> getActiveCosts() {
        if (activeCosts == null) return null;
        List<com.elmakers.mine.bukkit.api.spell.CastingCost> copy = new ArrayList<com.elmakers.mine.bukkit.api.spell.CastingCost>();
        copy.addAll(activeCosts);
        return copy;
    }

    @Override
    public ConfigurationSection getWorkingParameters() {
        return workingParameters;
    }

    @Override
    public void getParameters(Collection<String> parameters)
    {
        parameters.addAll(Arrays.asList(COMMON_PARAMETERS));
    }

    @Override
    public void getParameterOptions(Collection<String> examples, String parameterKey)
    {
        if (parameterKey.equals("duration")) {
            examples.addAll(Arrays.asList(EXAMPLE_DURATIONS));
        } else if (parameterKey.equals("range")) {
            examples.addAll(Arrays.asList(EXAMPLE_SIZES));
        } else if (parameterKey.equals("transparent")) {
            examples.addAll(controller.getMaterialSets());
        } else if (parameterKey.equals("player")) {
            examples.addAll(controller.getPlayerNames());
        } else if (parameterKey.equals("target")) {
            TargetType[] targetTypes = TargetType.values();
            for (TargetType targetType : targetTypes) {
                examples.add(targetType.name().toLowerCase());
            }
        } else if (parameterKey.equals("target")) {
            TargetType[] targetTypes = TargetType.values();
            for (TargetType targetType : targetTypes) {
                examples.add(targetType.name().toLowerCase());
            }
        } else if (parameterKey.equals("target_type")) {
            EntityType[] entityTypes = EntityType.values();
            for (EntityType entityType : entityTypes) {
                examples.add(entityType.name().toLowerCase());
            }
        } else if (booleanParameterMap.contains(parameterKey)) {
            examples.addAll(Arrays.asList(EXAMPLE_BOOLEANS));
        } else if (vectorParameterMap.contains(parameterKey)) {
            examples.addAll(Arrays.asList(EXAMPLE_VECTOR_COMPONENTS));
        } else if (worldParameterMap.contains(parameterKey)) {
            List<World> worlds = Bukkit.getWorlds();
            for (World world : worlds) {
                examples.add(world.getName());
            }
        } else if (percentageParameterMap.contains(parameterKey)) {
            examples.addAll(Arrays.asList(EXAMPLE_PERCENTAGES));
        }
    }

    @Override
    public String getMageCooldownDescription() {
        return getCooldownDescription(controller.getMessages(), mageCooldown);
    }

    @Override
    public String getCooldownDescription() {
        return getCooldownDescription(controller.getMessages(), cooldown);
    }

    public static String getCooldownDescription(Messages messages, int cooldown) {
        if (cooldown > 0) {
            int cooldownInSeconds = cooldown / 1000;
            if (cooldownInSeconds > 60 * 60 ) {
                int hours = cooldownInSeconds / (60 * 60);
                if (hours == 1) {
                    return messages.get("cooldown.description_hour");
                }
                return messages.get("cooldown.description_hours").replace("$hours", ((Integer)hours).toString());
            } else if (cooldownInSeconds > 60) {
                int minutes = cooldownInSeconds / 60;
                if (minutes == 1) {
                    return messages.get("cooldown.description_minute");
                }
                return messages.get("cooldown.description_minutes").replace("$minutes", ((Integer)minutes).toString());
            } else if (cooldownInSeconds > 1) {
                return messages.get("cooldown.description_seconds").replace("$seconds", ((Integer)cooldownInSeconds).toString());
            } else {
                return messages.get("cooldown.description_moment");
            }
        }
        return null;
    }

    @Override
    public long getCooldown()
    {
        return cooldown;
    }

    @Override
    public CastingCost getRequiredCost() {
        if (!mage.isCostFree())
        {
            if (costs != null && !isActive)
            {
                for (CastingCost cost : costs)
                {
                    if (!cost.has(this))
                    {
                        return cost;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public void clearCooldown() {
        cooldownExpiration = 0;
    }

    @Override
    public void setRemainingCooldown(long ms) {
        cooldownExpiration = Math.max(ms + System.currentTimeMillis(), cooldownExpiration);
    }

    @Override
    public long getRemainingCooldown() {
        long remaining = 0;
        if (mage.isCooldownFree()) return 0;
        if (cooldownExpiration > 0)
        {
            long now = System.currentTimeMillis();
            if (cooldownExpiration > now) {
                remaining = cooldownExpiration - now;
            } else {
                cooldownExpiration = 0;
            }
        }

        return bypassMageCooldown ? remaining : Math.max(mage.getRemainingCooldown(), remaining);
    }

    @Override
    public long getDuration()
    {
        if (totalDuration >= 0) {
            return totalDuration;
        }
        return duration;
    }

    @Override
    public int getRange()
    {
        return 0;
    }

    @Override
    public void setMage(Mage mage)
    {
        this.mage = mage;
    }

    @Override
    public boolean cancel()
    {
        boolean cancelled = onCancel();
        if (cancelled) {
            sendMessage(getMessage("cancel"));
        }
        if (currentCast != null) {
            currentCast.cancelEffects();
        }
        return cancelled;
    }

    @Override
    public void setActive(boolean active) {
        if (active && !isActive) {
            onActivate();
        } else if (!active && isActive) {
            onDeactivate();
        }
        isActive = active;
        lastActiveCost = System.currentTimeMillis();
    }

    @Override
    public void activate() {
        if (!isActive) {
            mage.activateSpell(this);
        }
    }

    @Override
    public boolean deactivate() {
        updateCooldown();
        return deactivate(false, false);
    }

    @Override
    public boolean deactivate(boolean force, boolean quiet) {
        if (!force && bypassDeactivate) {
            return false;
        }
        if (isActive) {
            isActive = false;
            onDeactivate();

            mage.deactivateSpell(this);
            if (!quiet) {
                sendMessage(getMessage("deactivate"));
            }
            if (currentCast != null) {
                currentCast.cancelEffects();
            }
        }

        return true;
    }

    @Override
    public Mage getMage() {
        return mage;
    }

    @Override
    public void load(SpellData spellData) {
        try {
            castCount = spellData.getCastCount();
            lastCast = spellData.getLastCast();
            lastEarn = spellData.getLastEarn();
            cooldownExpiration = spellData.getCooldownExpiration();
            if (category != null && template == null) {
                category.addCasts(castCount, lastCast);
            }
            onLoad(spellData.getExtraData());
        } catch (Exception ex) {
            controller.getPlugin().getLogger().warning("Failed to load data for spell " + name + ": " + ex.getMessage());
        }
    }

    @Override
    public void save(SpellData spellData) {
        try {
            spellData.setCastCount(castCount);
            spellData.setLastCast(lastCast);
            spellData.setLastEarn(lastEarn);
            spellData.setCooldownExpiration(cooldownExpiration);
            spellData.setIsActive(isActive);
            onSave(spellData.getExtraData());
        } catch (Exception ex) {
            controller.getPlugin().getLogger().warning("Failed to save data for spell " + name);
            ex.printStackTrace();
        }
    }

    @Override
    public void loadTemplate(String key, ConfigurationSection node)
    {
        spellKey = new SpellKey(key);
        this.configuration = node;
        this.loadTemplate(node);
    }

    @Override
    public void tick()
    {
        checkActiveDuration();
        checkActiveCosts();
    }

    @Override
    public boolean isActive()
    {
         return isActive;
    }

    @Override
    public int compareTo(com.elmakers.mine.bukkit.api.spell.SpellTemplate other)
    {
        return name.compareTo(other.getName());
    }

    @Override
    public boolean hasCastPermission(CommandSender sender)
    {
        if (sender == null || bypassPermissions) return true;

        return controller.hasCastPermission(sender, this);
    }

    @Override
    public Color getColor()
    {
        if (color != null) return color;
        if (category != null) return category.getColor();
        return null;
    }

    @Override
    public boolean isHidden()
    {
        return hidden;
    }

    //
    // Spell abstract interface
    //

    /**
     * Called when this spell is cast.
     *
     * This is where you do your work!
     *
     * If parameters were passed to this spell, either via a variant or the command line,
     * they will be passed in here.
     *
     * @param parameters Any parameters that were passed to this spell
     * @return true if the spell worked, false if it failed
     */
    public abstract SpellResult onCast(ConfigurationSection parameters);

    @Override
    public MageController getController() {
        return controller;
    }

    @Override
    public String getIconURL() {
        return iconURL;
    }

    @Override
    public String getRequiredUpgradePath() {
        return requiredUpgradePath;
    }

    @Override
    public long getRequiredUpgradeCasts() {
        return requiredUpgradeCasts;
    }

    @Override
    public String getUpgradeDescription() {
        return upgradeDescription == null ? "" : upgradeDescription;
    }

    @Override
    public SpellTemplate getUpgrade() {
        if (requiredUpgradeCasts <= 0) {
            return null;
        }
        SpellKey upgradeKey = new SpellKey(spellKey.getBaseKey(), spellKey.getLevel() + 1);
        return controller.getSpellTemplate(upgradeKey.getKey());
    }

    @Override
    public ConfigurationSection getConfiguration() {
        return this.configuration;
    }

    @Override
    public com.elmakers.mine.bukkit.api.action.CastContext getCurrentCast() {
        if (currentCast == null) {
            currentCast = new CastContext();
            this.currentCast.setSpell(this);
        }
        return currentCast;
    }

    @Override
    public Entity getEntity() {
        return mage.getEntity();
    }

    @Override
    public String getEffectParticle() {
        if (particle == null) {
            return mage.getEffectParticleName();
        }
        return particle;
    }

    @Override
    public Color getEffectColor() {
        if (color == null) {
            return mage.getEffectColor();
        }

        return color;
    }

    @Override
    public boolean showUndoable() {
        return showUndoable;
    }

    public int getVerticalSearchDistance() {
        return verticalSearchDistance;
    }

    @Override
    public void addLore(Messages messages, Mage mage, Wand wand, List<String> lore) {
        CostReducer reducer = wand == null ? mage : wand;
        if (levelDescription != null && levelDescription.length() > 0) {
            InventoryUtils.wrapText(ChatColor.GOLD + levelDescription, MAX_LORE_LENGTH, lore);
        }
        if (description != null && description.length() > 0) {
            InventoryUtils.wrapText(description, MAX_LORE_LENGTH, lore);
        }
        if (usage != null && usage.length() > 0) {
            InventoryUtils.wrapText(usage, MAX_LORE_LENGTH, lore);
        }
        if (quickCast && wand != null && !wand.isQuickCastDisabled()) {
            String quickCastText = messages.get("spell.quick_cast", "");
            if (!quickCastText.isEmpty()) {
                lore.add(quickCastText);
            }
        }
        String cooldownDescription = getCooldownDescription();
        if (cooldownDescription != null && !cooldownDescription.isEmpty()) {
            lore.add(messages.get("cooldown.description").replace("$time", cooldownDescription));
        }
        String mageCooldownDescription = getMageCooldownDescription();
        if (mageCooldownDescription != null && !mageCooldownDescription.isEmpty()) {
            lore.add(messages.get("cooldown.mage_description").replace("$time", mageCooldownDescription));
        }
        if (costs != null) {
            for (com.elmakers.mine.bukkit.api.spell.CastingCost cost : costs) {
                if (cost.hasCosts(reducer)) {
                    lore.add(ChatColor.YELLOW + messages.get("wand.costs_description").replace("$description", cost.getFullDescription(messages, reducer)));
                }
            }
        }
        if (activeCosts != null) {
            for (com.elmakers.mine.bukkit.api.spell.CastingCost cost : activeCosts) {
                if (cost.hasCosts(reducer)) {
                    lore.add(ChatColor.YELLOW + messages.get("wand.active_costs_description").replace("$description", cost.getFullDescription(messages, reducer)));
                }
            }
        }

        int range = getRange();
        if (range > 0) {
            lore.add(ChatColor.GRAY + messages.get("wand.range_description").replace("$range", Integer.toString(range)));
        }

        long effectiveDuration = this.getDuration();
        if (effectiveDuration > 0) {
            long seconds = effectiveDuration / 1000;
            if (seconds > 60 * 60 ) {
                long hours = seconds / (60 * 60);
                lore.add(ChatColor.GRAY + messages.get("duration.lasts_hours").replace("$hours", ((Long)hours).toString()));
            } else if (seconds > 60) {
                long minutes = seconds / 60;
                lore.add(ChatColor.GRAY + messages.get("duration.lasts_minutes").replace("$minutes", ((Long)minutes).toString()));
            } else {
                lore.add(ChatColor.GRAY + messages.get("duration.lasts_seconds").replace("$seconds", ((Long)seconds).toString()));
            }
        }
        else if (showUndoable()) {
            if (isUndoable()) {
                String undoableText = messages.get("spell.undoable", "");
                if (!undoableText.isEmpty()) {
                    lore.add(undoableText);
                }
            } else {
                String undoableText = messages.get("spell.not_undoable", "");
                if (!undoableText.isEmpty()) {
                    lore.add(undoableText);
                }
            }
        }

        if (usesBrush()) {
            String brushText = messages.get("spell.brush");
            if (!brushText.isEmpty()) {
                lore.add(ChatColor.GOLD + brushText);
            }
        }

        if (earns > 0 && controller.isSPEnabled()) {
            String earnsText = messages.get("spell.earns").replace("$earns", Integer.toString(earns));
            if (!earnsText.isEmpty()) {
                lore.add(earnsText);
            }
        }
    }

    @Override
    public com.elmakers.mine.bukkit.api.block.MaterialBrush getBrush()
    {
        if (mage == null) {
            return null;
        }
        return mage.getBrush();
    }

    @Override
    public boolean isCancellable() {
        return cancellable;
    }

    @Override
    public void finish(com.elmakers.mine.bukkit.api.action.CastContext context) {
        SpellResult result = context.getResult();

        // Notify other plugins of this spell cast
        CastEvent castEvent = new CastEvent(mage, this, result);
        Bukkit.getPluginManager().callEvent(castEvent);

        // Message targets
        if (result.isSuccess() && !mage.isQuiet()) {
            messageTargets("cast_player_message");
        }

        // Track cast counts
        if (result.isSuccess() && trackCasts) {
            castCount++;
            if (template != null) {
                template.castCount++;
            }

            // Reward SP
            Wand wand = context.getWand();
            WandUpgradePath path = wand == null ? null : wand.getPath();
            if (earns > 0 && wand != null && path != null && path.earnsSP() && controller.isSPEnabled() && !mage.isAtMaxSkillPoints()) {
                long now = System.currentTimeMillis();
                int scaledEarn = earns;
                if (lastEarn > 0 && earnCooldown > 0 && now < lastEarn + earnCooldown) {
                    scaledEarn = (int)Math.floor((double)earns * (now - lastEarn) / earnCooldown);
                    if (scaledEarn > 0) {
                        context.playEffects("earn_scaled_sp");
                    }
                } else {
                    context.playEffects("earn_sp");
                }
                if (scaledEarn > 0) {
                    mage.addSkillPoints(scaledEarn);
                    lastEarn = now;
                }
            }

            // Check for level up
            // This currently only works on wands.
            if (wand != null && !wand.isLocked() && controller.isSpellUpgradingEnabled() && wand.getSpellLevel(spellKey.getKey()) == spellKey.getLevel())
            {
                SpellTemplate upgrade = getUpgrade();
                long requiredCasts = getRequiredUpgradeCasts();
                if (upgrade != null && requiredCasts > 0 && getCastCount() >= requiredCasts)
                {
                    String upgradePath = getRequiredUpgradePath();
                    WandUpgradePath currentPath = wand.getPath();
                    if (upgradePath == null || upgradePath.isEmpty() || (currentPath != null && currentPath.hasPath(upgradePath)))
                    {
                        Spell newSpell = mage.getSpell(upgrade.getKey());
                        if (isActive()) {
                            deactivate(true, true);
                            if (newSpell != null && newSpell instanceof MageSpell) {
                                ((MageSpell)newSpell).activate();
                            }
                        }
                        wand.addSpell(upgrade.getKey());
                        Messages messages = controller.getMessages();
                        String levelDescription = upgrade.getLevelDescription();
                        if (levelDescription == null || levelDescription.isEmpty()) {
                            levelDescription = upgrade.getName();
                        }
                        playEffects("upgrade");
                        mage.sendMessage(messages.get("wand.spell_upgraded").replace("$name", upgrade.getName()).replace("$wand", getName()).replace("$level", levelDescription));
                        mage.sendMessage(upgrade.getUpgradeDescription().replace("$name", upgrade.getName()));

                        SpellUpgradeEvent upgradeEvent = new SpellUpgradeEvent(mage, this, newSpell);
                        Bukkit.getPluginManager().callEvent(upgradeEvent);
                    }
                }
            }
        }
    }

    public boolean getTargetsCaster() {
        return targetSelf;
    }

    public void setTargetsCaster(boolean target) {
        targetSelf = target;
    }

    @Override
    public double cancelOnDamage() {
        return cancelOnDamage;
    }

    @Override
    public ConfigurationSection getHandlerParameters(String handlerKey)
    {
        return null;
    }

    @Override
    public boolean hasHandlerParameters(String handlerKey)
    {
        return false;
    }
}
