package rogue;

import java.util.*;

/**
 * Central repository of all static game constants, lookup tables, and shared data
 * used throughout the Rogue game.
 *
 * Contains:
 * <ul>
 *   <li>Map dimension constants and tile characters</li>
 *   <li>Object type, armor type, and weapon type constants</li>
 *   <li>Creature and item bit-flag constants</li>
 *   <li>Trap type constants and names</li>
 *   <li>Monster, armor, weapon, potion, scroll, food, stick, and ring data tables</li>
 *   <li>Shared random number generator and dice-rolling utilities</li>
 * </ul>
 *
 * All fields and methods in this class are static; it is never instantiated.
 */
public class GameData {

    // ── Map dimensions ─────────────────────────────────────────────────────

    /** Number of rows in the dungeon map (classic Rogue: 24). */
    public static final int NUMLINES = 24;

    /** Number of columns in the dungeon map (classic Rogue: 80). */
    public static final int NUMCOLS  = 80;

    /** Maximum number of rooms on a level, arranged in a 3x3 grid. */
    public static final int MAXROOMS = 9;

    /** Maximum number of item slots in the player's pack. */
    public static final int MAXPACK  = 23;

    /** Maximum number of traps that can exist on a single level. */
    public static final int MAXTRAPS = 10;

    /** The dungeon level at which the Amulet of Yendor is placed. */
    public static final int AMULETLEVEL = 26;

    // ── Tile characters ────────────────────────────────────────────────────

    /** Map tile: corridor passage segment. */
    public static final char PASSAGE = '#';

    /** Map tile: doorway connecting a room to a corridor. */
    public static final char DOOR    = '+';

    /** Map tile: open floor inside a room. */
    public static final char FLOOR   = '.';

    /** Map tile: the player character. */
    public static final char PLAYER  = '@';

    /** Map tile: a trap (hidden until stepped on or searched). */
    public static final char TRAP    = '^';

    /** Map tile: stairs descending to the next level. */
    public static final char STAIRS  = '%';

    /** Map tile / item glyph: a gold pile. */
    public static final char GOLD    = '*';

    /** Map tile / item glyph: a potion. */
    public static final char POTION  = '!';

    /** Map tile / item glyph: a scroll. */
    public static final char SCROLL  = '?';

    /** Map tile / item glyph: food. */
    public static final char FOOD    = ':';

    /** Map tile / item glyph: a weapon. */
    public static final char WEAPON  = ')';

    /** Map tile / item glyph: a piece of armor. */
    public static final char ARMOR   = ']';

    /** Map tile / item glyph: the Amulet of Yendor. */
    public static final char AMULET  = ',';

    /** Map tile / item glyph: a ring. */
    public static final char RING    = '=';

    /** Map tile / item glyph: a wand or staff. */
    public static final char STICK   = '/';

    /** Map tile: horizontal wall segment (top or bottom of a room). */
    public static final char WALL_H  = '-';

    /** Map tile: vertical wall segment (left or right side of a room). */
    public static final char WALL_V  = '|';

    // ── Object type constants ──────────────────────────────────────────────

    /** Item type: potion. */
    public static final int O_POTION = 0;

    /** Item type: scroll. */
    public static final int O_SCROLL = 1;

    /** Item type: food ration or slime-mold. */
    public static final int O_FOOD   = 2;

    /** Item type: weapon. */
    public static final int O_WEAPON = 3;

    /** Item type: armor. */
    public static final int O_ARMOR  = 4;

    /** Item type: ring. */
    public static final int O_RING   = 5;

    /** Item type: wand or staff (stick). */
    public static final int O_STICK  = 6;

    /** Item type: gold pile. */
    public static final int O_GOLD   = 7;

    /** Item type: the Amulet of Yendor. */
    public static final int O_AMULET = 8;

    // ── Armor sub-type constants ───────────────────────────────────────────

    /** Armor sub-type: leather armor (AC 8). */
    public static final int LEATHER          = 0;

    /** Armor sub-type: ring mail (AC 7). */
    public static final int RING_MAIL        = 1;

    /** Armor sub-type: studded leather armor (AC 7). */
    public static final int STUDDED_LEATHER  = 2;

    /** Armor sub-type: scale mail (AC 6). */
    public static final int SCALE_MAIL       = 3;

    /** Armor sub-type: chain mail (AC 5). */
    public static final int CHAIN_MAIL       = 4;

