package rogue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for RogueController.
 *
 * These tests start the full Spring Boot application context (including
 * the embedded Tomcat) and exercise the REST API end-to-end.
 *
 * Run with:  mvn test
 */
@SpringBootTest
@AutoConfigureMockMvc
class RogueControllerTest {

    @Autowired
    private MockMvc mvc;

    /** Health endpoint must return HTTP 200 and status=UP */
    @Test
    void healthEndpointReturnsUp() throws Exception {
        mvc.perform(get("/actuator/health"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("UP"));
    }

    /** Starting a new game returns a valid state object */
    @Test
    void newGameReturnsState() throws Exception {
        mvc.perform(post("/api/new"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.map").isArray())
           .andExpect(jsonPath("$.player").exists())
           .andExpect(jsonPath("$.level").value(1))
           .andExpect(jsonPath("$.dead").value(false))
           .andExpect(jsonPath("$.won").value(false));
    }

    /** GET /api/state auto-starts a game and returns a valid state */
    @Test
    void getStateAutoStartsGame() throws Exception {
        mvc.perform(get("/api/state"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.player.hp").isNumber())
           .andExpect(jsonPath("$.player.maxHp").isNumber());
    }

    /** Sending a movement key returns an updated state */
    @Test
    void movementKeyReturnsUpdatedState() throws Exception {
        // Ensure a game exists first
        mvc.perform(post("/api/new")).andExpect(status().isOk());

        // Send a movement key and check the response is a valid state
        mvc.perform(post("/api/key")
               .contentType(MediaType.TEXT_PLAIN)
               .content("h"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.map").isArray())
           .andExpect(jsonPath("$.message").isString());
    }

    /** Sending '?' opens the help popup */
    @Test
    void helpKeyOpensPopup() throws Exception {
        mvc.perform(post("/api/new")).andExpect(status().isOk());

        mvc.perform(post("/api/key")
               .contentType(MediaType.TEXT_PLAIN)
               .content("?"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.showingHelp").value(true))
           .andExpect(jsonPath("$.popupLines").isArray());
    }

    /** Dismiss popup with SPACE */
    @Test
    void spaceDismissesPopup() throws Exception {
        mvc.perform(post("/api/new")).andExpect(status().isOk());
        mvc.perform(post("/api/key").contentType(MediaType.TEXT_PLAIN).content("?"))
           .andExpect(status().isOk());

        mvc.perform(post("/api/key")
               .contentType(MediaType.TEXT_PLAIN)
               .content(" "))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.showingHelp").value(false));
    }

    /** Static frontend is served at / */
    @Test
    void frontendServedAtRoot() throws Exception {
        mvc.perform(get("/"))
           .andExpect(status().isOk())
           .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }
}
