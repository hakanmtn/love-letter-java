package loveletter.model;
import java.util.ArrayList;
import java.util.List;

public class Player {

    private final String name;
    private int affectionTokens;
    private final List<CardType> hand;
    private final List<CardType> discardPile;
    private boolean eliminated;
    private boolean protectedFromEffects;

    public Player(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Player name must not be blank");
        }
        this.name = name;
        this.affectionTokens = 0;
        this.hand = new ArrayList<>();
        this.discardPile = new ArrayList<>();
        this.eliminated = false;
        this.protectedFromEffects = false;
    }

    public String getName() {
        return name;
    }

    public int getAffectionTokens() {
        return affectionTokens;
    }

    public void awardAffectionTokens() {
        affectionTokens++;
    }

    public void receiveCard(CardType card){
        if(card == null){
            throw new IllegalArgumentException("Card must not be null");
        }

        if(hand.size() >= 2){
            throw new IllegalStateException("Player cannot hold more than two cards");
        }
        hand.add(card);

    }

    public List<CardType> getHand() {
        return List.copyOf(hand);
    }

    public void discardCard(CardType card){
        if(card == null){
            throw new IllegalArgumentException("Card must not be null");
        }
        if(!hand.remove(card)){
            throw new IllegalArgumentException("Player does not hold this card");
        }
        discardPile.add(card);
    }

    public List<CardType> getDiscardPile() {
        return List.copyOf(discardPile);
    }

    public boolean isEliminated() {
        return eliminated;
    }

    public void eliminate(){
        discardPile.addAll(hand);
        hand.clear();
        protectedFromEffects = false;
        eliminated = true;
    }

    public boolean isProtectedFromEffects() {
        return protectedFromEffects;
    }

    public void protectFromEffects() {
        protectedFromEffects = true;
    }

    public void removeProtection(){
        protectedFromEffects = false;
    }

    public void resetForNewRound(){
        hand.clear();
        discardPile.clear();
        eliminated = false;
        protectedFromEffects = false;
    }
}
