package rogue;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * RogueController – REST API that the browser frontend talks to.
 *
 * All endpoints are session-scoped: each browser session has its own
 * independent {@link GameSession} (and therefore its own dungeon).
 *
 * Endpoints
 * ─────────
 *   POST /api/new          – start or restart a game
 *   GET  /api/state        – fetch the full game state as JSON
 *   POST /api/key          – send a keypress to the game engine
 *   GET  /actuator/health  – health check for AWS ALB / App Runner
 *                            (served automatically by Spring Actuator)
 */
@RestController
@CrossOrigin(origins = "*")          // Allow the browser to call from any origin
public class RogueController {

    /** Session-scoped bean: one per browser session, injected by Spring. */
    private final GameSession session;

    public RogueController(GameSession session) {


        System.out.println("Creating new RogueController for session ");

        this.session = session;
    }

    // ── POST /api/new ────────────────────────────────────────────────────

    /**
     * Starts a new game (or restarts an existing one) for this session.
     * Returns the initial state JSON so the browser can render immediately.
     *
     * @return the fresh game state
     */
    @PostMapping("/api/new")
    public Map<String, Object> newGame() {
        session.newGame();
        return buildStateJson(session.getState());
    }

    // ── GET /api/state ───────────────────────────────────────────────────

    /**
     * Returns the current game state.
     * If no game exists yet, starts one automatically.
     *
     * @return the current game state as a JSON-serialisable map
     */
    @GetMapping("/api/state")
    public Map<String, Object> getState() {
        if (!session.hasGame()) session.newGame();
        return buildStateJson(session.getState());
    }

    // ── POST /api/key ────────────────────────────────────────────────────

    /**
     * Accepts a single key string from the browser, dispatches it to the
     * game engine, and returns the updated state.
     *
     * The request body is the raw key string (e.g. "h", "ArrowLeft", " ").
     *
     * @param key the key string sent by the browser
     * @return the updated game state
     */
    @PostMapping("/api/key")
    public Map<String, Object> handleKey(@RequestBody String key) {
        if (!session.hasGame()) session.newGame();
        GameState gs     = session.getState();
        GameEngine engine = session.getEngine();

        // Restart command
        if ((key.equals("r") || key.equals("R")) && (gs.dead || gs.won)) {
            session.newGame();
            return buildStateJson(session.getState());
        }

        // Dismiss popup
        if (gs.showingInventory || gs.showingHelp || gs.showingDiscovered) {
            if (key.equals(" ") || key.equals("Escape") || key.equals("Esc")) {
                gs.showingInventory  = false;
                gs.showingHelp       = false;
                gs.showingDiscovered = false;
                gs.popupLines.clear();
                gs.message = "";
            }
            return buildStateJson(gs);
        }

        if (!gs.dead && !gs.won) {
            dispatchKey(key, gs, engine);
        }

        return buildStateJson(gs);
    }

    // ── Key dispatch ─────────────────────────────────────────────────────

