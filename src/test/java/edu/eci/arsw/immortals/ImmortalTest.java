package edu.eci.arsw.immortals;

import edu.eci.arsw.immortals.Immortal;
import edu.eci.arsw.immortals.ScoreBoard;
import edu.eci.arsw.concurrency.PauseController;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ImmortalTest {
    @Test
    public void testImmortalInitialization() {
        List<Immortal> pop = new ArrayList<>();
        ScoreBoard sb = new ScoreBoard();
        PauseController pc = new PauseController();
        Immortal im = new Immortal("A", 100, 10, pop, sb, pc);
        assertEquals("A", im.getName());
        assertEquals(100, im.getHealth());
        assertEquals(10, im.getDamage());
    }

    @Test
    public void testFightOrderedPreservesTotalHealth() throws InterruptedException {
        List<Immortal> pop = new ArrayList<>();
        ScoreBoard sb = new ScoreBoard();
        PauseController pc = new PauseController();
        Immortal im1 = new Immortal("Immortal-0", 100, 10, pop, sb, pc);
        Immortal im2 = new Immortal("Immortal-1", 100, 10, pop, sb, pc);
        pop.add(im1);
        pop.add(im2);

        // Ejecutar un pequeño tramo del ciclo de pelea en un hilo
        Thread t = new Thread(im1);
        t.start();
        Thread.sleep(20);
        im1.stop();
        t.join(1000);

        int total = im1.getHealth() + im2.getHealth();
        assertEquals(200, total);
        assertTrue(sb.totalFights() > 0);
    }
}
