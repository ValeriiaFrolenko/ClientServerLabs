package core;

import database.JdbcTemplate;
import database.ProductRepository;
import model.Product;
import model.ProductFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class ProductServiceTest {

    private static final String URL = "jdbc:h2:mem:servicedb;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    private ProductService service;

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
        service = new ProductService(repository);
    }

    @AfterEach
    void tearDown() throws Exception {
        try (var conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS products");
        }
    }

    @Test
    void create_andGetById_works() {
        Product product = Product.builder()
                .name("Apple").category("Fruit").price(1.5).quantity(100).build();

        Product saved = service.create(product);
        Product found = service.getById(saved.id());

        assertEquals(saved.id(), found.id());
        assertEquals("Apple", found.name());
    }

    @Test
    void getById_throws_whenNotExists() {
        assertThrows(NoSuchElementException.class, () -> service.getById(999L));
    }

    @Test
    void getByName_returnsProduct_whenExists() {
        service.create(Product.builder().name("Milk").category("Dairy").price(1.2).quantity(30).build());

        Product found = service.getByName("Milk");

        assertEquals("Milk", found.name());
    }

    @Test
    void getByName_throws_whenNotExists() {
        assertThrows(NoSuchElementException.class, () -> service.getByName("NonExistent"));
    }

    @Test
    void getAll_returnsAllProducts() {
        service.create(Product.builder().name("A").category("Cat").price(1.0).quantity(1).build());
        service.create(Product.builder().name("B").category("Cat").price(2.0).quantity(2).build());

        List<Product> all = service.getAll();

        assertEquals(2, all.size());
    }

    @Test
    void update_changesFields() {
        Product saved = service.create(
                Product.builder().name("Old").category("Cat").price(1.0).quantity(10).build()
        );
        Product updated = Product.builder()
                .id(saved.id()).name("New").category("NewCat").price(5.0).quantity(50).build();

        service.update(updated);

        Product found = service.getById(saved.id());
        assertEquals("New", found.name());
        assertEquals(5.0, found.price());
    }

    @Test
    void updatePrice_changesPrice() {
        service.create(Product.builder().name("Juice").category("Drinks").price(2.0).quantity(10).build());

        service.updatePrice("Juice", 4.0);

        assertEquals(4.0, service.getByName("Juice").price());
    }

    @Test
    void updatePrice_throws_whenProductNotExists() {
        assertThrows(JdbcTemplate.DatabaseException.class,
                () -> service.updatePrice("NonExistent", 1.0));
    }

    @Test
    void addStock_increasesQuantity() {
        service.create(Product.builder().name("Rice").category("Grains").price(1.0).quantity(10).build());

        service.addStock("Rice", 5);

        assertEquals(15, service.getQuantity("Rice"));
    }

    @Test
    void removeStock_decreasesQuantity() {
        service.create(Product.builder().name("Bread").category("Bakery").price(0.8).quantity(20).build());

        service.removeStock("Bread", 8);

        assertEquals(12, service.getQuantity("Bread"));
    }

    @Test
    void removeStock_throws_whenNotEnoughStock() {
        service.create(Product.builder().name("Eggs").category("Dairy").price(3.0).quantity(5).build());

        assertThrows(JdbcTemplate.DatabaseException.class, () -> service.removeStock("Eggs", 10));
    }

    @Test
    void delete_removesProduct() {
        Product saved = service.create(
                Product.builder().name("ToDelete").category("Cat").price(1.0).quantity(1).build()
        );

        service.delete(saved.id());

        assertThrows(NoSuchElementException.class, () -> service.getById(saved.id()));
    }

    @Test
    void search_byNameAndCategory_returnsCorrectResults() {
        service.create(Product.builder().name("Apple").category("Fruit").price(1.0).quantity(10).build());
        service.create(Product.builder().name("Milk").category("Dairy").price(1.2).quantity(30).build());
        service.create(Product.builder().name("Mango").category("Fruit").price(3.0).quantity(5).build());

        ProductFilter filter = ProductFilter.builder().category("Fruit").build();
        List<Product> results = service.search(filter);

        assertEquals(2, results.size());
    }

    @Test
    void search_byMinQuantity_returnsCorrectResults() {
        service.create(Product.builder().name("A").category("Cat").price(1.0).quantity(5).build());
        service.create(Product.builder().name("B").category("Cat").price(1.0).quantity(50).build());

        ProductFilter filter = ProductFilter.builder().minQuantity(10).build();
        List<Product> results = service.search(filter);

        assertEquals(1, results.size());
        assertEquals("B", results.getFirst().name());
    }

    @Test
    void count_returnsCorrectNumberWithFilter() {
        service.create(Product.builder().name("Apple").category("Fruit").price(1.0).quantity(10).build());
        service.create(Product.builder().name("Milk").category("Dairy").price(1.2).quantity(30).build());

        long count = service.count(ProductFilter.builder().category("Dairy").build());

        assertEquals(1, count);
    }
}