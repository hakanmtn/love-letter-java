# Love Letter – Java Learning Project

Dieses Projekt dient dazu, meine Java-Kenntnisse zu vertiefen.
Als Grundlage wird das Kartenspiel **Love Letter** für 2–4 Spieler verwendet.

## Lernziele

* Objektorientierte Programmierung mit Java
* Aufbau eines modularen Datenmodells
* Client-Server-Kommunikation über TCP
* Nebenläufigkeit und asynchrone Kommunikation
* Entwicklung einer Benutzeroberfläche mit JavaFX
* Trennung von Model, View und ViewModel (MVVM)
* Versionsverwaltung mit Git und GitHub
* Schreiben von automatisierten Tests

## Aktueller Milestone

* [x] Java und IntelliJ IDEA einrichten
* [x] Git-Repository einrichten
* [x] Grundlegendes Datenmodell entwerfen
* [x] UML-Klassendiagramm erstellen
* [ ] TCP-Server implementieren
* [ ] TCP-Client implementieren
* [ ] Anmeldung mit eindeutigem Nicknamen ermöglichen
* [ ] Chatnachrichten an alle Clients übertragen
* [ ] Verbindungsaufbau und Verbindungsende bekannt geben
* [ ] Verbindung mit dem Befehl `bye` beenden
* [ ] JavaFX-Oberfläche erstellen
* [ ] Asynchronen Nachrichtenempfang umsetzen

## Anforderungen an den Chat

* Mehrere Clients können sich mit dem Server verbinden.
* Jeder Client verwendet einen eindeutigen Nicknamen.
* Neue Benutzer erhalten eine Willkommensnachricht.
* Andere Benutzer werden über Beitritt und Verlassen informiert.
* Nachrichten werden an alle verbundenen Clients übertragen.
* Mit `bye` wird die Verbindung beendet.

## Geplante Projektstruktur

```text
src/
├── main/
│   ├── java/
│   │   └── loveletter/
│   │       ├── client/
│   │       ├── server/
│   │       ├── model/
│   │       ├── view/
│   │       └── viewmodel/
│   └── resources/
└── test/
    └── java/
```

## Verwendete Technologien

* Java 22
* JavaFX 22
* TCP-Sockets
* Git und GitHub
* IntelliJ IDEA
* Maven

## Aktuelles Datenmodell

```mermaid
classDiagram
    class CardType {
    <<enumeration>>
    GUARD
    PRIEST
    BARON
    HANDMAID
    PRINCE
    KING
    COUNTESS
    PRINCESS
    -int value
    -CardType(int value)
    +int getValue()
}
    class Deck {
    -List~CardType~ cards
    +Deck()
    -void addCopies(CardType cardType, int numberOfCopies)
    +int size()
    +CardType draw()
    +boolean isEmpty()
    +void shuffle()
}
    class Player {
    -String name
    -int affectionTokens
    -List~CardType~ hand
    -List~CardType~ discardPile
    -boolean eliminated
    -boolean protectedFromEffects
    +Player(String name)
    +String getName()
    +int getAffectionTokens()
    +void awardAffectionTokens()
    +List~CardType~ getHand()
    +List~CardType~ getDiscardPile()
    +void receiveCard(CardType card)
    +void discardCard(CardType card)
    +void eliminate()
    +boolean isEliminated()
    +void protectFromEffects()
    +void removeProtection()
    +boolean isProtectedFromEffects()
    +void resetForNewRound()
}
    class GameRound {
    -List~Player~ players
    -Deck deck
    -CardType reserveCard
    -List~CardType~ faceUpRemovedCards
    -int currentPlayerIndex
    -boolean turnInProgress
    +GameRound(List~Player~ players)
    +List~Player~ getPlayers()
    -void setupRound()
    +int getRemainingDeckSize()
    +boolean hasReserveCard()
    +List~CardType~ getFaceUpRemovedCards()
    +Player getCurrentPlayer()
    +void startCurrentTurn()
    +void endCurrentTurn()
    -void moveToNextActivePlayer()
    +boolean isRoundOver()
}

    GameRound "1" *-- "1" Deck : owns
    GameRound "1" o-- "2..4" Player : participants

    Deck "1" --> "0..16" CardType : contains

    Player "1" --> "0..2" CardType : hand
    Player "1" --> "0..*" CardType : discards

    GameRound "1" --> "0..1" CardType : reserve
    GameRound "1" --> "0..3" CardType : face-up removed
```

