package loveletter.protocol;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Objects;

/**
 * Converts game state snapshots to JSON.
 */
public class GameStateCodec {


    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .create();

    /**
     * Creates a codec for compact JSON output.
     */
    public GameStateCodec(){
    }

    /**
     * Converts a game state snapshot to compact JSON.
     *
     * @param state the snapshot to encode
     * @return the JSON representation
     * @throws NullPointerException if state is null
     */
    public String encode(GameState state){
        Objects.requireNonNull(state, "state must not be null");
        return gson.toJson(state);
    }

    public GameState decode(String json){
        Objects.requireNonNull(json, "json must not be null");

        if(json.isBlank()){
            throw new IllegalArgumentException("json must not be blank");
        }

        GameState state = gson.fromJson(json, GameState.class);

        if(state == null){
            throw new IllegalArgumentException( "json must represent a game state");
        }

        return state;
    }




}
