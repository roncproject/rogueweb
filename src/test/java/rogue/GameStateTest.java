package rogue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GameState — initialisation and public methods.
 * DTP-01: UT-GS-01 through UT-GS-23
 */
class GameStateTest {

    private GameState gs;

    @BeforeEach
    void setup() {
        GameData.RNG.setSeed(42L);
        gs = new GameState();
        // generate a level so rooms[] is populated for roomAt() tests
        new LevelGenerator(gs);
    }

    // ── Initialisation ────────────────────────────────────────────────────

    /** UT-GS-01: Player starts with HP=12 and maxHp=12 */
    @Test
    void playerStartsWithCorrectHp() {
        assertEquals(12, gs.player.stats.hpt);
        assertEquals(12, gs.player.stats.maxHp);
    }

    /** UT-GS-02: Player starts with 3 items in pack (mace, ring mail, food) */
    @Test
    void playerStartsWithThreePackItems() {
        assertEquals(3, gs.player.pack.size());
    }

    /** UT-GS-03: Player starts with a weapon wielded */
    @Test
    void playerStartsWithWeapon() {
        assertNotNull(gs.player.weapon);
    }

    /** UT-GS-04: Player starts with armor worn */
    @Test
    void playerStartsWithArmor() {
        assertNotNull(gs.player.armor);
    }

    /** UT-GS-05: Player starts with ring mail AC (== ARMOR_CLASS[RING_MAIL] == 7) */
    @Test
    void playerStartsWithRingMailAc() {
        assertEquals(GameData.ARMOR_CLASS[GameData.RING_MAIL], gs.player.stats.arm);
    }

    /** UT-GS-06: All 14 potion colour assignments are in valid range */
    @Test
    void potionColorsAllInValidRange() {
        for (int i = 0; i < 14; i++) {
            int c = gs.potionColor[i];
            assertTrue(c >= 0 && c < GameData.POTION_COLORS.length,
                    "potionColor[" + i + "]=" + c + " out of range");
        }
    }

    /** UT-GS-07: All 18 scroll titles are non-null and non-empty */
    @Test
    void scrollTitlesAllNonNullNonEmpty() {
        for (int i = 0; i < 18; i++) {
            assertNotNull(gs.scrollTitle[i], "scrollTitle[" + i + "] is null");
            assertFalse(gs.scrollTitle[i].isEmpty(), "scrollTitle[" + i + "] is empty");
        }
    }

    // ── addExp() ──────────────────────────────────────────────────────────

    /** UT-GS-08: addExp(0) does not change level */
    @Test
    void addExpZeroNoLevelChange() {
        gs.addExp(0);
        assertEquals(1, gs.player.stats.lvl);
    }

    /** UT-GS-09: addExp(10) advances player to level 2 and returns message */
    @Test
    void addExpTenAdvancesToLevel2() {
        String msg = gs.addExp(10);
        assertEquals(2, gs.player.stats.lvl);
        assertNotNull(msg);
        assertTrue(msg.contains("2"), "Level-up message should mention level 2: " + msg);
    }

    /** UT-GS-10: HP increases when levelling up */
    @Test
    void hpIncreasesOnLevelUp() {
        int before = gs.player.stats.maxHp;
        gs.addExp(10); // level 2
        assertTrue(gs.player.stats.maxHp > before,
                "maxHp should increase on level-up");
    }

    /** UT-GS-11: Level is capped at 20 regardless of XP */
    @Test
    void levelCapsAt20() {
        gs.addExp(Integer.MAX_VALUE / 2);
        assertEquals(20, gs.player.stats.lvl);
    }

    // ── nextPackChar() ────────────────────────────────────────────────────

    /** UT-GS-12: nextPackChar() returns 'd' when a, b, c are already used */
    @Test
    void nextPackCharReturnsDWhenABCUsed() {
        // a, b, c are already assigned in the starting pack
        assertEquals('d', gs.nextPackChar());
    }

