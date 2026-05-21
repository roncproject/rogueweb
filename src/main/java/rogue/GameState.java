package rogue;

import java.util.*;

/**
 * Holds the complete mutable state of an ongoing Rogue game session.
 *
 * This is the single source of truth for the dungeon map, all creatures, all items,
 * player data, identification knowledge, trap locations, messaging, and UI state.
 * The {@link GameEngine} and {@link LevelGenerator} both read from and write into
 * this object; the {@link GameRenderer} reads from it each frame.
 *
 * A new {@code GameState} represents a fresh game: the player is initialized with
 * starting equipment and the identification tables are randomized.
 */
public class GameState {

    // ── Dungeon map ────────────────────────────────────────────────────────

    /**
     * The character displayed at each map cell.
     * Indexed as {@code map[row][col]}.
     */
    public char[][] map   = new char[GameData.NUMLINES][GameData.NUMCOLS];

    /**
     * Bit-flag overlay for each map cell (F_SEEN, F_PASS, F_DROPPED, F_REAL).
     * Indexed as {@code flags[row][col]}.
     */
    public int[][]  flags = new int[GameData.NUMLINES][GameData.NUMCOLS];

    /**
     * Quick-lookup grid: the monster currently occupying each cell, or null.
     * Indexed as {@code monsterAt[row][col]}.
     */
    public Creature[][] monsterAt = new Creature[GameData.NUMLINES][GameData.NUMCOLS];

    /**
     * Quick-lookup grid: the item resting on the floor at each cell, or null.
     * Indexed as {@code itemAt[row][col]}.
     */
    public Item[][] itemAt = new Item[GameData.NUMLINES][GameData.NUMCOLS];

    // ── Rooms and entities ────────────────────────────────────────────────

    /** The 9 rooms (in a 3×3 grid layout) on the current level. Some may be "gone". */
    public Room[] rooms = new Room[GameData.MAXROOMS];

    /** The player creature. Initialized in the constructor; never null during play. */
    public Creature player;

    /** All living monsters on the current level. */
    public List<Creature> monsters = new ArrayList<>();

    /** All items lying on the floor of the current level. */
    public List<Item> floorItems = new ArrayList<>();

    // ── Game progress ──────────────────────────────────────────────────────

    /** The current dungeon depth (1 = shallowest). Increases when descending stairs. */
    public int level = 1;

    /** The deepest level the player has ever reached. Used for scoring. */
    public int maxLevel = 1;

    /** False when the game loop should stop (player died or won). */
    public boolean playing = true;

    /** True once the player has picked up the Amulet of Yendor. */
    public boolean amuletFound = false;

    // ── Message system ─────────────────────────────────────────────────────

    /**
     * The message currently shown in the top message bar.
     * Cleared or overwritten each turn.
     */
    public String message = "";

    /** The most recently displayed message, used by Ctrl+P (repeat last message). */
    public String lastMsg = "";

    // ── Identification state ───────────────────────────────────────────────

    /** Randomly assigned color index (into POTION_COLORS) for each of the 14 potions. */
    public int[]     potionColor  = new int[14];

    /** Whether each potion type has been fully identified by the player. */
    public boolean[] potionKnown  = new boolean[14];

    /** Player-supplied guess names for each potion type (or null if not guessed). */
    public String[]  potionGuess  = new String[14];

    /** Randomly generated nonsense title labels for each of the 18 scroll types. */
    public String[]  scrollTitle  = new String[18];

    /** Whether each scroll type has been fully identified by the player. */
    public boolean[] scrollKnown  = new boolean[18];

    /** Player-supplied guess names for each scroll type (or null if not guessed). */
    public String[]  scrollGuess  = new String[18];

    /** Randomly assigned gemstone index (into RING_STONES) for each of the 14 ring types. */
    public int[]     ringStone    = new int[14];

    /** Whether each ring type has been fully identified by the player. */
    public boolean[] ringKnown    = new boolean[14];

    /** Player-supplied guess names for each ring type (or null if not guessed). */
    public String[]  ringGuess    = new String[14];

    /** Randomly assigned material index (into STICK_MATERIALS) for each of the 14 sticks. */
    public int[]     stickMaterial = new int[14];

    /** Whether each stick (wand/staff) type has been fully identified by the player. */
    public boolean[] stickKnown   = new boolean[14];

    // ── Traps ─────────────────────────────────────────────────────────────

    /**
     * Trap data for the current level.
     * Each entry is an int[3]: { row, col, trapType }.
     * See the T_* constants in {@link GameData}.
     */
    public int[][] traps    = new int[GameData.MAXTRAPS][3];

    /** Number of traps currently placed on this level. */
    public int     numTraps = 0;

    // ── Scoring and end-game state ─────────────────────────────────────────

    /** Player's current score (primarily based on gold). */
    public int     score    = 0;

    /** Description of how the player died (e.g., "killed by a troll"). */
    public String  deathMsg = "";

    /** True once the player has died. Ends the game loop and shows the death screen. */
    public boolean dead     = false;

    /** True once the player escapes with the Amulet of Yendor. Shows the win screen. */
    public boolean won      = false;