    /**
     * Maps the incoming key string to a game action and calls the engine.
     * Handles movement, run mode, item selection, and single-key commands.
     *
     * @param key    the key string from the browser
     * @param gs     current game state
     * @param engine current game engine
     */
    private void dispatchKey(String key, GameState gs, GameEngine engine) {

        // ── Handle multi-step: waiting for an inventory letter ────────────
        if (gs.waitingForItem) {
            if (key.equals("Escape") || key.equals("Esc")) {
                gs.waitingForItem = false;
                gs.message = "Cancelled.";
                return;
            }
            if (key.length() == 1) {
                char ch = key.charAt(0);
                gs.waitingForItem = false;
                String purpose = gs.waitingItemPurpose;
                gs.waitingItemPurpose = "";
                gs.waitingItemType    = -1;
                switch (purpose) {
                    case "drop":  engine.dropItem(ch);    break;
                    case "quaff": engine.quaffPotion(ch); break;
                    case "read":  engine.readScroll(ch);  break;
                    case "wield": engine.wieldWeapon(ch); break;
                    case "wear":  engine.wearArmor(ch);   break;
                    case "eat":   engine.eatFood(ch);     break;
                }
                engine.endTurn();
            }
            return;
        }

        // ── Translate key to (dx, dy) if it is a movement key ────────────
        int[] dxdy = toDxDy(key);
        if (dxdy != null) {
            gs.message = "";
            boolean shifted = key.length() == 1 && Character.isUpperCase(key.charAt(0))
                           && "HJKLYUBN".indexOf(key.charAt(0)) >= 0;
            if (shifted) {
                runInDir(dxdy[0], dxdy[1], gs, engine);
            } else {
                engine.movePlayer(dxdy[0], dxdy[1]);
                engine.endTurn();
            }
            return;
        }

        // ── Single-character commands ─────────────────────────────────────
        if (key.length() != 1) return;
        char ch = key.charAt(0);

        switch (ch) {
            case '>': engine.goDownStairs();  engine.endTurn(); break;
            case '.': gs.msg("You rest.");    engine.endTurn(); break;
            case ',': engine.pickupItem();    engine.endTurn(); break;
            case 's': engine.search();        engine.endTurn(); break;
            case 'T': engine.takeOffArmor();  engine.endTurn(); break;
            case 'i': showInventory(gs);      break;
            case 'I': showInventory(gs);      break;
            case ')': showEquipment(gs);      break;
            case ']': showEquipment(gs);      break;
            case '@': showStats(gs);          break;
            case 'D': showDiscovered(gs);     break;
            case '?': showHelp(gs);           break;
            case 'Q': quit(gs);               break;
            case 'q': promptItem("quaff", GameData.O_POTION, "Quaff which potion?",  gs, engine); break;
            case 'r': promptItem("read",  GameData.O_SCROLL, "Read which scroll?",   gs, engine); break;
            case 'e': promptItem("eat",   GameData.O_FOOD,   "Eat which food?",      gs, engine); break;
            case 'w': promptItem("wield", GameData.O_WEAPON, "Wield which weapon?",  gs, engine); break;
            case 'W': promptItem("wear",  GameData.O_ARMOR,  "Wear which armor?",    gs, engine); break;
            case 'd': promptItem("drop",  -1,               "Drop which item?",      gs, engine); break;
            default: break;
        }
    }

    /** Runs the player up to 30 steps in the given direction. */
    private void runInDir(int dx, int dy, GameState gs, GameEngine engine) {
        for (int step = 0; step < 30; step++) {
            int nx = gs.player.pos.x + dx, ny = gs.player.pos.y + dy;
            if (!gs.isWalkable(nx, ny)) break;
            if (gs.monsterAt[ny][nx] != null) break;
            boolean moved = engine.movePlayer(dx, dy);
            if (!moved || gs.dead) break;
            engine.endTurn();
            if (gs.dead) break;
            Item it = gs.itemAt[ny][nx];
            if (it != null && it.type != GameData.O_GOLD) break;
            if (gs.map[ny][nx] == GameData.STAIRS) break;
            if (gs.map[ny][nx] == GameData.DOOR)   break;
        }
    }

    /** Maps a key string to (dx, dy), returns null for non-direction keys. */
    private int[] toDxDy(String key) {
        if (key == null) return null;
        switch (key) {
            case "h": case "H": case "ArrowLeft":  return new int[]{-1,  0};
            case "l": case "L": case "ArrowRight": return new int[]{ 1,  0};
            case "k": case "K": case "ArrowUp":    return new int[]{ 0, -1};
            case "j": case "J": case "ArrowDown":  return new int[]{ 0,  1};
            case "y": case "Y": return new int[]{-1, -1};
            case "u": case "U": return new int[]{ 1, -1};
            case "b": case "B": return new int[]{-1,  1};
            case "n": case "N": return new int[]{ 1,  1};
            default:  return null;
        }
    }

    // ── Popup builders ───────────────────────────────────────────────────

    private void promptItem(String purpose, int type, String prompt, GameState gs, GameEngine engine) {
        boolean any = gs.player.pack.stream().anyMatch(it -> type < 0 || it.type == type);
        if (!any) { gs.msg("You have nothing to " + purpose + "."); return; }
        gs.waitingForItem     = true;
        gs.waitingItemPurpose = purpose;
        gs.waitingItemType    = type;
        gs.popupLines.clear();
        gs.popupLines.add(prompt);
        gs.popupLines.add("");
        gs.player.pack.stream()
            .filter(it -> type < 0 || it.type == type)
            .forEach(it -> gs.popupLines.add(it.packCh + ") " + it.getName(gs)));
        gs.showingInventory = true;
    }

