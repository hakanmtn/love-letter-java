package loveletter.protocol;

import com.google.gson.Gson;

import java.util.Objects;

/** Converts game errors to and from JSON. */
public class GameErrorCodec {

  private final Gson gson = new Gson();

  /** Creates a codec for game-error messages. */
  public GameErrorCodec() {}


    /**
     * Encodes a game error as JSON.
     *
     * @param error the error to encode
     * @return the JSON representation
     * @throws NullPointerException if error is null
     */
    public String encode(GameError error) {
      Objects.requireNonNull(error, "error must not be null");
      return gson.toJson(error);
    }

    /**
     * Decodes a game error from JSON.
     *
     * @param json the JSON representation
     * @return the decoded error
     * @throws NullPointerException if json is null
     * @throws IllegalArgumentException if json is blank or represents null
     * @throws com.google.gson.JsonParseException if the JSON is malformed
     */
    public GameError decode(String json) {
      Objects.requireNonNull(json, "json must not be null");

      if (json.isBlank()) {
          throw new IllegalArgumentException("json must not be blank");
      }

      GameError error = gson.fromJson(json, GameError.class);

      if(error == null) {
          throw new IllegalArgumentException("json must contain an error");
      }

      return error;
    }
}
