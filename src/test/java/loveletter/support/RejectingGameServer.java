package loveletter.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import loveletter.model.CardType;
import loveletter.protocol.*;

/** Provides a local server for manually checking rejected GUI actions. */
public final class RejectingGameServer {

  private RejectingGameServer() {}

  public static void main(final String[] args) throws IOException {

    try (ServerSocket server = new ServerSocket(5500, 1, InetAddress.getLoopbackAddress())) {

      System.out.println("Test server listening on port 5500.");

      try (Socket client = server.accept()) {
        BufferedReader reader =
            new BufferedReader(
                new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
        PrintWriter writer =
            new PrintWriter(client.getOutputStream(), true, StandardCharsets.UTF_8);

        System.out.println("Client connected.");

        writer.println("Enter your nickname:");

        String nickname = reader.readLine();

        if (nickname == null) {
          return;
        }

        writer.println("Welcome, " + nickname + "!");

        PlayerState player = new PlayerState(nickname, 0, false, false, 2, List.of());

        GameState state =
            new GameState(
                GamePhase.ROUND_IN_PROGRESS,
                nickname,
                List.of(player),
                nickname,
                List.of(CardType.HANDMAID, CardType.PRINCESS),
                5,
                List.of(),
                List.of(),
                List.of());

        GameStateCodec codec = new GameStateCodec();
        GameActionCodec actionCodec = new GameActionCodec();
        GameErrorCodec errorCodec = new GameErrorCodec();
        int rejectedPlays = 0;

        String command;
        while ((command = reader.readLine()) != null) {
          System.out.println("Received: " + command);

          if (command.equals("/subscribe-state")) {
            writer.println("GAME_STATE " + codec.encode(state));

          } else if (command.startsWith("GAME_ACTION ")) {
            String json = command.substring("GAME_ACTION ".length());
            GameAction action = actionCodec.decode(json);

            if(action.type() == GameActionType.PLAY){
              rejectedPlays++;

              GameError error = new GameError(
                      GameErrorCode.ACTION_REJECTED,
                      "Deliberate rejection by test. Attempt: " + rejectedPlays
              );
              writer.println("GAME_ERROR " + errorCodec.encode(error));


            }

          }
        }
      }
    }
  }
}