    private void showInventory(GameState gs) {
        gs.popupLines.clear();
        if (gs.player.pack.isEmpty()) { gs.popupLines.add("  Your pack is empty."); }
        else {
            for (Item it : gs.player.pack) {
                String eq = it == gs.player.weapon   ? " (weapon in hand)"
                          : it == gs.player.armor    ? " (being worn)"
                          : it == gs.player.rings[0] ? " (on left hand)"
                          : it == gs.player.rings[1] ? " (on right hand)" : "";
                gs.popupLines.add(it.packCh + ") " + it.getName(gs) + eq);
            }
        }
        gs.showingInventory = true;
    }

    private void showEquipment(GameState gs) {
        gs.popupLines.clear();
        gs.popupLines.add("Weapon: " + (gs.player.weapon   != null ? gs.player.weapon.getName(gs)   : "(none)"));
        gs.popupLines.add("Armor:  " + (gs.player.armor    != null ? gs.player.armor.getName(gs)    : "(none)"));
        gs.popupLines.add("Left:   " + (gs.player.rings[0] != null ? gs.player.rings[0].getName(gs) : "(none)"));
        gs.popupLines.add("Right:  " + (gs.player.rings[1] != null ? gs.player.rings[1].getName(gs) : "(none)"));
        gs.showingInventory = true;
    }

    private void showStats(GameState gs) {
        gs.popupLines.clear();
        gs.popupLines.add("Level:      " + gs.player.stats.lvl);
        gs.popupLines.add("Exp:        " + gs.player.stats.exp);
        gs.popupLines.add("Strength:   " + gs.player.stats.str + "/" + gs.player.stats.maxStr);
        gs.popupLines.add("Hit Points: " + gs.player.stats.hpt + "/" + gs.player.stats.maxHp);
        gs.popupLines.add("Armor:      " + gs.player.stats.arm);
        gs.popupLines.add("Gold:       " + gs.player.purse);
        gs.popupLines.add("Dungeon Lvl:" + gs.level);
        gs.showingInventory = true;
    }

    private void showDiscovered(GameState gs) {
        gs.popupLines.clear();
        gs.popupLines.add("Identified Potions:");
        for (int i = 0; i < 14; i++) if (gs.potionKnown[i]) gs.popupLines.add("  " + GameData.POTION_NAMES_REAL[i]);
        gs.popupLines.add("Identified Scrolls:");
        for (int i = 0; i < 18; i++) if (gs.scrollKnown[i]) gs.popupLines.add("  " + GameData.SCROLL_NAMES_REAL[i]);
        gs.popupLines.add("Identified Sticks:");
        for (int i = 0; i < 14; i++) if (gs.stickKnown[i])  gs.popupLines.add("  " + GameData.STICK_NAMES_REAL[i]);
        gs.showingDiscovered = true;
    }

    private void showHelp(GameState gs) {
        gs.popupLines.clear();
        gs.popupLines.add("Movement:  h/j/k/l or arrow keys");
        gs.popupLines.add("           y/u/b/n  diagonal");
        gs.popupLines.add("           Shift+dir  run");
        gs.popupLines.add("");
        gs.popupLines.add("Items:");
        gs.popupLines.add("  ,  pick up    d  drop     i  inventory");
        gs.popupLines.add("  q  quaff      r  read     e  eat");
        gs.popupLines.add("  w  wield      W  wear     T  take off");
        gs.popupLines.add("");
        gs.popupLines.add("Info:");
        gs.popupLines.add("  )  weapon     ]  armor    @  stats");
        gs.popupLines.add("  D  discovered ?  help     Q  quit");
        gs.popupLines.add("");
        gs.popupLines.add("Actions:");
        gs.popupLines.add("  s  search     .  rest     >  stairs");
        gs.showingHelp = true;
    }

