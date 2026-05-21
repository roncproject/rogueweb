package rogue;

/**
 * Represents a game item that can appear on the dungeon floor or in a creature's inventory.
 * Items include potions, scrolls, food, weapons, armor, rings, sticks (wands/staves),
 * gold piles, and the Amulet of Yendor.
 *
 * The {@code type} field identifies the broad category (e.g., {@link GameData#O_POTION}),
 * while {@code which} identifies the specific sub-type within that category.
 */
class Item extends Thing {
    /**
     * The broad item category. One of the O_* constants in {@link GameData}
     * (e.g., O_POTION, O_SCROLL, O_WEAPON, etc.).
     */
    int type;

    /**
     * The specific sub-type index within the category.
     * For example, for a potion this indexes into {@link GameData#POTION_NAMES_REAL}.
     */
    int which;

    /** The position of this item on the dungeon map (null when in a pack). */
    //Coord pos;

    /**
     * The inventory letter assigned to this item when it is in the player's pack
     * (e.g., 'a', 'b', 'c', ...). Zero means unassigned.
     */
    char packCh;

    /** Stack count (primarily used for arrows and similar stackable weapons). */
    int count = 1;

    /** Hit-bonus enchantment on weapons (can be negative for cursed items). */
    int hplus;

    /** Damage-bonus enchantment on weapons (can be negative for cursed items). */
    int dplus;

    /**
     * Armor class modifier: positive values improve the armor class for armor items;
     * also used to store stick charges.
     */
    int arm;

    /**
     * Bit-flag field for item properties such as {@link GameData#ISCURSED},
     * {@link GameData#ISKNOW}, {@link GameData#ISPROT}, etc.
     */
    int flags;

    /** The gold value for gold-pile items. */
    int goldVal;

    /** An optional player-assigned name/label for this item. */
    String label;

    /** The text inscribed on a scroll (its random title label). */
    String text;

    /**
     * Constructs a new Item of the given type and sub-type.
     *
     * @param type  the broad category constant (O_POTION, O_WEAPON, etc.)
     * @param which the sub-type index within that category
     */
    Item(int type, int which) {
        this.type = type;
        this.which = which;
    }

    /**
     * Returns the single character used to display this item on the dungeon map.
     * The character is determined by the item's broad type.
     *
     * @return the map display character for this item
     */
    char displayChar() {
        switch (type) {
            case GameData.O_POTION: return GameData.POTION;
            case GameData.O_SCROLL: return GameData.SCROLL;
            case GameData.O_FOOD:   return GameData.FOOD;
            case GameData.O_WEAPON: return GameData.WEAPON;
            case GameData.O_ARMOR:  return GameData.ARMOR;
            case GameData.O_RING:   return GameData.RING;
            case GameData.O_STICK:  return GameData.STICK;
            case GameData.O_GOLD:   return GameData.GOLD;
            case GameData.O_AMULET: return GameData.AMULET;
        }
        return '?';
    }

    /**
     * Returns a human-readable name for this item, taking into account whether
     * the player has identified it, any player-supplied guesses, and random
     * color/stone/material assignments stored in the provided GameState.
     *
     * @param gs the current game state, used to look up identification knowledge
     * @return a descriptive name string such as "a blue potion" or "a potion of healing"
     */
    String getName(GameState gs) {
        switch (type) {
            case GameData.O_FOOD:
                return (which == 0 ? "some food" : "a slime-mold");

            case GameData.O_GOLD:
                return goldVal + " gold pieces";

            case GameData.O_AMULET:
                return "the Amulet of Yendor";

            case GameData.O_POTION: {
                if (gs.potionKnown[which])
                    return "a potion of " + GameData.POTION_NAMES_REAL[which];
                if (gs.potionGuess[which] != null)
                    return "a " + gs.potionGuess[which] + " potion ("
                            + GameData.POTION_COLORS[gs.potionColor[which]] + ")";
                return "a " + GameData.POTION_COLORS[gs.potionColor[which]] + " potion";
            }

            case GameData.O_SCROLL: {
                if (gs.scrollKnown[which])
                    return "a scroll of " + GameData.SCROLL_NAMES_REAL[which];
                if (gs.scrollGuess[which] != null)
                    return "a scroll labeled '" + gs.scrollTitle[which]
                            + "' (" + gs.scrollGuess[which] + ")";
                return "a scroll labeled '" + gs.scrollTitle[which] + "'";
            }

            case GameData.O_WEAPON: {
                String base = (count > 1 ? count + " " : "") + GameData.WEAPON_NAMES[which]
                            + (count > 1 ? "s" : "");
                String bonus = (hplus >= 0 ? "+" : "") + hplus + ","
                             + (dplus >= 0 ? "+" : "") + dplus;
                boolean known = (flags & GameData.ISKNOW) != 0;
                return known ? "a " + base + " (" + bonus + ")" : "a " + base;
            }

            case GameData.O_ARMOR: {
                String base = GameData.ARMOR_NAMES[which];
                boolean known = (flags & GameData.ISKNOW) != 0;
                int ac = GameData.ARMOR_CLASS[which] - arm;
                return known ? "a " + base + " [" + ac + "]" : "a " + base;
            }

            case GameData.O_RING: {
                if (gs.ringKnown[which])
                    return "a ring of " + GameData.RING_NAMES_REAL[which];
                if (gs.ringGuess[which] != null)
                    return "a " + gs.ringGuess[which] + " ring";
                return "a " + GameData.RING_STONES[gs.ringStone[which]] + " ring";
            }

            case GameData.O_STICK: {
                String mat = GameData.STICK_MATERIALS[gs.stickMaterial[which]];
                String typ = (which % 2 == 0 ? "wand" : "staff");
                if (gs.stickKnown[which])
                    return "a " + typ + " of " + GameData.STICK_NAMES_REAL[which];
                return "a " + mat + " " + typ;
            }
        }
        return "unknown item";
    }
}
