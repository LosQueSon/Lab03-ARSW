package edu.eci.arsw.immortals;

import edu.eci.arsw.concurrency.PauseController;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Representa un inmortal en la simulación estilo Highlander.
 * 
 * Cada inmortal ejecuta en su propio hilo virtual, seleccionando oponentes
 * aleatoriamente y peleando continuamente hasta ser detenido o morir.
 * 
 * PUNTO 1 DEL ENUNCIADO: Mecánica de pelea con invariante.
 * En cada pelea, el atacante resta M de salud al oponente y suma M a sí mismo,
 * manteniendo la suma total de health constante (invariante del sistema).
 * 
 * PUNTO 4 DEL ENUNCIADO: Pausa cooperativa.
 * Verifica el PauseController en cada iteración, permitiéndose pausar
 * sin usar Thread.suspend() (método deprecado y peligroso).
 */
public final class Immortal implements Runnable {
  private final String name;
  private int health;
  private final int damage;
  private final List<Immortal> population;
  private final ScoreBoard scoreBoard;
  private final PauseController controller;
  private volatile boolean running = true;

  /**
   * Constructor del inmortal.
   * 
   * @param name nombre identificador del inmortal
   * @param health salud inicial
   * @param damage daño que inflige en cada ataque
   * @param population lista compartida de todos los inmortales (para elegir oponentes)
   * @param scoreBoard marcador global de peleas
   * @param controller controlador de pausa compartido
   * @throws NullPointerException si name, population, scoreBoard o controller son null
   */
  public Immortal(String name, int health, int damage, List<Immortal> population, ScoreBoard scoreBoard, PauseController controller) {
    this.name = Objects.requireNonNull(name);
    this.health = health;
    this.damage = damage;
    this.population = Objects.requireNonNull(population);
    this.scoreBoard = Objects.requireNonNull(scoreBoard);
    this.controller = Objects.requireNonNull(controller);
  }

  /**
   * Obtiene el nombre del inmortal.
   * 
   * @return nombre identificador
   */
  public String getName() { return name; }

  public int getDamage() { return damage; }
  
  /**
   * Obtiene la salud actual del inmortal de forma thread-safe.
   * 
   * PUNTO 6 DEL ENUNCIADO: Región crítica sincronizada.
   * El método synchronized garantiza que la lectura de health no ocurra
   * durante una modificación en fightNaive() o fightOrdered(), evitando
   * lecturas inconsistentes (dirty reads) durante actualizaciones concurrentes.
   * 
   * @return salud actual del inmortal
   */
  public synchronized int getHealth() { return health; }
  
  /**
   * Verifica si el inmortal está vivo y en ejecución.
   * 
   * Un inmortal está vivo si tiene health > 0 y no ha sido detenido (stop()).
   * 
   * @return true si está vivo y corriendo, false en caso contrario
   */
  public boolean isAlive() { return getHealth() > 0 && running; }
  
  /**
   * Solicita la detención ordenada del inmortal.
   * 
   * PUNTO 11 DEL ENUNCIADO: Detención ordenada.
   * Marca la bandera running como false. El hilo terminará su iteración
   * actual y saldrá del bucle while en run().
   */
  public void stop() { running = false; }

  /**
   * Ciclo principal de ejecución del inmortal.
   * 
   * PUNTO 4 DEL ENUNCIADO: Checkpoint cooperativo.
   * En cada iteración verifica awaitIfPaused() para pausarse si es necesario.
   * Esto permite que el sistema pause todos los hilos de forma controlada,
   * sin usar Thread.suspend() que causa deadlocks.
   * 
   * El ciclo:
   * 1. Verifica si debe pausarse
   * 2. Selecciona oponente aleatorio
   * 3. Ejecuta pelea (naive o ordered según configuración)
   * 4. Duerme 2ms antes de siguiente iteración
   * 
   * Termina cuando running=false o es interrumpido.
   */
  @Override public void run() {
    try {
      while (running) {
        controller.awaitIfPaused();
        if (!running) break;
        var opponent = pickOpponent();
        if (opponent == null) continue;
        String mode = System.getProperty("fight", "ordered");
        if ("naive".equalsIgnoreCase(mode)) fightNaive(opponent);
        else fightOrdered(opponent);
        Thread.sleep(2);
      }
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Selecciona un oponente aleatorio de la población.
   * 
   * PUNTO 10 DEL ENUNCIADO: Uso de colección concurrente.
   * Accede a population (CopyOnWriteArrayList) sin sincronización externa.
   * Esta lista es thread-safe y permite iteraciones sin ConcurrentModificationException.
   * 
   * @return un inmortal diferente a this, o null si no hay otros inmortales
   */
  private Immortal pickOpponent() {
    if (population.size() <= 1) return null;
    Immortal other;
    do {
      other = population.get(ThreadLocalRandom.current().nextInt(population.size()));
    } while (other == this);
    return other;
  }

  /**
   * Implementación naive de pelea con locks anidados en orden variable.
   * 
   * PUNTO 6 DEL ENUNCIADO: Demostración de deadlock potencial.
   * Esta implementación adquiere locks en el orden this->other, que varía según
   * quién ataca a quién. Si dos inmortales se atacan simultáneamente:
   * - Thread A: lock(A) -> espera lock(B)
   * - Thread B: lock(B) -> espera lock(A)
   * Resultado: deadlock circular.
   * 
   * PUNTO 1 DEL ENUNCIADO: Corrección del invariante.
   * Se cambió += damage/2 por += damage para mantener suma constante:
   * atacante gana lo que el defensor pierde (suma cero).
   * 
   * @param other el inmortal oponente
   */
  private void fightNaive(Immortal other) {
    synchronized (this) {
      synchronized (other) {
        if (this.health <= 0 || other.health <= 0) return;
        other.health -= this.damage;
        this.health += this.damage;
        scoreBoard.recordFight();
      }
    }
  }

  /**
   * Implementación de pelea con orden total para prevenir deadlocks.
   * 
   * PUNTO 6 y 8 DEL ENUNCIADO: Estrategia de orden consistente.
   * Determina el orden de adquisición de locks ANTES de bloquear, usando
   * comparación alfabética de nombres. Esto garantiza que todos los hilos
   * adquieren locks en el mismo orden global, eliminando ciclos de dependencia.
   * 
   * Ejemplo:
   * - Thread A (Immortal-0 vs Immortal-1): lock(0) -> lock(1)
   * - Thread B (Immortal-1 vs Immortal-0): lock(0) -> lock(1) [mismo orden]
   * No hay ciclo = no hay deadlock.
   * 
   * PUNTO 1 DEL ENUNCIADO: Invariante corregido.
   * Suma de health se mantiene constante: -damage + damage = 0.
   * 
   * @param other el inmortal oponente
   */
  private void fightOrdered(Immortal other) {
    Immortal first = this.name.compareTo(other.name) < 0 ? this : other;
    Immortal second = this.name.compareTo(other.name) < 0 ? other : this;
    synchronized (first) {
      synchronized (second) {
        if (this.health <= 0 || other.health <= 0) return;
        other.health -= this.damage;
        this.health += this.damage;
        scoreBoard.recordFight();
      }
    }
  }
}
