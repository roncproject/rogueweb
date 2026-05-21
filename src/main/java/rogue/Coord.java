package rogue;

/**
 * Represents a two-dimensional integer coordinate (column, row) on the dungeon map.
 * Used throughout the game to refer to positions of rooms, items, creatures, and the player.
 */
class Coord {
    /** The horizontal position (column index, 0-based). */
    int x;
    /** The vertical position (row index, 0-based). */
    int y;

    /**
     * Constructs a new Coord with the given column and row values.
     *
     * @param x the column (horizontal) position
     * @param y the row (vertical) position
     */
    Coord(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Creates and returns a new Coord with the same x and y values as this one.
     *
     * @return a deep copy of this coordinate
     */
    Coord copy() {
        return new Coord(x, y);
    }

    /**
     * Checks whether this coordinate is equal to another coordinate.
     *
     * @param o the other Coord to compare against
     * @return true if both coordinates have the same x and y values, false otherwise
     */
    boolean equals(Coord o) {
        return o != null && o.x == x && o.y == y;
    }

    /**
     * Returns a human-readable string representation of this coordinate in the form "(x,y)".
     *
     * @return formatted string "(x,y)"
     */
    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}