    /** Armor sub-type: splint mail (AC 4). */
    public static final int SPLINT_MAIL      = 5;

    /** Armor sub-type: banded mail (AC 4). */
    public static final int BANDED_MAIL      = 6;

    /** Armor sub-type: plate mail (AC 3). */
    public static final int PLATE_MAIL       = 7;

    // ── Weapon sub-type constants ──────────────────────────────────────────

    /** Weapon sub-type: mace. */
    public static final int MACE     = 0;

    /** Weapon sub-type: long sword. */
    public static final int SWORD    = 1;

    /** Weapon sub-type: short bow. */
    public static final int BOW      = 2;

    /** Weapon sub-type: arrow. */
    public static final int ARROW    = 3;

    /** Weapon sub-type: dagger. */
    public static final int DAGGER   = 4;

    /** Weapon sub-type: two-handed sword. */
    public static final int TWOSWORD = 5;

    /** Weapon sub-type: dart. */
    public static final int DART     = 6;

    /** Weapon sub-type: shuriken. */
    public static final int SHIRAKEN = 7;

    /** Weapon sub-type: spear. */
    public static final int SPEAR    = 8;

    // ── Creature flag constants ────────────────────────────────────────────

    /** Creature flag: can confuse the player on hit. */
    public static final int CANHUH   = 0x001;

    /** Creature flag: has infravision / can see invisible creatures. */
    public static final int CANSEE   = 0x002;

    /** Creature flag: currently blind. */
    public static final int ISBLIND  = 0x004;

    /** Creature flag: currently under a cancellation effect. */
    public static final int ISCANC   = 0x008;

    /** Creature flag: has been found / revealed to the player. */
    public static final int ISFOUND  = 0x020;

    /** Creature flag: is greedy (attracted to gold rooms). */
    public static final int ISGREED  = 0x040;

    /** Creature flag: currently hasted (acts twice per turn). */
    public static final int ISHASTE  = 0x080;

    /** Creature flag: this creature is a designated fight target. */
    public static final int ISTARGET = 0x100;

    /** Creature flag: currently held / paralyzed (loses its turn). */
    public static final int ISHELD   = 0x200;

    /** Creature flag: currently confused (moves randomly). */
    public static final int ISHUH    = 0x400;

    /** Creature flag: invisible (cannot be seen without special ability). */
    public static final int ISINVIS  = 0x800;

    /** Creature flag: aggressive — attacks the player on sight. */
    public static final int ISMEAN   = 0x1000;

    /** Creature flag: regenerates hit points each turn. */
    public static final int ISREGEN  = 0x2000;

    /** Creature flag: currently running/chasing the player. */
    public static final int ISRUN    = 0x4000;

    /** Creature flag: can fly (ignores some terrain). */
    public static final int ISFLY    = 0x8000;

    /** Creature flag: currently slowed (only acts every other turn). */
    public static final int ISSLOW   = 0x10000;

    /** Creature flag: can see monsters through walls. */
    public static final int SEEMONST = 0x20000;

    /** Creature flag: currently hallucinating (sees random monsters). */
    public static final int ISHALU   = 0x40000;

    /** Creature flag: currently levitating. */
    public static final int ISLEVIT  = 0x80000;

    // ── Item flag constants ────────────────────────────────────────────────

    /** Item flag: item is cursed (cannot be removed once equipped). */
    public static final int ISCURSED = 0x01;

    /** Item flag: item has been identified and its true properties are known. */
    public static final int ISKNOW   = 0x02;

    /** Item flag: item is a missile weapon (can be thrown). */
    public static final int ISMISL   = 0x04;

    /** Item flag: item comes in stacks (e.g., arrows). */
    public static final int ISMANY   = 0x08;

    /** Item flag: armor is protected against rust traps. */
    public static final int ISPROT   = 0x20;

    // ── Place (cell) flag constants ────────────────────────────────────────

    /** Cell flag: this tile is part of a passage corridor. */
    public static final int F_PASS    = 0x80;

    /** Cell flag: this tile has been seen by the player and is drawn on screen. */
    public static final int F_SEEN    = 0x40;

    /** Cell flag: an item was dropped here (bookkeeping). */
    public static final int F_DROPPED = 0x20;

    /** Cell flag: the real tile type is stored (used for disguised traps). */
    public static final int F_REAL    = 0x10;

    // ── Room flag constants ────────────────────────────────────────────────

    /** Room flag: the room is dark (player can only see adjacent tiles). */
    public static final int ISDARK = 0x01;

