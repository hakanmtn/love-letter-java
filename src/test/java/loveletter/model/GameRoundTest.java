package loveletter.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

public class GameRoundTest {

  @Test
  void gameRoundShouldAcceptTwoPlayers() {
    List<Player> players = List.of(new Player("John"), new Player("Jane"));

    GameRound gameRound = new GameRound(players);

    assertEquals(2, gameRound.getPlayers().size());
  }

  @Test
  void gameRoundShouldRejectFewerThanTwoPlayers() {
    List<Player> players = List.of(new Player("Hakan"));

    assertThrows(IllegalArgumentException.class, () -> new GameRound(players));
  }

  @Test
  void gameRoundShouldRejectMoreThanFourPlayers() {
    List<Player> players =
        List.of(
            new Player("Hakan"),
            new Player("Nati"),
            new Player("Jane"),
            new Player("John"),
            new Player("Rafi"));
    assertThrows(IllegalArgumentException.class, () -> new GameRound(players));
  }

  @Test
  void twoPlayerRoundShouldPrepareCardsCorrectly() {
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
  void threePlayerRoundShouldNotRemoveFaceUpCards() {
    Player hakan = new Player("Hakan");
    Player nati = new Player("Nati");
    Player jane = new Player("Jane");

    GameRound gameRound = new GameRound(List.of(hakan, nati, jane));

    assertTrue(gameRound.hasReserveCard());
    assertTrue(gameRound.getFaceUpRemovedCards().isEmpty());
    assertEquals(1, hakan.getHand().size());
    assertEquals(1, nati.getHand().size());
    assertEquals(1, jane.getHand().size());
    assertEquals(12, gameRound.getRemainingDeckSize());
  }

  @Test
  void firstPlayerShouldBeCurrentPlayer() {
    Player hakan = new Player("Hakan");
    Player nati = new Player("Nati");

    GameRound gameRound = new GameRound(List.of(hakan, nati));
    gameRound.startCurrentTurn();
    assertSame(hakan, gameRound.getCurrentPlayer());
  }

  @Test
  void startingTurnShouldRemoveProtectionAndDrawCard() {
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
  void endingTurnShouldSkipEliminatedPlayers() {
    Player hakan = new Player("Hakan");
    Player nati = new Player("Nati");
    Player jane = new Player("Jane");

    GameRound gameRound = new GameRound(List.of(hakan, nati, jane));
    nati.eliminate();
    gameRound.startCurrentTurn();
    hakan.discardCard(hakan.getHand().getFirst());
    gameRound.endCurrentTurn();

    assertSame(jane, gameRound.getCurrentPlayer());
  }

  @Test
  void endingTurnBeforeDiscardingShouldThrowException() {
    Player hakan = new Player("Hakan");
    Player nati = new Player("Nati");

    GameRound gameRound = new GameRound(List.of(hakan, nati));
    gameRound.startCurrentTurn();
    assertThrows(IllegalStateException.class, gameRound::endCurrentTurn);
  }

  @Test
  void roundShouldEndWhenOnlyOnePlayerRemains() {
    Player hakan = new Player("Hakan");
    Player nati = new Player("Nati");
    GameRound gameRound = new GameRound(List.of(hakan, nati));

    gameRound.startCurrentTurn();
    nati.eliminate();

    assertTrue(gameRound.isRoundOver());
  }

  @Test
  void roundShouldEndWhenDeckIsEmpty() {
    Player hakan = new Player("Hakan");
    Player nati = new Player("Nati");
    GameRound gameRound = new GameRound(List.of(hakan, nati));

    while (gameRound.getRemainingDeckSize() > 0) {
      gameRound.startCurrentTurn();

      Player currentPlayer = gameRound.getCurrentPlayer();
      CardType cardToDiscard = currentPlayer.getHand().getFirst();
      currentPlayer.discardCard(cardToDiscard);

      gameRound.endCurrentTurn();
    }
    assertTrue(gameRound.isRoundOver());
  }

  @Test
  void startingTurnAfterRoundIsOverShouldThrowException() {
    Player hakan = new Player("Hakan");
    Player nati = new Player("Nati");
    GameRound gameRound = new GameRound(List.of(hakan, nati));

    nati.eliminate();
    assertTrue(gameRound.isRoundOver());
    assertThrows(IllegalStateException.class, gameRound::startCurrentTurn);
  }

  @Test
  void newRoundShouldNotBeOver() {
    Player hakan = new Player("Hakan");
    Player nati = new Player("Nati");
    GameRound gameRound = new GameRound(List.of(hakan, nati));

    assertFalse(gameRound.isRoundOver());
  }

  @Test
  void roundShouldEndOnlyAfterFinalTurnIsCompleted() {
    Player hakan = new Player("Hakan");
    Player nati = new Player("nati");

    GameRound gameRound = new GameRound(List.of(hakan, nati));

    while (gameRound.getRemainingDeckSize() > 1) {
      gameRound.startCurrentTurn();

      Player currentPlayer = gameRound.getCurrentPlayer();
      CardType card = currentPlayer.getHand().getFirst();
      currentPlayer.discardCard(card);

      gameRound.endCurrentTurn();
    }

    gameRound.startCurrentTurn();
    assertFalse(gameRound.isRoundOver());

    Player currentPlayer = gameRound.getCurrentPlayer();
    CardType card = currentPlayer.getHand().getFirst();
    currentPlayer.discardCard(card);

    gameRound.endCurrentTurn();

    assertTrue(gameRound.isRoundOver());
  }

  @Test
  void remainingActivePlayerShouldWinRound() {
    Player hakan = new Player("Hakan");
    Player nati = new Player("nati");
    GameRound gameRound = new GameRound(List.of(hakan, nati));

    nati.eliminate();

    assertEquals(List.of(hakan), gameRound.determineWinners());
  }

  @Test
  void playerWithHighestCardShouldWinWhenDeckIsEmpty() {
    Player hakan = new Player("Hakan");
    Player nati = new Player("nati");
    GameRound gameRound = new GameRound(List.of(hakan, nati));

    finishRoundByEmptyingDeck(gameRound);

    replaceHandWith(hakan, CardType.GUARD);
    replaceHandWith(nati, CardType.PRINCESS);

    assertEquals(List.of(nati), gameRound.determineWinners());
  }

  private void finishRoundByEmptyingDeck(GameRound gameRound) {
    while (!gameRound.isRoundOver()) {
      gameRound.startCurrentTurn();

      Player currentPlayer = gameRound.getCurrentPlayer();
      CardType card = currentPlayer.getHand().getFirst();
      currentPlayer.discardCard(card);

      gameRound.endCurrentTurn();
    }
  }

  private void replaceHandWith(Player player, CardType cardType) {

    while (!player.getHand().isEmpty()) {
      CardType currentCard = player.getHand().getFirst();
      player.discardCard(currentCard);
    }
    player.receiveCard(cardType);
  }

  @Test
  void discardPileShouldBreakTieBetweenEqualHandCards() {
    Player hakan = new Player("Hakan");
    Player nati = new Player("nati");
    GameRound gameRound = new GameRound(List.of(hakan, nati));

    finishRoundByEmptyingDeck(gameRound);

    replaceHandWith(hakan, CardType.PRINCESS);
    replaceHandWith(nati, CardType.PRINCESS);

    while (discardValueOf(nati) <= discardValueOf(hakan)) {
      addToDiscardPile(nati, CardType.GUARD);
    }
    assertEquals(List.of(nati), gameRound.determineWinners());
    assertEquals(CardType.PRINCESS, hakan.getHand().getFirst());
    assertEquals(CardType.PRINCESS, nati.getHand().getFirst());
    assertTrue(discardValueOf(nati) > discardValueOf(hakan));
  }

  private int discardValueOf(Player player) {
    return player.getDiscardPile().stream().mapToInt(CardType::getValue).sum();
  }

  private void addToDiscardPile(Player player, CardType cardType) {
    if (player.getHand().size() != 1) {
      throw new IllegalStateException("Test setup expected exactly one hand card");
    }
    player.receiveCard(cardType);
    player.discardCard(cardType);
  }

  @Test
  void equalHandAndDiscardValuesShouldProduceMultipleWinners(){

    Player hakan = new Player("Hakan");
    Player nati = new Player("Nati");

    GameRound gameRound = new GameRound(List.of(hakan,nati));

    finishRoundByEmptyingDeck(gameRound);

    replaceHandWith(hakan,CardType.PRINCESS);
    replaceHandWith(nati,CardType.PRINCESS);

    balanceDiscardValues(hakan,nati);

    assertEquals(List.of(hakan,nati),gameRound.determineWinners());
  }


  private void balanceDiscardValues(Player first, Player second){
    while(discardValueOf(first) < discardValueOf(second)){
      addToDiscardPile(first,CardType.GUARD);
    }

    while(discardValueOf(second) < discardValueOf(first)){
      addToDiscardPile(second,CardType.GUARD);
    }
  }

  @Test
  void winnerShouldReceiveAffectionToken(){
    Player hakan = new Player("Hakan");
    Player nati = new Player("Nati");
    GameRound gameRound = new GameRound(List.of(hakan, nati));

    nati.eliminate();

    List<Player> winners = gameRound.awardWinnerTokens();

    assertEquals(List.of(hakan), winners);
    assertEquals(1, hakan.getAffectionTokens());
    assertEquals(0,nati.getAffectionTokens());
  }

  @Test
  void winnerTokensShouldOnlyBeAwardedOnce(){
    Player hakan = new Player("Hakan");
    Player nati = new Player("Nati");
    GameRound gameRound = new GameRound(List.of(hakan,nati));

    nati.eliminate();

    gameRound.awardWinnerTokens();

    assertThrows(IllegalStateException.class, gameRound::awardWinnerTokens);

    assertEquals(1, hakan.getAffectionTokens());
  }


  @Test
  void playingHandmaidShouldDiscardCardAndProtectPlayer(){
    Player hakan = new Player("Hakan");
    Player nati = new Player("Nati");
    GameRound gameRound = new GameRound(List.of(hakan, nati));

    gameRound.startCurrentTurn();

    replaceHandWith(hakan,CardType.PRIEST);
    hakan.receiveCard(CardType.HANDMAID);

    int discardCountBefore =hakan.getDiscardPile().size();

    gameRound.playCard(hakan, CardType.HANDMAID);

    assertEquals(List.of(CardType.PRIEST), hakan.getHand());
    assertEquals(discardCountBefore + 1 , hakan.getDiscardPile().size());
    assertEquals(CardType.HANDMAID, hakan.getDiscardPile().getLast());

    assertTrue(hakan.isProtectedFromEffects());

    gameRound.endCurrentTurn();

    assertSame(nati,gameRound.getCurrentPlayer());
    assertTrue(hakan.isProtectedFromEffects());
  }

  @Test
  void handmainProtectionShouldExpireAtStartOfNextOwnTurn(){
    Player hakan = new Player("Hakan");
    Player nati = new Player("Nati");
    GameRound gameRound = new GameRound(List.of(hakan,nati));

    //Hakan plays the Handmade
    gameRound.startCurrentTurn();;
    replaceHandWith(hakan,CardType.PRIEST);
    hakan.receiveCard(CardType.HANDMAID);

    gameRound.playCard(hakan, CardType.HANDMAID);
    gameRound.endCurrentTurn();

    //Hakan is protected, while Nati is playing
    gameRound.startCurrentTurn();

    assertSame(nati, gameRound.getCurrentPlayer());
    assertTrue(hakan.isProtectedFromEffects());

    replaceHandWith(nati, CardType.PRIEST);
    nati.receiveCard(CardType.HANDMAID);

    gameRound.playCard(nati, CardType.HANDMAID);
    gameRound.endCurrentTurn();

    //Der Wechsel zu Hakan allein entfernt seinen Schutz noch nicht
    assertSame(hakan, gameRound.getCurrentPlayer());
    assertTrue(hakan.isProtectedFromEffects());

    //Erst der tatsächlichen Zugbeginn entfernt Hakans Schutz
    gameRound.startCurrentTurn();

    assertFalse(hakan.isProtectedFromEffects());
    assertTrue(nati.isProtectedFromEffects());
  }

  @Test
  void playerShouldNotPlayDuringAnotherPlayersTurn(){
    Player hakan = new Player("Hakan");
    Player nati = new Player("Nati");
    GameRound gameRound = new GameRound(List.of(hakan,nati));

    gameRound.startCurrentTurn();

    replaceHandWith(nati, CardType.PRIEST);
    nati.receiveCard(CardType.HANDMAID);

    List<CardType> handBefore = List.copyOf(nati.getHand());
    List<CardType> discardBefore = List.copyOf(nati.getDiscardPile());

    assertThrows(IllegalArgumentException.class, () -> gameRound.playCard(nati,CardType.HANDMAID));

    assertEquals(handBefore, nati.getHand());
    assertEquals(discardBefore, nati.getDiscardPile());
    assertFalse(nati.isProtectedFromEffects());
  }

  @Test
  void playerShouldNotPlayTwoCardsDuringSameTurn(){
    Player hakan = new Player("Hakan");
    Player nati = new Player("Nati");
    GameRound gameRound = new GameRound(List.of(hakan,nati));

    gameRound.startCurrentTurn();

    replaceHandWith(hakan, CardType.HANDMAID);
    hakan.receiveCard(CardType.HANDMAID);

    gameRound.playCard(hakan, CardType.HANDMAID);

    int discardCount = hakan.getDiscardPile().size();

    assertThrows(
            IllegalStateException.class,
            () -> gameRound.playCard(hakan, CardType.HANDMAID));

    assertEquals(List.of(CardType.HANDMAID), hakan.getHand());
    assertEquals(discardCount, hakan.getDiscardPile().size());
    assertTrue(hakan.isProtectedFromEffects());
  }

  @Test
  void playingPrincessShouldEliminatePlayer(){
    Player hakan = new Player("Hakan");
    Player nati = new Player("Natalie");
    GameRound gameRound = new GameRound(List.of(hakan,nati));

    gameRound.startCurrentTurn();

    replaceHandWith(hakan, CardType.HANDMAID);
    hakan.receiveCard(CardType.PRINCESS);

    int discardCountBefore = hakan.getDiscardPile().size();

    gameRound.playCard(hakan, CardType.PRINCESS);

    assertTrue(hakan.isEliminated());
    assertTrue(hakan.getHand().isEmpty());

    assertEquals(discardCountBefore + 2 , hakan.getDiscardPile().size());
    assertEquals(CardType.HANDMAID, hakan.getDiscardPile().getLast());

    gameRound.endCurrentTurn();

    assertTrue(gameRound.isRoundOver());
    assertEquals(List.of(nati), gameRound.determineWinners());
  }

  @Test
  void countessShouldBePlayableWithoutKingOrPrince(){
    Player hakan = new Player("Hakan");
    Player nati = new Player("Natalie");
    GameRound gameRound = new GameRound(List.of(hakan,nati));

    gameRound.startCurrentTurn();

    replaceHandWith(hakan, CardType.PRIEST);
    hakan.receiveCard(CardType.COUNTESS);

    gameRound.playCard(hakan, CardType.COUNTESS);

    assertEquals(List.of(CardType.PRIEST), hakan.getHand());
    assertEquals(
            CardType.COUNTESS, hakan.getDiscardPile().getLast()
    );

    assertFalse(hakan.isEliminated());
    assertFalse(hakan.isProtectedFromEffects());

    gameRound.endCurrentTurn();
    assertSame(nati, gameRound.getCurrentPlayer());
  }

  @Test
  void countessMustBePlayedWhenHoldingKingOrPrince(){
    for(CardType otherCard :
            List.of(CardType.KING,CardType.PRINCE)){
      Player hakan = new Player("Hakan");
      Player nati = new Player("Natalie");
      GameRound gameRound = new GameRound(List.of(hakan,nati));

      gameRound.startCurrentTurn();

      replaceHandWith(hakan, CardType.COUNTESS);
      hakan.receiveCard(otherCard);

      int discardCountBefore = hakan.getDiscardPile().size();

      assertThrows(
              IllegalArgumentException.class,
              () -> gameRound.playCard(hakan,otherCard)
      );
      assertEquals(
              discardCountBefore,
              hakan.getDiscardPile().size()
      );

      gameRound.playCard(hakan,CardType.COUNTESS);

      assertEquals(List.of(otherCard), hakan.getHand());

    }
  }

  @Test
  void availableOpponentsShouldExcludeSelfEliminatedAndProtectedPlayers(){

    Player hakan = new Player("Hakan");
    Player nati = new Player("Natalie");
    Player mika = new Player("Mika");
    Player lord = new Player("Lord");

    GameRound gameRound = new GameRound(List.of(hakan, nati,mika,lord));

    mika.eliminate();
    lord.protectFromEffects();

    assertEquals(List.of(nati), gameRound.getAvailableOpponents(hakan));
    lord.removeProtection();

    assertEquals(List.of(nati,lord), gameRound.getAvailableOpponents(hakan));
  }

  @Test
  void priestShouldRevealOpponentsCardWithoutChangingTheirHand(){
    Player hakan = new Player("Hakan");
    Player nati = new Player("Natalie");

    GameRound gameRound = new GameRound(List.of(hakan, nati));
    gameRound.startCurrentTurn();

    replaceHandWith(hakan, CardType.HANDMAID);
    hakan.receiveCard(CardType.PRIEST);
    replaceHandWith(nati, CardType.KING);

    List<CardType> natiDiscardBefore = List.copyOf(nati.getDiscardPile());

    Optional<CardType> revealed = gameRound.playCard(hakan,CardType.PRIEST,nati);

    assertEquals(Optional.of(CardType.KING), revealed);
    assertEquals(List.of(CardType.KING) , nati.getHand());
    assertEquals(natiDiscardBefore, nati.getDiscardPile());

    assertEquals(List.of(CardType.HANDMAID), hakan.getHand());
    assertEquals(CardType.PRIEST, hakan.getDiscardPile().getLast());

    gameRound.endCurrentTurn();
    assertSame(nati, gameRound.getCurrentPlayer());

  }

  @Test
  void priestShouldRejectSelfTargetWithoutChangingCards(){
    Player hakan = new Player("Hakan");
    Player nati = new Player("Natalie");

    GameRound gameRound = new GameRound(List.of(hakan, nati));
    gameRound.startCurrentTurn();

    replaceHandWith(hakan, CardType.HANDMAID);
    hakan.receiveCard(CardType.PRIEST);

    List<CardType> handBefore = List.copyOf(hakan.getHand());
    List<CardType> discardCardBefore = List.copyOf(hakan.getDiscardPile());

    assertThrows(IllegalArgumentException.class,
            () -> gameRound.playCard(hakan, CardType.PRIEST, hakan));

    assertEquals(handBefore, hakan.getHand());
    assertEquals(discardCardBefore, hakan.getDiscardPile());

    Optional<CardType> revealedCard = gameRound.playCard(hakan,CardType.PRIEST,nati);

    assertEquals(Optional.of(nati.getHand().getFirst()), revealedCard);

    gameRound.endCurrentTurn();
    assertEquals(nati, gameRound.getCurrentPlayer());
  }

  @Test
  void priestShouldHaveNoEffectWhenNoOpponentIsAvailable(){

    Player hakan = new Player("Hakan");
    Player nati = new Player("Natalie");

    GameRound gameRound = new GameRound(List.of(hakan, nati));
    gameRound.startCurrentTurn();

    replaceHandWith(hakan, CardType.HANDMAID);
    hakan.receiveCard(CardType.PRIEST);

    replaceHandWith(nati, CardType.KING);
    nati.protectFromEffects();

    List<CardType> natiDiscardCardBefore = List.copyOf(nati.getDiscardPile());

    Optional<CardType> revealedCard = gameRound.playCard(hakan, CardType.PRIEST,null);

    assertTrue(revealedCard.isEmpty());

    assertEquals(List.of(CardType.HANDMAID), hakan.getHand());
    assertEquals(CardType.PRIEST, hakan.getDiscardPile().getLast());

    assertEquals(List.of(CardType.KING), nati.getHand());
    assertEquals(natiDiscardCardBefore, nati.getDiscardPile());
    assertTrue(nati.isProtectedFromEffects());

    gameRound.endCurrentTurn();
    assertSame(nati, gameRound.getCurrentPlayer());
  }

  @Test
  void priestShouldRejectProtectedTargetWithoutChangingCards(){
    Player hakan = new Player("Hakan");
    Player nati = new Player("Natalie");

    GameRound gameRound = new GameRound(List.of(hakan, nati));
    gameRound.startCurrentTurn();

    replaceHandWith(hakan, CardType.HANDMAID);
    hakan.receiveCard(CardType.PRIEST);

    replaceHandWith(nati, CardType.KING);
    nati.protectFromEffects();

    List<CardType> handBefore = List.copyOf(hakan.getHand());
    List<CardType> discardBefore = List.copyOf(hakan.getDiscardPile());

    assertThrows(IllegalArgumentException.class,
            () -> gameRound.playCard(hakan,CardType.PRIEST,nati));

    assertEquals(handBefore, hakan.getHand());
    assertEquals(discardBefore,hakan.getDiscardPile());
    assertEquals(List.of(CardType.KING), nati.getHand());
    assertTrue(nati.isProtectedFromEffects());

    gameRound.playCard(hakan, CardType.PRIEST);
    gameRound.endCurrentTurn();

    assertSame(nati, gameRound.getCurrentPlayer());

  }

  @Test
  void baronShouldEliminateOpponentWithLowerHandValue(){
    Player hakan = new Player("Hakan");
    Player nati = new Player("Nati");
    GameRound gameRound = new GameRound(List.of(hakan, nati));

    gameRound.startCurrentTurn();

    replaceHandWith(hakan, CardType.KING);
    hakan.receiveCard(CardType.BARON);
    replaceHandWith(nati, CardType.PRIEST);

    gameRound.playCard(hakan,CardType.BARON, nati);

    assertTrue(nati.isEliminated());
    assertTrue(nati.getHand().isEmpty());
    assertEquals(CardType.PRIEST, nati.getDiscardPile().getLast());

    assertFalse(hakan.isEliminated());
    assertEquals(List.of(CardType.KING), hakan.getHand());
    assertEquals(CardType.BARON, hakan.getDiscardPile().getLast());

    gameRound.endCurrentTurn();
    assertTrue(gameRound.isRoundOver());

    assertEquals(List.of(hakan), gameRound.determineWinners());
  }
}

