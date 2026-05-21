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
    }

    /**
     * Generates a complete new dungeon level: clears the map, places rooms,
     * connects them with corridors, then places stairs, traps, gold, items,
     * and monsters. Finally positions the player in the first available room.
     */
    public void generateLevel() {
        System.out.println("Generating level " + gs.level);
        
        // Clear all map data from the previous level
        for (int y = 0; y < GameData.NUMLINES; y++) {
            for (int x = 0; x < GameData.NUMCOLS; x++) {
                gs.map[y][x]       = ' ';
                gs.flags[y][x]     = 0;
                gs.monsterAt[y][x] = null;
                gs.itemAt[y][x]    = null;
            }
        }
        gs.monsters.clear();
        gs.floorItems.clear();
        gs.numTraps = 0;

        // Divide map into a 3x3 grid of cells and place one room per cell
        int cellW = GameData.NUMCOLS / 3;         // ~26 columns per cell
        int cellH = (GameData.NUMLINES - 2) / 3;  // ~7 rows per cell

        // Randomly mark some rooms as "gone" (at most 3)
        int numGone = rng.nextInt(4);
        List<Integer> goneIdx = new ArrayList<>();
        while (goneIdx.size() < numGone) {
            int idx = rng.nextInt(9);
            if (!goneIdx.contains(idx)) goneIdx.add(idx);
        }

        // Place or mark gone all 9 rooms
        for (int i = 0; i < 9; i++) {
            int cellX = (i % 3) * cellW;
            int cellY = (i / 3) * cellH + 1;

            if (goneIdx.contains(i)) {
                // Gone room: just a center point used for passage routing
                int rx = cellX + cellW / 2;
                int ry = cellY + cellH / 2;
                Room r = new Room(new Coord(rx, ry), new Coord(1, 1));
                r.flags |= GameData.ISGONE;
                gs.rooms[i] = r;
            } else {
                // Real room: random size and position within the cell
                int maxW = Math.min(cellW - 4, 12);
                int maxH = Math.min(cellH - 2, 7);
                int rw   = 4 + rng.nextInt(Math.max(1, maxW - 3));
                int rh   = 3 + rng.nextInt(Math.max(1, maxH - 2));
                int rx   = cellX + 1 + rng.nextInt(Math.max(1, cellW - rw - 2));
                int ry   = cellY + rng.nextInt(Math.max(1, cellH - rh - 1));

                // Clamp to map boundaries
                rx = Math.max(1, Math.min(rx, GameData.NUMCOLS - rw - 1));
                ry = Math.max(1, Math.min(ry, GameData.NUMLINES - rh - 2));

                Room r = new Room(new Coord(rx, ry), new Coord(rw, rh));
                if (rng.nextInt(10) == 0) r.flags |= GameData.ISDARK; // 10% dark rooms
                gs.rooms[i] = r;
                drawRoom(r);
            }
        }

        // Connect all rooms with corridors
        connectRooms();

        // Place the descending staircase in a random room
        placeStairs();

        // Place traps (probability increases with dungeon level)
        int numTr = rng.nextInt(10) < gs.level ? 1 + rng.nextInt(3) : 0;
        for (int i = 0; i < numTr && gs.numTraps < GameData.MAXTRAPS; i++) placeTrap();

        // Place gold piles in roughly a third of the rooms
        for (Room r : gs.rooms) {
            if (r == null || r.gone()) continue;
            if (rng.nextInt(3) == 0) {
                Coord gc = randomFloor(r);
                if (gc != null) {
                    Item gold = new Item(GameData.O_GOLD, -1);
                    gold.goldVal = rng.nextInt(50 + 10 * gs.level) + 2;
                    gold.pos     = gc;
                    gs.map[gc.y][gc.x]    = GameData.GOLD;
                    gs.itemAt[gc.y][gc.x] = gold;
                    gs.floorItems.add(gold);
                }
            }
        }

        // Place a handful of random items
        int numItems = rng.nextInt(4) + 2;
        for (int i = 0; i < numItems; i++) placeRandomItem();

        // Place monsters (count scales with level, clamped to 3–12)
        int numMons = (gs.level + 5) / 3 + rng.nextInt(4);
        numMons = Math.max(3, Math.min(numMons, 12));
        for (int i = 0; i < numMons; i++) placeMonster();

        // Place the player in the first non-gone room
        for (Room r : gs.rooms) {
            if (r != null && !r.gone()) {
                Coord pc = randomFloor(r);
                if (pc != null) {
                    gs.player.pos  = pc;
                    gs.player.room = r;
                    break;
                }
            }
        }

        // Reveal the room the player starts in
        revealRoom(gs.player.pos);
    }

    /**
     * Draws a room onto the map: horizontal walls on top and bottom rows,
     * vertical walls on the left and right columns, and floor tiles in the interior.
     *
     * @param r the room to draw
     */
    private void drawRoom(Room r) {
        int x0 = r.pos.x, y0 = r.pos.y;
        int w  = r.size.x, h  = r.size.y;
        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {
                if      (y == y0 || y == y0 + h - 1) gs.map[y][x] = GameData.WALL_H;
                else if (x == x0 || x == x0 + w - 1) gs.map[y][x] = GameData.WALL_V;
                else                                   gs.map[y][x] = GameData.FLOOR;
            }
        }
    }

    /**
     * Connects all 9 rooms using a randomized minimum-spanning-tree approach:
     * start with room 0, then repeatedly connect a random already-connected room
     * to its nearest unconnected neighbor until all rooms are connected.
     * Two extra random connections are added to create loops.
     */
    private void connectRooms() {
        boolean[] connected = new boolean[9];
        connected[0] = true;
        List<Integer> done = new ArrayList<>();
        done.add(0);

        while (done.size() < 9) {
            int from = done.get(rng.nextInt(done.size()));
            Room rf  = gs.rooms[from];
            int bestTo   = -1;
            int bestDist = Integer.MAX_VALUE;

            // Find the nearest unconnected room to this one
            for (int j = 0; j < 9; j++) {
                if (connected[j]) continue;
                Room rj = gs.rooms[j];
                int dx  = (rf.pos.x + rf.size.x / 2) - (rj.pos.x + rj.size.x / 2);
                int dy  = (rf.pos.y + rf.size.y / 2) - (rj.pos.y + rj.size.y / 2);
                int d   = dx * dx + dy * dy;
                if (d < bestDist) { bestDist = d; bestTo = j; }
            }
            if (bestTo < 0) break;
            digCorridor(gs.rooms[from], gs.rooms[bestTo]);
            connected[bestTo] = true;
            done.add(bestTo);
        }

        // Add two random extra connections to create loop paths
        for (int i = 0; i < 2; i++) {
            int a = rng.nextInt(9), b = rng.nextInt(9);
            if (a != b) digCorridor(gs.rooms[a], gs.rooms[b]);
        }
    }

    /**
     * Returns the center coordinate of a room.
     * For "gone" rooms, returns the room's own position (which is already a center point).
     *
     * @param r the room
     * @return a Coord at the room's center
     */
    private Coord roomCenter(Room r) {
        if (r.gone()) return r.pos.copy();
        return new Coord(r.pos.x + r.size.x / 2, r.pos.y + r.size.y / 2);
    }

    /**
     * Digs an L-shaped corridor between two rooms by connecting their centers
     * with a horizontal segment followed by a vertical segment.
     * Wall tiles along the path are converted to doors.
     *
     * @param r1 the first room
     * @param r2 the second room
     */
    private void digCorridor(Room r1, Room r2) {
        Coord c1 = roomCenter(r1);
        Coord c2 = roomCenter(r2);

        int x = c1.x, y = c1.y;

        // Horizontal segment first
        while (x != c2.x) {
            placeCorridor(y, x);
            x += (c2.x > x) ? 1 : -1;
        }
        // Then vertical segment
        while (y != c2.y) {
            placeCorridor(y, x);
            y += (c2.y > y) ? 1 : -1;
        }
        placeCorridor(y, x); // place the corner/end tile
    }

    /**
     * Places a single corridor tile at the given map position.
     * Empty space becomes a PASSAGE tile (with F_PASS flag).
     * Wall tiles become DOOR tiles.
     * Existing floor and passage tiles are left unchanged.
     *
     * @param y the row
     * @param x the column
     */
    private void placeCorridor(int y, int x) {
        if (y <= 0 || y >= GameData.NUMLINES - 1 || x <= 0 || x >= GameData.NUMCOLS - 1)
            return;
        char cur = gs.map[y][x];
        if (cur == ' ') {
            gs.map[y][x]    = GameData.PASSAGE;
            gs.flags[y][x] |= GameData.F_PASS;
        } else if (cur == GameData.WALL_H || cur == GameData.WALL_V) {
            gs.map[y][x] = GameData.DOOR;
        }
        // Floor and passage remain as they are
    }

    /**
     * Places the descending staircase in the room farthest from room 0 (by shuffling
     * room candidates and picking the last entry).
     */
    private void placeStairs() {
        List<Room> candidates = new ArrayList<>();
        for (Room r : gs.rooms) if (r != null && !r.gone()) candidates.add(r);
        Collections.shuffle(candidates, rng);
        Room stairRoom = candidates.get(candidates.size() - 1);
        Coord sc = randomFloor(stairRoom);
        if (sc != null) gs.map[sc.y][sc.x] = GameData.STAIRS;
    }

    /**
     * Places a single trap of a random type at a random floor tile in a random room.
     * Updates the traps array and increments the trap count.
     */
    private void placeTrap() {
        Room r = randomRoom();
        if (r == null) return;
        Coord tc = randomFloor(r);
        if (tc == null) return;
        int type = rng.nextInt(GameData.NTRAPS);
        gs.traps[gs.numTraps][0] = tc.y;
        gs.traps[gs.numTraps][1] = tc.x;
        gs.traps[gs.numTraps][2] = type;
        gs.numTraps++;
        gs.map[tc.y][tc.x] = GameData.TRAP;
    }

    /**
     * Generates a random item and places it on the floor of a random room.
     */
    private void placeRandomItem() {
        Room r = randomRoom();
        if (r == null) return;
        Coord ic = randomFloor(r);
        if (ic == null) return;
        Item item = generateItem();
        if (item == null) return;
        item.pos              = ic;
        gs.map[ic.y][ic.x]    = item.displayChar();
        gs.itemAt[ic.y][ic.x] = item;
        gs.floorItems.add(item);
    }

    /**
     * Generates a random item with type chosen by probability weights matching
     * classic Rogue drop rates, and random sub-type and enchantment values.
     *
     * @return a newly created Item, ready to be placed on the map
     */
    public Item generateItem() {
        int roll = rng.nextInt(100);
        int type;
        if      (roll < 26) type = GameData.O_POTION;
        else if (roll < 62) type = GameData.O_SCROLL;
        else if (roll < 78) type = GameData.O_FOOD;
        else if (roll < 85) type = GameData.O_WEAPON;
        else if (roll < 92) type = GameData.O_ARMOR;
        else if (roll < 96) type = GameData.O_RING;
        else                type = GameData.O_STICK;

        Item item = new Item(type, 0);
        switch (type) {
            case GameData.O_POTION:
                item.which = rng.nextInt(14);
                break;
            case GameData.O_SCROLL:
                item.which = rng.nextInt(18);
                break;
            case GameData.O_FOOD:
                // 80% food ration, 20% slime-mold
                item.which = (rng.nextInt(10) < 8) ? 0 : 1;
                break;
            case GameData.O_WEAPON:
                item.which = rng.nextInt(9);
                item.hplus = randEnchant();
                item.dplus = randEnchant();
                if (item.hplus < 0 || item.dplus < 0) item.flags |= GameData.ISCURSED;
                break;
            case GameData.O_ARMOR:
                item.which = rng.nextInt(8);
                item.arm   = randEnchant();
                if (item.arm < 0) item.flags |= GameData.ISCURSED;
                break;
            case GameData.O_RING:
                item.which = rng.nextInt(14);
                item.arm   = (rng.nextBoolean() ? 1 : -1) * (1 + rng.nextInt(3));
                if (item.arm < 0) item.flags |= GameData.ISCURSED;
                break;
            case GameData.O_STICK:
                item.which = rng.nextInt(14);
                item.arm   = 3 + rng.nextInt(10); // number of charges
                break;
        }
        return item;
    }

    /**
     * Returns a random enchantment value for weapons and armor.
     * 20% chance of a negative value (cursed), 40% chance of 0, 40% chance of positive.
     *
     * @return a small integer enchantment value (can be negative, zero, or positive)
     */
    private int randEnchant() {
        int r = rng.nextInt(10);
        if (r < 2) return -rng.nextInt(3) - 1;
        if (r < 6) return 0;
        return rng.nextInt(3) + 1;
    }

    /**
     * Places a single monster at a random floor tile in a random room.
     * The monster's stats are scaled to the current dungeon level, and it is
     * given the standard flags for its type from {@link GameData#MONSTERS}.
     */
    private void placeMonster() {
        Room r = randomRoom();
        if (r == null) return;
        Coord mc = randomFloor(r);
        if (mc == null) return;

        char type = randomMonsterType();
        Creature m = new Creature(type, mc.copy());
        int idx    = type - 'A';
        GameData.MonsterInfo mi = GameData.MONSTERS[idx];

        // Scale level and stats to current dungeon depth
        int lvlAdd     = Math.max(0, gs.level - GameData.AMULETLEVEL);
        m.stats.lvl    = mi.lvl + lvlAdd;
        m.stats.maxHp  = m.stats.hpt = GameData.roll(m.stats.lvl, 8);
        m.stats.arm    = mi.arm - lvlAdd;
        m.stats.exp    = mi.exp + lvlAdd * 10;
        m.stats.dmg    = mi.dmg;
        m.flags        = mi.flags;

        // Extra-deep dungeons have hasted monsters
        if (gs.level > 29) m.setFlag(GameData.ISHASTE);
        m.room  = r;
        m.oldCh = GameData.FLOOR;

        gs.monsters.add(m);
        gs.monsterAt[mc.y][mc.x] = m;
    }

    /**
     * Selects a random monster type appropriate for the current dungeon level.
     * Monster types are drawn from a level-ordered pool; deeper levels unlock
     * progressively more dangerous monsters.
     *
     * @return the monster type character ('A'-'Z')
     */
    private char randomMonsterType() {
        char[] lvlMons = "KEBSHIROZ LCQANYF TWPX UMVGJD".replace(" ", "").toCharArray();
        int maxIdx     = Math.min(lvlMons.length - 1, gs.level + 4);
        int idx;
        do {
            int d = gs.level + (rng.nextInt(10) - 5);
            d   = Math.max(0, Math.min(d, maxIdx));
            idx = d;
        } while (idx >= lvlMons.length);
        return lvlMons[idx];
    }

    /**
     * Returns a randomly chosen non-gone room from the current level's room list.
     *
     * @return a valid Room, or null if no non-gone rooms exist
     */
    private Room randomRoom() {
        List<Room> valid = new ArrayList<>();
        for (Room r : gs.rooms) if (r != null && !r.gone()) valid.add(r);
        if (valid.isEmpty()) return null;
        return valid.get(rng.nextInt(valid.size()));
    }

    /**
     * Returns a random floor coordinate inside the given room that is not currently
     * occupied by a monster or an item.
     * Tries up to 50 times before giving up.
     *
     * @param r the room to search
     * @return an unoccupied floor Coord, or null if none found within 50 attempts
     */
    public Coord randomFloor(Room r) {
        if (r == null || r.gone()) return null;
        for (int attempt = 0; attempt < 50; attempt++) {
            int x = r.pos.x + 1 + rng.nextInt(r.size.x - 2);
            int y = r.pos.y + 1 + rng.nextInt(r.size.y - 2);
            if (y >= 0 && y < GameData.NUMLINES && x >= 0 && x < GameData.NUMCOLS
                    && gs.map[y][x]       == GameData.FLOOR
                    && gs.monsterAt[y][x] == null
                    && gs.itemAt[y][x]    == null)
                return new Coord(x, y);
        }
        return null;
    }

    /**
     * Marks all cells visible from the given position as seen (F_SEEN flag).
     *
     * In a lit room: the entire room is revealed plus immediately adjacent passages.
     * In a dark room: only the 8 surrounding cells are revealed.
     * In a corridor: only the 8 surrounding cells are revealed.
     *
     * @param pos the position from which to reveal the surroundings
     */
    public void revealRoom(Coord pos) {
        Room r = gs.roomAt(pos.x, pos.y);

        if (r == null) {
            // In a corridor: reveal immediate 3x3 neighborhood
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int ny = pos.y + dy, nx = pos.x + dx;
                    if (ny >= 0 && ny < GameData.NUMLINES && nx >= 0 && nx < GameData.NUMCOLS)
                        gs.flags[ny][nx] |= GameData.F_SEEN;
                }
            }
            gs.flags[pos.y][pos.x] |= GameData.F_SEEN;
            return;
        }

        if (r.dark()) {
            // In a dark room: only immediate neighborhood is visible
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int ny = pos.y + dy, nx = pos.x + dx;
                    if (ny >= 0 && ny < GameData.NUMLINES && nx >= 0 && nx < GameData.NUMCOLS)
                        gs.flags[ny][nx] |= GameData.F_SEEN;
                }
            }
            return;
        }

        // In a lit room: reveal the entire room
        for (int y = r.pos.y; y < r.pos.y + r.size.y; y++) {
            for (int x = r.pos.x; x < r.pos.x + r.size.x; x++) {
                if (y >= 0 && y < GameData.NUMLINES && x >= 0 && x < GameData.NUMCOLS)
                    gs.flags[y][x] |= GameData.F_SEEN;
            }
        }

        // Also reveal passage tiles immediately adjacent to the player
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int ny = pos.y + dy, nx = pos.x + dx;
                if (ny >= 0 && ny < GameData.NUMLINES && nx >= 0 && nx < GameData.NUMCOLS
                        && gs.map[ny][nx] == GameData.PASSAGE)
                    gs.flags[ny][nx] |= GameData.F_SEEN;
            }
        }
    }
}
