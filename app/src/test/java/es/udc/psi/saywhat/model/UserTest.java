package es.udc.psi.saywhat.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UserTest {

    @Test
    public void constructor_setsIdentityFields() {
        User user = new User("Ana", "ana@example.com", "uid-1");

        assertEquals("Ana", user.getName());
        assertEquals("ana@example.com", user.getEmail());
        assertEquals("uid-1", user.getUid());
    }

    @Test
    public void constructor_startsWithZeroHighScore() {
        User user = new User("Ana", "ana@example.com", "uid-1");

        assertEquals(0, user.getHighScore());
    }

    @Test
    public void constructor_startsWithEmptyDifficultyScores() {
        User user = new User("Ana", "ana@example.com", "uid-1");

        assertNotNull(user.getDifficultyScores());
        assertTrue(user.getDifficultyScores().isEmpty());
    }

    @Test
    public void noArgConstructor_requiredByFirestore_leavesFieldsUnset() {
        User user = new User();

        assertNull(user.getUid());
        assertNull(user.getName());
        assertNull(user.getEmail());
        assertEquals(0, user.getHighScore());
        assertNotNull(user.getDifficultyScores());
    }

    @Test
    public void setters_updateValues() {
        User user = new User();

        user.setUid("uid-2");
        user.setName("Rui");
        user.setHighScore(42);

        assertEquals("uid-2", user.getUid());
        assertEquals("Rui", user.getName());
        assertEquals(42, user.getHighScore());
    }

    @Test
    public void difficultyScores_isMutableAndKeepsEntries() {
        User user = new User("Ana", "ana@example.com", "uid-1");

        user.getDifficultyScores().put("easy", 10);
        user.getDifficultyScores().put("hard", 3);

        assertEquals(Integer.valueOf(10), user.getDifficultyScores().get("easy"));
        assertEquals(Integer.valueOf(3), user.getDifficultyScores().get("hard"));
        assertEquals(2, user.getDifficultyScores().size());
    }

    @Test
    public void difficultyScores_areNotSharedBetweenInstances() {
        User first = new User("Ana", "ana@example.com", "uid-1");
        User second = new User("Rui", "rui@example.com", "uid-2");

        first.getDifficultyScores().put("easy", 10);

        assertTrue(second.getDifficultyScores().isEmpty());
    }
}
