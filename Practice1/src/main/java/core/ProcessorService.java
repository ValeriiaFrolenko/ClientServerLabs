package core;

import model.Message;
import model.Packet;
import model.Product;
import model.ProductFilter;

import java.util.List;
import java.util.function.Consumer;

public class ProcessorService implements Processor {

    private final ProductService productService;
    private final Consumer<Packet> onMessageProcessed;

    public ProcessorService(ProductService productService, Consumer<Packet> onMessageProcessed) {
        this.productService = productService;
        this.onMessageProcessed = onMessageProcessed;
    }

    @Override
    public void process(Packet packet) {
        Message message = packet.getbMsq();
        Command command = Command.fromInt(message.getcType());
        String messageString = message.getMessage();

        try {
            switch (command) {
                case GET_QUANTITY      -> processGetQuantity(messageString, packet);
                case DECREASE_QUANTITY -> processDecreaseQuantity(messageString, packet);
                case INCREASE_QUANTITY -> processIncreaseQuantity(messageString, packet);
                case SET_PRICE         -> processSetPrice(messageString, packet);
                case CREATE_PRODUCT    -> processCreateProduct(messageString, packet);
                case GET_PRODUCT       -> processGetProduct(messageString, packet);
                case UPDATE_PRODUCT    -> processUpdateProduct(messageString, packet);
                case DELETE_PRODUCT    -> processDeleteProduct(messageString, packet);
                case SEARCH_PRODUCTS   -> processSearchProducts(messageString, packet);
                default -> throw new IllegalArgumentException("Unhandled command: " + command);
            }
        } catch (Exception e) {
            sendResponse(packet, "ERROR: " + e.getMessage());
        }
    }

    private void processGetQuantity(String messageString, Packet originalPacket) {
        int quantity = productService.getQuantity(messageString);
        sendResponse(originalPacket, String.valueOf(quantity));
    }

    private void processDecreaseQuantity(String messageString, Packet originalPacket) {
        String[] parts = messageString.split(":");
        productService.removeStock(parts[0], Integer.parseInt(parts[1]));
        sendResponse(originalPacket, "OK");
    }

    private void processIncreaseQuantity(String messageString, Packet originalPacket) {
        String[] parts = messageString.split(":");
        productService.addStock(parts[0], Integer.parseInt(parts[1]));
        sendResponse(originalPacket, "OK");
    }

    private void processSetPrice(String messageString, Packet originalPacket) {
        String[] parts = messageString.split(":");
        productService.updatePrice(parts[0], Double.parseDouble(parts[1]));
        sendResponse(originalPacket, "OK");
    }

    // format: name:category:price:quantity
    private void processCreateProduct(String messageString, Packet originalPacket) {
        String[] parts = messageString.split(":");
        Product product = Product.builder().name(parts[0]).category(parts[1]).price(Double.parseDouble(parts[2])).quantity(Integer.parseInt(parts[3])).build();
        Product created = productService.create(product);
        sendResponse(originalPacket, String.valueOf(created.id()));
    }

    // format: id
    private void processGetProduct(String messageString, Packet originalPacket) {
        Product product = productService.getById(Long.parseLong(messageString));
        sendResponse(originalPacket, product.toString());
    }

    // format: id:name:category:price:quantity
    private void processUpdateProduct(String messageString, Packet originalPacket) {
        String[] parts = messageString.split(":");
        Product product = Product.builder()
                .id(Long.parseLong(parts[0]))
                .name(parts[1])
                .category(parts[2])
                .price(Double.parseDouble(parts[3]))
                .quantity(Integer.parseInt(parts[4]))
                .build();
        productService.update(product);
        sendResponse(originalPacket, "OK");
    }

    // format: id
    private void processDeleteProduct(String messageString, Packet originalPacket) {
        productService.delete(Long.parseLong(messageString));
        sendResponse(originalPacket, "OK");
    }

    // format: name=X;category=Y;minPrice=Z;maxPrice=W;minQty=A;maxQty=B;page=0;pageSize=10
    // all parts are optional
    private void processSearchProducts(String messageString, Packet originalPacket) {
        ProductFilter filter = parseFilter(messageString);
        List<Product> products = productService.search(filter);
        long total = productService.count(filter);
        String result = "total=" + total + ";" + products.stream()
                .map(Product::toString)
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "|" + b);
        sendResponse(originalPacket, result);
    }

    private ProductFilter parseFilter(String messageString) {
        ProductFilter.Builder b = ProductFilter.builder();
        if (messageString == null || messageString.isEmpty()) return b.build();
        for (String part : messageString.split(";")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) continue;
            switch (kv[0]) {
                case "name"     -> b.name(kv[1]);
                case "category" -> b.category(kv[1]);
                case "minPrice" -> b.minPrice(Double.parseDouble(kv[1]));
                case "maxPrice" -> b.maxPrice(Double.parseDouble(kv[1]));
                case "minQty"   -> b.minQuantity(Integer.parseInt(kv[1]));
                case "maxQty"   -> b.maxQuantity(Integer.parseInt(kv[1]));
                case "page"     -> b.page(Integer.parseInt(kv[1]));
                case "pageSize" -> b.pageSize(Integer.parseInt(kv[1]));
            }
        }
        return b.build();
    }

    private void sendResponse(Packet originalPacket, String payload) {
        Message message = new Message(Command.OK.ordinal(), originalPacket.getbMsq().getbUserId(), payload);
        Packet packet = new Packet(originalPacket.getbSrc(), originalPacket.getbPktId(), message);
        onMessageProcessed.accept(packet);
    }
}