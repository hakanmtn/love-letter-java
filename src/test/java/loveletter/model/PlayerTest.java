package loveletter.model;

import org.junit.jupiter.api.Test;


import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    @Test
    void newPlayerShoulgHaveNameAndNoAffectionTokens(){
        Player player = new Player("Hakan");

        assertEquals("Hakan", player.getName());
        assertEquals(0,player.getAffectionTokens());
    }

    //Arrange – Act – Assert:
    @Test
    void awardingAffectionTokenShouldIncreaseTotal(){
        Player player = new Player("Hakan");
        player.awardAffectionTokens();
        assertEquals(1, player.getAffectionTokens());
    }

    @Test
    void newPlayerShouldHaveEmptyHand(){
        Player player = new Player("Hakan");
        assertEquals(0, player.getHand().size());
    }

    @Test
    void receivingCardShouldAddCardToHand(){
        Player player = new Player("Hakan");
        player.receiveCard(CardType.PRIEST);
        assertEquals(1, player.getHand().size());
        assertEquals(CardType.PRIEST, player.getHand().getFirst());
    }

    @Test
    void receivingThirdCardShouldThrowException(){
        Player player = new Player("Hakan");
        player.receiveCard(CardType.GUARD);
        player.receiveCard(CardType.PRIEST);

        assertThrows(
                IllegalStateException.class, () -> player.receiveCard(CardType.BARON)
        );
    }

    @Test
    void discardingCardShouldMoveItFromHandToDiscardPile(){
        Player player = new Player("Hakan");
        player.receiveCard(CardType.PRIEST);
        player.receiveCard(CardType.GUARD);

        player.discardCard(CardType.PRIEST);


        assertEquals(List.of(CardType.GUARD), player.getHand());
        assertEquals(List.of(CardType.PRIEST), player.getDiscardPile());
    }

    @Test
    void discardingCardNotInHandShouldThrowException(){
        Player player = new Player("Hakan");
        player.receiveCard(CardType.PRIEST);

        assertThrows(
                IllegalArgumentException.class, () -> player.discardCard(CardType.GUARD)
        );
    }

    @Test
    void eliminatingPlayerShouldDiscardHandAndMarkPlayerElimininated(){
        Player player = new Player("Hakan");
        player.receiveCard(CardType.PRIEST);

        player.eliminate();

        assertTrue(player.isEliminated());
        assertTrue(player.getHand().isEmpty());
        assertEquals(List.of(CardType.PRIEST), player.getDiscardPile());
    }

    @Test
    void protectingPlayerShouldMarkPlayerAsProtected() {
        Player player = new Player("Hakan");

        assertFalse(player.isProtectedFromEffects());

        player.protectFromEffects();

        assertTrue(player.isProtectedFromEffects());
    }

    @Test
    void removingProtectionShouldMarkPlayerAsUnProtected() {
        Player player = new Player("Hakan");
        //ungeschützt → geschützt → ungeschützt
        player.protectFromEffects();
        player.removeProtection();

        assertFalse(player.isProtectedFromEffects());
    }

    @Test
    void resettingForNewRoundShouldClearRoundStateAndKeepGameState(){
        Player player = new Player("Hakan");
        player.awardAffectionTokens();
        player.receiveCard(CardType.PRIEST);
        player.receiveCard(CardType.GUARD);
        player.discardCard(CardType.PRIEST);
        player.protectFromEffects();

        player.resetForNewRound();

        assertEquals("Hakan", player.getName());
        assertEquals(1, player.getAffectionTokens());
        assertTrue(player.getHand().isEmpty());
        assertTrue(player.getDiscardPile().isEmpty());
        assertFalse(player.isEliminated());
        assertFalse(player.isProtectedFromEffects());

    }

    @Test
    void resettingForNewRoundShouldReactiveEliminatedPlayer(){
        Player player = new Player("Hakan");
        player.receiveCard(CardType.PRIEST);
        player.eliminate();

        assertTrue(player.isEliminated());

        player.resetForNewRound();

        assertFalse(player.isEliminated());
        assertTrue(player.getDiscardPile().isEmpty());

    }
}
