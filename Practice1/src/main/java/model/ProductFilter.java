package model;

public record ProductFilter(
        String name,
        String category,
        Double minPrice,
        Double maxPrice,
        Integer minQuantity,
        Integer maxQuantity,
        int page,
        int pageSize
) {
    public int getOffset() {
        return page * pageSize;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String category;
        private Double minPrice;
        private Double maxPrice;
        private Integer minQuantity;
        private Integer maxQuantity;
        private int page = 0;
        private int pageSize = 10;

        public Builder name(String name) { this.name = name; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder minPrice(Double minPrice) { this.minPrice = minPrice; return this; }
        public Builder maxPrice(Double maxPrice) { this.maxPrice = maxPrice; return this; }
        public Builder minQuantity(Integer minQuantity) { this.minQuantity = minQuantity; return this; }
        public Builder maxQuantity(Integer maxQuantity) { this.maxQuantity = maxQuantity; return this; }
        public Builder page(int page) { this.page = page; return this; }
        public Builder pageSize(int pageSize) { this.pageSize = pageSize; return this; }

        public ProductFilter build() {
            return new ProductFilter(name, category, minPrice, maxPrice, minQuantity, maxQuantity, page, pageSize);
        }
    }
}