package rogue;

import java.util.*;

/**
 * Core game logic engine for Rogue.
 *
 * Handles all gameplay mechanics that change the {@link GameState} in response to
 * player input or the passage of a game turn:
 * <ul>
 *   <li>Player movement, collision, and trap triggering</li>
 *   <li>Melee combat between player and monsters</li>
 *   <li>Monster AI movement and attack</li>
 *   <li>Item pickup, drop, use (potions, scrolls, food, armor, weapons)</li>
 *   <li>Turn-end effects: hunger, regeneration, wandering monster spawning</li>
 * </ul>
 *
 * The engine does not perform rendering; it only mutates GameState and posts
 * messages via {@link GameState#msg(String)}.
 */
public class GameEngine {

    /** The game state this engine operates on. */
    private GameState gs;

    /** Level generator, used when descending stairs or teleporting. */
    private LevelGenerator lg;

    /** Shared random number generator. */
    private Random rng = GameData.RNG;

    /**
     * Constructs a GameEngine that operates on the given game state.
     *
     * @param gs the current game state
     */
    public GameEngine(GameState gs) {
        this.gs = gs;
        this.lg = new LevelGenerator(gs);
    }

    // ── Movement ─────────────────────────────────────────────────────────

    /**
     * Attempts to move the player one tile in the given direction.
     * If a monster occupies the destination, the player attacks it instead.
     * If the destination is not walkable, prints "Ouch!" and does nothing.
     * Otherwise the player moves, the room is revealed, and any floor items
     * or features at the new position are reported to the player.
     *
     * @param dx horizontal delta (-1, 0, or +1)
     * @param dy vertical delta   (-1, 0, or +1)
     * @return false if the player died this turn, true otherwise
     */
    public boolean movePlayer(int dx, int dy) {
        int nx = gs.player.pos.x + dx;
        int ny = gs.player.pos.y + dy;

        // Boundary check
        if (nx <= 0 || nx >= GameData.NUMCOLS - 1 || ny <= 0 || ny >= GameData.NUMLINES - 1)
            return true;

        // Monster at destination: attack instead of moving
        Creature m = gs.monsterAt[ny][nx];
        if (m != null) {
            return attackMonster(m);
        }

        // Unwalkable tile
        if (!gs.isWalkable(nx, ny)) {
            gs.msg("Ouch!");
            return true;
        }

        // Move player
        gs.monsterAt[gs.player.pos.y][gs.player.pos.x] = null;
        gs.player.pos.x = nx;
        gs.player.pos.y = ny;
        gs.monsterAt[ny][nx] = null; // player occupies this cell

        // Update field of view
        lg.revealRoom(gs.player.pos);

        // Report items on the ground
        Item item = gs.itemAt[ny][nx];
        if (item != null && item.type != GameData.O_GOLD) {
            gs.msg("You see " + item.getName(gs) + " here.");
        } else if (item != null && item.type == GameData.O_GOLD) {
            pickupGold(item);
        }

        // Report stairs
        if (gs.map[ny][nx] == GameData.STAIRS) {
            gs.msg("You see a staircase.");
        }

        // Trigger traps
        if (gs.map[ny][nx] == GameData.TRAP) {
            triggerTrap(nx, ny);
        }

        return !gs.dead;
    }

    /**
     * Automatically picks up a gold pile item: adds its value to the player's purse,
     * removes it from the floor, and posts a message.
     *
     * @param gold the gold item to pick up
     */
    private void pickupGold(Item gold) {
        gs.player.purse += gold.goldVal;
        gs.floorItems.remove(gold);
        gs.itemAt[gold.pos.y][gold.pos.x] = null;
        gs.map[gold.pos.y][gold.pos.x] = GameData.FLOOR;
        gs.msg("You pick up " + gold.goldVal + " gold pieces.");
    }

    /**
     * Finds the trap at the given position and applies its effect to the player.
     * Reveals the trap tile once triggered.
     *
     * @param x the column of the trap
     * @param y the row of the trap
     */
    private void triggerTrap(int x, int y) {
        for (int i = 0; i < gs.numTraps; i++) {
            if (gs.traps[i][0] == y && gs.traps[i][1] == x) {
                gs.flags[y][x] |= GameData.F_SEEN; // reveal the trap
                int type = gs.traps[i][2];
                switch (type) {
                    case GameData.T_DOOR:
                        gs.msg("You fell through a trapdoor!");
                        descend();
                        break;
                    case GameData.T_ARROW:
                        int dmg = GameData.roll(1, 6);
                        gs.player.stats.hpt -= dmg;
                        gs.msg("An arrow hits you for " + dmg + " damage.");
                        checkDeath("an arrow trap");
                        break;
                    case GameData.T_SLEEP:
                        gs.msg("A strange gas surrounds you. You fall asleep.");
                        gs.player.setFlag(GameData.ISHELD);
                        break;
                    case GameData.T_BEAR:
                        gs.msg("You step in a beartrap!");
                        gs.player.setFlag(GameData.ISHELD);
                        break;
                    case GameData.T_TELEP:
                        teleportPlayer();
                        gs.msg("You are teleported!");
                        break;
                    case GameData.T_DART:
                        dmg = GameData.roll(1, 4);
                        gs.player.stats.hpt -= dmg;
                        gs.player.stats.str = Math.max(3, gs.player.stats.str - 1);
                        gs.msg("A poison dart hits you!");
                        checkDeath("a dart trap");
                        break;
                    case GameData.T_RUST:
                        if (gs.player.armor != null
                                && (gs.player.armor.flags & GameData.ISPROT) == 0) {
                            gs.player.armor.arm--;
                            gs.player.stats.arm++;
                        }
                        gs.msg("A rust trap hits your armor!");
                        break;
                    default:
                        gs.msg("You stumble on a mysterious trap.");
                        break;
                }
            }
        }
    }

