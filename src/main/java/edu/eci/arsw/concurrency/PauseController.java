package edu.eci.arsw.concurrency;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public final class PauseController {
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition unpaused = lock.newCondition();
  private volatile boolean paused = false;
  private final AtomicInteger pausedThreads = new AtomicInteger(0);

  public void pause() { lock.lock(); try { paused = true; pausedThreads.set(0); } finally { lock.unlock(); } }
  public void resume() { lock.lock(); try { paused = false; unpaused.signalAll(); } finally { lock.unlock(); } }
  public boolean paused() { return paused; }
  public int getPausedThreadCount() { return pausedThreads.get(); }

  public void awaitIfPaused() throws InterruptedException {
    lock.lockInterruptibly();
    try {
      while (paused) {
        pausedThreads.incrementAndGet();
        try { unpaused.await(); }
        finally { pausedThreads.decrementAndGet(); }
      }
    } finally { lock.unlock(); }
  }

  public void waitUntilPaused(int expectedThreads, int timeoutMs) throws InterruptedException {
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (pausedThreads.get() < expectedThreads && System.currentTimeMillis() < deadline) {
      Thread.sleep(10);
    }
  }
}
