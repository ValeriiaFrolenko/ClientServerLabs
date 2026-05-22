package core;

import model.Message;
import model.Packet;

import java.util.function.Consumer;

public class ProcessorService implements Processor {

    private final WareHouse wareHouse;
    private final Consumer<Packet> onMessageProcessed;

    public ProcessorService(WareHouse wareHouse, Consumer<Packet> onMessageProcessed) {
        this.wareHouse = wareHouse;
        this.onMessageProcessed = onMessageProcessed;
    }

    @Override
    public void process(Packet packet) {
        Message message = packet.getbMsq();
        Command command = Command.fromInt(message.getcType());
        String messageString = message.getMessage();

        try {
            switch (command) {
                case GET_QUANTITY -> processGetQuantity(messageString, packet);
                case DECREASE_QUANTITY -> processDecreaseQuantity(messageString, packet);
                case INCREASE_QUANTITY -> processIncreaseQuantity(messageString, packet);
                case SET_PRICE -> processSetPrice(messageString, packet);
                default -> throw new IllegalArgumentException("Invalid command integer: " + command);
            }
        } catch (Exception e) {
            sendResponse(packet, "ERROR: " + e.getMessage());
        }
    }

    private void processGetQuantity(String messageString, Packet originalPacket) {
        int quantity = wareHouse.getProductQuantity(messageString);
        sendResponse(originalPacket, String.valueOf(quantity));
    }

    private void processDecreaseQuantity(String messageString, Packet originalPacket) {
        String[] parts = messageString.split(":");
        String productName = parts[0];
        int quantity = Integer.parseInt(parts[1]);
        wareHouse.removeStock(productName, quantity);
        sendResponse(originalPacket, "OK");
    }

    private void processIncreaseQuantity(String messageString, Packet originalPacket) {
        String[] parts = messageString.split(":");
        String productName = parts[0];
        int quantity = Integer.parseInt(parts[1]);
        wareHouse.addStock(productName, quantity);
        sendResponse(originalPacket, "OK");
    }

    private void processSetPrice(String messageString, Packet originalPacket) {
        String[] parts = messageString.split(":");
        String productName = parts[0];
        double price = Double.parseDouble(parts[1]);
        wareHouse.updatePrice(productName, price);
        sendResponse(originalPacket, "OK");
    }

    private void sendResponse(Packet originalPacket, String payload) {
        Message message = new Message(Command.OK.ordinal(), originalPacket.getbMsq().getbUserId(), payload);
        Packet packet = new Packet(originalPacket.getbSrc(), originalPacket.getbPktId(), message);
        onMessageProcessed.accept(packet);
    }
}