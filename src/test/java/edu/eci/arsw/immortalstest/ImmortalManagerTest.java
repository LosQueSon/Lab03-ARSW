package edu.eci.arsw.immortalstest;

import edu.eci.arsw.immortals.ImmortalManager;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ImmortalManagerTest {
    @Test
    public void testManagerInitialization() {
        ImmortalManager manager = new ImmortalManager(2, "ordered", 100, 10);
        assertNotNull(manager);
    }
}