    // ── Combat ──────────────────────────────────────────────────────────

    /**
     * Resolves a player melee attack against the given monster.
     * A hit roll (d20 + level bonus) versus the monster's armor class determines success.
     * On a hit, damage is computed from the player's weapon and strength bonus.
     * If the monster is reduced to 0 HP it is killed.
     *
     * @param m the monster being attacked
     * @return false if the player died from a counter-effect, true otherwise
     */
    private boolean attackMonster(Creature m) {
        int monAC  = m.stats.arm;
        int bonus  = gs.player.stats.lvl;
        int hitRoll = rng.nextInt(20) + 1 + bonus;
        if (hitRoll >= monAC + 10) {
            int dmg = calcPlayerDamage();
            m.stats.hpt -= dmg;
            gs.msg("You hit the " + monName(m) + ".");
            if (m.stats.hpt <= 0) {
                killMonster(m);
                return !gs.dead;
            }
            m.setFlag(GameData.ISRUN); // wounded monster will chase the player
        } else {
            gs.msg("You miss the " + monName(m) + ".");
        }
        return true;
    }

    /**
     * Calculates the damage the player deals in a melee attack.
     * Uses the wielded weapon's dice (or 1d4 for bare hands) plus a strength bonus.
     *
     * @return the damage amount, minimum 1
     */
    private int calcPlayerDamage() {
        int dmg;
        if (gs.player.weapon != null) {
            int[] dice = GameData.WEAPON_DMG[gs.player.weapon.which];
            dmg = GameData.roll(dice[0], dice[1]);
            dmg += gs.player.weapon.dplus;
        } else {
            dmg = GameData.roll(1, 4); // bare hands
        }
        dmg += strengthBonus(gs.player.stats.str);
        return Math.max(1, dmg);
    }

    /**
     * Returns the melee damage bonus (or penalty) that corresponds to a given strength score.
     * Mirrors the classic Rogue strength-to-bonus table.
     *
     * @param str the strength score
     * @return the damage modifier
     */
    private int strengthBonus(int str) {
        if (str <= 6)  return -3;
        if (str <= 8)  return -2;
        if (str <= 12) return -1;
        if (str <= 15) return  0;
        if (str <= 17) return  1;
        if (str == 18) return  2;
        return 3;
    }

    /**
     * Handles all consequences of a monster being reduced to 0 HP:
     * awards experience to the player (with level-up check), possibly drops loot,
     * and removes the monster from the map and monster list.
     *
     * @param m the monster that has just been killed
     */
    private void killMonster(Creature m) {
        String name = monName(m);
        gs.msg("You defeated the " + name + "! (+" + m.stats.exp + "xp)");
        String lvlUp = gs.addExp(m.stats.exp);
        if (lvlUp != null) gs.addMsg(lvlUp);

        // Possibly drop loot based on the monster's carry percentage
        GameData.MonsterInfo mi = GameData.MONSTERS[m.type - 'A'];
        if (rng.nextInt(100) < mi.carry) {
            dropMonsterLoot(m);
        }

        gs.monsterAt[m.pos.y][m.pos.x] = null;
        gs.monsters.remove(m);
    }

    /**
     * Generates a random item and places it at the position where the monster died,
     * if that cell is currently empty.
     *
     * @param m the monster that dropped the loot
     */
    private void dropMonsterLoot(Creature m) {
        Item drop = lg.generateItem();
        if (drop == null) return;
        drop.pos = m.pos.copy();
        if (gs.itemAt[m.pos.y][m.pos.x] == null) {
            gs.itemAt[m.pos.y][m.pos.x] = drop;
            gs.map[m.pos.y][m.pos.x]    = drop.displayChar();
            gs.floorItems.add(drop);
        }
    }

