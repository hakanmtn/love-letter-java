package loveletter.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

public class GameTest {

  @Test
  void newGameShouldHaveNoPlayersAndNotBeStarted() {
    Game game = new Game();

    assertTrue(game.getPlayers().isEmpty());
    assertFalse(game.isStarted());
  }

  @Test
  void playerShouldBeAbloToJoinBeforeGameStarts() {
    Game game = new Game();
    Player hakan = new Player("Hakan");

    game.join(hakan);

    assertEquals(List.of(hakan), game.getPlayers());
    assertFalse(game.isStarted());
  }

  @Test
  void samePlayerShouldNotJoinTwice() {
    Game game = new Game();
    Player hakan = new Player("Hakan");

    game.join(hakan);
    assertThrows(IllegalArgumentException.class, () -> game.join(hakan));

    assertEquals(List.of(hakan), game.getPlayers());
  }

  @Test
  void fifthPlayerShouldNotBeAbleToJoin() {
    Game game = new Game();

    Player hakan = new Player("Hakan");
    Player nati = new Player("Nati");
    Player mike = new Player("Mike");
    Player lea = new Player("Lea");

    game.join(hakan);
    game.join(nati);
    game.join(mike);
    game.join(lea);

    Player alex = new Player("Alex");

    assertThrows(IllegalStateException.class, () -> game.join(alex));
    assertEquals(List.of(hakan, nati, mike, lea), game.getPlayers());
  }

  @Test
  void nullPlayerShouldNoteBeAbleToJoin() {
    Game game = new Game();

    assertThrows(IllegalArgumentException.class, () -> game.join(null));

    assertTrue(game.getPlayers().isEmpty());
  }

  @Test
  void startingGameShouldCreateFirstRound() {
    Game game = new Game();
    Player hakan = new Player("Hakan");
    Player nati = new Player("Nati");

    game.join(hakan);
    game.join(nati);

    game.start();

    assertTrue(game.isStarted());

    GameRound round = game.getCurrentRound();

    assertEquals(List.of(hakan, nati), round.getPlayers());
    assertEquals(1, hakan.getHand().size());
    assertEquals(1, nati.getHand().size());
    assertFalse(round.isRoundOver());
  }

  @Test
  void playerShouldNotJoinAfterGameStarts() {
    Game game = new Game();
    Player hakan = new Player("Hakan");
    Player nati = new Player("Nati");

    game.join(hakan);
    game.join(nati);
    game.start();

    Player mike = new Player("Mike");

    assertThrows(IllegalStateException.class, () -> game.join(mike));
    assertEquals(List.of(hakan, nati), game.getPlayers());
  }

  @Test
  void gameShouldNotStartWithFewerThanTwoPlayers() {
    Game game = new Game();

    assertThrows(IllegalStateException.class, game::start);

    Player hakan = new Player("Hakan");
    game.join(hakan);

    assertThrows(IllegalStateException.class, game::start);

    assertFalse(game.isStarted());
    assertTrue(hakan.getHand().isEmpty());
  }

  @Test
  void gameShouldNotStartTwice() {
    Game game = new Game();
    game.join(new Player("Hakan"));
    game.join(new Player("Nati"));

    game.start();

    GameRound firstRound = game.getCurrentRound();

    assertThrows(IllegalStateException.class, game::start);

    assertTrue(game.isStarted());
    assertSame(firstRound, game.getCurrentRound());
  }

  @Test
    void finishingCurrentRoundShouldRewardItsWinner(){
      Game game = new Game();
      Player hakan = new Player("Hakan");
      Player nati = new Player("Nati");

      game.join(hakan);
      game.join(nati);
      game.start();

      nati.eliminate();
      List<Player> winners = game.finishCurrentRound();

      assertEquals(List.of(hakan), winners);
      assertEquals(1, hakan.getAffectionTokens());
      assertEquals(0, nati.getAffectionTokens());
  }

  @Test
    void twoPlayerGameShouldRequireSevenTokens(){
      Game game = new Game();
      Player hakan = new Player("Hakan");
      Player nati = new Player("Nati");

      game.join(hakan);
      game.join(nati);
      game.start();

      assertEquals(7,game.getRequiredTokensToWin());
  }

