package loveletter.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

}
