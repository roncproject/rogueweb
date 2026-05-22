package rogue;

import java.util.*;

/**
 * Procedurally generates a new dungeon level and populates it with rooms,
 * corridors, stairs, traps, gold, items, monsters, and a starting player position.
 *
 * The level layout uses the classic Rogue approach: 9 rooms are arranged in a
 * 3×3 grid. A subset may be "gone" (replaced by passage routing points).
 * Rooms are connected using a minimum-spanning-tree algorithm plus a few extra
 * random corridors to create loops. Each corridor is an L-shaped passage.
 */
public class LevelGenerator {

    /** The game state this generator writes into. */
    private GameState gs;
    public Level level;

    /** Shared random number generator. */
    private Random rng = GameData.RNG;

    /**
     * Constructs a LevelGenerator that operates on the given game state.
     *
     * @param gs the game state to populate when generating a level
     */
    public LevelGenerator(GameState gs) {
        System.out.println("LevelGenerator initialized with GameState: " + gs);

        this.gs = gs;
        this.level = new Level(gs);
        level.generate();
    }

   
}
