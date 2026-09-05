package loveletter.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServer {
    private static final int DEFAULT_PORT = 5500;
    private final int port;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final Set<String> nicknames = ConcurrentHashMap.newKeySet();

    public ChatServer(int port){
        this.port = port;
    }

    public void start(){
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Chat server started on port " + port);

            while(true){
                Socket clientSocket = serverSocket.accept();

                ClientHandler clientHandler = new ClientHandler(clientSocket,this);

                Thread clientThread = new Thread(clientHandler);
                clientThread.start();
            }
        }catch (IOException e) {
            System.err.println("Server Error: " + e.getMessage());
        }
    }

    void addClient(ClientHandler clientHandler){
        clients.add(clientHandler);
    }

    void removeClient(ClientHandler clientHandler){
        clients.remove(clientHandler);
    }

    void broadcast(String message){
        for(ClientHandler client: clients){
            client.sendMessage(message);
        }
    }

    boolean registerNickname(String nickname){
        if(nickname == null || nickname.isBlank()){
            return false;
        }

        String normalizedname = nickname.toLowerCase(Locale.ROOT);

        return nicknames.add(normalizedname);
    }
    void unregisterNickname(String nickname){
        String normalizedNickname = nickname.toLowerCase(Locale.ROOT);
        nicknames.remove(normalizedNickname);
    }

    void broadcastToOthers(String message, ClientHandler excludedClient){
        for(ClientHandler client : clients){
            if(client != excludedClient){
                client.sendMessage(message);
            }
        }

    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer(DEFAULT_PORT);
        server.start();
    }
}
