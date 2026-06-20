package tcp_udp_core;

public enum Command {
    OK,
    GET_QUANTITY,
    DECREASE_QUANTITY,
    INCREASE_QUANTITY,
    SET_PRICE,
    CREATE_PRODUCT,
    GET_PRODUCT,
    UPDATE_PRODUCT,
    DELETE_PRODUCT,
    SEARCH_PRODUCTS;

    public static Command fromInt(int i) {
        return switch (i) {
            case 0 -> OK;
            case 1 -> GET_QUANTITY;
            case 2 -> DECREASE_QUANTITY;
            case 3 -> INCREASE_QUANTITY;
            case 4 -> SET_PRICE;
            case 5 -> CREATE_PRODUCT;
            case 6 -> GET_PRODUCT;
            case 7 -> UPDATE_PRODUCT;
            case 8 -> DELETE_PRODUCT;
            case 9 -> SEARCH_PRODUCTS;
            default -> throw new IllegalArgumentException("Invalid command integer: " + i);
        };
    }
}