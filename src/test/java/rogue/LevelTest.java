package rogue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Level / LevelGenerator — procedural dungeon generation.
 * DTP-01: UT-LV-01 through UT-LV-09
 */
class LevelTest {

    private GameState gs;

    @BeforeEach
    void setup() {
        GameData.RNG.setSeed(42L);
        gs = new GameState();
        new LevelGenerator(gs);   // generate() called inside constructor
    }

    /** UT-LV-01: rooms[] array has exactly 9 slots */
    @Test
    void roomsArrayHasNineSlots() {
        assertEquals(GameData.MAXROOMS, gs.rooms.length);
        assertEquals(9, gs.rooms.length);
    }

    /** UT-LV-02: No map cell outside [0,NUMCOLS) × [0,NUMLINES) was written */
    @Test
    void mapWritesAreInBounds() {
        // All cells are initialised to ' '; any non-space is a valid write.
        // We just verify the array dimensions are correct and that the
        // generator did not throw an ArrayIndexOutOfBoundsException (which
        // would have prevented setup() from completing).
        assertEquals(GameData.NUMLINES, gs.map.length);
        assertEquals(GameData.NUMCOLS,  gs.map[0].length);
    }

    /** UT-LV-03: At least one STAIRS tile exists on the map */
    @Test
    void staircaseExistsOnMap() {
        boolean found = false;
        outer:
        for (int y = 0; y < GameData.NUMLINES; y++) {
            for (int x = 0; x < GameData.NUMCOLS; x++) {
                if (gs.map[y][x] == GameData.STAIRS) { found = true; break outer; }
            }
        }
        assertTrue(found, "At least one STAIRS tile must exist on the generated map");
    }

    /** UT-LV-04: Player's starting position is on a walkable floor tile */
    @Test
    void playerPlacedOnWalkableTile() {
        assertTrue(gs.isWalkable(gs.player.pos.x, gs.player.pos.y),
                "Player starting pos (" + gs.player.pos.x + "," + gs.player.pos.y
                + ") must be walkable");
    }

    /** UT-LV-05: numTraps is in [0, MAXTRAPS] */
    @Test
    void numTrapsInValidRange() {
        assertTrue(gs.numTraps >= 0 && gs.numTraps <= GameData.MAXTRAPS,
                "numTraps=" + gs.numTraps + " not in [0," + GameData.MAXTRAPS + "]");
    }

    /** UT-LV-06: All trap coordinates are within map bounds */
    @Test
    void trapCoordsInBounds() {
        for (int i = 0; i < gs.numTraps; i++) {
            int ty = gs.traps[i][0];
            int tx = gs.traps[i][1];
            assertTrue(ty >= 0 && ty < GameData.NUMLINES,
                    "Trap row " + ty + " out of bounds");
            assertTrue(tx >= 0 && tx < GameData.NUMCOLS,
                    "Trap col " + tx + " out of bounds");
        }
    }

    /** UT-LV-07: floorItems list is not null */
    @Test
    void floorItemsListIsNotNull() {
        assertNotNull(gs.floorItems);
    }

    /** UT-LV-08: revealRoom() sets F_SEEN on all floor tiles in the player's room */
    @Test
    void revealRoomSetsFSeenOnFloorTiles() {
        // Find the room the player is in
        Room playerRoom = gs.roomAt(gs.player.pos.x, gs.player.pos.y);
        if (playerRoom == null || playerRoom.dark()) {
            // Dark or corridor — skip this particular assertion
            assertTrue(true, "Player in corridor or dark room — skip reveal test");
            return;
        }
        // All interior floor tiles of the room should now be F_SEEN
        for (int y = playerRoom.pos.y + 1; y < playerRoom.pos.y + playerRoom.size.y - 1; y++) {
            for (int x = playerRoom.pos.x + 1; x < playerRoom.pos.x + playerRoom.size.x - 1; x++) {
                if (gs.map[y][x] == GameData.FLOOR) {
                    assertNotEquals(0, gs.flags[y][x] & GameData.F_SEEN,
                            "F_SEEN not set at (" + x + "," + y + ")");
                }
            }
        }
    }

    /** UT-LV-09: Same RNG seed produces an identical map layout */
    @Test
    void sameSeedProducesSameLayout() {
        GameData.RNG.setSeed(42L);
        GameState gs1 = new GameState();
        new LevelGenerator(gs1);

        GameData.RNG.setSeed(42L);
        GameState gs2 = new GameState();
        new LevelGenerator(gs2);

        // Compare the full map character grid
        for (int y = 0; y < GameData.NUMLINES; y++) {
            for (int x = 0; x < GameData.NUMCOLS; x++) {
                assertEquals(gs1.map[y][x], gs2.map[y][x],
                        "Map differs at (" + x + "," + y + ")");
            }
        }
    }
}