    // ── Timers ────────────────────────────────────────────────────────────

    /** Unused healing timer (placeholder for future timed healing effects). */
    public int healTimer   = 0;

    /** Countdown in turns until the next wandering monster spawns on this level. */
    public int wanderTimer = 100;

    /** Total number of game turns elapsed since the level was generated. */
    public int turnCount   = 0;

    // ── Running state (auto-movement) ─────────────────────────────────────

    /** True when the player is in "run" mode (holding shift+direction). */
    public boolean running      = false;

    /** The direction character the player is running in ('h','j','k','l','y','u','b','n'). */
    public char    runDir       = 0;

    /** True when the player has committed to fighting a specific target to the death. */
    public boolean toDeathFight = false;

    /** The specific monster targeted in a to-death fight, or null. */
    public Creature fightTarget = null;

    // ── Multi-step command state ───────────────────────────────────────────

    /** True when the game is waiting for the player to press a direction key. */
    public boolean waitingForDir      = false;

    /** The command name that triggered the direction-wait (e.g., "run"). */
    public String  waitingCommand     = "";

    /** True when the game is waiting for the player to press an inventory letter. */
    public boolean waitingForItem     = false;

    /** The action that will use the selected item (e.g., "drop", "quaff", "wield"). */
    public String  waitingItemPurpose = "";

    /** The required item type for the pending selection (-1 = any type). */
    public int     waitingItemType    = -1;

    // ── Popup / overlay state ──────────────────────────────────────────────

    /** True when the inventory overlay is being displayed. */
    public boolean showingInventory  = false;

    /** True when the help overlay is being displayed. */
    public boolean showingHelp       = false;

    /** True when the discovered-items overlay is being displayed. */
    public boolean showingDiscovered = false;

    /** Lines of text to show in the current popup overlay. */
    public List<String> popupLines   = new ArrayList<>();

    /**
     * Constructs a new GameState, randomizing identification assignments and
     * placing the player in their starting configuration with initial equipment.
     */
    public GameState() {
        initIdentification();
        initPlayer();
    }

    /**
     * Randomizes the per-run appearance assignments for potions, scrolls, rings,
     * and sticks so that each new game the player must re-identify these item types.
     * Also generates random nonsense title strings for scrolls.
     */
    private void initIdentification() {
        // Shuffle potion colors: each potion type gets a unique color this game
        List<Integer> colors = new ArrayList<>();
        for (int i = 0; i < GameData.POTION_COLORS.length; i++) colors.add(i);
        Collections.shuffle(colors, new Random(GameData.RNG.nextLong()));
        for (int i = 0; i < 14; i++) potionColor[i] = colors.get(i);

        // Generate random nonsense scroll titles (3 pseudo-words per scroll)
        Random r = new Random(GameData.RNG.nextLong());
        for (int i = 0; i < 18; i++) {
            StringBuilder sb = new StringBuilder();
            for (int w = 0; w < 3; w++) {
                if (w > 0) sb.append(" ");
                int len = 4 + r.nextInt(4);
                for (int c = 0; c < len; c++) sb.append((char) ('a' + r.nextInt(26)));
            }
            scrollTitle[i] = sb.toString();
        }

        // Shuffle ring stones: each ring type gets a unique gemstone appearance this game
        List<Integer> stones = new ArrayList<>();
        for (int i = 0; i < GameData.RING_STONES.length; i++) stones.add(i);
        Collections.shuffle(stones, new Random(GameData.RNG.nextLong()));
        for (int i = 0; i < 14; i++) ringStone[i] = stones.get(i % stones.size());

        // Shuffle stick materials: each wand/staff type gets a unique material this game
        List<Integer> mats = new ArrayList<>();
        for (int i = 0; i < GameData.STICK_MATERIALS.length; i++) mats.add(i);
        Collections.shuffle(mats, new Random(GameData.RNG.nextLong()));
        for (int i = 0; i < 14; i++) stickMaterial[i] = mats.get(i % mats.size());
    }

    /**
     * Creates the player creature with starting stats and equips them with
     * a +1/+1 mace (identified), ring mail armor, and one food ration.
     */
    private void initPlayer() {
        player = new Creature('@', new Coord(0, 0));
        player.isPlayer = true;
        player.stats.str    = 16;
        player.stats.maxStr = 16;
        player.stats.lvl    = 1;
        player.stats.exp    = 0;
        player.stats.arm    = 10; // no armor = AC 10
        player.stats.hpt    = 12;
        player.stats.maxHp  = 12;
        player.stats.dmg    = "1x4";

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

        player.pack.add(startWeapon);
        player.pack.add(startArmor);
        player.pack.add(startFood);
        player.weapon       = startWeapon;
        player.armor        = startArmor;
        player.stats.arm    = GameData.ARMOR_CLASS[GameData.RING_MAIL];
        player.foodLeft     = 2000;

        System.out.println("Initialized player with starting equipment and stats.");
    }

    /**
     * Sets the current message to the given string, replacing any existing message.
     * Also saves the message as the last-shown message for Ctrl+P recall.
     *
     * @param s the message to display
     */
    public void msg(String s) {
        lastMsg = s;
        message = s;
    }

