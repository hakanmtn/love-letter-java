package loveletter.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the participants and lifecycle of a Love Letter game.
 */
public class Game {

    private final List<Player> players = new ArrayList<>();
    private boolean started;
    private GameRound currentRound;

    /**
     * Returns an unmodifiable snapshot of the participants.
     *
     * @return the players currently registered for this game
     */
    public List<Player> getPlayers(){
        return List.copyOf(players);
    }

    /**
     * Indicates whether the game has started.
     *
     * @return true if the game has started
     */
    public boolean isStarted(){
        return started;
    }


    /**
     * Adds a player to the game before it starts.
     *
     * @param player the player who wants to join
     * @throws IllegalArgumentException if the player is null or already registered
     * @throws IllegalStateException if the game has started or is full
     */
    public void join(Player player){
        if(player == null){
            throw new IllegalArgumentException("Player must not be null");
        }

        if(started){
            throw new IllegalStateException("Game has already started");
        }

        if(players.contains(player)){
            throw new IllegalArgumentException("Player has already joined");
        }

        if(players.size() >= 4){
            throw new IllegalStateException("Game is  full");
        }

        players.add(player);
    }

    /**
     * Starts the game and creates its first round.
     *
     * @throws IllegalStateException if the game has already started
     *                               or fewer than two players have joined
     */
    public void start(){
        if(started){
            throw new IllegalStateException("Game has already started");
        }

        if(players.size() < 2 ){
            throw new IllegalStateException("At least two players are required");
        }

        currentRound = new GameRound(players);
        started = true;
    }

    /**
     * Returns the current round.
     *
     * @return the current round
     * @throws IllegalStateException if the game has not started
     */
    public GameRound getCurrentRound() {
        if(!isStarted()){
            throw new IllegalStateException("Game has not started");

        }

        return currentRound;
    }

    /**
     * Awards affection tokens to the winners of the current round.
     *
     * @return the winners of the completed round
     * @throws IllegalStateException if the game has not started,
     *                               the round is still running,
     *                               or tokens have already been awarded
     */
    public List<Player> finishCurrentRound(){
        GameRound round = getCurrentRound();

        return round.awardWinnerTokens();
    }

    public int getRequiredTokensToWin() {
        if(!started){
            throw new IllegalStateException("Game has not started");
        }

        return switch(players.size()){
            case 2 -> 7;
            case 3 -> 5;
            case 4 -> 4;
            default -> throw new IllegalStateException("Unsupported number of players");

        };
    }

    public boolean isGameOver(){
        if(!started){
            return false;
        }

        int requiredTokens = getRequiredTokensToWin();

        for(Player player : players){
            if(player.getAffectionTokens() >= requiredTokens){

                return true;
            }

        }
        return false;

    }

    /**
     * Returns the players who have reached the winning token count.
     *
     * @return the winners of the game
     * @throws IllegalStateException if the game is not over
     */
    public List<Player> getWinners(){
        if(!isGameOver()){
            throw new IllegalStateException("Game is not over");
        }

        int requiredTokens = getRequiredTokensToWin();

        return players.stream()
                .filter(player -> player.getAffectionTokens() >= requiredTokens)
                .toList();
    }

    /**
     * Starts a new round after the current round has been scored.
     *
     * @throws IllegalStateException if the game has not started,
     *                               the current round has not been scored,
     *                               or the game is already over
     */
    public void startNextRound(){
        GameRound previousRound = getCurrentRound();

        if(!previousRound.isWinnerTokensAwarded()){
            throw new IllegalStateException("The current round must be scored first");

        }

        if(isGameOver()){
            throw new IllegalStateException("Game is already over");
        }

        List<Player> winners = previousRound.determineWinners();

        //Temporary policy: the first tied winner starts
        Player startingPlayer = winners.getFirst();
        int startingPlayerIndex = players.indexOf(startingPlayer);

        currentRound = new GameRound(players,startingPlayerIndex);
    }

}
