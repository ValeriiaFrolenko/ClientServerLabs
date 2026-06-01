package network.client;

import model.Packet;

public class ResponseHandler {

    public void handle(Packet packet) {
        System.out.println("Response received: " + packet.getbMsq().getMessage());
    }
}