    /**
     * Resolves a monster's melee attack against the player.
     * Uses the monster's level as its hit bonus and applies any special effect
     * the monster type may have (rust, freeze, theft, etc.).
     *
     * @param m the attacking monster
     * @return false if the player died from this attack, true otherwise
     */
    private boolean monsterAttackPlayer(Creature m) {
        int playerAC = gs.player.stats.arm;
        int bonus    = m.stats.lvl;
        int hitRoll  = rng.nextInt(20) + 1 + bonus;
        if (hitRoll >= playerAC + 10) {
            int dmg = monDamage(m);
            gs.player.stats.hpt -= dmg;
            applyMonsterSpecial(m, dmg);
            if (gs.player.stats.hpt <= 0) {
                gs.deathMsg = "killed by a " + monName(m);
                gs.dead     = true;
                gs.playing  = false;
                return false;
            }
        }
        return true;
    }

    /**
     * Rolls the damage for a single monster attack using the monster's damage string.
     *
     * @param m the attacking monster
     * @return the damage dealt, minimum 1
     */
    private int monDamage(Creature m) {
        return Math.max(1, GameData.parseDamage(m.stats.dmg));
    }

    /**
     * Applies any special on-hit effect unique to this monster type.
     * For example: aquators rust armor, ice monsters freeze the player,
     * leprechauns steal gold, nymphs steal items, and vampires drain max HP.
     *
     * @param m   the attacking monster
     * @param dmg the damage already dealt (informational, for possible scaling)
     */
    private void applyMonsterSpecial(Creature m, int dmg) {
        switch (m.type) {
            case 'A': // Aquator – rusts the player's armor
                if (gs.player.armor != null
                        && (gs.player.armor.flags & GameData.ISPROT) == 0) {
                    gs.player.armor.arm--;
                    gs.player.stats.arm++;
                    gs.msg("Your armor weakens!");
                }
                break;
            case 'I': // Ice monster – freezes (paralyzes) the player
                gs.player.setFlag(GameData.ISHELD);
                gs.msg("You are frozen!");
                break;
            case 'L': // Leprechaun – steals gold proportional to dungeon level
                int stolen = Math.min(gs.player.purse, rng.nextInt(gs.level * 10) + 1);
                gs.player.purse -= stolen;
                gs.msg("The leprechaun stole some gold!");
                break;
            case 'N': // Nymph – steals a random item from the pack
                stealItem();
                break;
            case 'V': // Vampire – 50% chance to drain one max-HP point
                if (rng.nextBoolean()) {
                    gs.player.stats.maxHp = Math.max(1, gs.player.stats.maxHp - 1);
                    gs.player.stats.hpt   = Math.min(gs.player.stats.hpt, gs.player.stats.maxHp);
                    gs.msg("The vampire drains your life!");
                }
                break;
        }
    }

    /**
     * Removes a random item from the player's pack (simulating a nymph stealing it),
     * unequipping it if it was the active weapon or armor, and posting a message.
     */
    private void stealItem() {
        if (gs.player.pack.isEmpty()) return;
        int idx    = rng.nextInt(gs.player.pack.size());
        Item stolen = gs.player.pack.get(idx);
        gs.player.pack.remove(idx);
        if (stolen == gs.player.weapon) gs.player.weapon = null;
        if (stolen == gs.player.armor)  gs.player.armor  = null;
        gs.msg("The nymph stole " + stolen.getName(gs) + "!");
    }

    // ── Monster AI ──────────────────────────────────────────────────────

    /**
     * Processes one movement tick for every monster on the current level.
     * Monsters that are held skip their turn. Slowed monsters skip every other turn.
     * Monsters that can see the player (or are already chasing) chase and attack;
     * others may wander randomly with a small probability.
     */
    public void moveMonsters() {
        List<Creature> toMove = new ArrayList<>(gs.monsters);
        for (Creature m : toMove) {
            if (!gs.monsters.contains(m)) continue;
            if (m.hasFlag(GameData.ISHELD)) { m.clearFlag(GameData.ISHELD); continue; }
            if (m.hasFlag(GameData.ISSLOW) && m.turn) { m.turn = false; continue; }
            m.turn = true;

            boolean canSeePlayer = canSee(m, gs.player.pos);
            if (canSeePlayer || m.hasFlag(GameData.ISRUN)) {
                m.setFlag(GameData.ISRUN);
                chasePlayer(m);
            } else if (rng.nextInt(100) < 5) {
                wanderMonster(m); // small chance to wander randomly
            }
        }
    }

    /**
     * Returns true if the monster {@code m} can see the target coordinate.
     * In a lit room both share, visibility extends up to 16 tiles.
     * In corridors or dark rooms, only adjacent tiles (≤3) are visible.
     *
     * @param m      the observer monster
     * @param target the position being checked for visibility
     * @return true if m can see the target
     */
    private boolean canSee(Creature m, Coord target) {
        int dist2 = (m.pos.x - target.x) * (m.pos.x - target.x)
                  + (m.pos.y - target.y) * (m.pos.y - target.y);
        Room mr = gs.roomAt(m.pos.x, m.pos.y);
        Room pr = gs.roomAt(target.x, target.y);
        if (mr != null && mr == pr && !mr.dark()) return dist2 <= 16 * 16;
        return dist2 <= 3 * 3; // passage/dark room: adjacent only
    }