    /** Room flag: the room is "gone" — it is only a routing point, not drawn. */
    public static final int ISGONE = 0x02;

    /** Room flag: the room is a maze-type room. */
    public static final int ISMAZE = 0x04;

    // ── Trap type constants ────────────────────────────────────────────────

    /** Trap type: trapdoor — player falls to the next level immediately. */
    public static final int T_DOOR  = 0;

    /** Trap type: arrow trap — fires an arrow at the player for damage. */
    public static final int T_ARROW = 1;

    /** Trap type: sleeping gas trap — paralyzes the player for several turns. */
    public static final int T_SLEEP = 2;

    /** Trap type: bear trap — immobilizes the player. */
    public static final int T_BEAR  = 3;

    /** Trap type: teleport trap — moves the player to a random location. */
    public static final int T_TELEP = 4;

    /** Trap type: poison dart trap — damages and reduces the player's strength. */
    public static final int T_DART  = 5;

    /** Trap type: rust trap — weakens the player's equipped armor. */
    public static final int T_RUST  = 6;

    /** Trap type: mysterious trap — produces a vague message with no effect. */
    public static final int T_MYST  = 7;

    /** Total number of distinct trap types. */
    public static final int NTRAPS  = 8;

    /** Human-readable names for each trap type, indexed by T_* constants. */
    public static final String[] TR_NAME = {
        "a trapdoor", "an arrow trap", "a sleeping gas trap", "a beartrap",
        "a teleport trap", "a poison dart trap", "a rust trap", "a mysterious trap"
    };

    // ── Monster data ───────────────────────────────────────────────────────

    /**
     * Holds the static (template) data for a single monster type as defined in the
     * original Rogue monster table. One entry exists for each letter 'A' through 'Z'.
     */
    public static class MonsterInfo {
        /** The full display name of this monster (e.g., "dragon"). */
        public String name;

        /** Percentage chance (0-100) that this monster drops an item on death. */
        public int carry;

        /** Initial bit-flags for newly created instances of this monster type. */
        public int flags;

        /** Strength score (unused in this implementation, kept for fidelity). */
        public int str;

        /** Base experience points awarded for killing this monster. */
        public int exp;

        /** Dungeon level at which this monster type typically appears. */
        public int lvl;

        /** Base armor class of this monster (lower = harder to hit). */
        public int arm;

        /** Number of dice per attack (legacy fields, superseded by {@code dmg}). */
        public int dmg_n;

        /** Number of sides per attack die (legacy fields). */
        public int dmg_s;

        /**
         * Damage string in "NxM/NxM/..." format describing all of this monster's attacks.
         * Each segment is rolled independently and totaled.
         */
        public String dmg;

        /**
         * Constructs a new MonsterInfo entry.
         *
         * @param n  monster name
         * @param c  carry (loot drop) percentage
         * @param f  initial flags
         * @param e  base experience value
         * @param l  typical dungeon level
         * @param a  base armor class
         * @param d  damage string
         */
        MonsterInfo(String n, int c, int f, int e, int l, int a, String d) {
            name = n; carry = c; flags = f; exp = e; lvl = l; arm = a; dmg = d;
        }
    }

