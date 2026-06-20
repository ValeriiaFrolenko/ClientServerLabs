package tcp_udp_network;

import tcp_udp_core.Command;
import model.Message;
import model.Packet;

import java.util.Random;

public class PacketGenerator {

    private final byte clientId;
    private final Random random = new Random();

    public PacketGenerator(byte clientId) {
        this.clientId = clientId;
    }

    public Packet generate() {
        Command command = randomCommand();
        String payload = buildPayload(command);
        Message message = new Message(command.ordinal(), 42, payload);
        return new Packet(clientId, System.currentTimeMillis(), message);
    }

    private Command randomCommand() {
        Command[] commands = {
                Command.GET_QUANTITY,
                Command.INCREASE_QUANTITY,
                Command.DECREASE_QUANTITY,
                Command.SET_PRICE
        };
        return commands[random.nextInt(commands.length)];
    }

    private String buildPayload(Command command) {
        return switch (command) {
            case GET_QUANTITY -> "Apple";
            case INCREASE_QUANTITY -> "Apple:" + (random.nextInt(10) + 1);
            case DECREASE_QUANTITY -> "Apple:" + (random.nextInt(10) + 1);
            case SET_PRICE -> "Apple:" + (random.nextInt(100) + 1) + ".0";
            default -> "Apple";
        };
    }
}