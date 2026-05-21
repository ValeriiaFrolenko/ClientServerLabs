package network;

import java.util.Arrays;

public class FakeSender implements Sender {
    @Override
    public void sendMessage(byte[] encryptedPacket) {
        System.out.println(Arrays.toString(encryptedPacket));
    }
}