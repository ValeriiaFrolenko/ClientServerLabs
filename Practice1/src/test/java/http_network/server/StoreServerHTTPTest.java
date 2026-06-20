package http_network.server;

import database.JdbcTemplate;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class StoreServerHTTPTest {

    private static final String DB_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    private static StoreServerHTTP server;
    private static String token;

    @BeforeAll
    static void startServer() throws Exception {
        try (var conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
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
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                        login         VARCHAR(255) NOT NULL UNIQUE,
                        password_hash VARCHAR(255) NOT NULL
                    )
                    """);
        }

        JdbcTemplate jdbc = new JdbcTemplate(
                () -> DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)
        );

        CountDownLatch ready = new CountDownLatch(1);
        server = new StoreServerHTTP(8082, jdbc);
        Thread serverThread = new Thread(() -> server.start(ready));
        serverThread.setDaemon(true);
        serverThread.start();
        ready.await();

        RestAssured.baseURI = "http://127.0.0.1";
        RestAssured.port = 8082;

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("login", "testuser", "password", "testpass"))
                .post("/register");

        token = given()
                .contentType(ContentType.JSON)
                .body(Map.of("login", "testuser", "password", "testpass"))
                .post("/login")
                .then()
                .statusCode(200)
                .extract()
                .path("token");
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @BeforeEach
    void clearProducts() throws Exception {
        try (var conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM products");
        }
    }

    // --- login ---

    @Test
    void login_validCredentials_returns200AndToken() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("login", "testuser", "password", "testpass"))
                .post("/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue());
    }

    @Test
    void login_invalidPassword_returns401() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("login", "testuser", "password", "wrongpass"))
                .post("/login")
                .then()
                .statusCode(401);
    }

    // --- auth ---

    @Test
    void getProduct_withoutToken_returns401() {
        given()
                .get("/products/1")
                .then()
                .statusCode(401);
    }

    // --- PUT /products ---

    @Test
    void createProduct_returns201() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("name", "Banana", "category", "Fruits", "price", 1.5, "quantity", 100))
                .put("/products")
                .then()
                .statusCode(201)
                .body("name", equalTo("Banana"))
                .body("id", notNullValue());
    }

    @Test
    void createProduct_duplicateName_returns409() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("name", "Orange", "category", "Fruits", "price", 2.0, "quantity", 50))
                .put("/products");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("name", "Orange", "category", "Fruits", "price", 2.0, "quantity", 50))
                .put("/products")
                .then()
                .statusCode(409);
    }

    // --- GET /products/{id} ---

    @Test
    void getProduct_existingId_returns200() {
        long id = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("name", "Mango", "category", "Fruits", "price", 3.0, "quantity", 20))
                .put("/products")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        given()
                .header("Authorization", "Bearer " + token)
                .get("/products/" + id)
                .then()
                .statusCode(200)
                .body("name", equalTo("Mango"));
    }

    @Test
    void getProduct_nonExistingId_returns404() {
        given()
                .header("Authorization", "Bearer " + token)
                .get("/products/99999")
                .then()
                .statusCode(404);
    }

    // --- POST /products/{id} ---

    @Test
    void updateProduct_returns200WithUpdatedData() {
        long id = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("name", "Apple", "category", "Fruits", "price", 1.0, "quantity", 10))
                .put("/products")
                .then()
                .extract()
                .path("id");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("name", "Apple", "category", "UpdatedFruits", "price", 2.0, "quantity", 5))
                .post("/products/" + id)
                .then()
                .statusCode(200)
                .body("category", equalTo("UpdatedFruits"))
                .body("price", equalTo(2.0f));
    }

    // --- DELETE /products/{id} ---

    @Test
    void deleteProduct_returns204() {
        long id = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("name", "Grape", "category", "Fruits", "price", 4.0, "quantity", 30))
                .put("/products")
                .then()
                .extract()
                .path("id");

        given()
                .header("Authorization", "Bearer " + token)
                .delete("/products/" + id)
                .then()
                .statusCode(204);
    }

    @Test
    void deleteProduct_thenGet_returns404() {
        long id = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("name", "Pear", "category", "Fruits", "price", 2.5, "quantity", 15))
                .put("/products")
                .then()
                .extract()
                .path("id");

        given()
                .header("Authorization", "Bearer " + token)
                .delete("/products/" + id);

        given()
                .header("Authorization", "Bearer " + token)
                .get("/products/" + id)
                .then()
                .statusCode(404);
    }
}