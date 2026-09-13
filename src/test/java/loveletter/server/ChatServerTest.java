package loveletter.server;

import loveletter.model.CardType;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Native;
import java.util.ArrayList;
import java.util.List;

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

    @Test
    void joinWithoutGameShouldOnlyNotifySender(){
        // Arrange: Server und zwei Clients vorbereiten
        ChatServer server = new ChatServer(5500);
        TestClientHandler hakan = new TestClientHandler(server,"hakan");
        TestClientHandler nati = new TestClientHandler(server, "nati");

        server.addClient(hakan);
        server.addClient(nati);

        //Act: Hakan versucht, ohne Vorhandenes Spiel beizutreten
        boolean handled = server.handleCommand(hakan, "/join");

        //Assert: Befehl erkannt, Fehlermeldung nur an Hakan
        assertTrue(handled);

        assertEquals(List.of("Create a game first with /create."), hakan.getMessages());

        assertTrue(nati.getMessages().isEmpty());
    }

    @Test
    void createShouldNotifyAllClients(){
        //Arrange
        ChatServer server = new ChatServer(5500);
        TestClientHandler hakan = new TestClientHandler(server,"Hakan");
        TestClientHandler nati = new TestClientHandler(server, "nati");

        server.addClient(hakan);
        server.addClient(nati);

        //Act
        boolean handled = server.handleCommand(hakan,"/create");

        //Assert
        assertTrue(handled);

        List<String> expenctedMessages = List.of(
                "A new Love Letter game has been created."
        );

        assertEquals(expenctedMessages, hakan.getMessages());
        assertEquals(expenctedMessages, nati.getMessages());
    }

    private static class TestClientHandler extends ClientHandler {

        private final String nickname;
        private final List<String> messages = new ArrayList<>();

        TestClientHandler(ChatServer server, String nickname){
            super(null,server);
            this.nickname = nickname;
        }

        @Override
        public String getNickname(){
            return nickname;
        }

        @Override
        public synchronized void sendMessage(String message){
            messages.add(message);
        }

        List<String> getMessages() {
            return List.copyOf(messages);
        }
    }

}
