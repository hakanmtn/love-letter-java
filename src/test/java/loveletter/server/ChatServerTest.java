package loveletter.server;

import loveletter.model.CardType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ChatServerTest {

    @Test
    void nicknamesShouldBeUniqueIgnoringCase(){
        ChatServer server = new ChatServer(5500);

        assertTrue(server.registerNickname("Hakan"));
        assertFalse(server.registerNickname("Hakan"));
        assertFalse(server.registerNickname("hakan"));

    }

    @Test
    void blankNicknameShouldBeRejected(){
        ChatServer server = new ChatServer(5500);

        assertFalse(server.registerNickname(""));
        assertFalse(server.registerNickname("    "));

    }

    @Test
    void nicknameShouldBeReusableAfterUnregistering(){
        ChatServer server = new ChatServer(5500);

        assertTrue(server.registerNickname("Hakan"));

        server.unregisterNickname("Hakan");
        assertTrue(server.registerNickname("Hakan"));

    }

    @Test
    void parseCardTypeShouldAcceptMixedCaseAndSurroundingSpaces(){
        ChatServer server = new ChatServer(5500);

        assertEquals(CardType.PRIEST, server.parseCardType(" PRiest "));
    }

    @Test
    void parseCardTypeShouldRejectUnknowCardName(){
        ChatServer server = new ChatServer(5500);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> server.parseCardType("wizard"));

        assertTrue(exception.getMessage().contains("Unknown card: wizard"));
    }
}
