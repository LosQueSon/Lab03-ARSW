package edu.eci.arsw.concurrency;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Controlador de pausa cooperativa para sincronización de hilos.
 * 
 * PUNTO 4 DEL ENUNCIADO: Pausa correcta y Resume.
 * Implementa un mecanismo de checkpoint cooperativo donde los hilos
 * verifican voluntariamente si deben pausarse, evitando el uso de
 * métodos deprecados (Thread.suspend/resume) que causan deadlocks.
 * 
 * PUNTO 5 DEL ENUNCIADO: Validación de consistencia.
 * El contador pausedThreads permite verificar que todos los hilos
 * están efectivamente pausados antes de leer el estado del sistema.
 */
public final class PauseController {
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition unpaused = lock.newCondition();
  private volatile boolean paused = false;
  private final AtomicInteger pausedThreads = new AtomicInteger(0);

  /**
   * Solicita la pausa de todos los hilos.
   * 
   * PUNTO 4 DEL ENUNCIADO: Implementación de pausa.
   * Marca la bandera paused y resetea el contador. Los hilos se pausarán
   * al llegar al siguiente checkpoint (awaitIfPaused) en su ciclo.
   */
  public void pause() { lock.lock(); try { paused = true; pausedThreads.set(0); } finally { lock.unlock(); } }
  
  /**
   * Reanuda la ejecución de todos los hilos pausados.
   * 
   * PUNTO 4 DEL ENUNCIADO: Implementación de Resume.
   * Desactiva la bandera y envía señal broadcast (signalAll) a todos
   * los hilos esperando en await(), permitiéndoles continuar ejecución.
   */
  public void resume() { lock.lock(); try { paused = false; unpaused.signalAll(); } finally { lock.unlock(); } }
  
  public boolean isPaused() { return paused; }
  
  /**
   * Obtiene el número de hilos actualmente pausados.
   * 
   * PUNTO 5 DEL ENUNCIADO: Validación de consistencia.
   * Permite verificar que todos los hilos esperados están pausados
   * antes de realizar lecturas del estado del sistema.
   * 
   * @return cantidad de hilos en estado await()
   */
  public int getPausedThreadCount() { return pausedThreads.get(); }

  /**
   * Checkpoint cooperativo: el hilo se pausa si la bandera está activa.
   * 
   * PUNTO 4 DEL ENUNCIADO: Pausa cooperativa sin suspend/resume.
   * Los hilos llaman a este método en cada iteración de su ciclo.
   * Si paused==true, incrementan el contador (confirmación) y esperan
   * en await() hasta recibir señal de resume(). El while protege contra
   * spurious wakeups, re-verificando la condición al despertar.
   * 
   * El finally garantiza decremento del contador incluso si await() lanza
   * InterruptedException, manteniendo consistencia del contador.
   * 
   * @throws InterruptedException si el hilo es interrumpido mientras espera
   */
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

  /**
   * Espera activa hasta que el número esperado de hilos estén pausados.
   * 
   * PUNTO 4 DEL ENUNCIADO: Asegurar que todos los hilos están pausados.
   * Implementa polling con timeout para verificar que todos los hilos
   * han llegado al checkpoint (await) antes de leer el estado del sistema.
   * Esto garantiza una "fotografía atómica" consistente, sin updates en curso.
   * 
   * Sin esta espera, algunos hilos podrían estar ejecutando fight() cuando
   * se lee health, resultando en valores de diferentes momentos temporales.
   * 
   * @param expectedThreads número esperado de hilos que deben pausarse
   * @param timeoutMs tiempo máximo de espera en milisegundos
   * @throws InterruptedException si el hilo es interrumpido durante sleep
   */
  public void waitUntilPaused(int expectedThreads, int timeoutMs) throws InterruptedException {
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (pausedThreads.get() < expectedThreads && System.currentTimeMillis() < deadline) {
      Thread.sleep(10);
    }
  }
}
