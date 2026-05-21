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
class Creature extends Thing {
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
}