    /**
     * Moves a monster one step toward the player, attacking if adjacent.
     * Tries a diagonal move first, then falls back to cardinal directions.
     *
     * @param m the chasing monster
     */
    private void chasePlayer(Creature m) {
        int px = gs.player.pos.x, py = gs.player.pos.y;
        int mx = m.pos.x,         my = m.pos.y;

        // Attack if adjacent (but not same cell)
        if (Math.abs(px - mx) <= 1 && Math.abs(py - my) <= 1 && (px != mx || py != my)) {
            monsterAttackPlayer(m);
            return;
        }

        // Prefer diagonal approach, then axis-aligned
        int dx = Integer.signum(px - mx);
        int dy = Integer.signum(py - my);
        int[][] dirs = { {dx, dy}, {dx, 0}, {0, dy}, {-dx, dy}, {dx, -dy} };
        for (int[] d : dirs) {
            if (d[0] == 0 && d[1] == 0) continue;
            if (tryMonsterMove(m, mx + d[0], my + d[1])) break;
        }
    }

    /**
     * Moves a monster in a random direction (for idle wandering).
     * Tries all 8 directions starting from a random offset.
     *
     * @param m the wandering monster
     */
    private void wanderMonster(Creature m) {
        int[] dxs = { 1, -1,  0, 0,  1, -1,  1, -1 };
        int[] dys = { 0,  0,  1, -1, 1, -1, -1,  1 };
        int start = rng.nextInt(8);
        for (int i = 0; i < 8; i++) {
            int ni = (start + i) % 8;
            if (tryMonsterMove(m, m.pos.x + dxs[ni], m.pos.y + dys[ni])) return;
        }
    }

    /**
     * Attempts to move a monster to the given target cell.
     * The move succeeds only if the cell is in bounds, walkable, not occupied by
     * another monster, and not the player's position.
     *
     * @param m  the monster to move
     * @param nx target column
     * @param ny target row
     * @return true if the move succeeded
     */
    private boolean tryMonsterMove(Creature m, int nx, int ny) {
        if (nx <= 0 || nx >= GameData.NUMCOLS - 1 || ny <= 0 || ny >= GameData.NUMLINES - 1)
            return false;
        if (!gs.isWalkable(nx, ny)) return false;
        if (gs.monsterAt[ny][nx] != null) return false;
        if (nx == gs.player.pos.x && ny == gs.player.pos.y) return false;

        gs.monsterAt[m.pos.y][m.pos.x] = null;
        m.pos.x = nx;
        m.pos.y = ny;
        gs.monsterAt[ny][nx] = m;
        m.room = gs.roomAt(nx, ny);
        return true;
    }

    // ── Item Actions ────────────────────────────────────────────────────

    /**
     * Picks up the item at the player's current position.
     * Gold is auto-collected. Other items are added to the pack with a new inventory letter.
     * Reports an error if there is nothing here or the pack is full.
     */
    public void pickupItem() {
        int x = gs.player.pos.x, y = gs.player.pos.y;
        Item item = gs.itemAt[y][x];
        if (item == null) { gs.msg("Nothing here to pick up."); return; }
        if (gs.player.pack.size() >= GameData.MAXPACK) { gs.msg("Your pack is full!"); return; }

        if (item.type == GameData.O_GOLD) {
            pickupGold(item);
            return;
        }

        char ch = gs.nextPackChar();
        if (ch == 0) { gs.msg("Your pack is too full!"); return; }
        item.packCh = ch;
        gs.player.pack.add(item);
        gs.floorItems.remove(item);
        gs.itemAt[y][x] = null;
        gs.map[y][x] = GameData.FLOOR;
        gs.msg("You pick up " + item.getName(gs) + ".");
    }

    /**
     * Drops the pack item identified by the given inventory letter at the player's feet.
     * Unequips the item first if it was the active weapon or armor.
     *
     * @param ch the inventory letter of the item to drop
     */
    public void dropItem(char ch) {
        Item item = findPackItem(ch);
        if (item == null) { gs.msg("You don't have that."); return; }

        int x = gs.player.pos.x, y = gs.player.pos.y;
        if (gs.itemAt[y][x] != null) { gs.msg("Something is already here."); return; }

        gs.player.pack.remove(item);
        if (item == gs.player.weapon) gs.player.weapon = null;
        if (item == gs.player.armor)  gs.player.armor  = null;

        item.pos = new Coord(x, y);
        gs.itemAt[y][x]  = item;
        gs.map[y][x]     = item.displayChar();
        gs.floorItems.add(item);
        gs.msg("Dropped " + item.getName(gs) + ".");
    }

