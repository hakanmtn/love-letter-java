package loveletter.protocol;

import com.google.gson.JsonParseException;
import loveletter.model.CardType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GameActionCodecTest {

    @Test
    void shouldPreservePlayActionDuringRoundTrip(){
        GameActionCodec codec = new GameActionCodec();

        GameAction original = new GameAction(
                GameActionType.PLAY,
                CardType.GUARD,
                "nati",
                CardType.KING
        );

        String json = codec.encode(original);
        GameAction decoded = codec.decode(json);

        assertEquals(original,decoded);

    }

    @Test
    void shouldPreserveCreateActionDuringRoundTrip(){
        GameActionCodec codec = new GameActionCodec();

        GameAction original = new GameAction(
                GameActionType.CREATE,
                null,
                null,
                null
        );

        String json = codec.encode(original);
        GameAction decoded = codec.decode(json);
        assertEquals(original,decoded);
    }

    @Test
    void shouldRejectJsonNull(){
        GameActionCodec codec = new GameActionCodec();

        assertThrows(
                IllegalArgumentException.class,
                () -> codec.decode("null")
        );
    }

    @Test
    void shouldRejectMalformedJson(){
        GameActionCodec codec = new GameActionCodec();

        assertThrows(
                JsonParseException.class,
                () -> codec.decode("{")
        );
    }
}
