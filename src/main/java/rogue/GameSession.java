package rogue;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

/**
 * GameSession – holds the live game objects for one browser session.
 *
 * Spring creates a fresh instance of this bean for every HTTP session
 * (i.e., every browser tab/window that visits the game).  Multiple
 * players can therefore use the same server simultaneously without
 * interfering with each other.
 *
 * Scoped to the HTTP session so it survives across multiple requests
 * from the same browser but is discarded when the session expires.
 */
@Component
@SessionScope
public class GameSession {

    /** Current game state for this session; null = no game started yet. */
    private GameState state;

    /** Game logic engine operating on {@link #state}. */
    private GameEngine engine;

    /**
     * Starts a brand-new game, replacing any existing game for this session.
     * Generates the first dungeon level and posts a welcome message.
     */
    public void newGame() {
        state  = new GameState();
        engine = new GameEngine(state);
        LevelGenerator lg = new LevelGenerator(state);
        lg.generateLevel();
        state.msg("Welcome to Rogue! Press ? for help.");
    }

    /**
     * Returns true when a game is currently in progress for this session.
     *
     * @return true if state is non-null
     */
    public boolean hasGame() { return state != null; }

    public GameState getState()  { return state; }
    public GameEngine getEngine() { return engine; }
}
