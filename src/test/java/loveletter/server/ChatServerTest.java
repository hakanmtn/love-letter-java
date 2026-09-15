package loveletter.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import loveletter.model.CardType;
import org.junit.jupiter.api.Test;

public class ChatServerTest {

  @Test
  void nicknamesShouldBeUniqueIgnoringCase() {
    ChatServer server = new ChatServer(5500);

    assertTrue(server.registerNickname("Hakan"));
    assertFalse(server.registerNickname("Hakan"));
    assertFalse(server.registerNickname("hakan"));
  }

  @Test
  void blankNicknameShouldBeRejected() {
    ChatServer server = new ChatServer(5500);

    assertFalse(server.registerNickname(""));
    assertFalse(server.registerNickname("    "));
  }

  @Test
  void nicknameShouldBeReusableAfterUnregistering() {
    ChatServer server = new ChatServer(5500);

    assertTrue(server.registerNickname("Hakan"));

    server.unregisterNickname("Hakan");
    assertTrue(server.registerNickname("Hakan"));
  }

  @Test
  void parseCardTypeShouldAcceptMixedCaseAndSurroundingSpaces() {
    ChatServer server = new ChatServer(5500);

    assertEquals(CardType.PRIEST, server.parseCardType(" PRiest "));
  }

  @Test
  void parseCardTypeShouldRejectUnknowCardName() {
    ChatServer server = new ChatServer(5500);

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> server.parseCardType("wizard"));

