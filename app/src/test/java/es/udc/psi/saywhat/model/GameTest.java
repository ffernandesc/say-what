package es.udc.psi.saywhat.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameTest {

    @Test
    public void constructor_setsIdsAndStartsWaiting() {
        Game game = new Game("game-1", "host-1");

        assertEquals("host-1", game.getHostId());
        assertEquals("WAITING", game.getStatus());
    }

    @Test
    public void newGame_hasEmptyCollectionsAndDefaultButtons() {
        Game game = new Game("game-1", "host-1");

        assertTrue(game.getPlayers().isEmpty());
        assertTrue(game.getTurnOrder().isEmpty());
        assertTrue(game.getSequence().isEmpty());
        assertNull(game.getCurrentTurnUid());
        assertEquals(4, game.getButtonsUnlocked());
    }

    @Test
    public void noArgConstructor_requiredByFirestore_leavesStatusUnset() {
        Game game = new Game();

        assertNull(game.getHostId());
        assertNull(game.getStatus());
        assertNotNull(game.getPlayers());
        assertNotNull(game.getTurnOrder());
        assertNotNull(game.getSequence());
    }

    @Test
    public void setters_updateValues() {
        Game game = new Game("game-1", "host-1");
        List<String> order = Arrays.asList("a", "b", "c");
        List<Integer> sequence = Arrays.asList(1, 3, 2);

        game.setHostId("host-2");
        game.setStatus("IN_PROGRESS");
        game.setTurnOrder(order);
        game.setSequence(sequence);
        game.setCurrentTurnUid("b");

        assertEquals("host-2", game.getHostId());
        assertEquals("IN_PROGRESS", game.getStatus());
        assertEquals(order, game.getTurnOrder());
        assertEquals(sequence, game.getSequence());
        assertEquals("b", game.getCurrentTurnUid());
    }

    @Test
    public void players_canBeReplacedAndLookedUpByUid() {
        Game game = new Game("game-1", "host-1");
        Map<String, Game.PlayerInfo> players = new HashMap<>();
        Game.PlayerInfo ana = new Game.PlayerInfo("Ana", "ALIVE", 100L);
        players.put("uid-1", ana);

        game.setPlayers(players);

        assertEquals(1, game.getPlayers().size());
        assertSame(ana, game.getPlayers().get("uid-1"));
    }

    @Test
    public void sequence_isMutableInPlace() {
        Game game = new Game("game-1", "host-1");

        game.getSequence().add(2);
        game.getSequence().add(4);

        assertEquals(Arrays.asList(2, 4), game.getSequence());
    }

    @Test
    public void playerInfo_constructor_setsFields() {
        Game.PlayerInfo info = new Game.PlayerInfo("Ana", "ALIVE", 1234L);

        assertEquals("Ana", info.getName());
        assertEquals("ALIVE", info.getState());
        assertEquals(1234L, info.getJoinedAt());
        assertEquals(0L, info.getEliminationTime());
    }

    @Test
    public void playerInfo_noArgConstructor_hasDefaults() {
        Game.PlayerInfo info = new Game.PlayerInfo();

        assertNull(info.getName());
        assertNull(info.getState());
        assertEquals(0L, info.getJoinedAt());
        assertEquals(0L, info.getEliminationTime());
    }

    @Test
    public void playerInfo_setters_updateValues() {
        Game.PlayerInfo info = new Game.PlayerInfo();

        info.setName("Rui");
        info.setState("ELIMINATED");
        info.setJoinedAt(10L);
        info.setEliminationTime(99L);

        assertEquals("Rui", info.getName());
        assertEquals("ELIMINATED", info.getState());
        assertEquals(10L, info.getJoinedAt());
        assertEquals(99L, info.getEliminationTime());
    }
}