    /**
     * Appends the given string to the current message, separated by a space.
     * If the current message is empty, the string becomes the entire message.
     * Also updates the last-message buffer.
     *
     * @param s the text to append to the current message
     */
    public void addMsg(String s) {
        if (message.isEmpty()) message = s;
        else message += " " + s;
        lastMsg = message;
    }

    /**
     * Returns the character at the given map cell, or a space if out of bounds.
     *
     * @param y the row index
     * @param x the column index
     * @return the map character at (y, x), or ' ' if out of range
     */
    public char cellChar(int y, int x) {
        if (y < 0 || y >= GameData.NUMLINES || x < 0 || x >= GameData.NUMCOLS) return ' ';
        return map[y][x];
    }

    /**
     * Returns true if the cell at the given position has been seen by the player
     * and should be drawn on screen.
     *
     * @param y the row index
     * @param x the column index
     * @return true if the F_SEEN flag is set for this cell
     */
    public boolean isSeen(int y, int x) {
        return (flags[y][x] & GameData.F_SEEN) != 0;
    }

    /**
     * Returns true if the cell at the given position is marked as a passage tile.
     *
     * @param y the row index
     * @param x the column index
     * @return true if the F_PASS flag is set for this cell
     */
    public boolean isPass(int y, int x) {
        return (flags[y][x] & GameData.F_PASS) != 0;
    }

    /**
     * Returns the Room that contains the given map coordinate, or null if the
     * position is in a corridor, out of bounds, or in a "gone" room slot.
     *
     * @param x the column index
     * @param y the row index
     * @return the containing Room, or null
     */
    public Room roomAt(int x, int y) {
        for (Room r : rooms) {
            if (r != null && !r.gone() && r.contains(x, y)) return r;
        }
        return null;
    }

    /**
     * Returns true if a creature can walk onto the given map cell.
     * Walkable cells include floor, passage, door, stairs, trap, and all item glyphs.
     * Boundary cells and walls are not walkable.
     *
     * @param x the column index
     * @param y the row index
     * @return true if movement onto this cell is permitted
     */
    public boolean isWalkable(int x, int y) {
        if (x <= 0 || x >= GameData.NUMCOLS - 1 || y <= 0 || y >= GameData.NUMLINES - 1)
            return false;
        char c = map[y][x];
        return c == GameData.FLOOR   || c == GameData.PASSAGE || c == GameData.DOOR
            || c == GameData.STAIRS  || c == GameData.TRAP    || c == GameData.GOLD
            || c == GameData.POTION  || c == GameData.SCROLL  || c == GameData.FOOD
            || c == GameData.WEAPON  || c == GameData.ARMOR   || c == GameData.RING
            || c == GameData.STICK   || c == GameData.AMULET;
    }

    /**
     * Returns true if the given map cell blocks line-of-sight (i.e., is a wall or void space).
     *
     * @param x the column index
     * @param y the row index
     * @return true if the cell is opaque
     */
    public boolean isOpaque(int x, int y) {
        if (x <= 0 || x >= GameData.NUMCOLS - 1 || y <= 0 || y >= GameData.NUMLINES - 1)
            return true;
        char c = map[y][x];
        return c == ' ' || c == GameData.WALL_H || c == GameData.WALL_V;
    }

    /**
     * Adds the given experience points to the player's total and checks whether
     * they have reached the threshold for the next level. If a level-up occurs,
     * the player's maximum and current HP increase and their level is updated.
     *
     * @param xp the number of experience points to add
     * @return a "Welcome to level N!" message string if a level-up occurred, null otherwise
     */
    public String addExp(int xp) {
        // XP thresholds for levels 1–20 (doubles each level starting from 10)
        int[] thresholds = {
            0, 10, 20, 40, 80, 160, 320, 640, 1280, 2560, 5120,
            10240, 20480, 40960, 81920, 163840, 327680, 655360, 1310720, 2621440
        };
        int oldLvl = player.stats.lvl;
        player.stats.exp += xp;
        int newLvl = 1;
        for (int i = 1; i < thresholds.length; i++) {
            if (player.stats.exp >= thresholds[i]) newLvl = i + 1;
        }
        newLvl = Math.min(newLvl, 20);
        if (newLvl > oldLvl) {
            int gained = GameData.roll(newLvl - oldLvl, 10);
            player.stats.maxHp += gained;
            player.stats.hpt = Math.min(player.stats.hpt + gained, player.stats.maxHp);
            player.stats.lvl = newLvl;
            return "Welcome to level " + newLvl + "!";
        }
        return null;
    }

    /**
     * Returns the next unused inventory letter ('a' through 'z') for assigning to
     * a newly picked-up item. Returns the null character (0) if all 26 slots are used.
     *
     * @return an available pack character, or 0 if the pack is full
     */
    public char nextPackChar() {
        boolean[] used = new boolean[26];
        for (Item it : player.pack) {
            if (it.packCh >= 'a' && it.packCh <= 'z') used[it.packCh - 'a'] = true;
        }
        for (int i = 0; i < 26; i++) if (!used[i]) return (char) ('a' + i);
        return 0;
    }
}