    assertTrue(exception.getMessage().contains("Unknown card: wizard"));
  }

  @Test
  void joinWithoutGameShouldOnlyNotifySender() {
    // Arrange: Server und zwei Clients vorbereiten
    ChatServer server = new ChatServer(5500);
    TestClientHandler hakan = new TestClientHandler(server, "hakan");
    TestClientHandler nati = new TestClientHandler(server, "nati");

    server.addClient(hakan);
    server.addClient(nati);

    // Act: Hakan versucht, ohne Vorhandenes Spiel beizutreten
    boolean handled = server.handleCommand(hakan, "/join");

    // Assert: Befehl erkannt, Fehlermeldung nur an Hakan
    assertTrue(handled);

    assertEquals(List.of("Create a game first with /create."), hakan.getMessages());

    assertTrue(nati.getMessages().isEmpty());
  }

  @Test
  void createShouldNotifyAllClients() {
    // Arrange
    ChatServer server = new ChatServer(5500);
    TestClientHandler hakan = new TestClientHandler(server, "Hakan");
    TestClientHandler nati = new TestClientHandler(server, "nati");

    server.addClient(hakan);
    server.addClient(nati);

    // Act
    boolean handled = server.handleCommand(hakan, "/create");

    // Assert
    assertTrue(handled);

    List<String> expectedMessages = List.of("A new Love Letter game has been created.");

    assertEquals(expectedMessages, hakan.getMessages());
    assertEquals(expectedMessages, nati.getMessages());
  }

  @Test
  void createWhenGameExistsShouldOnlyNotifySender() {
    // Arrange: Zwei Clients und ein bereits erstelltes Spiel
    ChatServer server = new ChatServer(5500);
    TestClientHandler hakan = new TestClientHandler(server, "Hakan");
    TestClientHandler nati = new TestClientHandler(server, "Nati");

    server.addClient(hakan);
    server.addClient(nati);

    server.handleCommand(hakan, "/create");

    // Act: Nati versucht, ein weiteres Spiel zu erstellen
    boolean handled = server.handleCommand(nati, "/create");

    // Assert
    assertTrue(handled);

    assertEquals(List.of("A new Love Letter game has been created."), hakan.getMessages());

    assertEquals(
        List.of("A new Love Letter game has been created.", "A game already exists."),
        nati.getMessages());
  }

  @Test
  void joinShouldNotifyAllClient() {
    // Arrange
    ChatServer server = new ChatServer(5500);
    TestClientHandler hakan = new TestClientHandler(server, "Hakan");
    TestClientHandler nati = new TestClientHandler(server, "Nati");

    server.addClient(hakan);
    server.addClient(nati);
    server.handleCommand(hakan, "/create");

    // Act
    boolean handled = server.handleCommand(hakan, "/join");

    // Assert
    assertTrue(handled);

    List<String> expectedMessages =
        List.of("A new Love Letter game has been created.", "Hakan joined the game. Players: 1/4");

    assertEquals(expectedMessages, hakan.getMessages());
    assertEquals(expectedMessages, nati.getMessages());
  }

  @Test
  void joinTwiceShouldOnlyNotifySender() {

    // Arrange: Hakan ist bereits dem Spiel beigetreten
    ChatServer server = new ChatServer(5500);

    TestClientHandler hakan = new TestClientHandler(server, "Hakan");
    TestClientHandler nati = new TestClientHandler(server, "Nati");

    server.addClient(hakan);
    server.addClient(nati);

    server.handleCommand(hakan, "/create");
    server.handleCommand(hakan, "/join");

    // Act: Hakan versucht erneut beizutreten
    boolean handled = server.handleCommand(hakan, "/join");

    // Assert
    assertTrue(handled);

    assertEquals(
        List.of(
            "A new Love Letter game has been created.",
            "Hakan joined the game. Players: 1/4",
            "You have already joined the game."),
        hakan.getMessages());

    assertEquals(
        List.of("A new Love Letter game has been created.", "Hakan joined the game. Players: 1/4"),
        nati.getMessages());

    server.handleCommand(nati, "/join");

    String expectedJoinMessage = "Nati joined the game. Players: 2/4";

    assertEquals(expectedJoinMessage, hakan.getMessages().getLast());
    assertEquals(expectedJoinMessage, nati.getMessages().getLast());
  }

  @Test
  void startWithoutJoiningShouldOnlyNotifySender() {
    // Arrange: Spiel vorhanden, aber Nati ist nicht beigetreten
    ChatServer server = new ChatServer(5500);
    TestClientHandler hakan = new TestClientHandler(server, "hakan");
    TestClientHandler nati = new TestClientHandler(server, "nati");

    server.addClient(hakan);
    server.addClient(nati);

    server.handleCommand(hakan, "/create");
    server.handleCommand(hakan, "/join");

    // Act
    boolean handled = server.handleCommand(nati, "/start");

    // Assert
    assertTrue(handled);

    assertEquals(
        List.of("A new Love Letter game has been created.", "hakan joined the game. Players: 1/4"),
        hakan.getMessages());

    assertEquals(
        List.of(
            "A new Love Letter game has been created.",
            "hakan joined the game. Players: 1/4",
            "Join the game first with /join."),
        nati.getMessages());
  }

  @Test
  void startWithOnePlayerShouldOnlyNotifySender() {
    // Arrange: 2 Chat-Clients, aber nur ein Spieler
    ChatServer server = new ChatServer(5500);
    TestClientHandler hakan = new TestClientHandler(server, "Hakan");
    TestClientHandler nati = new TestClientHandler(server, "Nati");

    server.addClient(hakan);
    server.addClient(nati);

    server.handleCommand(hakan, "/create");
    server.handleCommand(hakan, "/join");

    // Act
    boolean handled = server.handleCommand(hakan, "/start");

    // Assert
    assertTrue(handled);

    assertEquals(
        List.of(
            "A new Love Letter game has been created.",
            "Hakan joined the game. Players: 1/4",
            "Cannot start game: At least two players are required"),
        hakan.getMessages());

    assertEquals(
        List.of("A new Love Letter game has been created.", "Hakan joined the game. Players: 1/4"),
        nati.getMessages());
  }

  @Test
  void startWithTwoPlayersShouldNotifyPlayersAndDealHands() {
    // Arrange: 2 Chat-Clients, aber nur ein Spieler
    ChatServer server = new ChatServer(5500);
    TestClientHandler hakan = new TestClientHandler(server, "Hakan");
    TestClientHandler nati = new TestClientHandler(server, "Nati");

    server.addClient(hakan);
    server.addClient(nati);

    server.handleCommand(hakan, "/create");
    server.handleCommand(hakan, "/join");
    server.handleCommand(nati, "/join");

    int hakanMessagesBefore = hakan.getMessages().size();
    int natiMessagesBefore = nati.getMessages().size();

    boolean handled = server.handleCommand(hakan, "/start");

    assertTrue(handled);

    List<String> hakanMessages = hakan.getMessages();
    List<String> natiMessages = nati.getMessages();

    assertEquals(hakanMessagesBefore + 3, hakanMessages.size());
    assertEquals(natiMessagesBefore + 3, natiMessages.size());

    assertEquals("The Love Letter game has started.", hakanMessages.get(hakanMessagesBefore));
    assertEquals("The Love Letter game has started.", natiMessages.get(natiMessagesBefore));

    assertEquals("Current Player: Hakan", hakanMessages.get(hakanMessagesBefore + 1));
    assertEquals("Current Player: Hakan", natiMessages.get(natiMessagesBefore + 1));

    String cardNames = "GUARD|PRIEST|BARON|HANDMAID|PRINCE|KING|COUNTESS|PRINCESS";
    String cardPattern = "(" + cardNames + ")";

    assertTrue(
        hakanMessages
            .getLast()
            .matches("Your hand: \\[" + cardPattern + ", " + cardPattern + "\\]"));

    assertTrue(natiMessages.getLast().matches("Your hand: \\[" + cardPattern + "\\]"));
  }

  @Test
  void handShouldOnlyNotifySender(){
      // Arrange: Ein gestartetes Spiel mit zwei Spielern
      ChatServer server = new ChatServer(5500);
      TestClientHandler hakan = new TestClientHandler(server, "hakan");
      TestClientHandler nati = new TestClientHandler(server, "nati");

      server.addClient(hakan);
      server.addClient(nati);

      server.handleCommand(hakan, "/create");
      server.handleCommand(hakan, "/join");
      server.handleCommand(nati, "/join");
      server.handleCommand(hakan, "/start");

      String expectedHand = hakan.getMessages().getLast();
      int hakanMessagesBefore = hakan.getMessages().size();
      List<String> natiMessagesBefore = nati.getMessages();

      boolean handled = server.handleCommand(hakan, "/hand");

      assertTrue(handled);

      assertEquals(hakanMessagesBefore + 1 , hakan.getMessages().size());
      assertEquals(expectedHand, hakan.getMessages().getLast());

      assertEquals(natiMessagesBefore, nati.getMessages());


  }

  @Test
  void startTwiceShouldRejectRestartAndKeepHand(){
      ChatServer server = new ChatServer(5500);
      TestClientHandler hakan = new TestClientHandler(server, "hakan");
      TestClientHandler nati = new TestClientHandler(server, "nati");

      server.addClient(hakan);
      server.addClient(nati);

      server.handleCommand(hakan, "/create");
      server.handleCommand(hakan, "/join");
      server.handleCommand(nati, "/join");
      server.handleCommand(hakan, "/start");

      String originalHand = hakan.getMessages().getLast();
      int hakanMessagesBefore = hakan.getMessages().size();
      List<String > natiMessagesBefore = nati.getMessages();

      boolean handled = server.handleCommand(hakan, "/start");

      assertTrue(handled);

      assertEquals(hakanMessagesBefore + 1 , hakan.getMessages().size());
      assertEquals("Cannot start game: Game has already started", hakan.getMessages().getLast());

      assertEquals(natiMessagesBefore, nati.getMessages());

      server.handleCommand(hakan, "/hand");

      assertEquals(originalHand, hakan.getMessages().getLast());
      assertEquals(natiMessagesBefore, nati.getMessages());

  }

  @Test
  void nextDuringActiveRoundShouldOnlyNotifySender(){
      ChatServer server = new ChatServer(5500);
      TestClientHandler hakan = new TestClientHandler(server, "hakan");
      TestClientHandler nati = new TestClientHandler(server, "nati");

      server.addClient(hakan);
      server.addClient(nati);

      server.handleCommand(hakan, "/create");
      server.handleCommand(hakan, "/join");
      server.handleCommand(nati, "/join");
      server.handleCommand(hakan, "/start");

      int hakanMessagesBefore = hakan.getMessages().size();
      List<String> natiMessagesBefore = nati.getMessages();

      boolean handled = server.handleCommand(hakan, "/next");

      assertTrue(handled);
      assertEquals(hakanMessagesBefore + 1, hakan.getMessages().size());
      assertEquals("Cannot start next round: The current round must be scored first",
              hakan.getMessages().getLast());

      assertEquals(natiMessagesBefore, nati.getMessages());
  }

  @Test
  void scoreShouldShowAllPlayersOnlyToSender(){
      ChatServer server = new ChatServer(5500);
      TestClientHandler hakan = new TestClientHandler(server, "hakan");
      TestClientHandler nati = new TestClientHandler(server, "nati");

      server.addClient(hakan);
      server.addClient(nati);

      server.handleCommand(hakan, "/create");
      server.handleCommand(hakan, "/join");
      server.handleCommand(nati, "/join");

      int hakanMessagesBefore = hakan.getMessages().size();
      List<String> natiMessagesBefore = nati.getMessages();

      boolean handled = server.handleCommand(hakan, "/score");

      assertTrue(handled);

      List<String> hakanMessages = hakan.getMessages();
      List<String> scoreMessages = hakanMessages.subList(hakanMessagesBefore, hakanMessages.size());

      assertEquals(
              List.of(
                  "Score:",
                  "hakan: 0 affection Token(s)",
                  "nati: 0 affection Token(s)"
                   ), scoreMessages
      );

      assertEquals(natiMessagesBefore, nati.getMessages());
  }


  @Test
  void handWithoutJoiningShouldOnlyNotifySender(){
      ChatServer server = new ChatServer(5500);
      TestClientHandler hakan = new TestClientHandler(server, "hakan");
      TestClientHandler nati = new TestClientHandler(server, "nati");
      TestClientHandler rafi = new TestClientHandler(server, "rafi");

      server.addClient(hakan);
      server.addClient(nati);
      server.addClient(rafi);

      server.handleCommand(hakan, "/create");
      server.handleCommand(hakan, "/join");
      server.handleCommand(nati, "/join");
      server.handleCommand(hakan, "/start");

      List<String> hakanMessagesBefore = hakan.getMessages();
      List<String> natiMessagesBefore = nati.getMessages();
      int rafiMessagesBefore = rafi.getMessages().size();

      boolean handled = server.handleCommand(rafi, "/hand");

      assertTrue(handled);

      assertEquals(rafiMessagesBefore + 1 , rafi.getMessages().size());

      assertEquals("Join the game first with /join.", rafi.getMessages().getLast());

      assertEquals(hakanMessagesBefore, hakan.getMessages());
      assertEquals(natiMessagesBefore, nati.getMessages());
  }

  @Test
  void playWithoutCardShouldShowUsageAndKeepHand(){
      ChatServer server = new ChatServer(5500);
      TestClientHandler hakan = new TestClientHandler(server, "hakan");
      TestClientHandler nati = new TestClientHandler(server, "nati");

      server.addClient(hakan);
      server.addClient(nati);

      server.handleCommand(hakan, "/create");
      server.handleCommand(hakan, "/join");
      server.handleCommand(nati, "/join");
      server.handleCommand(hakan, "/start");

      String originalHand = hakan.getMessages().getLast();
      int hakanMessagesBefore = hakan.getMessages().size();
      List<String> natiMessagesBefore = nati.getMessages();

      boolean handled = server.handleCommand(hakan, "/play");

      assertTrue(handled);

      assertEquals(hakanMessagesBefore + 1, hakan.getMessages().size());

      assertEquals("Usage: /play CARD [TARGET] [GUESS]", hakan.getMessages().getLast());

      assertEquals(natiMessagesBefore,nati.getMessages());

      server.handleCommand(hakan, "/hand");
      assertEquals(originalHand, hakan.getMessages().getLast());
      assertEquals(natiMessagesBefore, nati.getMessages());


  }

  @Test
  void playUnknownCardShouldOnlyNotifySenderAndKeepHand(){
      ChatServer server = new ChatServer(5500);
      TestClientHandler hakan = new TestClientHandler(server, "hakan");
      TestClientHandler nati = new TestClientHandler(server, "nati");

      server.addClient(hakan);
      server.addClient(nati);

      server.handleCommand(hakan, "/create");
      server.handleCommand(hakan, "/join");
      server.handleCommand(nati, "/join");
      server.handleCommand(hakan, "/start");

      String originalHand =  hakan.getMessages().getLast();
      int hakanMessagesBefore = hakan.getMessages().size();
      List<String> natiMessagesBefore = nati.getMessages();

      boolean handled = server.handleCommand(hakan, "/play wizard");

      assertTrue(handled);

      assertEquals(hakanMessagesBefore + 1, hakan.getMessages().size());
      assertEquals("Cannot play cards: Unknown card: wizard. "
                      + "Use GUARD, PRIEST, BARON, HANDMAID, "
                      + "PRINCE, KING, COUNTESS or PRINCESS.",
              hakan.getMessages().getLast());

      assertEquals(natiMessagesBefore, nati.getMessages() );

      //Nach dem Fehler kann Hakan weiterhin seine Hand abfragen
      server.handleCommand(hakan, "/hand");

      assertEquals(originalHand, hakan.getMessages().getLast());
      assertEquals(natiMessagesBefore, nati.getMessages());
  }

  @Test
  void playWithUnknownTargetShouldOnlyNotifySender(){
      ChatServer server = new ChatServer(5500);
      TestClientHandler hakan = new TestClientHandler(server, "hakan");
      TestClientHandler nati = new TestClientHandler(server, "nati");

      server.addClient(hakan);
      server.addClient(nati);

      server.handleCommand(hakan, "/create");
      server.handleCommand(hakan, "/join");
      server.handleCommand(nati, "/join");
      server.handleCommand(hakan, "/start");

      int hakanMessagesBefore = hakan.getMessages().size();
      List<String> natiMessagesBefore = nati.getMessages();

      boolean handled = server.handleCommand(hakan, "/play PRIEST nobody");

      assertTrue(handled);

      assertEquals(hakanMessagesBefore + 1, hakan.getMessages().size());

      assertEquals("Cannot play cards: Unknown player: nobody", hakan.getMessages().getLast());

      assertEquals(natiMessagesBefore, nati.getMessages());
  }

  @Test
  void playGuardWithoutGuessShouldOnlyNotifySender(){
      ChatServer server = new ChatServer(5500);
      TestClientHandler hakan = new TestClientHandler(server, "hakan");
      TestClientHandler nati = new TestClientHandler(server, "nati");

      server.addClient(hakan);
      server.addClient(nati);

      server.handleCommand(hakan, "/create");
      server.handleCommand(hakan, "/join");
      server.handleCommand(nati, "/join");
      server.handleCommand(hakan, "/start");

      int hakanMessagesBefore = hakan.getMessages().size();
      List<String> natiMessagesBefore = nati.getMessages();

      boolean handled = server.handleCommand(hakan, "/play GUARD nati");

      assertTrue(handled);

      assertEquals(hakanMessagesBefore + 1 , hakan.getMessages().size());
      assertEquals("Cannot play cards: Usage: /play GUARD TARGET GUESS", hakan.getMessages().getLast());

      assertEquals(natiMessagesBefore, nati.getMessages());
  }




  private static class TestClientHandler extends ClientHandler {

    private final String nickname;
    private final List<String> messages = new ArrayList<>();

    TestClientHandler(ChatServer server, String nickname) {
      super(null, server);
      this.nickname = nickname;
    }

    @Override
    public String getNickname() {
      return nickname;
    }

    @Override
    public synchronized void sendMessage(String message) {
      messages.add(message);
    }

    List<String> getMessages() {
      return List.copyOf(messages);
    }
  }
}
