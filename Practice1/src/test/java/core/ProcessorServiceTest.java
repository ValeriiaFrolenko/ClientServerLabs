package core;

import database.JdbcTemplate;
import database.ProductRepository;
import model.Message;
import model.Packet;
import model.Product;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProcessorServiceTest {

    private static final String URL = "jdbc:h2:mem:processordb;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    private ProcessorService processor;
    private ProductService productService;
    private List<Packet> responses;

    @BeforeEach
    void setUp() throws Exception {
        try (var conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS products");
            stmt.execute("""
                    CREATE TABLE products (
                        id       BIGINT AUTO_INCREMENT PRIMARY KEY,
                        name     VARCHAR(255) NOT NULL,
                        category VARCHAR(255) NOT NULL,
                        price    DOUBLE       NOT NULL,
                        quantity INT          NOT NULL
                    )
                    """);
        }
        JdbcTemplate jdbc = new JdbcTemplate(
                () -> DriverManager.getConnection(URL, USER, PASSWORD)
        );
        ProductRepository repository = new ProductRepository(jdbc);
        productService = new ProductService(repository);

        responses = new ArrayList<>();
        processor = new ProcessorService(productService, responses::add);
    }

    @AfterEach
    void tearDown() throws Exception {
        try (var conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS products");
        }
    }

    private Packet makePacket(Command command, String payload) {
        Message message = new Message(command.ordinal(), 1, payload);
        return new Packet((byte) 1, 1L, message);
    }

    private String responsePayload() {
        assertFalse(responses.isEmpty(), "no response was sent");
        return responses.getLast().getbMsq().getMessage();
    }

    private long createProduct(String name, String category, double price, int quantity) {
        Packet packet = makePacket(Command.CREATE_PRODUCT, name + ":" + category + ":" + price + ":" + quantity);
        processor.process(packet);
        return Long.parseLong(responsePayload());
    }

    @Test
    void createProduct_returnsGeneratedId() {
        processor.process(makePacket(Command.CREATE_PRODUCT, "Apple:Fruit:1.5:100"));

        String response = responsePayload();
        assertTrue(Long.parseLong(response) > 0);
    }

    @Test
    void getProduct_returnsProductString() {
        long id = createProduct("Apple", "Fruit", 1.5, 100);

        processor.process(makePacket(Command.GET_PRODUCT, String.valueOf(id)));

        String response = responsePayload();
        assertTrue(response.contains("Apple"));
        assertTrue(response.contains("Fruit"));
    }

    @Test
    void getProduct_returnsError_whenNotExists() {
        processor.process(makePacket(Command.GET_PRODUCT, "999"));

        assertTrue(responsePayload().startsWith("ERROR:"));
    }

    @Test
    void updateProduct_returnsOk() {
        long id = createProduct("OldName", "Cat", 1.0, 10);

        processor.process(makePacket(Command.UPDATE_PRODUCT, id + ":NewName:NewCat:9.9:99"));

        assertEquals("OK", responsePayload());
        Product found = productService.getById(id);
        assertEquals("NewName", found.name());
        assertEquals(9.9, found.price());
    }

    @Test
    void deleteProduct_returnsOk() {
        long id = createProduct("ToDelete", "Cat", 1.0, 1);

        processor.process(makePacket(Command.DELETE_PRODUCT, String.valueOf(id)));

        assertEquals("OK", responsePayload());
    }

    @Test
    void deleteProduct_returnsError_whenNotExists() {
        processor.process(makePacket(Command.DELETE_PRODUCT, "999"));

        assertTrue(responsePayload().startsWith("ERROR:"));
    }

    @Test
    void getQuantity_returnsCorrectQuantity() {
        createProduct("Rice", "Grains", 1.0, 42);

        processor.process(makePacket(Command.GET_QUANTITY, "Rice"));

        assertEquals("42", responsePayload());
    }

    @Test
    void getQuantity_returnsError_whenNotExists() {
        processor.process(makePacket(Command.GET_QUANTITY, "NonExistent"));

        assertTrue(responsePayload().startsWith("ERROR:"));
    }

    @Test
    void increaseQuantity_returnsOkAndUpdatesStock() {
        createProduct("Bread", "Bakery", 0.8, 10);

        processor.process(makePacket(Command.INCREASE_QUANTITY, "Bread:5"));

        assertEquals("OK", responsePayload());
        assertEquals(15, productService.getQuantity("Bread"));
    }

    @Test
    void decreaseQuantity_returnsOkAndUpdatesStock() {
        createProduct("Milk", "Dairy", 1.2, 20);

        processor.process(makePacket(Command.DECREASE_QUANTITY, "Milk:8"));

        assertEquals("OK", responsePayload());
        assertEquals(12, productService.getQuantity("Milk"));
    }

    @Test
    void decreaseQuantity_returnsError_whenNotEnoughStock() {
        createProduct("Eggs", "Dairy", 3.0, 5);

        processor.process(makePacket(Command.DECREASE_QUANTITY, "Eggs:10"));

        assertTrue(responsePayload().startsWith("ERROR:"));
    }

    @Test
    void setPrice_returnsOkAndUpdatesPrice() {
        createProduct("Juice", "Drinks", 2.0, 10);

        processor.process(makePacket(Command.SET_PRICE, "Juice:5.5"));

        assertEquals("OK", responsePayload());
        assertEquals(5.5, productService.getByName("Juice").price());
    }

    @Test
    void searchProducts_noFilters_returnsAll() {
        createProduct("Apple", "Fruit", 1.0, 10);
        createProduct("Milk", "Dairy", 1.2, 30);

        processor.process(makePacket(Command.SEARCH_PRODUCTS, ""));

        String response = responsePayload();
        assertTrue(response.contains("total=2"));
    }

    @Test
    void searchProducts_byCategory_returnsFiltered() {
        createProduct("Apple", "Fruit", 1.0, 10);
        createProduct("Mango", "Fruit", 3.0, 5);
        createProduct("Milk", "Dairy", 1.2, 30);

        processor.process(makePacket(Command.SEARCH_PRODUCTS, "category=Fruit"));

        String response = responsePayload();
        assertTrue(response.contains("total=2"));
        assertTrue(response.contains("Apple"));
        assertTrue(response.contains("Mango"));
        assertFalse(response.contains("Milk"));
    }

    @Test
    void searchProducts_byMinPrice_returnsFiltered() {
        createProduct("Cheap", "Cat", 1.0, 10);
        createProduct("Expensive", "Cat", 10.0, 5);

        processor.process(makePacket(Command.SEARCH_PRODUCTS, "minPrice=5.0"));

        String response = responsePayload();
        assertTrue(response.contains("total=1"));
        assertTrue(response.contains("Expensive"));
    }

    @Test
    void searchProducts_withPagination_returnsCorrectPage() {
        for (int i = 1; i <= 5; i++) {
            createProduct("Product" + i, "Cat", i, i);
        }

        processor.process(makePacket(Command.SEARCH_PRODUCTS, "page=0;pageSize=2"));

        String response = responsePayload();
        assertTrue(response.contains("total=5"));
    }
}