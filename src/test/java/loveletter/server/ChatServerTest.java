package loveletter.server;

import loveletter.model.CardType;
import org.junit.jupiter.api.Test;
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

        List<String> expectedMessages = List.of(
                "A new Love Letter game has been created."
        );

        assertEquals(expectedMessages, hakan.getMessages());
        assertEquals(expectedMessages, nati.getMessages());
    }

    @Test
    void createWhenGameExistsShouldOnlyNotifySender(){
        //Arrange: Zwei Clients und ein bereits erstelltes Spiel
        ChatServer server = new ChatServer(5500);
        TestClientHandler hakan = new TestClientHandler(server, "Hakan");
        TestClientHandler nati = new TestClientHandler(server, "Nati");

        server.addClient(hakan);
        server.addClient(nati);

        server.handleCommand(hakan, "/create");

        //Act: Nati versucht, ein weiteres Spiel zu erstellen
        boolean handled = server.handleCommand(nati,"/create");

        //Assert
        assertTrue(handled);

        assertEquals(List.of("A new Love Letter game has been created."), hakan.getMessages());

        assertEquals(List.of("A new Love Letter game has been created.",
        "A game already exists."), nati.getMessages());

    }

    @Test
    void joinShouldNotifyAllClient(){
        //Arrange
        ChatServer server = new ChatServer(5500);
        TestClientHandler hakan = new TestClientHandler(server, "Hakan");
        TestClientHandler nati = new TestClientHandler(server, "Nati");

        server.addClient(hakan);
        server.addClient(nati);
        server.handleCommand(hakan, "/create");

        //Act
        boolean handled = server.handleCommand(hakan, "/join");

        //Assert
        assertTrue(handled);

        List<String> expectedMessages = List.of("A new Love Letter game has been created.",
                "Hakan joined the game. Players: 1/4");

        assertEquals(expectedMessages, hakan.getMessages());
        assertEquals(expectedMessages, nati.getMessages());

    }

   @Test
   void joinTwiceShouldOnlyNotifySender(){

        //Arrange: Hakan ist bereits dem Spiel beigetreten
        ChatServer server = new ChatServer(5500);

        TestClientHandler hakan = new TestClientHandler(server, "Hakan");
        TestClientHandler nati = new TestClientHandler(server, "Nati");

        server.addClient(hakan);
        server.addClient(nati);

        server.handleCommand(hakan, "/create");
        server.handleCommand(hakan, "/join");

        //Act: Hakan versucht erneut beizutreten
        boolean handled = server.handleCommand(hakan, "/join");

        //Assert
        assertTrue(handled);

        assertEquals(List.of("A new Love Letter game has been created.",
                            "Hakan joined the game. Players: 1/4",
                            "You have already joined the game."),
                hakan.getMessages());

        assertEquals(List.of("A new Love Letter game has been created.",
                       "Hakan joined the game. Players: 1/4"),
               nati.getMessages());

        server.handleCommand(nati, "/join");

        String expectedJoinMessage = "Nati joined the game. Players: 2/4";

        assertEquals(expectedJoinMessage, hakan.getMessages().getLast());
        assertEquals(expectedJoinMessage, nati.getMessages().getLast());


   }
   @Test
   void startWithoutJoiningShouldOnlyNotifySender(){
       // Arrange: Spiel vorhanden, aber Nati ist nicht beigetreten
       ChatServer server = new ChatServer(5500);
       TestClientHandler hakan = new TestClientHandler(server, "hakan");
       TestClientHandler nati = new TestClientHandler(server, "nati");

       server.addClient(hakan);
       server.addClient(nati);

       server.handleCommand(hakan, "/create");
       server.handleCommand(hakan, "/join");

       //Act
       boolean handled = server.handleCommand(nati, "/start");

       //Assert
       assertTrue(handled);

       assertEquals(List.of("A new Love Letter game has been created.",
                             "hakan joined the game. Players: 1/4" ), hakan.getMessages());

       assertEquals(List.of("A new Love Letter game has been created.",
               "hakan joined the game. Players: 1/4",
               "Join the game first with /join." ), nati.getMessages());


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
