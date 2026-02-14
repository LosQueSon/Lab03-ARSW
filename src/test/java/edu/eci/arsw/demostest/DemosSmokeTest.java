package edu.eci.arsw.demostest;

import edu.eci.arsw.demos.DeadlockDemo;
import edu.eci.arsw.demos.OrderedTransferDemo;
import edu.eci.arsw.demos.TryLockTransferDemo;
import org.junit.jupiter.api.Test;

public class DemosSmokeTest {
    @Test
    public void testDeadlockDemoRuns() throws Exception {
        // This will deadlock, so we just check it starts
        Thread t = new Thread(() -> {
            try { DeadlockDemo.run(); } catch (Exception ignored) {}
        });
        t.start();
        Thread.sleep(500); // Let it start
        t.interrupt();
    }

    @Test
    public void testOrderedTransferDemoRuns() throws Exception {
        OrderedTransferDemo.run();
    }

    @Test
    public void testTryLockTransferDemoRuns() throws Exception {
        TryLockTransferDemo.run();
    }
}
