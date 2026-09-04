package loveletter.model;

import java.util.List;

public class GameRound {

    private final List<Player> players;

    public GameRound(List<Player> players) {
        if (players == null) {
            throw new IllegalArgumentException("players cannot be null");
        }
        if (players.size()< 2 || players.size()>4) {
            throw new IllegalArgumentException("A game round requires betwenn two and four players");
        }
        this.players = List.copyOf(players);
    }

    public List<Player> getPlayers() {
        return players;
    }
}
