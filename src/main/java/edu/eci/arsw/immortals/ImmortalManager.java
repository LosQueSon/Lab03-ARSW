package edu.eci.arsw.immortals;

import edu.eci.arsw.concurrency.PauseController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Gestor del ciclo de vida de la simulación de inmortales.
 * 
 * PUNTO 10 DEL ENUNCIADO: Remover inmortales muertos sin bloqueo.
 * Se usa CopyOnWriteArrayList para la población, permitiendo modificaciones
 * concurrentes seguras sin sincronización global. Esta colección crea una
 * copia en cada write, evitando ConcurrentModificationException cuando
 * un hilo itera mientras otro modifica la lista.
 * 
 * Trade-off: writes costosos pero reads muy eficientes (sin locks).
 * Apropiado para escenarios con muchas lecturas (pickOpponent) y pocas
 * escritas (remover muertos).
 */
public final class ImmortalManager implements AutoCloseable {
  private final List<Immortal> population = new CopyOnWriteArrayList<>();
  private final List<Future<?>> futures = new ArrayList<>();
  private final PauseController controller = new PauseController();
  private final ScoreBoard scoreBoard = new ScoreBoard();
  private ExecutorService exec;

  private final String fightMode;
  private final int initialHealth;
  private final int damage;

  /**
   * Constructor con modo de pelea, usando valores de health y damage del sistema.
   * 
   * @param n número de inmortales a crear
   * @param fightMode "ordered" para prevenir deadlocks, "naive" para demostrarlos
   */
  public ImmortalManager(int n, String fightMode) {
    this(n, fightMode, Integer.getInteger("health", 100), Integer.getInteger("damage", 10));
  }

  /**
   * Constructor completo del gestor de inmortales.
   * 
   * PUNTO 1 DEL ENUNCIADO: Invariante del sistema.
   * Crea N inmortales con salud inicial H cada uno. La suma total de health
   * debe permanecer constante = N * H durante toda la simulación.
   * 
   * @param n número de inmortales
   * @param fightMode modo de pelea ("ordered" o "naive")
   * @param initialHealth salud inicial de cada inmortal
   * @param damage daño por ataque
   */
  public ImmortalManager(int n, String fightMode, int initialHealth, int damage) {
    this.fightMode = fightMode;
    this.initialHealth = initialHealth;
    this.damage = damage;
    for (int i=0;i<n;i++) {
      population.add(new Immortal("Immortal-"+i, initialHealth, damage, population, scoreBoard, controller));
    }
  }

  /**
   * Inicia la simulación creando y lanzando hilos para cada inmortal.
   * 
   * Usa Executors.newVirtualThreadPerTaskExecutor() (Java 21+) para hilos
   * virtuales ligeros, permitiendo escalar a miles de inmortales sin
   * degradación de rendimiento.
   * 
   * Si ya hay una simulación en curso, la detiene antes de iniciar nueva.
   */
  public synchronized void start() {
    if (exec != null) stop();
    exec = Executors.newVirtualThreadPerTaskExecutor();
    for (Immortal im : population) {
      futures.add(exec.submit(im));
    }
  }

  /**
   * Solicita la pausa de todos los hilos de inmortales.
   * 
   * PUNTO 3 DEL ENUNCIADO: Pause &amp; Check.
   * Delega al PauseController que marca la bandera de pausa.
   * Los hilos se pausarán al llegar a su próximo checkpoint.
   * 
   * @see PauseController#pause()
   */
  public void pause() { controller.pause(); }
  
  /**
   * Reanuda la ejecución de todos los hilos pausados.
   * 
   * PUNTO 4 DEL ENUNCIADO: Resume.
   * Delega al PauseController que envía señal broadcast.
   * 
   * @see PauseController#resume()
   */
  public void resume() { controller.resume(); }
  
  /**
   * Detiene ordenadamente la simulación, esperando terminación de hilos.
   * 
   * PUNTO 11 DEL ENUNCIADO: Implementar STOP completamente.
   * Marca running=false en todos los inmortales, luego llama shutdownNow()
   * para interrumpir hilos bloqueados. Finalmente, awaitTermination() espera
   * hasta 5 segundos a que todos los hilos terminen limpiamente.
   * 
   * Sin awaitTermination(), los hilos pueden quedar zombies o el executor
   * no liberar recursos correctamente. El timeout previene espera infinita
   * si algún hilo no responde a la interrupción.
   * 
   * @see ExecutorService#shutdownNow()
   * @see ExecutorService#awaitTermination(long, TimeUnit)
   */
  public void stop() {
    for (Immortal im : population) im.stop();
    if (exec != null) {
      exec.shutdownNow();
      try {
        if (!exec.awaitTermination(5, TimeUnit.SECONDS)) {
          System.err.println("Warning: Some threads did not terminate in time");
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      exec = null;
    }
  }

  /**
   * Cuenta el número de inmortales vivos (health > 0 y running = true).
   * 
   * @return cantidad de inmortales vivos
   */
  public int aliveCount() {
    int c = 0;
    for (Immortal im : population) if (im.isAlive()) c++;
    return c;
  }

  /**
   * Calcula la suma total de salud de todos los inmortales.
   * 
   * PUNTO 1 DEL ENUNCIADO: Validación del invariante.
   * Con el sistema pausado, este valor debe ser igual a N * initialHealth.
   * Si difiere, indica condición de carrera o error en la lógica de pelea.
   * 
   * @return suma de health de todos los inmortales
   */
  public long totalHealth() {
    long sum = 0;
    for (Immortal im : population) sum += im.getHealth();
    return sum;
  }

  /**
   * Obtiene una copia inmutable de la población actual.
   * 
   * PUNTO 3 y 5 DEL ENUNCIADO: Snapshot consistente.
   * Crea una nueva lista (snapshot) de la población en el momento de la llamada.
   * Al estar pausado el sistema, este snapshot representa un estado consistente
   * sin updates en progreso.
   * 
   * @return vista no modificable de la población
   */
  public List<Immortal> populationSnapshot() {
    return Collections.unmodifiableList(new ArrayList<>(population));
  }

  /**
   * Obtiene el marcador global de peleas.
   * 
   * @return instancia de ScoreBoard compartida por todos los inmortales
   */
  public ScoreBoard scoreBoard() { return scoreBoard; }
  
  /**
   * Obtiene el controlador de pausa.
   * 
   * PUNTO 4 y 5 DEL ENUNCIADO: Acceso a estado de pausa.
   * Permite a la UI verificar cuántos hilos están pausados y esperar
   * sincronización completa antes de leer el estado del sistema.
   * 
   * @return instancia de PauseController compartida
   */
  public PauseController controller() { return controller; }

  /**
   * Cierra el gestor deteniendo la simulación.
   * 
   * Implementa AutoCloseable para uso con try-with-resources.
   */
  @Override public void close() { stop(); }
}
