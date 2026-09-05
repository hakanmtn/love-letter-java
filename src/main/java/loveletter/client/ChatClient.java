package loveletter.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient {

    private static final String HOST = "localhost";
    private static final int PORT = 5500;

    public static void main(String[] args) {
    try (Socket socket = new Socket(HOST, PORT);
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter serverWriter = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Connected to server at " + HOST + ":" + PORT);
            Thread receiverThread = new Thread(() -> {
                try{
                    String serverMessage;
                    while ((serverMessage = serverReader.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                }catch (IOException exception){
                    System.err.println("Connection to server lost: " + exception.getMessage());
                }
            });

            receiverThread.setDaemon(true);
            receiverThread.start();

            String message;

            while((message = consoleReader.readLine()) != null){
                serverWriter.println(message);
                if("bye".equalsIgnoreCase(message.trim())){
                    break;
                }
            }



        }catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}
