package loveletter.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final ChatServer server;
    private PrintWriter writer;
    private String nickname;



    public ClientHandler(Socket clientSocket, ChatServer server) {

        this.clientSocket = clientSocket;
        this.server = server;
    }

    @Override
    public void run() {
        System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress());

    try (Socket socket = clientSocket;
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter writer = new PrintWriter(socket.getOutputStream(),true))
        {
            this.writer = writer;
            nickname = requestNickname(reader);

            if(nickname == null){
                return;
            }
            server.addClient(this);

            sendMessage("Welcome, " + nickname + "!");
            server.broadcastToOthers(nickname + " joined the chat.", this);

            String message;
            while((message = reader.readLine()) != null){
                if("bye".equalsIgnoreCase(message.trim())){
                    break;
                }

                String formattedMessage = nickname + ": " + message;

                System.out.println(formattedMessage);
                server.broadcast(formattedMessage);
            }
        }catch(IOException e){
            System.err.println("Client connection error: " + e.getMessage());
        }finally{
            disconnectClient();
        }
    }

    private String requestNickname(BufferedReader reader) throws IOException{
        sendMessage("Enter your nickname: ");
        String requestedNickname;

        while((requestedNickname = reader.readLine()) != null){
            String trimmedNickname = requestedNickname.trim();

            if(trimmedNickname.isEmpty()) {
                sendMessage("Nickname connot be empty. " + "Enter another nickname: ");
            }else if(server.registerNickname(trimmedNickname)){
                return trimmedNickname;
            }else{
                sendMessage("Nickname already in use. " + "Enter another nickname:");
            }
        }
        return null;
    }

    private void disconnectClient(){
        server.removeClient(this);

        if(nickname!= null){
            server.unregisterNickname(nickname);
            server.broadcast(nickname + " left the chat.");

            System.out.println("Client disconnected: " + nickname);
        }
    }

  public synchronized void sendMessage(String message) {
        if (writer != null){
            writer.println(message);
        }
    }


}
