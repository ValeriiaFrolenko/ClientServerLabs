import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageStructureTest {

    @Test
    void shouldHaveCorrectLengths() {
        assertEquals(4, MessageStructure.LEN_CMD_TYPE);
        assertEquals(4, MessageStructure.LEN_USER_ID);
    }

    @Test
    void shouldHaveCorrectOffsets() {
        assertEquals(0, MessageStructure.OFFSET_CMD_TYPE);
        assertEquals(4, MessageStructure.OFFSET_USER_ID);
        assertEquals(8, MessageStructure.OFFSET_PAYLOAD);
        assertEquals(8, MessageStructure.MESSAGE_HEADER_SIZE);
    }
}