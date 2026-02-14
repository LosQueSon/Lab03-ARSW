package edu.eci.arsw.coretest;

import edu.eci.arsw.core.BankAccount;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BankAccountTest {
    @Test
    public void testInitialBalance() {
        BankAccount acc = new BankAccount(1, 500);
        assertEquals(500, acc.balance());
        assertEquals(1, acc.id());
    }

    @Test
    public void testDepositInternal() {
        BankAccount acc = new BankAccount(2, 100);
        acc.depositInternal(50);
        assertEquals(150, acc.balance());
    }

    @Test
    public void testWithdrawInternal() {
        BankAccount acc = new BankAccount(3, 200);
        acc.withdrawInternal(80);
        assertEquals(120, acc.balance());
    }

    @Test
    public void testLockNotNull() {
        BankAccount acc = new BankAccount(4, 0);
        assertNotNull(acc.lock());
    }
}
