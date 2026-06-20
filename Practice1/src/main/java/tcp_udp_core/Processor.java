package tcp_udp_core;

import model.Packet;

public interface Processor {
    void process(Packet packet);
}
