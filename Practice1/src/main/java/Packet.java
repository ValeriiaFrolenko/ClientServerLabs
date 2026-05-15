public class Packet {
    private byte bSrc;
    private long bPktId;
    private Message bMsq;

    public Packet() {}

    public Packet(byte bSrc, long bPktId, Message bMsq) {
        this.bSrc = bSrc;
        this.bPktId = bPktId;
        this.bMsq = bMsq;
    }

    public byte getbSrc() {
        return bSrc;
    }

    public void setbSrc(byte bSrc) {
        this.bSrc = bSrc;
    }

    public long getbPktId() {
        return bPktId;
    }

    public void setbPktId(long bPktId) {
        this.bPktId = bPktId;
    }

    public Message getbMsq() {
        return bMsq;
    }

    public void setbMsq(Message bMsq) {
        this.bMsq = bMsq;
    }
}