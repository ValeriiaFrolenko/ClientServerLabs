package core;

public enum Command {
    OK,
    GET_QUANTITY,
    DECREASE_QUANTITY,
    INCREASE_QUANTITY,
    SET_PRICE;

    public static Command fromInt(int i) {
        return switch (i) {
            case 0 -> OK;
            case 1 -> GET_QUANTITY;
            case 2 -> DECREASE_QUANTITY;
            case 3 -> INCREASE_QUANTITY;
            case 4 -> SET_PRICE;
            default -> throw new IllegalArgumentException("Invalid command integer: " + i);
        };
    }
}