    /**
     * The monster table: one entry per letter 'A' through 'Z'.
     * Access a monster by its type character with {@code MONSTERS[type - 'A']}.
     */
    public static final MonsterInfo[] MONSTERS = {
        new MonsterInfo("aquator",        0, ISMEAN,                    20,  5,  2, "0x0/0x0"),
        new MonsterInfo("bat",            0, ISFLY,                      1,  1,  3, "1x2"),
        new MonsterInfo("centaur",       15, 0,                         17,  4,  4, "1x2/1x5/1x5"),
        new MonsterInfo("dragon",       100, ISMEAN,                  5000, 10, -1, "1x8/1x8/3x10"),
        new MonsterInfo("emu",            0, ISMEAN,                     2,  1,  7, "1x2"),
        new MonsterInfo("venus flytrap",  0, ISMEAN,                    80,  8,  3, "1x6"),
        new MonsterInfo("griffin",       20, ISMEAN | ISFLY | ISREGEN, 2000, 13,  2, "4x3/3x5"),
        new MonsterInfo("hobgoblin",      0, ISMEAN,                     3,  1,  5, "1x8"),
        new MonsterInfo("ice monster",    0, 0,                          5,  1,  9, "0x0"),
        new MonsterInfo("jabberwock",    70, 0,                       3000, 15,  6, "2x12/2x4"),
        new MonsterInfo("kestrel",        0, ISMEAN | ISFLY,             1,  1,  7, "1x4"),
        new MonsterInfo("leprechaun",     0, 0,                         10,  3,  8, "1x1"),
        new MonsterInfo("medusa",        40, ISMEAN,                   200,  8,  2, "3x4/3x4/2x5"),
        new MonsterInfo("nymph",        100, 0,                         37,  3,  9, "0x0"),
        new MonsterInfo("orc",           15, ISGREED,                    5,  1,  6, "1x8"),
        new MonsterInfo("phantom",        0, ISINVIS,                  120,  8,  3, "4x4"),
        new MonsterInfo("quagga",         0, ISMEAN,                    15,  3,  3, "1x5/1x5"),
        new MonsterInfo("rattlesnake",    0, ISMEAN,                     9,  2,  3, "1x6"),
        new MonsterInfo("snake",          0, ISMEAN,                     2,  1,  5, "1x3"),
        new MonsterInfo("troll",         50, ISREGEN | ISMEAN,         120,  6,  4, "1x8/1x8/2x6"),
        new MonsterInfo("black unicorn",  0, ISMEAN,                   190,  7, -2, "1x9/1x9/2x9"),
        new MonsterInfo("vampire",       20, ISREGEN | ISMEAN,         350,  8,  1, "1x10"),
        new MonsterInfo("wraith",         0, 0,                         55,  5,  4, "1x6"),
        new MonsterInfo("xeroc",         30, 0,                        100,  7,  7, "4x4"),
        new MonsterInfo("yeti",          30, 0,                         50,  4,  6, "1x6/1x6"),
        new MonsterInfo("zombie",         0, ISMEAN,                     6,  2,  8, "1x8"),
    };

    // ── Armor tables ───────────────────────────────────────────────────────

    /** Display names for each armor type, indexed by the armor sub-type constant. */
    public static final String[] ARMOR_NAMES = {
        "leather armor", "ring mail", "studded leather armor", "scale mail",
        "chain mail", "splint mail", "banded mail", "plate mail"
    };

    /** Base armor class values for each armor type (lower is better). */
    public static final int[] ARMOR_CLASS = { 8, 7, 7, 6, 5, 4, 4, 3 };

    /** Gold worth of each armor type when sold. */
    public static final int[] ARMOR_WORTH = { 20, 25, 20, 30, 75, 80, 90, 150 };

    // ── Weapon tables ──────────────────────────────────────────────────────

    /** Display names for each weapon type, indexed by the weapon sub-type constant. */
    public static final String[] WEAPON_NAMES = {
        "mace", "long sword", "short bow", "arrow", "dagger",
        "two handed sword", "dart", "shuriken", "spear"
    };

    /**
     * Melee damage dice for each weapon: {@code [num, sides]}.
     * Roll {@code num} dice of {@code sides} sides each.
     */
    public static final int[][] WEAPON_DMG = {
        {2, 4}, {3, 4}, {1, 1}, {1, 6}, {1, 6}, {4, 4}, {1, 3}, {2, 4}, {1, 8}
    };

    /**
     * Thrown/hurled damage dice for each weapon: {@code [num, sides]}.
     * Used when the weapon is thrown at a target.
     */
    public static final int[][] WEAPON_HURL = {
        {1, 3}, {1, 2}, {1, 1}, {2, 6}, {1, 4}, {1, 2}, {3, 4}, {2, 4}, {2, 6}
    };

    /** Gold worth of each weapon type. */
    public static final int[] WEAPON_WORTH = { 8, 15, 15, 1, 3, 75, 2, 5, 5 };

    // ── Potion tables ──────────────────────────────────────────────────────

    /**
     * True (identified) names for each of the 14 potions.
     * Displayed once the player has quaffed and identified a potion of this type.
     */
    public static final String[] POTION_NAMES_REAL = {
        "confusion", "hallucination", "poison", "gain strength", "see invisible",
        "healing", "monster detection", "magic detection", "raise level",
        "extra healing", "haste self", "restore strength", "blindness", "levitation"
    };

    /**
     * Pool of random color names used to disguise potions before identification.
     * Each potion type is assigned one of these colors at the start of each game.
     */
    public static final String[] POTION_COLORS = {
        "amber", "aquamarine", "black", "blue", "brown", "clear", "crimson", "cyan",
        "ecru", "gold", "green", "grey", "magenta", "orange", "pink", "plaid",
        "purple", "red", "silver", "tan", "tangerine", "topaz", "turquoise",
        "vermilion", "violet", "white", "yellow"
    };