    private void quit(GameState gs) {
        gs.dead     = true;
        gs.playing  = false;
        gs.deathMsg = "You quit.";
    }

    // ── State serialisation ──────────────────────────────────────────────

    /**
     * Converts the GameState into a JSON-serialisable Map.
     * The browser frontend receives this as its data model.
     *
     * @param gs the game state to serialise
     * @return a nested Map / List structure ready for Jackson serialisation
     */
    private Map<String, Object> buildStateJson(GameState gs) {
        Map<String, Object> out = new LinkedHashMap<>();

        // ── Map: array of 24 strings (one per row, 80 chars each) ─────────
        // Only cells with F_SEEN are included; unseen cells are spaces.
        List<String> mapRows = new ArrayList<>();
        for (int y = 0; y < GameData.NUMLINES; y++) {
            StringBuilder row = new StringBuilder();
            for (int x = 0; x < GameData.NUMCOLS; x++) {
                if ((gs.flags[y][x] & GameData.F_SEEN) != 0) {
                    // Show monster if visible or found via potion
                    Creature mon = gs.monsterAt[y][x];
                    boolean isPlayer = gs.player.pos.x == x && gs.player.pos.y == y;
                    if (isPlayer) {
                        row.append(GameData.PLAYER);
                    } else if (mon != null && isVisible(gs, mon)) {
                        row.append(mon.hasFlag(GameData.ISINVIS) ? ' ' : mon.disguise);
                    } else {
                        Item item = gs.itemAt[y][x];
                        if (item != null) row.append(item.displayChar());
                        else              row.append(gs.map[y][x]);
                    }
                } else {
                    row.append(' ');
                }
            }
            mapRows.add(row.toString());
        }
        out.put("map", mapRows);

        // ── Player stats ──────────────────────────────────────────────────
        Map<String, Object> player = new LinkedHashMap<>();
        player.put("hp",     gs.player.stats.hpt);
        player.put("maxHp",  gs.player.stats.maxHp);
        player.put("str",    gs.player.stats.str);
        player.put("maxStr", gs.player.stats.maxStr);
        player.put("ac",     gs.player.stats.arm);
        player.put("lvl",    gs.player.stats.lvl);
        player.put("exp",    gs.player.stats.exp);
        player.put("gold",   gs.player.purse);
        player.put("hunger", gs.player.hungryState);
        player.put("blind",  gs.player.hasFlag(GameData.ISBLIND));
        player.put("confused", gs.player.hasFlag(GameData.ISHUH));
        player.put("haste",  gs.player.hasFlag(GameData.ISHASTE));
        player.put("levit",  gs.player.hasFlag(GameData.ISLEVIT));
        out.put("player", player);

        // ── Game flow fields ──────────────────────────────────────────────
        out.put("level",    gs.level);
        out.put("message",  gs.message);
        out.put("dead",     gs.dead);
        out.put("won",      gs.won);
        out.put("deathMsg", gs.deathMsg);

        // ── Popup state ───────────────────────────────────────────────────
        out.put("popupLines",        gs.popupLines);
        out.put("showingInventory",  gs.showingInventory);
        out.put("showingHelp",       gs.showingHelp);
        out.put("showingDiscovered", gs.showingDiscovered);
        out.put("waitingForItem",    gs.waitingForItem);
        out.put("waitingItemPurpose",gs.waitingItemPurpose);

        return out;
    }

    /**
     * Returns true if a monster should be displayed to the player
     * (i.e. it is adjacent, or in the same lit room, or marked ISFOUND).
     *
     * @param gs game state
     * @param m  the monster to check
     * @return true if visible
     */
    private boolean isVisible(GameState gs, Creature m) {
        if (m.hasFlag(GameData.ISFOUND)) return true;
        int px = gs.player.pos.x, py = gs.player.pos.y;
        int dist2 = (px - m.pos.x) * (px - m.pos.x) + (py - m.pos.y) * (py - m.pos.y);
        if (dist2 <= 2) return true;
        Room pr = gs.roomAt(px, py);
        Room mr = gs.roomAt(m.pos.x, m.pos.y);
        return pr != null && pr == mr && !pr.dark();
    }
}
