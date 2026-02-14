package edu.eci.arsw.concurrencytest;

import edu.eci.arsw.concurrency.PauseController;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class PauseControllerTest {
    @Test
    public void testPauseAndResume() throws InterruptedException {
        PauseController controller = new PauseController();
        AtomicBoolean paused = new AtomicBoolean(false);
        Thread t = new Thread(() -> {
            try {
                controller.pause();
                paused.set(true);
                controller.resume();
            } catch (Exception e) {
                fail();
            }
        });
        t.start();
        t.join();
        assertTrue(paused.get());
    }

    @Test
    public void testPauseState() {
        PauseController controller = new PauseController();
        controller.pause();
        assertTrue(controller.isPaused());
        controller.resume();
        assertFalse(controller.isPaused());
    }
}
