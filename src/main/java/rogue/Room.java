package rogue;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single rectangular room on a dungeon level.
 * Rooms are arranged in a 3x3 grid across the map and may be connected by corridors.
 * A room may be "gone" (replaced by a passage point), dark (limiting visibility),
 * or a maze room. Each room can optionally hold gold and has a list of exit coordinates.
 */
class Room {
    /** The upper-left corner of this room on the map. */
    Coord pos;

    /** The width (x) and height (y) of this room in tiles, including walls. */
    Coord size;

    /** The position of the gold pile in this room, or null if none. */
    Coord gold;

    /** The value of the gold pile in this room (0 if no gold). */
    int goldVal;

    /**
     * Bit-flag field for room properties.
     * Possible flags: {@link GameData#ISDARK}, {@link GameData#ISGONE}, {@link GameData#ISMAZE}.
     */
    int flags;

    /** Coordinates of doorway/exit tiles connecting this room to corridors. */
    List<Coord> exits = new ArrayList<>();

    /** Whether this room has been connected to the corridor graph during level generation. */
    boolean connected = false;

    /**
     * Constructs a new Room at the given position with the given dimensions.
     *
     * @param pos  the upper-left corner of the room (including wall)
     * @param size the total width and height of the room (including walls)
     */
    Room(Coord pos, Coord size) {
        this.pos = pos;
        this.size = size;
    }

    /**
     * Returns true if this room has been marked as "gone" (i.e., it is a placeholder
     * routing point rather than a real room that is drawn on the map).
     *
     * @return true if ISGONE flag is set
     */
    boolean gone() {
        return (flags & GameData.ISGONE) != 0;
    }

    /**
     * Returns true if this room is dark, meaning the player can only see adjacent tiles
     * rather than the whole room when inside it.
     *
     * @return true if ISDARK flag is set
     */
    boolean dark() {
        return (flags & GameData.ISDARK) != 0;
    }

    /**
     * Checks whether a given map coordinate is inside the walkable interior of this room
     * (not on the wall border).
     *
     * @param x column to test
     * @param y row to test
     * @return true if (x, y) is strictly within the room's inner floor area
     */
    boolean contains(int x, int y) {
        return x > pos.x && x < pos.x + size.x - 1
            && y > pos.y && y < pos.y + size.y - 1;
    }

    /**
     * Checks whether a given map coordinate falls on the wall border of this room.
     *
     * @param x column to test
     * @param y row to test
     * @return true if (x, y) lies on the perimeter wall of this room
     */
    boolean onWall(int x, int y) {
        return (x >= pos.x && x <= pos.x + size.x - 1 && (y == pos.y || y == pos.y + size.y - 1))
            || (y >= pos.y && y <= pos.y + size.y - 1 && (x == pos.x || x == pos.x + size.x - 1));
    }
}
