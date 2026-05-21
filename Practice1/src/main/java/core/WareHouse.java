package core;

import model.Product;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class WareHouse {
    private final ConcurrentMap<String, Product> products = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> groups = new ConcurrentHashMap<>();

    public void addProduct(Product product) {
        if (products.putIfAbsent(product.getName(), product) != null) {
            throw new IllegalArgumentException("Product already exists: " + product.getName());
        }
    }

    public int getProductQuantity(String name) {
        Product product = products.get(name);
        if (product == null) {
            throw new NoSuchElementException("Product not found: " + name);
        }
        return product.getQuantity();
    }

    public void addStock(String name, int quantity) {
        products.compute(name, (_, product) -> {
            if (product == null) {
                throw new NoSuchElementException("Product not found: " + name);
            }
            product.setQuantity(product.getQuantity() + quantity);
            return product;
        });
    }

    public void removeStock(String name, int quantity) {
        products.compute(name, (_, product) -> {
            if (product == null) {
                throw new NoSuchElementException("Product not found: " + name);
            }
            if (product.getQuantity() < quantity) {
                throw new IllegalArgumentException("Not enough stock for product: " + name);
            }
            product.setQuantity(product.getQuantity() - quantity);
            return product;
        });
    }

    public void addGroup(String groupName) {
        groups.putIfAbsent(groupName, ConcurrentHashMap.newKeySet());
    }

    public void addProductToGroup(String groupName, String productName) {
        Set<String> group = groups.get(groupName);
        if (group == null) {
            throw new NoSuchElementException("Group not found: " + groupName);
        }
        if (!products.containsKey(productName)) {
            throw new NoSuchElementException("Product not found: " + productName);
        }
        group.add(productName);
    }

    public void updatePrice(String productName, double price) {
        products.compute(productName, (_, product) -> {
            if (product == null) {
                throw new NoSuchElementException("Product not found: " + productName);
            }
            product.setPrice(price);
            return product;
        });
    }
}