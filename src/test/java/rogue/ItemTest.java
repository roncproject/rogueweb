package rogue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Item — construction, flag manipulation, getName().
 * DTP-01: UT-IT-01 through UT-IT-10
 */
class ItemTest {

    private GameState gs;

    @BeforeEach
    void setup() {
        GameData.RNG.setSeed(42L);
        gs = new GameState();
    }

    /** UT-IT-01: Item(O_WEAPON, MACE) has type == O_WEAPON */
    @Test
    void weaponItemHasCorrectType() {
        Item item = new Item(GameData.O_WEAPON, GameData.MACE);
        assertEquals(GameData.O_WEAPON, item.type);
    }

    /** UT-IT-02: Item(O_GOLD, -1) starts with goldVal==0 and count==1 */
    @Test
    void goldItemStartsWithZeroGoldVal() {
        Item item = new Item(GameData.O_GOLD, -1);
        assertEquals(0, item.goldVal);
        assertEquals(1, item.count);
    }

    /** UT-IT-03: Setting ISCURSED flag makes hasFlag detect it */
    @Test
    void iscursedFlagSetAndDetected() {
        Item item = new Item(GameData.O_WEAPON, GameData.MACE);
        item.flags |= GameData.ISCURSED;
        assertNotEquals(0, item.flags & GameData.ISCURSED);
    }

    /** UT-IT-04: getName on a known potion shows real name */
    @Test
    void getNameKnownPotionShowsRealName() {
        Item item = new Item(GameData.O_POTION, 0); // potion of confusion
        gs.potionKnown[0] = true;
        assertTrue(item.getName(gs).contains("confusion"),
                "Expected 'confusion' in: " + item.getName(gs));
    }

    /** UT-IT-05: getName on an unknown potion shows a colour name */
    @Test
    void getNameUnknownPotionShowsColour() {
        Item item = new Item(GameData.O_POTION, 0);
        gs.potionKnown[0] = false;
        String name = item.getName(gs);
        // Must contain at least one of the known colour words
        boolean hasColour = false;
        for (String c : GameData.POTION_COLORS) {
            if (name.contains(c)) { hasColour = true; break; }
        }
        assertTrue(hasColour, "Expected a colour name in: " + name);
    }

    /** UT-IT-06: Known weapon with hplus=2, dplus=1 shows "+2,+1" */
    @Test
    void getNameKnownWeaponShowsEnchantment() {
        Item item = new Item(GameData.O_WEAPON, GameData.MACE);
        item.hplus = 2;
        item.dplus = 1;
        item.flags |= GameData.ISKNOW;
        String name = item.getName(gs);
        assertTrue(name.contains("+2"), "Expected '+2' in: " + name);
        assertTrue(name.contains("+1"), "Expected '+1' in: " + name);
    }

    /** UT-IT-07: Unknown scroll starts with "scroll" and contains "titled" */
    @Test
    void getNameUnknownScrollShowsTitleLabel() {
        Item item = new Item(GameData.O_SCROLL, 0);
        gs.scrollKnown[0] = false;
        String name = item.getName(gs);
        assertTrue(name.startsWith("a scroll"), "Expected 'a scroll' prefix, got: " + name);
        assertTrue(name.contains("titled") || name.contains("labeled"),
                "Expected 'titled' or 'labeled' in: " + name);
    }

    /** UT-IT-08: Food item (which=0) name is "some food" */
    @Test
    void getNameFoodIsCorrect() {
        Item item = new Item(GameData.O_FOOD, 0);
        assertEquals("some food", item.getName(gs));
    }

    /** UT-IT-09: Plate mail armor name contains "plate mail" */
    @Test
    void getNameArmorContainsPlateMail() {
        Item item = new Item(GameData.O_ARMOR, GameData.PLATE_MAIL);
        String name = item.getName(gs);
        assertTrue(name.contains("plate mail"), "Expected 'plate mail' in: " + name);
    }

    /** UT-IT-10: Gold pile with goldVal=50 returns "50 gold pieces" */
    @Test
    void getNameGoldShowsAmount() {
        Item item = new Item(GameData.O_GOLD, -1);
        item.goldVal = 50;
        assertEquals("50 gold pieces", item.getName(gs));
    }
}
