package org.example;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        Data d = new Data();

        Thread w1 = new Thread(new Worker(1, d));
        Thread w2 = new Thread(new Worker(2, d));
        Thread w3 = new Thread(new Worker(3, d));

        w1.start();
        w2.start();
        w3.start();

        w3.join();
        System.out.println("end of main...");
    }
}