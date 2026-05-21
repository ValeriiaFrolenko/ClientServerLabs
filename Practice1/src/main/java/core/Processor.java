package core;

import model.Packet;

public interface Processor {
    void process(Packet packet);
}
