package rogue;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Coord — coordinate arithmetic.
 * DTP-01: UT-CO-01 through UT-CO-08
 */
class CoordTest {

    /** UT-CO-01: Constructor sets x and y correctly */
    @Test
    void constructorSetsXAndY() {
        Coord c = new Coord(3, 7);
        assertEquals(3, c.x);
        assertEquals(7, c.y);
    }

    /** UT-CO-02: add(2,3) on (3,7) returns (5,10) */
    @Test
    void addPositiveDelta() {
        Coord c = new Coord(3, 7).add(2, 3);
        assertEquals(5, c.x);
        assertEquals(10, c.y);
    }

    /** UT-CO-03: add(-1,-1) on (3,7) returns (2,6) */
    @Test
    void addNegativeDelta() {
        Coord c = new Coord(3, 7).add(-1, -1);
        assertEquals(2, c.x);
        assertEquals(6, c.y);
    }

    /** UT-CO-04: copy() returns equal value but different reference */
    @Test
    void copyIsEqualButDistinct() {
        Coord orig = new Coord(3, 7);
        Coord copy = orig.copy();
        assertTrue(copy.equals(orig), "copy must equal original");
        assertNotSame(orig, copy, "copy must be a different object");
    }

    /** UT-CO-05: equals() returns true for same coordinates */
    @Test
    void equalsReturnsTrueForSameCoords() {
        Coord a = new Coord(3, 7);
        Coord b = new Coord(3, 7);
        assertTrue(a.equals(b));
    }

    /** UT-CO-06: equals() returns false when x differs */
    @Test
    void equalsReturnsFalseForDifferentX() {
        Coord a = new Coord(3, 7);
        Coord b = new Coord(4, 7);
        assertFalse(a.equals(b));
    }

    /** UT-CO-07: equals(null) returns false without throwing */
    @Test
    void equalsNullReturnsFalse() {
        Coord a = new Coord(3, 7);
        assertFalse(a.equals(null));
    }

    /** UT-CO-08: toString() returns "(x,y)" format */
    @Test
    void toStringFormat() {
        assertEquals("(3,7)", new Coord(3, 7).toString());
    }
}
