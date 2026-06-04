package database;

import model.Product;
import model.ProductFilter;
import org.junit.jupiter.api.*;

import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProductRepositoryTest {

    private static final String URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    private ProductRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        try (var conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS products (
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
        repository = new ProductRepository(jdbc);
    }

    @AfterEach
    void tearDown() throws Exception {
        try (var conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS products");
        }
    }

    @Test
    void create_savesProductAndReturnsWithId() {
        Product product = Product.builder()
                .name("Apple").category("Fruit").price(1.5).quantity(100).build();

        Product saved = repository.create(product);

        assertTrue(saved.id() > 0);
        assertEquals("Apple", saved.name());
        assertEquals("Fruit", saved.category());
        assertEquals(1.5, saved.price());
        assertEquals(100, saved.quantity());
    }

    @Test
    void findById_returnsProduct_whenExists() {
        Product saved = repository.create(
                Product.builder().name("Banana").category("Fruit").price(0.5).quantity(50).build()
        );

        Optional<Product> found = repository.findById(saved.id());

        assertTrue(found.isPresent());
        assertEquals("Banana", found.get().name());
    }

    @Test
    void findById_returnsEmpty_whenNotExists() {
        Optional<Product> found = repository.findById(999L);
        assertTrue(found.isEmpty());
    }

    @Test
    void findByName_returnsProduct_whenExists() {
        repository.create(
                Product.builder().name("Milk").category("Dairy").price(1.2).quantity(30).build()
        );

        Optional<Product> found = repository.findByName("Milk");

        assertTrue(found.isPresent());
        assertEquals("Milk", found.get().name());
    }

    @Test
    void findByName_returnsEmpty_whenNotExists() {
        Optional<Product> found = repository.findByName("NonExistent");
        assertTrue(found.isEmpty());
    }

    @Test
    void findAll_returnsAllProducts() {
        repository.create(Product.builder().name("A").category("Cat1").price(1.0).quantity(10).build());
        repository.create(Product.builder().name("B").category("Cat2").price(2.0).quantity(20).build());

        List<Product> all = repository.findAll();

        assertEquals(2, all.size());
    }

    @Test
    void update_changesProductFields() {
        Product saved = repository.create(
                Product.builder().name("OldName").category("Cat").price(1.0).quantity(10).build()
        );
        Product updated = Product.builder()
                .id(saved.id()).name("NewName").category("NewCat").price(9.9).quantity(99).build();

        repository.update(updated);

        Product found = repository.findById(saved.id()).orElseThrow();
        assertEquals("NewName", found.name());
        assertEquals("NewCat", found.category());
        assertEquals(9.9, found.price());
        assertEquals(99, found.quantity());
    }

    @Test
    void update_throws_whenProductNotExists() {
        Product nonExistent = Product.builder()
                .id(999L).name("X").category("Y").price(1.0).quantity(1).build();

        assertThrows(JdbcTemplate.DatabaseException.class, () -> repository.update(nonExistent));
    }

    @Test
    void updatePrice_changesPrice() {
        repository.create(
                Product.builder().name("Juice").category("Drinks").price(2.0).quantity(10).build()
        );

        repository.updatePrice("Juice", 3.5);

        Product found = repository.findByName("Juice").orElseThrow();
        assertEquals(3.5, found.price());
    }

    @Test
    void addStock_increasesQuantity() {
        repository.create(
                Product.builder().name("Rice").category("Grains").price(1.0).quantity(10).build()
        );

        repository.addStock("Rice", 5);

        assertEquals(15, repository.findByName("Rice").orElseThrow().quantity());
    }

    @Test
    void removeStock_decreasesQuantity() {
        repository.create(
                Product.builder().name("Bread").category("Bakery").price(0.8).quantity(20).build()
        );

        repository.removeStock("Bread", 5);

        assertEquals(15, repository.findByName("Bread").orElseThrow().quantity());
    }

    @Test
    void removeStock_throws_whenNotEnoughStock() {
        repository.create(
                Product.builder().name("Eggs").category("Dairy").price(3.0).quantity(5).build()
        );

        assertThrows(JdbcTemplate.DatabaseException.class, () -> repository.removeStock("Eggs", 10));
    }

    @Test
    void delete_removesProduct() {
        Product saved = repository.create(
                Product.builder().name("ToDelete").category("Cat").price(1.0).quantity(1).build()
        );

        repository.delete(saved.id());

        assertTrue(repository.findById(saved.id()).isEmpty());
    }

    @Test
    void delete_throws_whenNotExists() {
        assertThrows(JdbcTemplate.DatabaseException.class, () -> repository.delete(999L));
    }

    @Test
    void search_byName_returnsMatchingProducts() {
        repository.create(Product.builder().name("Apple").category("Fruit").price(1.0).quantity(10).build());
        repository.create(Product.builder().name("Pineapple").category("Fruit").price(2.0).quantity(5).build());
        repository.create(Product.builder().name("Milk").category("Dairy").price(1.2).quantity(30).build());

        ProductFilter filter = ProductFilter.builder().name("apple").build();
        List<Product> results = repository.search(filter);

        assertEquals(2, results.size());
    }

    @Test
    void search_byCategory_returnsMatchingProducts() {
        repository.create(Product.builder().name("Apple").category("Fruit").price(1.0).quantity(10).build());
        repository.create(Product.builder().name("Milk").category("Dairy").price(1.2).quantity(30).build());
        repository.create(Product.builder().name("Cheese").category("Dairy").price(3.0).quantity(15).build());

        ProductFilter filter = ProductFilter.builder().category("Dairy").build();
        List<Product> results = repository.search(filter);

        assertEquals(2, results.size());
    }

    @Test
    void search_byMinPrice_returnsMatchingProducts() {
        repository.create(Product.builder().name("Cheap").category("Cat").price(1.0).quantity(10).build());
        repository.create(Product.builder().name("Expensive").category("Cat").price(10.0).quantity(5).build());

        ProductFilter filter = ProductFilter.builder().minPrice(5.0).build();
        List<Product> results = repository.search(filter);

        assertEquals(1, results.size());
        assertEquals("Expensive", results.getFirst().name());
    }

    @Test
    void search_byPriceRange_returnsMatchingProducts() {
        repository.create(Product.builder().name("A").category("Cat").price(1.0).quantity(10).build());
        repository.create(Product.builder().name("B").category("Cat").price(5.0).quantity(10).build());
        repository.create(Product.builder().name("C").category("Cat").price(10.0).quantity(10).build());

        ProductFilter filter = ProductFilter.builder().minPrice(2.0).maxPrice(8.0).build();
        List<Product> results = repository.search(filter);

        assertEquals(1, results.size());
        assertEquals("B", results.getFirst().name());
    }

    @Test
    void search_byCategoryAndMinPrice_returnsMatchingProducts() {
        repository.create(Product.builder().name("Apple").category("Fruit").price(1.0).quantity(10).build());
        repository.create(Product.builder().name("Mango").category("Fruit").price(5.0).quantity(5).build());
        repository.create(Product.builder().name("Milk").category("Dairy").price(5.0).quantity(30).build());

        ProductFilter filter = ProductFilter.builder().category("Fruit").minPrice(3.0).build();
        List<Product> results = repository.search(filter);

        assertEquals(1, results.size());
        assertEquals("Mango", results.getFirst().name());
    }

    @Test
    void search_noFilters_returnsAllProducts() {
        repository.create(Product.builder().name("A").category("Cat").price(1.0).quantity(1).build());
        repository.create(Product.builder().name("B").category("Cat").price(2.0).quantity(2).build());

        ProductFilter filter = ProductFilter.builder().build();
        List<Product> results = repository.search(filter);

        assertEquals(2, results.size());
    }

    @Test
    void search_withPagination_returnsCorrectPage() {
        for (int i = 1; i <= 5; i++) {
            repository.create(Product.builder().name("Product" + i).category("Cat").price(i).quantity(i).build());
        }

        ProductFilter filter = ProductFilter.builder().pageSize(2).page(1).build();
        List<Product> results = repository.search(filter);

        assertEquals(2, results.size());
    }

    @Test
    void count_returnsCorrectTotal() {
        repository.create(Product.builder().name("Apple").category("Fruit").price(1.0).quantity(10).build());
        repository.create(Product.builder().name("Milk").category("Dairy").price(1.2).quantity(30).build());

        ProductFilter filter = ProductFilter.builder().category("Fruit").build();
        long count = repository.count(filter);

        assertEquals(1, count);
    }
}