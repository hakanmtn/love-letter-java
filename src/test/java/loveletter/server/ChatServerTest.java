package loveletter.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
