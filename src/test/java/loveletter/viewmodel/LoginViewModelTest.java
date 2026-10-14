package loveletter.viewmodel;

import loveletter.model.CardType;
import loveletter.protocol.GamePhase;
import loveletter.protocol.GameState;
import loveletter.protocol.PlayerState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoginViewModelTest {

    @Test
    void playerOverviewShouldMarkOnlyLocalPlayer(){
        //Arrange
        LoginViewModel viewModel = new LoginViewModel();

        try{
            PlayerState hakan = new PlayerState("hakan", 0, false, false, 0, List.of());

            PlayerState nati = new PlayerState("nati", 0, false, false, 0, List.of());

            GameState state = new GameState(GamePhase.WAITING_FOR_PLAYERS,
                    "nati",
                    List.of(hakan,nati),
                    null,
                    List.of(),
                    0,
                    List.of(),
                    List.of(),
                    List.of());

            viewModel.applyGameState(state);
            String overview = viewModel.playerOverviewText();

            assertEquals("hakan - Tokens: 0\nnati (You) - Tokens: 0", overview);
        }finally{
            viewModel.close();
}
    }

    @Test
    void playerOverviewShouldShowTurnProtectionAndTokens(){
        LoginViewModel viewModel = new LoginViewModel();

        try{

            PlayerState hakan = new PlayerState("hakan", 2, false, true, 1, List.of(CardType.HANDMAID));

            PlayerState nati = new PlayerState("nati", 1, false, false, 2, List.of());

            GameState state = new GameState(
                    GamePhase.ROUND_IN_PROGRESS,
                    "hakan",
                    List.of(hakan,nati),
                    "nati",
                    List.of(CardType.PRIEST),
                    8,
                    List.of(),
                    List.of(),
                    List.of()
            );

            viewModel.applyGameState(state);
            String overview = viewModel.playerOverviewText();

            assertEquals(
                    "hakan (You) - Tokens: 2 - Protected\n"
                            + "nati - Tokens: 1 - Current turn",
                    overview
            );
        }finally{
            viewModel.close();
        }
    }

    @Test
    void finishedRoundShouldShowEliminationAndRoundWinner(){
        LoginViewModel viewModel = new LoginViewModel();

        try{
            PlayerState hakan = new PlayerState("hakan", 0, true, false, 0, List.of(CardType.PRINCESS));
            PlayerState nati = new PlayerState("nati", 1, false, false, 1, List.of());

            GameState state = new GameState(
                    GamePhase.ROUND_OVER,
                    "hakan",
                    List.of(hakan,nati),
                    null,
                    List.of(),
                    8,
                    List.of(),
                    List.of("nati"),
                    List.of()
            );

            viewModel.applyGameState(state);

            String overview = viewModel.playerOverviewText();
            String result = viewModel.gameResultText();

            // Assert
            assertEquals(
                    "hakan (You) - Tokens: 0 - Eliminated\n"
                            + "nati - Tokens: 1",
                    overview
            );

            assertEquals("Round over. Winner(s): nati", result);

        }finally{
            viewModel.close();
        }


    }

    @Test
    void gameResultShouldDistinguishRoundWinnersFromGameWinners(){

        LoginViewModel viewModel = new LoginViewModel();

        try{
            PlayerState hakan = new PlayerState("hakan", 7, false, false, 1, List.of());
            PlayerState nati = new PlayerState("nati", 3, false, false, 1, List.of());

            GameState state = new GameState(
                    GamePhase.GAME_OVER,
                    "hakan",
                    List.of(hakan,nati),
                    null,
                    List.of(CardType.KING),
                    0,
                    List.of(),
                    List.of("hakan", "nati"),
                    List.of("hakan")
            );

            viewModel.applyGameState(state);
            String result = viewModel.gameResultText();

            assertEquals("Round winner(s): hakan, nati\n"
            + "Game winner(s): hakan", result);

        }finally{
            viewModel.close();
        }
    }


    @Test
    void newRoundShouldClearPreviousResultAndElimination(){

        LoginViewModel viewModel = new LoginViewModel();
        try{
            GameState finishedRound = new GameState(
                    GamePhase.ROUND_OVER,
                    "hakan",
                    List.of(new PlayerState("hakan", 0, true, false,0 ,List.of(CardType.PRINCESS)),
                            new PlayerState("nati", 1, false, false, 1, List.of())),
                    null, List.of(),8,List.of(),  List.of("nati"), List.of()

            );

            viewModel.applyGameState(finishedRound);

            assertEquals("Round over. Winner(s): nati", viewModel.gameResultText());

            GameState nextRound = new GameState(
                    GamePhase.ROUND_IN_PROGRESS,
                    "hakan",
                    List.of(new PlayerState("hakan", 0 , false, false,1,List.of()),
                    new PlayerState("nati", 1, false, false, 2, List.of())), "nati", List.of(CardType.PRIEST),
                    9,
                    List.of(),
                    List.of(),
                    List.of());

            viewModel.applyGameState(nextRound);
            assertEquals("", viewModel.gameResultText());

            assertEquals(
                    "hakan (You) - Tokens: 0\n"
                            + "nati - Tokens: 1 - Current turn",
                    viewModel.playerOverviewText()
            );
        } finally {
            viewModel.close();
        }
    }

    @Test
    void countessWithKingShouldOnlyAllowPlayingCountess(){
        LoginViewModel viewModel = new LoginViewModel();

        try {
            GameState state = new GameState(
                    GamePhase.ROUND_IN_PROGRESS,
                    "hakan",
                    List.of(
                            new PlayerState(
                                    "hakan", 0, false, false,
                                    2, List.of()),
                            new PlayerState(
                                    "nati", 0, false, false,
                                    1, List.of())
                    ),
                    "hakan",
                    List.of(CardType.COUNTESS, CardType.KING),
                    9,
                    List.of(),
                    List.of(),
                    List.of()
            );

            viewModel.applyGameState(state);

            // Trotz gültigem Ziel darf der König nicht gespielt werden.
            viewModel.selectCard(CardType.KING);
            viewModel.selectTarget("nati");

            assertFalse(viewModel.canPlaySelectedCard());

            // Die Gräfin muss dagegen spielbar sein.
            viewModel.selectCard(CardType.COUNTESS);

            assertTrue(viewModel.canPlaySelectedCard());
        } finally {
            viewModel.close();
        }


    }

    @Test
    void countessWithPrinceShouldOnlyAllowPlayingCountess() {
        LoginViewModel viewModel = new LoginViewModel();

        try {
            GameState state = new GameState(
                    GamePhase.ROUND_IN_PROGRESS,
                    "hakan",
                    List.of(
                            new PlayerState(
                                    "hakan", 0, false, false,
                                    2, List.of()),
                            new PlayerState(
                                    "nati", 0, false, false,
                                    1, List.of())
                    ),
                    "hakan",
                    List.of(CardType.COUNTESS, CardType.PRINCE),
                    9,
                    List.of(),
                    List.of(),
                    List.of()
            );

            viewModel.applyGameState(state);

            // Der Prinz darf sich selbst als Ziel wählen.
            viewModel.selectCard(CardType.PRINCE);
            viewModel.selectTarget("hakan");

            // Trotzdem verhindert die Gräfin-Regel das Ausspielen.
            assertFalse(viewModel.canPlaySelectedCard());

            viewModel.selectCard(CardType.COUNTESS);

            assertTrue(viewModel.canPlaySelectedCard());
        } finally {
            viewModel.close();
        }
    }

    @Test
    void countessWithHandmaidShouldAllowPlayingEitherCard() {
        LoginViewModel viewModel = new LoginViewModel();

        try {
            GameState state = new GameState(
                    GamePhase.ROUND_IN_PROGRESS,
                    "hakan",
                    List.of(
                            new PlayerState(
                                    "hakan", 0, false, false,
                                    2, List.of()),
                            new PlayerState(
                                    "nati", 0, false, false,
                                    1, List.of())
                    ),
                    "hakan",
                    List.of(CardType.COUNTESS, CardType.HANDMAID),
                    9,
                    List.of(),
                    List.of(),
                    List.of()
            );

            viewModel.applyGameState(state);

            // Ohne König oder Prinz ist die andere Karte erlaubt.
            viewModel.selectCard(CardType.HANDMAID);
            assertTrue(viewModel.canPlaySelectedCard());

            // Die Gräfin darf auch freiwillig gespielt werden.
            viewModel.selectCard(CardType.COUNTESS);
            assertTrue(viewModel.canPlaySelectedCard());
        } finally {
            viewModel.close();
        }
    }
    @Test
    void guardShouldRequireTargetAndGuessWhenTargetIsAvailable() {
        LoginViewModel viewModel = new LoginViewModel();

        try {
            GameState state = new GameState(
                    GamePhase.ROUND_IN_PROGRESS,
                    "hakan",
                    List.of(
                            new PlayerState(
                                    "hakan", 0, false, false,
                                    2, List.of()),
                            new PlayerState(
                                    "nati", 0, false, false,
                                    1, List.of())
                    ),
                    "hakan",
                    List.of(CardType.GUARD, CardType.PRIEST),
                    9,
                    List.of(),
                    List.of(),
                    List.of()
            );

            viewModel.applyGameState(state);
            viewModel.selectCard(CardType.GUARD);

            // Weder Ziel noch Vermutung ausgewählt.
            assertFalse(viewModel.canPlaySelectedCard());

            viewModel.selectTarget("nati");

            // Das Ziel allein reicht noch nicht.
            assertFalse(viewModel.canPlaySelectedCard());

            viewModel.selectGuess(CardType.KING);

            // Jetzt sind Ziel und gültige Vermutung vorhanden.
            assertTrue(viewModel.canPlaySelectedCard());
        } finally {
            viewModel.close();
        }
    }

    @Test
    void guardShouldBePlayableWithoutGuessWhenNoTargetIsAvailable() {
        LoginViewModel viewModel = new LoginViewModel();

        try {
            GameState state = new GameState(
                    GamePhase.ROUND_IN_PROGRESS,
                    "hakan",
                    List.of(
                            new PlayerState(
                                    "hakan", 0, false, false,
                                    2, List.of()),
                            new PlayerState(
                                    "nati", 0, false, true,
                                    1, List.of(CardType.HANDMAID))
                    ),
                    "hakan",
                    List.of(CardType.GUARD, CardType.PRIEST),
                    8,
                    List.of(),
                    List.of(),
                    List.of()
            );

            viewModel.applyGameState(state);
            viewModel.selectCard(CardType.GUARD);

            // Nati ist geschützt; Hakan darf sich nicht selbst wählen.
            assertTrue(viewModel.availableTargetNames().isEmpty());

            // Ohne verfügbares Ziel ist keine Vermutung erforderlich.
            assertTrue(viewModel.canPlaySelectedCard());
        } finally {
            viewModel.close();
        }
    }

    @Test
    void changingSelectedCardShouldClearTargetAndGuess() {
        LoginViewModel viewModel = new LoginViewModel();

        try {
            GameState state = new GameState(
                    GamePhase.ROUND_IN_PROGRESS,
                    "hakan",
                    List.of(
                            new PlayerState(
                                    "hakan", 0, false, false,
                                    2, List.of()),
                            new PlayerState(
                                    "nati", 0, false, false,
                                    1, List.of())
                    ),
                    "hakan",
                    List.of(CardType.GUARD, CardType.PRIEST),
                    9,
                    List.of(),
                    List.of(),
                    List.of()
            );

            viewModel.applyGameState(state);

            viewModel.selectCard(CardType.GUARD);
            viewModel.selectTarget("nati");
            viewModel.selectGuess(CardType.KING);

            // Sicherstellen, dass beide Werte vorher gesetzt waren.
            assertEquals("nati", viewModel.selectedTargetProperty().get());
            assertEquals(
                    CardType.KING,
                    viewModel.selectedGuessProperty().get()
            );

            // Act: Vom Wächter zum Priester wechseln.
            viewModel.selectCard(CardType.PRIEST);

            // Assert: Die alte Auswahl wurde zurückgesetzt.
            assertNull(viewModel.selectedTargetProperty().get());
            assertNull(viewModel.selectedGuessProperty().get());

            // Für den Priester muss erneut ein Ziel gewählt werden.
            assertFalse(viewModel.canPlaySelectedCard());
        } finally {
            viewModel.close();
        }
    }
}
