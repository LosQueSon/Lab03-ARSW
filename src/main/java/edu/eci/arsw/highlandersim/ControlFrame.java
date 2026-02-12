package edu.eci.arsw.highlandersim;

import edu.eci.arsw.immortals.Immortal;
import edu.eci.arsw.immortals.ImmortalManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public final class ControlFrame extends JFrame {

  private ImmortalManager manager;
  private final JTextArea output = new JTextArea(14, 40);
  private final JButton startBtn = new JButton("Start");
  private final JButton pauseAndCheckBtn = new JButton("Pause & Check");
  private final JButton resumeBtn = new JButton("Resume");
  private final JButton stopBtn = new JButton("Stop");

  private final JSpinner countSpinner = new JSpinner(new SpinnerNumberModel(8, 2, 5000, 1));
  private final JSpinner healthSpinner = new JSpinner(new SpinnerNumberModel(100, 10, 10000, 10));
  private final JSpinner damageSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 1000, 1));
  private final JComboBox<String> fightMode = new JComboBox<>(new String[]{"ordered", "naive"});

  public ControlFrame(int count, String fight) {
    setTitle("Highlander Simulator — ARSW");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLayout(new BorderLayout(8,8));

    JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
    top.add(new JLabel("Count:"));
    countSpinner.setValue(count);
    top.add(countSpinner);
    top.add(new JLabel("Health:"));
    top.add(healthSpinner);
    top.add(new JLabel("Damage:"));
    top.add(damageSpinner);
    top.add(new JLabel("Fight:"));
    fightMode.setSelectedItem(fight);
    top.add(fightMode);
    add(top, BorderLayout.NORTH);

    output.setEditable(false);
    output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    add(new JScrollPane(output), BorderLayout.CENTER);

    JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
    bottom.add(startBtn);
    bottom.add(pauseAndCheckBtn);
    bottom.add(resumeBtn);
    bottom.add(stopBtn);
    add(bottom, BorderLayout.SOUTH);

    startBtn.addActionListener(this::onStart);
    pauseAndCheckBtn.addActionListener(this::onPauseAndCheck);
    resumeBtn.addActionListener(this::onResume);
    stopBtn.addActionListener(this::onStop);

    pack();
    setLocationByPlatform(true);
    setVisible(true);
  }

  private void onStart(ActionEvent e) {
    safeStop();
    int n = (Integer) countSpinner.getValue();
    int health = (Integer) healthSpinner.getValue();
    int damage = (Integer) damageSpinner.getValue();
    String fight = (String) fightMode.getSelectedItem();
    manager = new ImmortalManager(n, fight, health, damage);
    manager.start();
    output.setText("Simulation started with %d immortals (health=%d, damage=%d, fight=%s)%n"
      .formatted(n, health, damage, fight));
  }

  private void onPauseAndCheck(ActionEvent e) {
    if (manager == null) return;
    manager.pause();
    
    // Esperar a que todos los hilos se pasen (dar tiempo de sincronización)
    try {
      int aliveCount = manager.aliveCount();
      manager.controller().waitUntilPaused(aliveCount, 500);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
    
    List<Immortal> pop = manager.populationSnapshot();
    long sum = 0;
    int alive = 0;
    int dead = 0;
    StringBuilder sb = new StringBuilder();
    
    // Calcular estadísticas
    for (Immortal im : pop) {
      int h = im.getHealth();
      sum += h;
      if (h > 0) alive++;
      else dead++;
    }
    
    // Mostrar lista completa si hay pocos, o resumen si hay muchos
    if (pop.size() <= 100) {
      for (Immortal im : pop) {
        int h = im.getHealth();
        sb.append(String.format("%-14s : %5d%n", im.name(), h));
      }
    } else {
      sb.append("Showing summary for ").append(pop.size()).append(" immortals\n");
      sb.append("(First 50 shown)\n\n");
      for (int i = 0; i < Math.min(50, pop.size()); i++) {
        Immortal im = pop.get(i);
        int h = im.getHealth();
        sb.append(String.format("%-14s : %5d%n", im.name(), h));
      }
      if (pop.size() > 50) {
        sb.append("... (" + (pop.size() - 50) + " more)\n");
      }
    }
    
    sb.append("================================\n");
    sb.append("Alive: ").append(alive).append(" | Dead: ").append(dead).append('\n');
    sb.append("Total Health: ").append(sum).append('\n');
    int expectedTotal = (Integer) countSpinner.getValue() * (Integer) healthSpinner.getValue();
    sb.append("Expected Total: ").append(expectedTotal).append('\n');
    sb.append("Invariant: ").append(sum == expectedTotal ? "✓ OK" : "✗ BROKEN").append('\n');
    sb.append("Score (fights): ").append(manager.scoreBoard().totalFights()).append('\n');
    sb.append("Paused threads: ").append(manager.controller().getPausedThreadCount()).append('\n');
    output.setText(sb.toString());
  }

  private void onResume(ActionEvent e) {
    if (manager == null) return;
    manager.resume();
  }

  private void onStop(ActionEvent e) { safeStop(); }

  private void safeStop() {
    if (manager != null) {
      manager.stop();
      manager = null;
    }
  }

  public static void main(String[] args) {
    int count = Integer.getInteger("count", 8);
    String fight = System.getProperty("fight", "ordered");
    SwingUtilities.invokeLater(() -> new ControlFrame(count, fight));
  }
}