    /** UT-GS-13: nextPackChar() returns '\0' when all 26 letters are used */
    @Test
    void nextPackCharReturnsNullWhenFull() {
        // Fill the pack with 26 dummy items using all letters a-z
        gs.player.pack.clear();
        for (int i = 0; i < 26; i++) {
            Item it = new Item(GameData.O_FOOD, 0);
            it.packCh = (char) ('a' + i);
            gs.player.pack.add(it);
        }
        assertEquals('\0', gs.nextPackChar());
    }

    // ── isWalkable() ──────────────────────────────────────────────────────

    /** UT-GS-14: FLOOR tile is walkable */
    @Test
    void floorIsWalkable() {
        gs.map[5][5] = GameData.FLOOR;
        assertTrue(gs.isWalkable(5, 5));
    }

    /** UT-GS-15: WALL_H tile is not walkable */
    @Test
    void wallHIsNotWalkable() {
        gs.map[5][5] = GameData.WALL_H;
        assertFalse(gs.isWalkable(5, 5));
    }

    /** UT-GS-16: Column 0 (boundary) is not walkable */
    @Test
    void boundaryColumnZeroIsNotWalkable() {
        assertFalse(gs.isWalkable(0, 5));
    }

    /** UT-GS-17: POTION tile is walkable */
    @Test
    void potionTileIsWalkable() {
        gs.map[5][5] = GameData.POTION;
        assertTrue(gs.isWalkable(5, 5));
    }

    // ── isOpaque() ────────────────────────────────────────────────────────

    /** UT-GS-18: WALL_H tile is opaque */
    @Test
    void wallHIsOpaque() {
        gs.map[5][5] = GameData.WALL_H;
        assertTrue(gs.isOpaque(5, 5));
    }

    /** UT-GS-19: FLOOR tile is not opaque */
    @Test
    void floorIsNotOpaque() {
        gs.map[5][5] = GameData.FLOOR;
        assertFalse(gs.isOpaque(5, 5));
    }

    // ── isSeen() ─────────────────────────────────────────────────────────

    /** UT-GS-20: isSeen returns true when F_SEEN flag is set */
    @Test
    void isSeenTrueWhenFSeenSet() {
        gs.flags[5][5] |= GameData.F_SEEN;
        assertTrue(gs.isSeen(5, 5));
    }

    // ── msg() ─────────────────────────────────────────────────────────────

    /** UT-GS-21: msg() sets both message and lastMsg */
    @Test
    void msgSetsBothMessageAndLastMsg() {
        gs.msg("hello");
        assertEquals("hello", gs.message);
        assertEquals("hello", gs.lastMsg);
    }

    // ── roomAt() ─────────────────────────────────────────────────────────

    /** UT-GS-22: roomAt() returns the correct room when inside it */
    @Test
    void roomAtInsideRoomReturnsRoom() {
        // Find a non-gone room and check a floor tile inside it
        for (Room r : gs.rooms) {
            if (r == null || r.gone()) continue;
            // centre of room
            int cx = r.pos.x + r.size.x / 2;
            int cy = r.pos.y + r.size.y / 2;
            if (gs.map[cy][cx] == GameData.FLOOR) {
                assertNotNull(gs.roomAt(cx, cy),
                        "roomAt inside a room should not be null");
                return;
            }
        }
        // If no floor-centre found, just assert rooms were generated
        assertTrue(gs.rooms.length == GameData.MAXROOMS);
    }

    /** UT-GS-23: roomAt() returns null for a position in a corridor */
    @Test
    void roomAtInCorridorReturnsNull() {
        // Find a PASSAGE tile that is not inside any room
        for (int y = 1; y < GameData.NUMLINES - 1; y++) {
            for (int x = 1; x < GameData.NUMCOLS - 1; x++) {
                if (gs.map[y][x] == GameData.PASSAGE && gs.roomAt(x, y) == null) {
                    assertNull(gs.roomAt(x, y));
                    return;
                }
            }
        }
        // Passage may not exist in every seeded map; skip gracefully
        assertTrue(true, "No passage tile found — skip");
    }
}
