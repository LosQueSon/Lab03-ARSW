package edu.eci.arsw.coretest;

import edu.eci.arsw.core.BankAccount;
import edu.eci.arsw.core.TransferService;
import org.junit.jupiter.api.Test;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class TransferServiceTest {
    @Test
    public void testTransferNaive() {
        BankAccount a = new BankAccount(1, 100);
        BankAccount b = new BankAccount(2, 50);
        TransferService.transferNaive(a, b, 30);
        assertEquals(70, a.balance());
        assertEquals(80, b.balance());
    }

    @Test
    public void testTransferOrdered() {
        BankAccount a = new BankAccount(1, 200);
        BankAccount b = new BankAccount(2, 100);
        TransferService.transferOrdered(a, b, 50);
        assertEquals(150, a.balance());
        assertEquals(150, b.balance());
    }

    @Test
    public void testTransferTryLockSuccess() throws InterruptedException {
        BankAccount a = new BankAccount(1, 300);
        BankAccount b = new BankAccount(2, 100);
        TransferService.transferTryLock(a, b, 100, Duration.ofSeconds(1));
        assertEquals(200, a.balance());
        assertEquals(200, b.balance());
    }

    @Test
    public void testTransferInsufficientFunds() {
        BankAccount a = new BankAccount(1, 10);
        BankAccount b = new BankAccount(2, 10);
        assertThrows(IllegalArgumentException.class,
                () -> TransferService.transferNaive(a, b, 20));
    }

}
