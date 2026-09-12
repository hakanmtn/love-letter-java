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
        PrintWriter serverWriter = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Connected to server at " + HOST + ":" + PORT);

            Thread inputThread = new Thread(
                    () -> sendConsoleInput(socket,serverWriter), "console-input"
            );

            inputThread.setDaemon(true);
            inputThread.start();

            String serverMessage;

            while ((serverMessage = serverReader.readLine()) != null) {
                System.out.println(serverMessage);
            }

            System.out.println("Server closed the connection.");


        }catch (IOException e) {
            System.err.println("Connection ended or could not be established: " + e.getMessage());
        }
    }

    private static void sendConsoleInput(Socket socket, PrintWriter serverWriter){
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

        try{
            String message;

            while((message = consoleReader.readLine()) != null){
                serverWriter.println(message);

                if(serverWriter.checkError()) {
                    System.err.println("Message could not be sent");
                    break;
                }

                if("bye".equalsIgnoreCase(message.trim())){
                    break;
                }
            }

            if(!socket.isClosed()){
                socket.shutdownOutput();
            }
        }catch(IOException e){
                System.err.println("Console or connection error: " + e.getMessage());

                try{
                    socket.close();
                }catch (IOException closeException){
                    System.err.println("Could not close conenction. " + closeException.getMessage());
                }

        }
    }
}
