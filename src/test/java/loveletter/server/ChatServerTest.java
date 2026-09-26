package loveletter.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import loveletter.model.CardType;
import loveletter.model.Game;
import loveletter.model.GameRound;
import loveletter.model.Player;
import org.junit.jupiter.api.Test;
import loveletter.protocol.GamePhase;
import loveletter.protocol.GameState;
import loveletter.protocol.PlayerState;


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
            .matches("Your hand: \\[" + cardPattern + ", " + cardPattern + "]"));

    assertTrue(natiMessages.getLast().matches("Your hand: \\[" + cardPattern + "]"));
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

  @Test
  void priestShouldRevealCardOnlyToSenderAndAdvanceTurn(){
      //Arrange: Server und Test verwenden dasselbe Spiel
      Game game = new Game();
      ChatServer server = new ChatServer(5500, game);

      TestClientHandler hakanClient = new TestClientHandler(server, "hakan");
      TestClientHandler natiClient = new TestClientHandler(server, "nati");

      server.addClient(hakanClient);
      server.addClient(natiClient);

      joinPlayersAndStart(server,hakanClient,natiClient);

      GameRound round = game.getCurrentRound();
      Player hakan = game.getPlayers().get(0);
      Player nati = game.getPlayers().get(1);

      //Feste Handkarten nach dem Start vorbereiten
      replaceHandWith(hakan, CardType.HANDMAID);
      hakan.receiveCard(CardType.PRIEST);
      replaceHandWith(nati, CardType.KING);

      int hakanMessagesBefore = hakanClient.getMessages().size();
      int natiMessagesBefore = natiClient.getMessages().size();
      int deckSizeBefore = round.getRemainingDeckSize();

      //Act
      boolean handled = server.handleCommand(hakanClient, "/play PRIEST nati");

      //Assert: Nur Hakan bekommt die aufgedeckte Karte mitgeteilt
      assertTrue(handled);

      List<String> hakanMessages = hakanClient.getMessages();
      List<String> natiMessages = natiClient.getMessages();

      List<String> hakanNewMessages = hakanMessages.subList(hakanMessagesBefore, hakanMessages.size());
      List<String> natiNewMessages = natiMessages.subList(natiMessagesBefore, natiMessages.size());

      assertEquals(1L, hakanNewMessages.stream()
              .filter(message -> message.equals("Viewed card: KING"))
              .count());

      assertFalse(natiNewMessages.stream()
              .anyMatch(message -> message.startsWith("Viewed Card:")));

      //Die Karte wurde gespielt
      assertEquals(List.of(CardType.HANDMAID), hakan.getHand());
      assertEquals(CardType.PRIEST, hakan.getDiscardPile().getLast());

      //Nati ist dran
      assertSame(nati, round.getCurrentPlayer());
      assertEquals(2, nati.getHand().size());
      assertTrue(nati.getHand().contains(CardType.KING));
      assertEquals(deckSizeBefore - 1 , round.getRemainingDeckSize());

      //beide clients erfahren den Zugwechsel
      assertTrue(hakanNewMessages.contains("Current player: nati"));
      assertTrue(natiNewMessages.contains("Current player: nati"));

      assertEquals("Your hand: [HANDMAID]", hakanClient.getMessages().getLast());
      assertEquals("Your hand: " + nati.getHand(), natiClient.getMessages().getLast());
  }

  @Test
  void priestAgainstProtectedPlayerShouldNotChangeGameState(){
      Game game = new Game();
      ChatServer server = new ChatServer(5500, game);

      TestClientHandler hakanClient = new TestClientHandler(server, "hakan");
      TestClientHandler natiClient = new TestClientHandler(server, "nati");

      server.addClient(hakanClient);
      server.addClient(natiClient);

      joinPlayersAndStart(server,hakanClient,natiClient);

      GameRound round = game.getCurrentRound();
      Player hakan = round.getPlayers().getFirst();
      Player nati = round.getPlayers().get(1);

      replaceHandWith(hakan, CardType.HANDMAID);
      hakan.receiveCard(CardType.PRIEST);
      replaceHandWith(nati, CardType.KING);
      nati.protectFromEffects();

      List<CardType> discardBefore = List.copyOf(hakan.getDiscardPile());
      int deckSizeBefore = round.getRemainingDeckSize();

      int hakanMessagesBefore = hakanClient.getMessages().size();
      List<String> natiMessagesBefore = natiClient.getMessages();

      server.handleCommand(hakanClient, "/play PRIEST nati");

      //Assert: Nur die Fehlermeldung wird gesendet
      List<String> hakanMessages = hakanClient.getMessages();

      assertEquals(List.of("Cannot play cards: " + "No opponent is available; omit the target"),
              hakanMessages.subList(hakanMessagesBefore,hakanMessages.size()));

      assertEquals(natiMessagesBefore, natiClient.getMessages());

      //Keine Karte wurde ausgespielt, kein Zugwechsel fand statt
      assertEquals(List.of(CardType.HANDMAID, CardType.PRIEST), hakan.getHand());
      assertEquals(discardBefore, hakan.getDiscardPile());
      assertEquals(List.of(CardType.KING), nati.getHand());
      assertTrue(nati.isProtectedFromEffects());

      assertSame(hakan, round.getCurrentPlayer());
      assertEquals(deckSizeBefore, round.getRemainingDeckSize());

  }


  @Test
  void correctGuardGuessShouldFinishRoundAndAwardToken(){
      Game game = new Game();
      ChatServer server = new ChatServer(5500, game);

      TestClientHandler hakanClient = new TestClientHandler(server, "hakan");
      TestClientHandler natiClient = new TestClientHandler(server, "nati");

      server.addClient(hakanClient);
      server.addClient(natiClient);

      joinPlayersAndStart(server,hakanClient,natiClient);

      GameRound round = game.getCurrentRound();
      Player hakan = game.getPlayers().getFirst();
      Player nati = game.getPlayers().get(1);

      replaceHandWith(hakan,CardType.HANDMAID);
      hakan.receiveCard(CardType.GUARD);
      replaceHandWith(nati, CardType.KING);

      int deckSizeBefore = round.getRemainingDeckSize();
      int hakanMessagesBefore = hakanClient.getMessages().size();
      int natiMessagesBefore = natiClient.getMessages().size();

      //Act
      boolean handled = server.handleCommand(hakanClient, "/play GUARD nati KING");

      assertTrue(handled);
      assertTrue(nati.isEliminated());
      assertTrue(round.isRoundOver());
      assertTrue(round.isWinnerTokensAwarded());

      assertEquals(1, hakan.getAffectionTokens());
      assertEquals(0, nati.getAffectionTokens());

      //Nach dem Rundenende darf keine neue Karte gezogen werden
      assertEquals(deckSizeBefore, round.getRemainingDeckSize());

      //Beide Clients erhalten die Meldungen zum Rundenende
      List<String> hakanMessages = hakanClient.getMessages();
      List<String> natiMessages = natiClient.getMessages();

      List<String> hakanNewMessages = hakanMessages.subList(hakanMessagesBefore,hakanMessages.size());
      List<String> natiNewMessages = natiMessages.subList(natiMessagesBefore, natiMessages.size());

      List<String> expectedMessages = List.of("hakan played GUARD targeting nati and guessed KING .",
              "nati was eliminated: the guess was correct.",
              "Round over. Winners: hakan",
              "Each round winner receives one affection token.",
              "Use /next to start the next round.");

      assertEquals(expectedMessages, hakanNewMessages);
      assertEquals(expectedMessages, natiNewMessages);
  }

  @Test
  void playAfterRoundOverShouldNotAwardAnotherToken(){
      //Arrange: hakan gewinnt die Runde
      Game game = new Game();
      ChatServer server = new ChatServer(5500, game);

      TestClientHandler hakanClient = new TestClientHandler(server, "hakan");
      TestClientHandler natiClient = new TestClientHandler(server, "nati");

      server.addClient(hakanClient);
      server.addClient(natiClient);

      joinPlayersAndStart(server,hakanClient,natiClient);

      GameRound round = game.getCurrentRound();
      Player hakan = game.getPlayers().getFirst();
      Player nati = game.getPlayers().get(1);

      replaceHandWith(hakan,CardType.HANDMAID);
      hakan.receiveCard(CardType.GUARD);
      replaceHandWith(nati, CardType.KING);

      server.handleCommand(hakanClient, "/play GUARD nati KING");

      assertTrue(round.isRoundOver());
      assertEquals(1, hakan.getAffectionTokens());

      int hakanMessagesBefore = hakanClient.getMessages().size();
      List<String> natiMessagesBefore = natiClient.getMessages();
      int deckSizeBefore = round.getRemainingDeckSize();

      //Act: hakan versucht, nach dem Rundenende weiterzuspielen
      server.handleCommand(hakanClient, "/play HANDMAID");

      List<String> hakanMessages = hakanClient.getMessages();

      assertEquals(List.of("This round is over."), hakanMessages.subList( hakanMessagesBefore, hakanMessages.size()));

      assertEquals(natiMessagesBefore,natiClient.getMessages());

      assertEquals(1, hakan.getAffectionTokens());
      assertEquals(0, nati.getAffectionTokens());
      assertEquals(List.of(CardType.HANDMAID), hakan.getHand());
      assertEquals(deckSizeBefore, round.getRemainingDeckSize());
      assertSame(round, game.getCurrentRound());


  }

  @Test
  void nextShouldStartNewRoundAndKeepScores(){
      //Arrange: hakan gewinnt die erste Runde
      Game game = new Game();
      ChatServer server = new ChatServer(5500, game);

      TestClientHandler hakanClient =
              new TestClientHandler(server, "hakan");
      TestClientHandler natiClient =
              new TestClientHandler(server, "nati");

      server.addClient(hakanClient);
      server.addClient(natiClient);

      joinPlayersAndStart(server,hakanClient,natiClient);

      GameRound previousRound = game.getCurrentRound();
      Player hakan = game.getPlayers().get(0);
      Player nati = game.getPlayers().get(1);

      replaceHandWith(hakan, CardType.HANDMAID);
      hakan.receiveCard(CardType.GUARD);
      replaceHandWith(nati, CardType.KING);

      server.handleCommand(hakanClient, "/play GUARD nati KING");

      assertTrue(previousRound.isWinnerTokensAwarded());
      assertTrue(nati.isEliminated());
      assertEquals(1, hakan.getAffectionTokens());

      int hakanMessagesBefore = hakanClient.getMessages().size();
      int natiMessagesBefore = natiClient.getMessages().size();

      // Act: auch die zuvor ausgeschiedene Nati darf /next aufrufen
      boolean handled = server.handleCommand(natiClient, "/next");

      assertTrue(handled);

      GameRound newRound = game.getCurrentRound();

      assertNotSame(previousRound,newRound);
      assertFalse(hakan.isEliminated());
      assertFalse(nati.isEliminated());
      assertTrue(hakan.getDiscardPile().isEmpty());
      assertTrue(nati.getDiscardPile().isEmpty());

      assertEquals(1, hakan.getAffectionTokens());
      assertEquals(0, nati.getAffectionTokens());

      //hakan beginnt und hat bereits seine zweite Karte gezogen
      assertSame(hakan, newRound.getCurrentPlayer());
      assertEquals(2, hakan.getHand().size());
      assertEquals(1, nati.getHand().size());

      //beide clients erhalten den neuen Stand
      List<String> hakanMessages = hakanClient.getMessages();
      List<String> natiMessages = natiClient.getMessages();

      assertEquals(List.of(
              "A new round has started.",
              "Current player: hakan",
              "Your hand: " + hakan.getHand()), hakanMessages.subList(hakanMessagesBefore, hakanMessages.size()));

      assertEquals(List.of(
              "A new round has started.",
              "Current player: hakan",
              "Your hand: " + nati.getHand()), natiMessages.subList(natiMessagesBefore, natiMessages.size()));



  }

  @Test
  void winningFinalTokenShouldAnnounceGameOver(){
      // Arrange
      Game game = new Game();
      ChatServer server = new ChatServer(5500, game);

      TestClientHandler hakanClient =
              new TestClientHandler(server, "hakan");
      TestClientHandler natiClient =
              new TestClientHandler(server, "nati");

      server.addClient(hakanClient);
      server.addClient(natiClient);

      joinPlayersAndStart(server,hakanClient,natiClient);

      Player hakan = game.getPlayers().get(0);
      Player nati = game.getPlayers().get(1);

      int requiredTokens = game.getRequiredTokensToWin();

      for(int i = 0; i < requiredTokens - 1; i++){
          hakan.awardAffectionTokens();
      }

      replaceHandWith(hakan, CardType.HANDMAID);
      hakan.receiveCard(CardType.GUARD);
      replaceHandWith(nati, CardType.KING);

      assertFalse(game.isGameOver());

      int hakanMessagesBefore = hakanClient.getMessages().size();
      int natiMessagesBefore = natiClient.getMessages().size();

      //Act: hakan gewinnt den entscheidenden Punkt
      server.handleCommand(hakanClient, "/play GUARD nati KING" );

      //Assert: tatsächlischer gesamtsieg
      assertEquals(requiredTokens, hakan.getAffectionTokens());
      assertEquals(0, nati.getAffectionTokens());
      assertTrue(game.isGameOver());
      assertEquals(List.of(hakan), game.getWinners());

      //beide clients enthalten die spielende meldung
      List<String> expectedMessages = List.of("hakan played GUARD targeting nati and guessed KING .",
              "nati was eliminated: the guess was correct.",
              "Round over. Winners: hakan",
              "Each round winner receives one affection token.",
              "Game over. Overall winners: hakan",
              "Use /score to see the final scores.");

      List<String> hakanMessages = hakanClient.getMessages();
      List<String> natiMessages = natiClient.getMessages();

      assertEquals(expectedMessages, hakanMessages.subList(hakanMessagesBefore, hakanMessages.size()));
      assertEquals(expectedMessages, natiMessages.subList(natiMessagesBefore, natiMessages.size()));



  }

  @Test
  void nextAfterGameOverShouldOnlyNotifySenderAndKeepState() {
      //Arrange: hakan gewinnt das gesamte Spiel
      Game game = new Game();
      ChatServer server = new ChatServer(5500, game);

      TestClientHandler hakanClient =
              new TestClientHandler(server, "hakan");
      TestClientHandler natiClient =
              new TestClientHandler(server, "nati");

      server.addClient(hakanClient);
      server.addClient(natiClient);

      joinPlayersAndStart(server,hakanClient,natiClient);

      Player hakan = game.getPlayers().get(0);
      Player nati = game.getPlayers().get(1);

      int requiredTokens = game.getRequiredTokensToWin();

      for (int i = 0; i < requiredTokens - 1; i++) {
          hakan.awardAffectionTokens();
      }

      replaceHandWith(hakan, CardType.HANDMAID);
      hakan.receiveCard(CardType.GUARD);
      replaceHandWith(nati, CardType.KING);

      server.handleCommand(hakanClient, "/play GUARD nati KING");

      assertTrue(game.isGameOver());

      GameRound finalRound = game.getCurrentRound();
      int deckSizeBefore = finalRound.getRemainingDeckSize();
      int natiMessageBefore = natiClient.getMessages().size();
      List<String> hakanMessagesBefore = hakanClient.getMessages();

      //Act
      boolean handled = server.handleCommand(natiClient, "/next");

      //Assert: nur nati erhält die Ablehnung
      assertTrue(handled);

      List<String> natiMessages = natiClient.getMessages();

      assertEquals(List.of("Cannot start next round: Game is already over"),
              natiMessages.subList(natiMessageBefore, natiMessages.size()));

      assertEquals(hakanMessagesBefore, hakanClient.getMessages());

      // keine neue Runde und keine Änderung am Endstand
      assertSame(finalRound, game.getCurrentRound());
      assertTrue(game.isGameOver());
      assertEquals(List.of(hakan), game.getWinners());

      assertEquals(requiredTokens, hakan.getAffectionTokens());
      assertEquals(0, nati.getAffectionTokens());

      assertEquals(List.of(CardType.HANDMAID), hakan.getHand());
      assertTrue(nati.isEliminated());
      assertEquals(deckSizeBefore, finalRound.getRemainingDeckSize());


  }
  @Test
  void disconnectingPlayerShouldCloseGameAndAllowNewGame(){
      //Arrange: Eine laufende Partie mit zwei Spielern
      ChatServer server = new ChatServer(5500);

      TestClientHandler hakan = new TestClientHandler(server, "hakan");
      TestClientHandler nati = new TestClientHandler(server, "nati");

      server.addClient(hakan);
      server.addClient(nati);

      server.handleCommand(hakan, "/create");
      server.handleCommand(hakan,"/join");
      server.handleCommand(nati, "/join");
      server.handleCommand(hakan, "/start");

      List<String> hakanMessagesBefore = hakan.getMessages();
      int natiMessagesBefore = nati.getMessages().size();

      //Act: Der aktuelle Spieler verlässt die Verbindung
      server.removeClient(hakan);

      //Nur der verbleibende Client enthält die Meldung
      List<String> natiMessages = nati.getMessages();

      assertEquals(
              List.of(
                      "The game was closed because hakan disconnected. "
                              + "Use /create to start a new game."
              ),
              natiMessages.subList(
                      natiMessagesBefore, natiMessages.size()
              )
      );

      assertEquals(hakanMessagesBefore, hakan.getMessages());

      //das alte Spiel ist nicht mehr verfügbar
      server.handleCommand(nati, "/hand");

      assertEquals("Create a game first with /create.", nati.getMessages().getLast());

      //Nati kann ein neues Spiel erstellen und erneut beitreten
      server.handleCommand(nati, "/create");
      assertEquals("A new Love Letter game has been created.", nati.getMessages().getLast());

      server.handleCommand(nati, "/join");

      assertEquals("nati joined the game. Players: 1/4",
              nati.getMessages().getLast());

      // Der entfernte Client erhält auch später keine Nachrichten
      assertEquals(hakanMessagesBefore, hakan.getMessages());

  }

  @Test
  void disconnectingSpectatorShouldKeepGameRunning(){
      ChatServer server = new ChatServer(5500);

      TestClientHandler hakan =
              new TestClientHandler(server, "hakan");
      TestClientHandler nati =
              new TestClientHandler(server, "nati");
      TestClientHandler rafi =
              new TestClientHandler(server, "rafi");

      server.addClient(hakan);
      server.addClient(nati);
      server.addClient(rafi);

      server.handleCommand(hakan, "/create");
      server.handleCommand(hakan, "/join");
      server.handleCommand(nati, "/join");
      server.handleCommand(hakan, "/start");

      List<String> hakanMessagesBefore = hakan.getMessages();
      List<String> natiMessagesBefore = nati.getMessages();
      List<String> rafiMessagesBefore = rafi.getMessages();

      String hakanHandBefore = hakanMessagesBefore.getLast();
      String natiHandBefore = natiMessagesBefore.getLast();

      //Act: Rafi verlässt als Zuschauer den Chat
      server.removeClient(rafi);

      //Assert: keine Spielabbruchmeldung
      assertEquals(hakanMessagesBefore, hakan.getMessages());
      assertEquals(natiMessagesBefore, nati.getMessages());

      //Beide Spieler können weiterhin ihre bisherigen Karten abfragen
      server.handleCommand(hakan, "/hand");
      server.handleCommand(nati, "/hand");

      assertEquals(hakanHandBefore, hakan.getMessages().getLast());
      assertEquals(natiHandBefore, nati.getMessages().getLast());

      //Rafi wurde tatsächlich aus der Nachrichtenverteilung entfernt
      server.broadcast("Test message");

      assertEquals("Test message", hakan.getMessages().getLast());
      assertEquals("Test message", nati.getMessages().getLast());
      assertEquals(rafiMessagesBefore, rafi.getMessages());

  }

  @Test
  void directMessageShouldOnlyReachRecipientAndSender(){
      //Arrange: Drei Clients im Chat, kein Spiel erforderlich
      ChatServer server = new ChatServer(5500);

      TestClientHandler hakan =
              new TestClientHandler(server,"hakan");
      TestClientHandler nati =
              new TestClientHandler(server, "nati");

      TestClientHandler raffi =
              new TestClientHandler(server, "raffi");

      server.addClient(hakan);
      server.addClient(nati);
      server.addClient(raffi);

      //Act
      server.sendDirectMessage(hakan, "nati", "Hallo Nati!");

      //Assert
      assertEquals(List.of("[Private from hakan] Hallo Nati!"), nati.getMessages());
      assertEquals(List.of("[Private to nati] Hallo Nati!"), hakan.getMessages());

      assertTrue(raffi.getMessages().isEmpty());

  }

  @Test
  void msgCommandShouldOnlyReachRecipientAndSender(){
      ChatServer server = new ChatServer(5500);

      TestClientHandler hakan = new TestClientHandler(server, "hakan");
      TestClientHandler nati = new TestClientHandler(server, "nati");
      TestClientHandler rafi = new TestClientHandler(server,"rafi");

      server.addClient(hakan);
      server.addClient(nati);
      server.addClient(rafi);

      boolean handled = server.handleCommand(hakan, "/msg nati Hallo Nati! Wie geht es dir?");
      assertTrue(handled);

      assertEquals(List.of("[Private from hakan] Hallo Nati! Wie geht es dir?"), nati.getMessages());
      assertEquals(List.of("[Private to nati] Hallo Nati! Wie geht es dir?"), hakan.getMessages());

     assertTrue(rafi.getMessages().isEmpty());
  }

  @Test
  void msgWithUnknownRecipientShouldOnlyNotifySender(){
      ChatServer server = new ChatServer(5500);

      TestClientHandler hakan  = new TestClientHandler(server, "hakan");
      TestClientHandler nati = new TestClientHandler(server, "nati");

      server.addClient(hakan);
      server.addClient(nati);

      boolean handled = server.handleCommand(hakan, "/msg nobody Hallo!");

      assertTrue(handled);

      assertEquals(List.of("Unknown recipient: nobody"), hakan.getMessages());

      assertTrue(nati.getMessages().isEmpty());
  }

  @Test
  void msgWithoutMessageShouldOnlyNotifySender(){
      ChatServer server = new ChatServer(5500);

      TestClientHandler hakan  = new TestClientHandler(server, "hakan");
      TestClientHandler nati = new TestClientHandler(server, "nati");

      server.addClient(hakan);
      server.addClient(nati);

      //Act: Nach dem Empfänger folgen nur Leerzeichen
      boolean handled = server.handleCommand(hakan, "/msg nati   ");

      assertTrue(handled);

      assertEquals(List.of("Usage: /msg RECIPIENT MESSAGE"), hakan.getMessages());

      assertTrue(nati.getMessages().isEmpty());
  }

  @Test
  void msgToSelfShouldDeliverMessageOnlyOnce(){
      ChatServer server = new ChatServer(5500);

      TestClientHandler hakan  = new TestClientHandler(server, "hakan");
      TestClientHandler nati = new TestClientHandler(server, "nati");

      server.addClient(hakan);
      server.addClient(nati);

      //Act: Nach dem Empfänger folgen nur Leerzeichen
      boolean handled = server.handleCommand(hakan, "/msg hakan Erinnerung für mich");

      assertTrue(handled);

      assertEquals(List.of("[Private from hakan] Erinnerung für mich"), hakan.getMessages());
      assertTrue(nati.getMessages().isEmpty());
  }

  @Test
  void msgToDisconnectedRecipientShouldOnlyNotifySender() {
      ChatServer server = new ChatServer(5500);

      TestClientHandler hakan  = new TestClientHandler(server, "hakan");
      TestClientHandler nati = new TestClientHandler(server, "nati");

      server.addClient(hakan);
      server.addClient(nati);

      server.removeClient(nati);

      boolean handled = server.handleCommand(hakan, "/msg nati Bist du noch da?");

      assertTrue(handled);

      assertEquals(List.of("Unknown recipient: nati"), hakan.getMessages());

      assertTrue(nati.getMessages().isEmpty());
  }

  @Test
  void snapshotWithoutGameShouldBeEmpty(){
      ChatServer server = new ChatServer(5500);
      TestClientHandler hakan = new TestClientHandler(server, "hakan");
      server.addClient(hakan);

      GameState state = server.createGameState(hakan);

      assertEquals(GamePhase.NO_GAME, state.phase());
      assertEquals("hakan" , state.recipientName());
      assertTrue(state.players().isEmpty());
      assertNull(state.currentPlayerName());
      assertTrue(state.ownHand().isEmpty());
      assertEquals(0, state.remainingDeckSize());
      assertTrue(state.faceUpRemovedCards().isEmpty());
      assertTrue(state.roundWinners().isEmpty());
      assertTrue(state.gameWinners().isEmpty());
  }

  @Test
  void snapshotBeforeStartShouldShowParticipantsWithoutCards(){
      ChatServer server = new ChatServer(5500);
      TestClientHandler hakan = new TestClientHandler(server, "hakan");
      TestClientHandler nati = new TestClientHandler(server, "nati");

      server.addClient(hakan);
      server.addClient(nati);

      server.handleCommand(hakan, "/create");
      server.handleCommand(hakan, "/join");
      server.handleCommand(nati,"/join");

      GameState state = server.createGameState(hakan);

      assertEquals(GamePhase.WAITING_FOR_PLAYERS, state.phase());
      assertEquals(
              List.of(
                      new PlayerState(
                              "hakan", 0, false, false, 0, List.of()
                      ),
                      new PlayerState(
                              "nati", 0, false, false, 0, List.of()
                      )
              ),
              state.players()
      );

      assertNull(state.currentPlayerName());
      assertTrue(state.ownHand().isEmpty());
      assertTrue(state.roundWinners().isEmpty());
      assertTrue(state.gameWinners().isEmpty());

  }

    @Test
    void snapshotShouldContainOnlyTheRecipientsHand(){
      Game game = new Game();
      ChatServer server = new ChatServer(5500, game);

      TestClientHandler hakan = new TestClientHandler(server, "hakan");
      TestClientHandler nati = new TestClientHandler(server, "nati");

      server.addClient(hakan);
      server.addClient(nati);

      joinPlayersAndStart(server, hakan, nati);

      Player hakanPlayer = game.getPlayers().getFirst();
      Player natiPlayer = game.getPlayers().get(1);

      replaceHandWith(hakanPlayer, CardType.HANDMAID);
      hakanPlayer.receiveCard(CardType.GUARD);
      replaceHandWith(natiPlayer, CardType.KING);

      GameState hakanState = server.createGameState(hakan);
      GameState natiState = server.createGameState(nati);

      assertEquals(GamePhase.ROUND_IN_PROGRESS, hakanState.phase());
      assertEquals(GamePhase.ROUND_IN_PROGRESS, natiState.phase());

      assertEquals("hakan", hakanState.recipientName());
      assertEquals("nati", natiState.recipientName());

      assertEquals(List.of(CardType.HANDMAID, CardType.GUARD), hakanState.ownHand());
      assertEquals(List.of(CardType.KING), natiState.ownHand());

      assertEquals(hakanState.players(), natiState.players());
      assertEquals("hakan", hakanState.currentPlayerName());
      assertEquals("hakan", natiState.currentPlayerName());

      assertEquals(2, hakanState.players().getFirst().handSize());
      assertEquals(1, hakanState.players().get(1).handSize());

      assertEquals(
              game.getCurrentRound().getRemainingDeckSize(), hakanState.remainingDeckSize()
      );

      assertEquals(
              game.getCurrentRound().getFaceUpRemovedCards(),
              hakanState.faceUpRemovedCards()
      );

    }

    @Test
    void spectatorSnapShotShouldContainHandCards(){
      Game game = new Game();
      ChatServer server = new ChatServer(5500, game);

      TestClientHandler hakan = new TestClientHandler(server, "hakan");
      TestClientHandler nati = new TestClientHandler(server, "nati");
      TestClientHandler rafi = new TestClientHandler(server, "rafi");

      server.addClient(hakan);
      server.addClient(nati);
      server.addClient(rafi);

      joinPlayersAndStart(server, hakan, nati);

      GameState spectatorState = server.createGameState(rafi);
      GameState playerState = server.createGameState(hakan);

      assertEquals(GamePhase.ROUND_IN_PROGRESS, spectatorState.phase());
      assertEquals("rafi", spectatorState.recipientName());
      assertTrue(spectatorState.ownHand().isEmpty());

      assertEquals(playerState.players(), spectatorState.players());
      assertEquals(playerState.currentPlayerName(), spectatorState.currentPlayerName());
      assertEquals(2, spectatorState.players().size());

    }

    @Test
    void snapshotShouldRemainUnchangedAfterAPlay(){
        Game game = new Game();
        ChatServer server = new ChatServer(5500, game);

        TestClientHandler hakan = new TestClientHandler(server, "hakan");
        TestClientHandler nati = new TestClientHandler(server, "nati");

        server.addClient(hakan);
        server.addClient(nati);

        joinPlayersAndStart(server, hakan, nati);

        Player hakanPlayer = game.getPlayers().getFirst();
        Player natiPlayer = game.getPlayers().get(1);

        replaceHandWith(hakanPlayer, CardType.HANDMAID);
        hakanPlayer.receiveCard(CardType.GUARD);
        replaceHandWith(natiPlayer, CardType.KING);

        List<CardType> previousDiscards = List.copyOf(hakanPlayer.getDiscardPile());

        GameState before = server.createGameState(hakan);

        server.handleCommand(hakan, "/play HANDMAID");

        GameState after = server.createGameState(hakan);

        //The new snapshot reflects the completed play.
        assertEquals("nati", after.currentPlayerName());
        assertEquals(List.of(CardType.GUARD), after.ownHand());
        assertTrue(after.players().getFirst().protectedFromEffects());
        assertEquals(CardType.HANDMAID, after.players().getFirst().discardPile().getLast());

        //The previous snapshot retains its original values.
        assertEquals("hakan", before.currentPlayerName());
        assertEquals(List.of(CardType.HANDMAID, CardType.GUARD), before.ownHand());

        assertEquals(2, before.players().getFirst().handSize());
        assertFalse(before.players().getFirst().protectedFromEffects());
        assertEquals(previousDiscards, before.players().getFirst().discardPile());

        assertThrows(UnsupportedOperationException.class,
                () -> before.ownHand().clear());

        assertThrows(UnsupportedOperationException.class,
                () -> before.players().clear());

        assertThrows(UnsupportedOperationException.class,
                () -> before.players().getFirst().discardPile().clear());

    }



  private void replaceHandWith(Player player, CardType card){
      while(!player.getHand().isEmpty()){
          player.discardCard(player.getHand().getFirst());
      }
      player.receiveCard(card);
  }


  private void joinPlayersAndStart(ChatServer server, TestClientHandler firstClient,
                                   TestClientHandler secondClient){
      server.handleCommand(firstClient, "/join");
      server.handleCommand(secondClient, "/join");
      server.handleCommand(firstClient,"/start");
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