    /**
     * Quaffs (drinks) the potion identified by the given inventory letter.
     * Marks the potion type as identified and applies its effect.
     *
     * @param ch the inventory letter of the potion to quaff
     */
    public void quaffPotion(char ch) {
        Item item = findPackItemType(ch, GameData.O_POTION);
        if (item == null) { gs.msg("You don't have a potion with that letter."); return; }
        gs.player.pack.remove(item);
        gs.potionKnown[item.which] = true;
        applyPotion(item.which);
    }

    /**
     * Applies the effect of a potion to the player, switching on the potion's sub-type index.
     *
     * @param which the potion sub-type index (0..13)
     */
    private void applyPotion(int which) {
        switch (which) {
            case 0:  gs.player.setFlag(GameData.ISHUH);    gs.msg("You feel confused."); break;
            case 1:  gs.player.setFlag(GameData.ISHALU);   gs.msg("Oh wow! The colors!"); break;
            case 2:
                gs.player.stats.str = Math.max(3, gs.player.stats.str - 1);
                gs.msg("You feel very sick.");
                break;
            case 3:
                gs.player.stats.str    = Math.min(31, gs.player.stats.str + 1);
                gs.player.stats.maxStr = Math.max(gs.player.stats.maxStr, gs.player.stats.str);
                gs.msg("You feel stronger.");
                break;
            case 4:  gs.player.setFlag(GameData.CANSEE);   gs.msg("Your eyes tingle."); break;
            case 5: {
                int h = GameData.roll(gs.player.stats.lvl, 8);
                gs.player.stats.hpt = Math.min(gs.player.stats.maxHp, gs.player.stats.hpt + h);
                gs.msg("You feel better.");
                break;
            }
            case 6:
                gs.msg("You sense monsters nearby.");
                for (Creature m : gs.monsters) m.setFlag(GameData.ISFOUND);
                break;
            case 7:  gs.msg("You sense magic items nearby."); break;
            case 8: {
                String r = gs.addExp(gs.player.stats.exp + 1);
                if (r != null) gs.msg(r);
                else gs.msg("You feel more experienced!");
                break;
            }
            case 9: {
                int h2 = GameData.roll(gs.player.stats.lvl, 8) * 2;
                gs.player.stats.hpt = Math.min(gs.player.stats.maxHp, gs.player.stats.hpt + h2);
                gs.msg("You feel much better.");
                break;
            }
            case 10: gs.player.setFlag(GameData.ISHASTE); gs.msg("You feel hasty!"); break;
            case 11:
                gs.player.stats.str = gs.player.stats.maxStr;
                gs.msg("Your strength returns.");
                break;
            case 12: gs.player.setFlag(GameData.ISBLIND);  gs.msg("You can't see anything!"); break;
            case 13: gs.player.setFlag(GameData.ISLEVIT);  gs.msg("You begin to float upward."); break;
        }
        doAfterPotion();
    }

    /**
     * Placeholder for any cleanup or follow-up logic performed after quaffing a potion.
     * Currently unused but kept for future expansion.
     */
    private void doAfterPotion() {
        // reserved for future timed effects
    }

    /**
     * Reads (uses) the scroll identified by the given inventory letter.
     * Marks the scroll type as identified and applies its effect.
     *
     * @param ch the inventory letter of the scroll to read
     */
    public void readScroll(char ch) {
        Item item = findPackItemType(ch, GameData.O_SCROLL);
        if (item == null) { gs.msg("You don't have a scroll with that letter."); return; }
        gs.player.pack.remove(item);
        gs.scrollKnown[item.which] = true;
        applyScroll(item.which);
    }

