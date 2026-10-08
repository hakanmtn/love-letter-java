package loveletter.viewmodel;

import loveletter.model.CardType;
import loveletter.protocol.GamePhase;
import loveletter.protocol.GameState;
import loveletter.protocol.PlayerState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