    @Test
    void threePlayerGameShouldRequireSevenTokens(){
        Game game = new Game();
        Player hakan = new Player("Hakan");
        Player nati = new Player("Nati");
        Player mike = new Player("Mike");

        game.join(hakan);
        game.join(nati);
        game.join(mike);
        game.start();

        assertEquals(5,game.getRequiredTokensToWin());
    }
    @Test
    void fourPlayerGameShouldRequireSevenTokens(){
        Game game = new Game();
        Player hakan = new Player("Hakan");
        Player nati = new Player("Nati");
        Player mike = new Player("Mike");
        Player jasmin = new Player("Jasmin");

        game.join(hakan);
        game.join(nati);
        game.join(mike);
        game.join(jasmin);

        game.start();

        assertEquals(4,game.getRequiredTokensToWin());
    }
    @Test
    void gameShouldNotBeOverBeforeItStarts(){
      Game game = new Game();

    }

    @Test
    void gameShouldEndWhenPlayerReachesRequiredTokens(){
      Game game = new Game();
      Player hakan = new Player("Hakan");
      Player nati = new Player("Nati");

      game.join(hakan);
      game.join(nati);
      game.start();

      assertFalse(game.isGameOver());

      for(int i = 0; i <6; i++) {
          nati.awardAffectionTokens();
      }

      assertFalse(game.isGameOver());

      hakan.eliminate();
      game.finishCurrentRound();

      assertEquals(7, nati.getAffectionTokens());
      assertTrue(game.isGameOver());
    }

    @Test
    void gameShouldReturnPlayerWhoReachedWinningScore(){
        Game game = new Game();
        Player hakan = new Player("Hakan");
        Player nati = new Player("Nati");

        game.join(hakan);
        game.join(nati);
        game.start();

        for(int i = 0; i <6; i++) {
            nati.awardAffectionTokens();
        }

        hakan.eliminate();
        game.finishCurrentRound();

        assertEquals(List.of(nati), game.getWinners());
    }

    @Test
    void winnersShouldNotBeAvailableWhileGameIsRunning(){
        Game game = new Game();
        Player hakan = new Player("Hakan");
        Player nati = new Player("Nati");

        game.join(hakan);
        game.join(nati);
        game.start();

        assertThrows(IllegalStateException.class, game::getWinners);

    }

    @Test
    void nextRoundShouldResetPlayersAndKeepTokens(){
        Game game = new Game();
        Player hakan = new Player("Hakan");
        Player nati = new Player("Nati");

        game.join(hakan);
        game.join(nati);
        game.start();

        GameRound firstRound = game.getCurrentRound();

        hakan.protectFromEffects();
        nati.eliminate();
        game.finishCurrentRound();

        game.startNextRound();

        assertNotSame(firstRound,game.getCurrentRound());
        assertFalse(game.getCurrentRound().isRoundOver());

        assertEquals(1,hakan.getAffectionTokens());
        assertEquals(0,nati.getAffectionTokens());

        assertFalse(nati.isEliminated());
        assertFalse(hakan.isProtectedFromEffects());

        assertEquals(1,hakan.getHand().size());
        assertEquals(1,nati.getHand().size());

        assertTrue(hakan.getDiscardPile().isEmpty());
        assertTrue(nati.getDiscardPile().isEmpty());

        assertFalse(game.getCurrentRound().isWinnerTokensAwarded());


    }

    @Test
    void nextRoundShouldNotStartBeforeCurrentRoundIsScored(){
        Game game = new Game();
        Player hakan = new Player("Hakan");
        Player nati = new Player("Nati");

        game.join(hakan);
        game.join(nati);
        game.start();

        GameRound firstRound = game.getCurrentRound();
        nati.eliminate();

        assertThrows(IllegalStateException.class, game::startNextRound);

        assertSame(firstRound, game.getCurrentRound());
        assertEquals(0, hakan.getAffectionTokens());
  }

  @Test
    void previousRoundWinnerShouldStartNextRound(){
      Game game = new Game();
      Player hakan = new Player("Hakan");
      Player nati = new Player("Nati");

      game.join(hakan);
      game.join(nati);
      game.start();

      hakan.eliminate();
      game.finishCurrentRound();

      game.startNextRound();
      assertSame(nati,game.getCurrentRound().getCurrentPlayer());

      assertEquals(List.of(hakan,nati), game.getCurrentRound().getPlayers());
  }

  @Test
    void nextRoundShouldNotStartAfterGameIsOver(){
      Game game = new Game();
      Player hakan = new Player("Hakan");
      Player nati = new Player("Nati");

      game.join(hakan);
      game.join(nati);
      game.start();

      GameRound finalRound = game.getCurrentRound();

      for(int i = 0; i < 6 ; i++){
          nati.awardAffectionTokens();
      }

      hakan.eliminate();
      game.finishCurrentRound();

      assertTrue(game.isGameOver());

      assertThrows(IllegalStateException.class, game::startNextRound);
      assertSame(finalRound, game.getCurrentRound());
      assertEquals(7, nati.getAffectionTokens());
      assertTrue(hakan.isEliminated());

  }
}
