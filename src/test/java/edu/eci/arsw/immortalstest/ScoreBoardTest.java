package edu.eci.arsw.immortalstest;

import edu.eci.arsw.immortals.ScoreBoard;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ScoreBoardTest {
    @Test
    public void testRecordFight() {
        ScoreBoard sb = new ScoreBoard();
        assertEquals(0, sb.totalFights());
        sb.recordFight();
        assertEquals(1, sb.totalFights());
        sb.recordFight();
        assertEquals(2, sb.totalFights());
    }
}
