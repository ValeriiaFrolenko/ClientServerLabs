package network;

import model.Packet;
import java.util.function.Consumer;

public class ResponseHandler {

    private final Consumer<Packet> onResponse;

    public ResponseHandler(Consumer<Packet> onResponse) {
        this.onResponse = onResponse;
    }

    public void handle(Packet packet) {
        onResponse.accept(packet);
    }
}