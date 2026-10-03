package loveletter.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Objects;

/** Converts game actions to JSON. */
public class GameActionCodec {

  private final Gson gson = new GsonBuilder().serializeNulls().create();

  /** Creates a codec for game-action messages. */
  public GameActionCodec() {}

  /**
   * Encodes a game action as JSON.
   *
   * @param action the action to encode
   * @return the JSON representation
   * @throws NullPointerException if action is null
   */
  public String encode(GameAction action) {
    Objects.requireNonNull(action, "gson must not be null");
    return gson.toJson(action);
  }

  /**
   * Decodes a game action from JSON.
   *
   * @param json the JSON representation
   * @return the decoded action
   * @throws NullPointerException if json is null
   * @throws IllegalArgumentException if json is blank or represents null
   * @throws com.google.gson.JsonParseException if JSON cannot be decoded
   */
  public GameAction decode(String json) {
    Objects.requireNonNull(json, "json must not be null");

    if (json.isBlank()) {
      throw new IllegalArgumentException("json must not be blank");
    }

    GameAction action = gson.fromJson(json, GameAction.class);

    if (action == null) {
      throw new IllegalArgumentException("json must contain an action");
    }
    return action;
  }
}
