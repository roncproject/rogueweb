package rogue;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents any mobile entity in the game — either the player character or a monster.
 * Creatures have a position on the map, a type identifier, combat statistics, an inventory
 * (pack), and a set of bit-flags that govern special conditions (haste, confusion, etc.).
 *
 * For the player, additional fields track equipped items, gold, hunger, and other
 * player-specific state that monsters don't need.
 */
class Creature extends Thing implements Movable{

    public Creature() {
        // Default constructor
    }   


       /**
     * Constructs a new Creature of the given type at the given map position.
     * The disguise is initialized to match the type character.
     *
     * @param t the type character ('A'-'Z' for monsters, '@' for player)
     * @param p the initial map position
     */
    Creature(char t, Coord p) {
        
        System.out.println("Creating creature of type '" + t + "' at " + p);

        type = t;
        disguise = t;
        pos = p;
    }


    /**
     * Creates the player creature with starting stats and equips them with
     * a +1/+1 mace (identified), ring mail armor, and one food ration.
     */
    //private void initPlayer() {
     public Creature(boolean isplayer, char t, Coord p) {
        //player = new Creature('@', new Coord(0, 0));
        this.isPlayer = isplayer;
        this.stats.str    = 16;
        this.stats.maxStr = 16;
        this.stats.lvl    = 1;
        this.stats.exp    = 0;
        this.stats.arm    = 10; // no armor = AC 10
        this.stats.hpt    = 12;
        this.stats.maxHp  = 12;
        this.stats.dmg    = "1x4";

        // Starting armor: ring mail (unenchanted)
        Item startArmor = new Item(GameData.O_ARMOR, GameData.RING_MAIL);
        startArmor.arm    = 0;
        startArmor.packCh = 'b';

        // Starting weapon: +1/+1 mace (already identified)
        Item startWeapon = new Item(GameData.O_WEAPON, GameData.MACE);
        startWeapon.hplus  = 1;
        startWeapon.dplus  = 1;
        startWeapon.packCh = 'a';
        startWeapon.flags |= GameData.ISKNOW;

        // Starting food: one food ration
        Item startFood = new Item(GameData.O_FOOD, 0);
        startFood.count   = 1;
        startFood.packCh  = 'c';

        this.pack.add(startWeapon);
        this.pack.add(startArmor);
        this.pack.add(startFood);
        this.weapon       = startWeapon;
        this.armor        = startArmor;
        this.stats.arm    = GameData.ARMOR_CLASS[GameData.RING_MAIL];
        this.foodLeft     = 2000;

        System.out.println("Initialized player with starting equipment and stats.");
    }





    public void move (Coord newPos) {
        // Implementation of movement logic goes here

        System.out.println("Moving creature of type '" + type + "' from " + pos + " to " + newPos); 

        pos = newPos;
    } 

    public void move(int dx, int dy) {
        // Implementation of movement logic goes here
        System.out.println("Moving creature of type '" + type + "' from " + pos + " by delta (" + dx + "," + dy + ") to " + pos.add(dx, dy));
        
        move(pos.add(dx, dy));  
    }   


    /** Current map position of this creature. */
    // Coord pos;

    /**
     * The creature's type character.
     * For monsters this is an uppercase letter ('A'..'Z') matching an entry in
     * {@link GameData#MONSTERS}. For the player this is always '@'.
     */
    char type;

    /**
     * The character this creature appears as on the map.
     * Normally equals {@code type}, but can differ (e.g., for a xeroc disguised as an item).
     */
    char disguise;

    /**
     * The map character that was underneath this creature before it moved here.
     * Restored when the creature leaves a tile.
     */
    char oldCh;

    /**
     * Bit-flag field for active status conditions and creature properties.
     * See the flag constants in {@link GameData} (e.g., ISHELD, ISRUN, ISMEAN, etc.).
     */
    int flags;

    /** Combat and character statistics for this creature. */
    Stats stats = new Stats();

    /**
     * Inventory: items carried by this creature.
     * For monsters this may contain loot they drop on death.
     * For the player this is the full pack contents.
     */
    List<Item> pack = new ArrayList<>();

    /** The room this creature is currently standing in, or null if in a corridor. */
    Room room;

    /**
     * Turn-alternation flag used to implement the ISSLOW condition.
     * Toggled each game tick so slowed creatures only act every other turn.
     */
    boolean turn = true;

    /** Destination coordinate used when this creature is actively running toward a target. */
    Coord dest;

    /**
     * True if this creature represents the player character.
     * Controls which player-specific fields are meaningful.
     */
    boolean isPlayer = false;

    // ── Player-only fields ─────────────────────────────────────────────────

    /** The weapon currently wielded by the player (null = bare hands). */
    Item weapon;

    /** The armor currently worn by the player (null = unarmored). */
    Item armor;

    /**
     * Rings currently worn by the player.
     * Index 0 = left hand ring, index 1 = right hand ring.
     */
    Item[] rings = new Item[2];

    /** Amount of gold the player is carrying. */
    int purse = 0;

    /**
     * Remaining food points before the player starts suffering hunger penalties.
     * Starts at 2000, decrements each turn.
     */
    int foodLeft = 2000;

    /**
     * Hunger state of the player.
     * 0 = normal, 1 = hungry, 2 = weak, 3 = fainting.
     */
    int hungryState = 0;

    /** True if the player has picked up the Amulet of Yendor. */
    boolean hasAmulet = false;

    /** The last movement direction key pressed by the player (used for run-mode). */
    char lastDir = 0;

 

    /**
     * Returns true if all bits in the given flag mask are set in this creature's flags field.
     *
     * @param f the flag mask to test (one or more OR'd flag constants from {@link GameData})
     * @return true if the flag(s) are currently active
     */
    boolean hasFlag(int f) {
        return (flags & f) != 0;
    }

    /**
     * Sets (activates) the given flag bits in this creature's flags field.
     *
     * @param f the flag mask to set (one or more OR'd flag constants from {@link GameData})
     */
    void setFlag(int f) {
        flags |= f;
    }

    /**
     * Clears (deactivates) the given flag bits in this creature's flags field.
     *
     * @param f the flag mask to clear (one or more OR'd flag constants from {@link GameData})
     */
    void clearFlag(int f) {
        flags &= ~f;
    }


    class Stats {
    /**
     * Current strength score.
     * Affects hit probability and damage dealt in melee combat.
     */
        int str;

    /**
     * Maximum (restored) strength score.
     * Strength can be temporarily drained but never regenerates above this value.
     */
        int maxStr;

    /** Total experience points accumulated (used for the player's level-up tracking). */
        int exp;

    /**
     * Character level (1-based).
     * Higher levels grant better hit bonuses and more hit points.
     */
        int lvl;

    /**
     * Armor class value.
     * Lower values mean better protection (classic Rogue convention).
     * Unarmored player defaults to AC 10.
     */
        int arm;

    /** Current hit points. Reaching 0 or below is lethal. */
        int hpt;

    /** Maximum hit points that can be currently held. */
        int maxHp;

    /**
     * Damage dice string in "NxM/NxM/..." format.
     * Each "NxM" segment means roll N dice of M sides.
     * Multiple segments separated by "/" represent multiple attacks.
     * Defaults to "1x4" (one four-sided die per hit).
     */
        String dmg = "1x4";
    }



}
