package database;

import model.Product;
import model.ProductFilter;

import java.util.List;
import java.util.Optional;

public class ProductRepository {

    private final JdbcTemplate jdbc;

    private static final JdbcTemplate.RowMapper<Product> PRODUCT_MAPPER = rs -> Product.builder()
            .id(rs.getLong("id"))
            .name(rs.getString("name"))
            .category(rs.getString("category"))
            .price(rs.getDouble("price"))
            .quantity(rs.getInt("quantity"))
            .build();

    public ProductRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Product create(Product product) {
        long id = jdbc.insert(
                "INSERT INTO products (name, category, price, quantity) VALUES (?, ?, ?, ?)",
                product.name(),
                product.category(),
                product.price(),
                product.quantity()
        );
        return Product.builder()
                .id(id)
                .name(product.name())
                .category(product.category())
                .price(product.price())
                .quantity(product.quantity())
                .build();
    }

    public Optional<Product> findById(long id) {
        return jdbc.queryOne(
                "SELECT * FROM products WHERE id = ?",
                PRODUCT_MAPPER,
                id
        );
    }

    public Optional<Product> findByName(String name) {
        return jdbc.queryOne(
                "SELECT * FROM products WHERE name = ?",
                PRODUCT_MAPPER,
                name
        );
    }

    public List<Product> findAll() {
        return jdbc.query("SELECT * FROM products", PRODUCT_MAPPER);
    }

    public List<Product> search(ProductFilter filter) {
        QueryBuilder qb = new QueryBuilder("SELECT * FROM products");
        applyFilters(qb, filter);
        qb.limit(filter.pageSize(), filter.getOffset());
        return jdbc.query(qb.sql(), PRODUCT_MAPPER, qb.params());
    }

    public long count(ProductFilter filter) {
        QueryBuilder qb = new QueryBuilder("SELECT COUNT(*) FROM products");
        applyFilters(qb, filter);
        return jdbc.queryForLong(qb.sql(), qb.params());
    }

    public Product update(Product product) {
        int affected = jdbc.update(
                "UPDATE products SET name = ?, category = ?, price = ?, quantity = ? WHERE id = ?",
                product.name(),
                product.category(),
                product.price(),
                product.quantity(),
                product.id()
        );
        if (affected == 0) {
            throw new JdbcTemplate.DatabaseException("product not found: id=" + product.id());
        }
        return product;
    }

    public void updatePrice(String name, double price) {
        int affected = jdbc.update(
                "UPDATE products SET price = ? WHERE name = ?",
                price, name
        );
        if (affected == 0) {
            throw new JdbcTemplate.DatabaseException("product not found: " + name);
        }
    }

    public void addStock(String name, int quantity) {
        int affected = jdbc.update(
                "UPDATE products SET quantity = quantity + ? WHERE name = ?",
                quantity, name
        );
        if (affected == 0) {
            throw new JdbcTemplate.DatabaseException("product not found: " + name);
        }
    }

    public void removeStock(String name, int quantity) {
        int affected = jdbc.update(
                "UPDATE products SET quantity = quantity - ? WHERE name = ? AND quantity >= ?",
                quantity, name, quantity
        );
        if (affected == 0) {
            throw new JdbcTemplate.DatabaseException("product not found or not enough stock: " + name);
        }
    }

    public void delete(long id) {
        int affected = jdbc.update("DELETE FROM products WHERE id = ?", id);
        if (affected == 0) {
            throw new JdbcTemplate.DatabaseException("product not found: id=" + id);
        }
    }

    private void applyFilters(QueryBuilder qb, ProductFilter filter) {
        if (filter.name() != null) {
            qb.andIfNotNull("LOWER(name) LIKE ?", "%" + filter.name().toLowerCase() + "%");
        }
        qb.andIfNotNull("category = ?", filter.category());
        qb.andIfNotNull("price >= ?", filter.minPrice());
        qb.andIfNotNull("price <= ?", filter.maxPrice());
        qb.andIfNotNull("quantity >= ?", filter.minQuantity());
        qb.andIfNotNull("quantity <= ?", filter.maxQuantity());
    }
}