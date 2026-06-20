package model;

public record Product(
        long id,
        String name,
        String category,
        double price,
        int quantity
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long id;
        private String name;
        private String category;
        private double price;
        private int quantity;

        public Builder id(long id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder price(double price) { this.price = price; return this; }
        public Builder quantity(int quantity) { this.quantity = quantity; return this; }

        public Product build() {
            return new Product(id, name, category, price, quantity);
        }
    }
}