package loveletter.protocol;

import loveletter.model.CardType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GameStateCodecTest {

    @Test
    void shouldPreserveAnEmptyGameStateDuringRoundTrip(){
        GameStateCodec codec = new GameStateCodec();

        GameState original = new GameState(
            GamePhase.NO_GAME,
            "hakan",
            List.of(),
            null,
            List.of(),
            0,
            List.of(),
            List.of(),
            List.of());


        String json = codec.encode(original);
        GameState decoded =  codec.decode(json);

        assertEquals(original, decoded);
    }

    @Test
    void shouldPreservePlayersAndCardsDuringRoundTrip(){
        GameStateCodec codec = new GameStateCodec();

        PlayerState hakan = new PlayerState(
                "hakan",
                2,
                false,
                false,
                2,
                List.of(CardType.PRIEST)
        );

        PlayerState nati = new PlayerState(
                "nati",
                1,
                false,
                true,
                1,
                List.of(CardType.HANDMAID)
        );

        GameState original = new GameState(
                GamePhase.ROUND_IN_PROGRESS,
                "hakan",
                List.of(hakan,nati),
                "hakan",
                List.of(CardType.GUARD, CardType.BARON),
                5,
                List.of(CardType.GUARD,
                        CardType.PRIEST,
                        CardType.KING
                ),
                List.of(),
                List.of()
        );

        String json = codec.encode(original);
        GameState decoded = codec.decode(json);

        assertEquals(original, decoded);

    }

    @Test
    void shouldRejectBlankJson(){
        GameStateCodec codec = new GameStateCodec();

        assertThrows(IllegalArgumentException.class,
                () -> codec.decode("    "));
    }

    @Test
    void shouldRejectJsonNull(){
        GameStateCodec codec = new GameStateCodec();

        assertThrows(
                IllegalArgumentException.class,
                () -> codec.decode("null")
        );
    }

}