    /**
     * Applies the effect of a scroll to the current game state,
     * switching on the scroll's sub-type index.
     *
     * @param which the scroll sub-type index (0..17)
     */
    private void applyScroll(int which) {
        switch (which) {
            case 0:  gs.player.setFlag(GameData.CANHUH);   gs.msg("You feel a surge of power."); break;
            case 1:  doMagicMap();                          gs.msg("A map appears in your mind!"); break;
            case 2:
                for (Creature m : gs.monsters)
                    if (canSee(gs.player, m.pos)) m.setFlag(GameData.ISHELD);
                gs.msg("The monsters freeze!");
                break;
            case 3:
                for (Creature m : gs.monsters) m.setFlag(GameData.ISHELD);
                gs.msg("You feel drowsy...");
                break;
            case 4:
                if (gs.player.armor != null) {
                    gs.player.armor.arm++;
                    gs.player.stats.arm--;
                    gs.msg("Your armor glows blue.");
                } else gs.msg("Nothing happens.");
                break;
            case 5:  gs.potionKnown[rng.nextInt(14)] = true;  gs.msg("You identify a potion."); break;
            case 6:  gs.scrollKnown[rng.nextInt(18)] = true;  gs.msg("You identify a scroll."); break;
            case 7:
                if (gs.player.weapon != null) {
                    gs.player.weapon.flags |= GameData.ISKNOW;
                    gs.msg("You identify your weapon.");
                }
                break;
            case 8:
                if (gs.player.armor != null) {
                    gs.player.armor.flags |= GameData.ISKNOW;
                    gs.msg("You identify your armor.");
                }
                break;
            case 9:
                if (gs.player.rings[0] != null) gs.ringKnown[gs.player.rings[0].which] = true;
                if (gs.player.rings[1] != null) gs.ringKnown[gs.player.rings[1].which] = true;
                gs.msg("You identify your ring/wand/staff.");
                break;
            case 10:
                gs.msg("The monsters are scared!");
                for (Creature m : gs.monsters) m.clearFlag(GameData.ISRUN);
                break;
            case 11:
                gs.msg("You detect food.");
                for (Item it : gs.floorItems)
                    if (it.type == GameData.O_FOOD) gs.flags[it.pos.y][it.pos.x] |= GameData.F_SEEN;
                break;
            case 12: teleportPlayer(); gs.msg("You teleport!"); break;
            case 13:
                if (gs.player.weapon != null) {
                    gs.player.weapon.hplus += rng.nextInt(3) + 1;
                    gs.msg("Your weapon glows blue.");
                }
                break;
            case 14:
                for (int i = 0; i < rng.nextInt(3) + 1; i++) createMonster();
                gs.msg("A monster appears!");
                break;
            case 15:
                for (Item it : gs.player.pack) it.flags &= ~GameData.ISCURSED;
                gs.msg("You feel a lifting of the curse.");
                break;
            case 16:
                for (Creature m : gs.monsters) m.setFlag(GameData.ISRUN);
                gs.msg("The monsters get aggravated!");
                break;
            case 17:
                if (gs.player.armor != null) {
                    gs.player.armor.flags |= GameData.ISPROT;
                    gs.msg("Your armor glows gold.");
                }
                break;
            default: gs.msg("Nothing seems to happen."); break;
        }
    }

    /**
     * Reveals the entire map (except hidden traps) in the player's memory,
     * simulating the effect of a scroll of magic mapping.
     */
    private void doMagicMap() {
        for (int y = 0; y < GameData.NUMLINES; y++)
            for (int x = 0; x < GameData.NUMCOLS; x++)
                if (gs.map[y][x] != GameData.TRAP)
                    gs.flags[y][x] |= GameData.F_SEEN;
    }

    /**
     * Spawns a new monster adjacent to the player (within 1 tile in any direction).
     * The monster is immediately set to chase mode.
     * Used by the scroll of create monster.
     */
    private void createMonster() {
        for (int attempt = 0; attempt < 20; attempt++) {
            int dx = rng.nextInt(3) - 1, dy = rng.nextInt(3) - 1;
            if (dx == 0 && dy == 0) continue;
            int nx = gs.player.pos.x + dx, ny = gs.player.pos.y + dy;
            if (nx > 0 && nx < GameData.NUMCOLS - 1 && ny > 0 && ny < GameData.NUMLINES - 1
                    && gs.isWalkable(nx, ny) && gs.monsterAt[ny][nx] == null) {
                char type = (char) ('A' + rng.nextInt(26));
                Creature m = new Creature(type, new Coord(nx, ny));
                m.stats.lvl  = 1 + rng.nextInt(gs.level);
                m.stats.hpt  = m.stats.maxHp = GameData.roll(m.stats.lvl, 8);
                m.stats.arm  = 5;
                m.stats.dmg  = "1x4";
                m.setFlag(GameData.ISRUN);
                gs.monsters.add(m);
                gs.monsterAt[ny][nx] = m;
                return;
            }
        }
    }

    /**
     * Equips the weapon identified by the given inventory letter.
     * Refuses if the current weapon is cursed.
     *
     * @param ch the inventory letter of the weapon to wield
     */
    public void wieldWeapon(char ch) {
        Item item = findPackItem(ch);
        if (item == null) { gs.msg("You don't have that."); return; }
        if (item.type != GameData.O_WEAPON) { gs.msg("That's not a weapon."); return; }
        if (gs.player.weapon != null && (gs.player.weapon.flags & GameData.ISCURSED) != 0) {
            gs.msg("Your current weapon is cursed!");
            return;
        }
        gs.player.weapon = item;
        gs.msg("You are now wielding " + item.getName(gs) + ".");
    }

