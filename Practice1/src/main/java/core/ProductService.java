package core;

import database.ProductRepository;
import model.Product;
import model.ProductFilter;

import java.util.List;
import java.util.NoSuchElementException;

public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public Product create(Product product) {
        return repository.create(product);
    }

    public Product getById(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found: id=" + id));
    }

    public Product getByName(String name) {
        return repository.findByName(name)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + name));
    }

    public List<Product> getAll() {
        return repository.findAll();
    }

    public List<Product> search(ProductFilter filter) {
        return repository.search(filter);
    }

    public long count(ProductFilter filter) {
        return repository.count(filter);
    }

    public Product update(Product product) {
        return repository.update(product);
    }

    public void updatePrice(String name, double price) {
        repository.updatePrice(name, price);
    }

    public void addStock(String name, int quantity) {
        repository.addStock(name, quantity);
    }

    public void removeStock(String name, int quantity) {
        repository.removeStock(name, quantity);
    }

    public int getQuantity(String name) {
        return getByName(name).quantity();
    }

    public void delete(long id) {
        repository.delete(id);
    }
}