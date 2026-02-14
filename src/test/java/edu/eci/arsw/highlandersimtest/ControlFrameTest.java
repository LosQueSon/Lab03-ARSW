package edu.eci.arsw.highlandersimtest;

import edu.eci.arsw.highlandersim.ControlFrame;
import org.junit.jupiter.api.Test;
import javax.swing.*;

import static org.junit.jupiter.api.Assertions.*;

public class ControlFrameTest {
    @Test
    public void testFrameInitializes() {
        SwingUtilities.invokeLater(() -> {
            ControlFrame frame = new ControlFrame(5, "ordered");
            assertNotNull(frame);
            assertEquals("Highlander Simulator — ARSW", frame.getTitle());
            frame.dispose();
        });
    }
}
