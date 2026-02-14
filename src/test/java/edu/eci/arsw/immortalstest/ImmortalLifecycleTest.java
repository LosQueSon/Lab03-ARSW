package edu.eci.arsw.immortalstest;

import edu.eci.arsw.concurrency.PauseController;
import edu.eci.arsw.immortals.Immortal;
import edu.eci.arsw.immortals.ImmortalManager;
import edu.eci.arsw.immortals.ScoreBoard;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ImmortalLifecycleTest {

    @Test
    public void testManagerStartPauseResumeStopAndInvariant() throws Exception {
        try (ImmortalManager manager = new ImmortalManager(4, "ordered", 100, 10)) {
            // Los inmortales ya existen pero aún no están peleando
            assertEquals(4, manager.aliveCount());

            manager.start();
            Thread.sleep(50); // dejar que peleen un poco

            // Debe haber al menos un inmortal vivo
            assertTrue(manager.aliveCount() > 0);

            manager.pause();
            int aliveAtPause = manager.aliveCount();
            manager.controller().waitUntilPaused(aliveAtPause, 1000);

            long totalHealth = manager.totalHealth();
            assertEquals(4L * 100L, totalHealth);

            manager.resume();
            Thread.sleep(50);
        }
    }
}