    /**
     * Equips the armor identified by the given inventory letter.
     * Removes any previously worn armor first (if not cursed).
     * Updates the player's AC to match the new armor's class.
     *
     * @param ch the inventory letter of the armor to wear
     */
    public void wearArmor(char ch) {
        Item item = findPackItem(ch);
        if (item == null) { gs.msg("You don't have that."); return; }
        if (item.type != GameData.O_ARMOR) { gs.msg("That's not armor."); return; }
        if (gs.player.armor != null) {
            if ((gs.player.armor.flags & GameData.ISCURSED) != 0) {
                gs.msg("Your armor is cursed!");
                return;
            }
            gs.player.armor = null;
        }
        gs.player.armor  = item;
        int ac           = GameData.ARMOR_CLASS[item.which] - item.arm;
        gs.player.stats.arm = ac;
        gs.msg("You put on " + item.getName(gs) + ".");
    }

    /**
     * Removes the player's currently worn armor, returning AC to the unarmored default (10).
     * Refuses if the armor is cursed.
     */
    public void takeOffArmor() {
        if (gs.player.armor == null) { gs.msg("You have no armor on."); return; }
        if ((gs.player.armor.flags & GameData.ISCURSED) != 0) {
            gs.msg("Your armor is cursed!");
            return;
        }
        gs.msg("You take off " + gs.player.armor.getName(gs) + ".");
        gs.player.armor     = null;
        gs.player.stats.arm = 10; // unarmored AC
    }

    /**
     * Eats the food item identified by the given inventory letter.
     * Restores the hunger counter to full and resets the hungry state.
     *
     * @param ch the inventory letter of the food to eat
     */
    public void eatFood(char ch) {
        Item item = findPackItemType(ch, GameData.O_FOOD);
        if (item == null) { gs.msg("You don't have food with that letter."); return; }
        gs.player.pack.remove(item);
        gs.player.foodLeft    = 2000;
        gs.player.hungryState = 0;
        if (item.which == 1) gs.msg("My, that slime-mold is delicious!");
        else                  gs.msg("Yum, that was tasty!");
    }

    /**
     * Descends the staircase at the player's current position.
     * Reports an error if there is no staircase here.
     */
    public void goDownStairs() {
        if (gs.map[gs.player.pos.y][gs.player.pos.x] != GameData.STAIRS) {
            gs.msg("You see no staircase here.");
            return;
        }
        descend();
    }

    /**
     * Generates a new dungeon level and moves the player down to it.
     * Increments the level counter and updates the deepest-level record.
     */
    private void descend() {
        gs.level++;
        gs.maxLevel = Math.max(gs.maxLevel, gs.level);
        lg.generateLevel();
        gs.msg("You descend to dungeon level " + gs.level + ".");
    }

