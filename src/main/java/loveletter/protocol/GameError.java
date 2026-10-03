package loveletter.protocol;


import java.util.Objects;

/**
 * Describes an error reported by the game server.
 *
 * @param code the error category
 * @param message the explanation displayed to the user
 */
public record GameError(GameErrorCode code, String message) {


    /**
     * Creates an error with a required code and non-blank message.
     *
     * @throws NullPointerException if code or message is null
     * @throws IllegalArgumentException if message is blank
     */
    public GameError {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(message, "message must not be null");

        if(message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
