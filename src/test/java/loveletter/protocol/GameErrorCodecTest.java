package loveletter.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GameErrorCodecTest {

    @Test
    void shouldPreserveErrorDuringRoundTrip(){
        GameErrorCodec codec = new GameErrorCodec();

        GameError original = new GameError(GameErrorCode.ACTION_REJECTED, "A game already exists.");

        String json = codec.encode(original);
        GameError decoded = codec.decode(json);

        assertEquals(original, decoded);
    }
}
