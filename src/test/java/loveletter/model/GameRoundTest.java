package loveletter.model;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GameRoundTest {

    @Test
    void gameRoundShouldAcceptTwoPlayers(){
        List<Player> players = List.of(new Player("John"),
                                       new Player("Jane"));

        GameRound gameRound = new GameRound(players);

        assertEquals(2, gameRound.getPlayers().size());

    }

    @Test
    void gameRoundShouldRejectFewerThanTwoPlayers(){
        List<Player> players = List.of(new Player("Hakan"));


        assertThrows(
                IllegalArgumentException.class,
                () -> new GameRound(players) );
    }

    @Test
    void gameRoundShouldRejectMoreThanFourPlayers(){
        List<Player> players = List.of(
                new Player("Hakan"),
                new Player("Nati"),
                new Player("Jane"),
                new Player("John"),
                new Player("Rafi")
        );
        assertThrows(
                IllegalArgumentException.class, () -> new GameRound(players));
    }

    @Test
    void twoPlayerRoundShouldPrepareCardsCorrectly(){
        Player hakan = new Player("Hakan");
        Player nati = new Player("Nati");

        GameRound gameRound = new GameRound(List.of(hakan, nati));

        assertTrue(gameRound.hasReserveCard());
        assertEquals(3, gameRound.getFaceUpRemovedCards().size());
        assertEquals(1, hakan.getHand().size());
        assertEquals(1, nati.getHand().size());
        assertEquals(10, gameRound.getRemainingDeckSize());

    }

    @Test
    void threePlayerRoundShouldNotRemoveFaceUpCards(){
        Player hakan = new Player("Hakan");
        Player nati = new Player("Nati");
        Player jane = new Player("Jane");

        GameRound gameRound = new GameRound(List.of(hakan, nati,jane));

        assertTrue(gameRound.hasReserveCard());
        assertTrue(gameRound.getFaceUpRemovedCards().isEmpty());
        assertEquals(1, hakan.getHand().size());
        assertEquals(1, nati.getHand().size());
        assertEquals(1, jane.getHand().size());
        assertEquals(12, gameRound.getRemainingDeckSize());
    }

    @Test
    void firstPlayerShouldBeCurrentPlayer(){
        Player hakan = new Player("Hakan");
        Player nati = new Player("Nati");

        GameRound gameRound = new GameRound(List.of(hakan, nati));
        gameRound.startCurrentTurn();
        assertSame(hakan, gameRound.getCurrentPlayer());

    }

    @Test
    void startingTurnShouldRemoveProtectionAndDrawCard(){
        Player hakan = new Player("Hakan");
        Player nati = new Player("Nati");
        GameRound gameRound = new GameRound(List.of(hakan, nati));

        hakan.protectFromEffects();
        int deckSizeBeforeTurn = gameRound.getRemainingDeckSize();

        gameRound.startCurrentTurn();

        assertFalse(hakan.isProtectedFromEffects());
        assertEquals(2, hakan.getHand().size());
        assertEquals(deckSizeBeforeTurn - 1, gameRound.getRemainingDeckSize());
    }

    @Test
    void endingTurnShouldAdvanceToNextPlayer() {
        Player hakan = new Player("Hakan");
        Player nati = new Player("Nati");
        GameRound gameRound = new GameRound(List.of(hakan, nati));
        gameRound.startCurrentTurn();
        hakan.discardCard(hakan.getHand().getFirst());
        gameRound.endCurrentTurn();

        assertSame(nati, gameRound.getCurrentPlayer());
    }

    @Test
    void endingTurnShouldSkipEliminatedPlayers(){
        Player hakan = new Player("Hakan");
        Player nati = new Player("Nati");
        Player jane = new Player("Jane");

        GameRound gameRound = new GameRound(List.of(hakan, nati,jane));
        nati.eliminate();
        gameRound.startCurrentTurn();
        hakan.discardCard(hakan.getHand().getFirst());
        gameRound.endCurrentTurn();

        assertSame(jane, gameRound.getCurrentPlayer());
    }

    @Test
    void endingTurnBeforeDiscardingShouldThrowException(){
        Player hakan = new Player("Hakan");
        Player nati = new Player("Nati");

        GameRound gameRound = new GameRound(List.of(hakan, nati));
        gameRound.startCurrentTurn();
        assertThrows(IllegalStateException.class, gameRound::endCurrentTurn);
    }

    @Test
    void roundShouldEndWhenOnlyOnePlayerRemains(){
        Player hakan = new Player("Hakan");
        Player nati = new Player("Nati");
        GameRound gameRound = new GameRound(List.of(hakan, nati));

        gameRound.startCurrentTurn();
        nati.eliminate();

        assertTrue(gameRound.isRoundOver());

    }

    @Test
    void roundShouldEndWhenDeckIsEmpty(){
        Player hakan = new Player("Hakan");
        Player nati = new Player("Nati");
        GameRound gameRound = new GameRound(List.of(hakan, nati));

        while(gameRound.getRemainingDeckSize() > 0){
            gameRound.startCurrentTurn();

            Player currentPlayer = gameRound.getCurrentPlayer();
            CardType cardToDiscard = currentPlayer.getHand().getFirst();
            currentPlayer.discardCard(cardToDiscard);

            gameRound.endCurrentTurn();
        }
        assertTrue(gameRound.isRoundOver());
    }

    @Test
    void startingTurnAfterRoundIsOverShouldThrowException(){
        Player hakan = new Player("Hakan");
        Player nati = new Player("Nati");
        GameRound gameRound = new GameRound(List.of(hakan, nati));

        nati.eliminate();
        assertTrue(gameRound.isRoundOver());
        assertThrows(IllegalStateException.class, gameRound::startCurrentTurn);
    }

    @Test
    void newRoundShouldNotBeOver(){
        Player hakan = new Player("Hakan");
        Player nati = new Player("Nati");
        GameRound gameRound = new GameRound(List.of(hakan, nati));

        assertFalse(gameRound.isRoundOver());
    }
}
