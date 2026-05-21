package core;

import model.Product;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WareHouseTest {

    @Test
    public void testConcurrentStockUpdates() throws InterruptedException {
        WareHouse wareHouse = new WareHouse();
        wareHouse.addProduct(new Product("Buckwheat", 50.0, 100));

        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    wareHouse.addStock("Buckwheat", 20);
                    wareHouse.removeStock("Buckwheat", 10);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        assertEquals(1100, wareHouse.getProductQuantity("Buckwheat"));
    }
}