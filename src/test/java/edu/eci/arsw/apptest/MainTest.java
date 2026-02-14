package edu.eci.arsw.apptest;

import edu.eci.arsw.app.Main;
import org.junit.jupiter.api.Test;

public class MainTest {
    @Test
    public void testMainRunsWithDefault() throws Exception {
        String[] args = {};
        Main.main(args);
    }
}
