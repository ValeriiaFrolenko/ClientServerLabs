package model;

public class Message{
    private int cType;
    private int bUserId;
    private String message;

    public Message() {}

    public Message(int cType, int bUserId, String message) {
        this.cType = cType;
        this.bUserId = bUserId;
        this.message = message;
    }

    public int getcType() {
        return cType;
    }

    public void setcType(int cType) {
        this.cType = cType;
    }

    public int getbUserId() {
        return bUserId;
    }

    public void setbUserId(int bUserId) {
        this.bUserId = bUserId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}