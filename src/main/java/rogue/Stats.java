package rogue;

/**
 * Holds the core combat and character statistics for a creature (monster or player).
 * These values directly affect combat calculations, experience progression,
 * and survival mechanics.
 */
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
