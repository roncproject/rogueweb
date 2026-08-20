package rogue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GameData — static constants, utility functions.
 * DTP-01: UT-GD-01 through UT-GD-15
 */
class GameDataTest {

    @BeforeEach
    void seed() {
        GameData.RNG.setSeed(42L);
    }

    // ── rnd() ─────────────────────────────────────────────────────────────

    /** UT-GD-01: rnd(0) returns 0 */
    @Test
    void rndZeroRangeReturnsZero() {
        assertEquals(0, GameData.rnd(0));
    }

    /** UT-GD-02: rnd(1) always returns 0 (only possible value) */
    @Test
    void rndOneAlwaysReturnsZero() {
        for (int i = 0; i < 50; i++) {
            assertEquals(0, GameData.rnd(1), "rnd(1) must always be 0");
        }
    }

    /** UT-GD-03: rnd(10) returns value in [0, 9] */
    @Test
    void rndTenInRange() {
        for (int i = 0; i < 200; i++) {
            int v = GameData.rnd(10);
            assertTrue(v >= 0 && v < 10, "Expected 0<=v<10, got " + v);
        }
    }

    // ── roll() ────────────────────────────────────────────────────────────

    /** UT-GD-04: roll(1,6) returns value in [1,6] */
    @Test
    void rollOneDSixInRange() {
        for (int i = 0; i < 200; i++) {
            int v = GameData.roll(1, 6);
            assertTrue(v >= 1 && v <= 6, "Expected 1<=v<=6, got " + v);
        }
    }

    /** UT-GD-05: roll(3,6) returns value in [3,18] */
    @Test
    void rollThreeDSixInRange() {
        for (int i = 0; i < 200; i++) {
            int v = GameData.roll(3, 6);
            assertTrue(v >= 3 && v <= 18, "Expected 3<=v<=18, got " + v);
        }
    }

    // ── parseDamage() ─────────────────────────────────────────────────────

    /** UT-GD-06: parseDamage(null) returns 0 */
    @Test
    void parseDamageNullReturnsZero() {
        assertEquals(0, GameData.parseDamage(null));
    }

    /** UT-GD-07: parseDamage("") returns 0 */
    @Test
    void parseDamageEmptyReturnsZero() {
        assertEquals(0, GameData.parseDamage(""));
    }

    /** UT-GD-08: parseDamage("1x6") returns value in [1,6] */
    @Test
    void parseDamageOneDSixInRange() {
        for (int i = 0; i < 100; i++) {
            int v = GameData.parseDamage("1x6");
            assertTrue(v >= 1 && v <= 6, "Expected 1<=v<=6, got " + v);
        }
    }

    /** UT-GD-09: parseDamage("2x4/1x6") sums two rolls → [3,14] */
    @Test
    void parseDamageTwoSegments() {
        for (int i = 0; i < 100; i++) {
            int v = GameData.parseDamage("2x4/1x6");
            assertTrue(v >= 3 && v <= 14, "Expected 3<=v<=14, got " + v);
        }
    }

    /** UT-GD-10: parseDamage("0x0") returns 0 */
    @Test
    void parseDamageZeroXZeroReturnsZero() {
        assertEquals(0, GameData.parseDamage("0x0"));
    }

    // ── MONSTERS table ────────────────────────────────────────────────────

    /** UT-GD-11: All 26 MONSTERS entries are non-null */
    @Test
    void monstersTableHas26NonNullEntries() {
        assertEquals(26, GameData.MONSTERS.length);
        for (int i = 0; i < 26; i++) {
            assertNotNull(GameData.MONSTERS[i], "MONSTERS[" + i + "] must not be null");
        }
    }

    /** UT-GD-12: Dragon (index 3, 'D') has exp == 5000 */
    @Test
    void dragonExpIs5000() {
        assertEquals(5000, GameData.MONSTERS[3].exp);
    }

    // ── ARMOR_CLASS ───────────────────────────────────────────────────────

    /** UT-GD-13: Plate mail (index 7) has AC == 3 */
    @Test
    void platMailArmorClassIsThree() {
        assertEquals(3, GameData.ARMOR_CLASS[7]);
    }

    // ── Name tables ───────────────────────────────────────────────────────

    /** UT-GD-14: POTION_NAMES_REAL has length 14 */
    @Test
    void potionNamesRealLength14() {
        assertEquals(14, GameData.POTION_NAMES_REAL.length);
    }

    /** UT-GD-15: SCROLL_NAMES_REAL has length 18 */
    @Test
    void scrollNamesRealLength18() {
        assertEquals(18, GameData.SCROLL_NAMES_REAL.length);
    }
}