    /**
     * Searches the eight cells adjacent to the player for hidden traps.
     * Reveals any found traps and posts a message for each one.
     * Posts "You find nothing." if no traps were found.
     */
    public void search() {
        int x = gs.player.pos.x, y = gs.player.pos.y;
        boolean found = false;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int ny = y + dy, nx = x + dx;
                if (ny < 0 || ny >= GameData.NUMLINES || nx < 0 || nx >= GameData.NUMCOLS)
                    continue;
                for (int i = 0; i < gs.numTraps; i++) {
                    if (gs.traps[i][0] == ny && gs.traps[i][1] == nx
                            && (gs.flags[ny][nx] & GameData.F_SEEN) == 0) {
                        gs.flags[ny][nx] |= GameData.F_SEEN;
                        gs.msg("You find " + GameData.TR_NAME[gs.traps[i][2]] + "!");
                        found = true;
                    }
                }
            }
        }
        if (!found) gs.msg("You find nothing.");
    }

    /**
     * Teleports the player to a random walkable, unoccupied tile on the current level.
     * Tries up to 100 times before giving up silently.
     */
    private void teleportPlayer() {
        for (int attempt = 0; attempt < 100; attempt++) {
            int nx = 1 + rng.nextInt(GameData.NUMCOLS - 2);
            int ny = 1 + rng.nextInt(GameData.NUMLINES - 2);
            if (gs.isWalkable(nx, ny) && gs.monsterAt[ny][nx] == null) {
                gs.player.pos.x = nx;
                gs.player.pos.y = ny;
                lg.revealRoom(gs.player.pos);
                return;
            }
        }
    }

    // ── Turn end ──────────────────────────────────────────────────────────

    /**
     * Processes all end-of-turn effects: hunger countdown, player HP regeneration,
     * monster HP regeneration, temporary flag expiry, monster movement, and
     * wandering monster spawning.
     * Does nothing if the player is already dead.
     */
    public void endTurn() {
        if (gs.dead) return;
        gs.turnCount++;

        // Hunger: decrement food and apply escalating penalties
        gs.player.foodLeft--;
        if (gs.player.foodLeft <= 0) {
            gs.player.stats.hpt--;
            if (gs.player.stats.hpt <= 0) {
                gs.deathMsg = "starvation";
                gs.dead     = true;
                gs.playing  = false;
                return;
            }
            if (gs.player.hungryState < 3) gs.player.hungryState = 3;
        } else if (gs.player.foodLeft < 50  && gs.player.hungryState < 3) {
            gs.player.hungryState = 3;
            gs.msg("You are fainting from hunger!");
        } else if (gs.player.foodLeft < 200 && gs.player.hungryState < 2) {
            gs.player.hungryState = 2;
            gs.msg("You are getting weak from hunger.");
        } else if (gs.player.foodLeft < 500 && gs.player.hungryState < 1) {
            gs.player.hungryState = 1;
            gs.msg("You are hungry.");
        }

        // Player HP regeneration: +1 HP every 30 turns if below maximum
        if (gs.turnCount % 30 == 0 && gs.player.stats.hpt < gs.player.stats.maxHp) {
            gs.player.stats.hpt++;
        }

        // Monster regeneration: ISREGEN monsters may heal each turn
        for (Creature m : gs.monsters) {
            if (m.hasFlag(GameData.ISREGEN) && rng.nextInt(20) == 0)
                m.stats.hpt = Math.min(m.stats.maxHp, m.stats.hpt + 1);
        }

        // Clear temporary hold/haste flags every 20 turns
        if (gs.turnCount % 20 == 0) {
            gs.player.clearFlag(GameData.ISHELD);
            gs.player.clearFlag(GameData.ISHASTE);
        }

        // All monsters take their turn
        moveMonsters();

        // Periodically spawn a wandering monster far from the player
        gs.wanderTimer--;
        if (gs.wanderTimer <= 0) {
            gs.wanderTimer = 100 + rng.nextInt(50);
            spawnWanderer();
        }
    }

    /**
     * Attempts to spawn a new wandering monster at a random walkable tile that is
     * at least 10 tiles away from the player. Tries up to 20 times.
     */
    private void spawnWanderer() {
        for (int attempt = 0; attempt < 20; attempt++) {
            int nx = 1 + rng.nextInt(GameData.NUMCOLS - 2);
            int ny = 1 + rng.nextInt(GameData.NUMLINES - 2);
            if (gs.isWalkable(nx, ny) && gs.monsterAt[ny][nx] == null) {
                int dist2 = (nx - gs.player.pos.x) * (nx - gs.player.pos.x)
                          + (ny - gs.player.pos.y) * (ny - gs.player.pos.y);
                if (dist2 > 100) {
                    char type = (char) ('A' + rng.nextInt(26));
                    Creature m = new Creature(type, new Coord(nx, ny));
                    m.stats.lvl = 1 + rng.nextInt(Math.max(1, gs.level));
                    m.stats.hpt = m.stats.maxHp = GameData.roll(m.stats.lvl, 8);
                    m.stats.arm = 5;
                    m.stats.dmg = "1x4";
                    gs.monsters.add(m);
                    gs.monsterAt[ny][nx] = m;
                    return;
                }
            }
        }
    }

    /**
     * Checks whether the player's HP has dropped to 0 or below, and if so
     * marks them as dead with the given cause-of-death string.
     *
     * @param cause a description of what killed the player (e.g., "an arrow trap")
     */
    private void checkDeath(String cause) {
        if (gs.player.stats.hpt <= 0) {
            gs.deathMsg = "killed by " + cause;
            gs.dead     = true;
            gs.playing  = false;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /**
     * Returns the display name of a monster based on its type character.
     *
     * @param m the monster
     * @return the monster's name from {@link GameData#MONSTERS}, or "creature" if unknown
     */
    private String monName(Creature m) {
        int idx = m.type - 'A';
        if (idx >= 0 && idx < GameData.MONSTERS.length) return GameData.MONSTERS[idx].name;
        return "creature";
    }

    /**
     * Returns true if the given source coordinate can "see" the target coordinate
     * (used for player-based line-of-sight checks, within 8 tiles).
     *
     * @param from the observer position
     * @param to   the target position
     * @return true if the two positions are within 8 tiles of each other
     */
    private boolean canSee(Coord from, Coord to) {
        int dist2 = (from.x - to.x) * (from.x - to.x)
                  + (from.y - to.y) * (from.y - to.y);
        return dist2 <= 8 * 8;
    }

    /**
     * Finds the item in the player's pack that has the given inventory letter.
     *
     * @param ch the pack character to look up
     * @return the matching Item, or null if not found
     */
    public Item findPackItem(char ch) {
        for (Item it : gs.player.pack) if (it.packCh == ch) return it;
        return null;
    }

    /**
     * Finds the item in the player's pack with the given inventory letter AND item type.
     * If {@code ch} is 0, returns the first item of the matching type regardless of letter.
     *
     * @param ch   the pack character to look up (or 0 for any)
     * @param type the required item type (O_POTION, O_SCROLL, etc.)
     * @return the matching Item, or null if not found
     */
    public Item findPackItemType(char ch, int type) {
        for (Item it : gs.player.pack) if (it.packCh == ch && it.type == type) return it;
        if (ch == 0) {
            for (Item it : gs.player.pack) if (it.type == type) return it;
        }
        return null;
    }
}