    // ── Scroll tables ──────────────────────────────────────────────────────

    /**
     * True (identified) names for each of the 18 scrolls.
     * Displayed once the player has read and identified a scroll of this type.
     */
    public static final String[] SCROLL_NAMES_REAL = {
        "monster confusion", "magic mapping", "hold monster", "sleep",
        "enchant armor", "identify potion", "identify scroll", "identify weapon",
        "identify armor", "identify ring/wand/staff", "scare monster", "food detection",
        "teleportation", "enchant weapon", "create monster", "remove curse",
        "aggravate monsters", "protect armor"
    };

    // ── Food tables ────────────────────────────────────────────────────────

    /** Display names for each food sub-type. Index 0 = food ration, 1 = slime-mold. */
    public static final String[] FOOD_NAMES = { "food ration", "slime-mold" };

    // ── Stick (wand/staff) tables ──────────────────────────────────────────

    /**
     * True (identified) names for each of the 14 wands/staves.
     * Even-indexed entries are wands; odd-indexed entries are staves.
     */
    public static final String[] STICK_NAMES_REAL = {
        "light", "invisibility", "lightning", "fire", "cold", "polymorph",
        "magic missile", "haste monster", "slow monster", "drain life", "nothing",
        "teleport away", "teleport to", "cancellation"
    };

    /**
     * Pool of material names used to disguise wands and staves before identification.
     * Each stick type is assigned one of these materials at the start of each game.
     */
    public static final String[] STICK_MATERIALS = {
        "oak", "ash", "willow", "pine", "cherry", "teak", "hawthorn", "bamboo",
        "iron", "zinc", "copper", "gold", "silver", "platinum", "steel", "mithril"
    };

    // ── Ring tables ────────────────────────────────────────────────────────

    /**
     * True (identified) names for each of the 14 ring types.
     * Displayed once the player has worn and identified a ring of this type.
     */
    public static final String[] RING_NAMES_REAL = {
        "protection", "add strength", "sustain strength", "searching", "see invisible",
        "adornment", "aggravate monster", "dexterity", "increase damage", "regeneration",
        "slow digestion", "teleportation", "stealth", "maintain armor"
    };

    /**
     * Pool of gemstone names used to disguise rings before identification.
     * Each ring type is assigned one of these stones at the start of each game.
     */
    public static final String[] RING_STONES = {
        "agate", "alexandrite", "amethyst", "carnelian", "diamond", "emerald",
        "garnet", "jade", "pearl", "ruby", "sapphire", "topaz", "turquoise"
    };

    // ── Shared random number generator ────────────────────────────────────

    /** Shared Random instance used by all game components for reproducibility. */
    public static final Random RNG = new Random();

    /**
     * Returns a random integer in the range [0, range-1].
     * Returns 0 if range is 0 or negative.
     *
     * @param range the exclusive upper bound
     * @return a random non-negative integer less than range
     */
    public static int rnd(int range) {
        return range <= 0 ? 0 : RNG.nextInt(range);
    }

    /**
     * Simulates rolling {@code num} dice each with {@code sides} sides,
     * returning the total.  Each die produces a value in [1, sides].
     *
     * @param num   number of dice to roll
     * @param sides number of sides on each die
     * @return the sum of all dice rolls
     */
    public static int roll(int num, int sides) {
        int total = 0;
        for (int i = 0; i < num; i++) total += rnd(sides) + 1;
        return total;
    }

    /**
     * Parses a damage string in "NxM/NxM/..." format, rolls the dice for each
     * segment, and returns the summed total damage.
     *
     * @param dmg the damage string (e.g., "1x8/2x4"), or null/empty for 0 damage
     * @return the total rolled damage across all attack segments
     */
    public static int parseDamage(String dmg) {
        if (dmg == null || dmg.isEmpty()) return 0;
        String[] parts = dmg.split("/");
        int total = 0;
        for (String p : parts) {
            String[] nd = p.split("x");
            if (nd.length == 2) {
                try {
                    int n = Integer.parseInt(nd[0].trim());
                    int d = Integer.parseInt(nd[1].trim());
                    total += roll(n, d);
                } catch (Exception ignore) {}
            }
        }
        return total;
    }
}
