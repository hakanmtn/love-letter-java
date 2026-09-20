package loveletter.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Provides a console-based TCP client for the chat and
 * Love Letter commands.
 *
 * <p>Connects to localhost on port 5500.
 * The main thread displays server messages while a daemon
 * thread forwards console input to the server.
 */
public class ChatClient {

    private static final String HOST = "localhost";
    private static final int PORT = 5500;


    /**
     * Prevents instantiation of this console application entry point.
     */
    private ChatClient() {
    }

    /**
     * Connects to the server and displays incoming messages
     * until the server's output ends or an I/O error occurs.
     *
     * <p>Starts a daemon thread for console input.
     * The socket and its input and output streams are closed
     * when the try-with-resources block is exited.
     *
     * <p>Connection and receive errors are logged.
     *
     * @param args command-line arguments; currently ignored
     */
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

    /**
     * Reads console lines and forwards them to the server.
     *
     * <p>Stops reading when console input ends, the writer reports
     * an error, or a trimmed, case-insensitive "bye" is entered.
     * The "bye" message is sent before the loop ends.
     *
     * <p>After the loop, shuts down the socket's output if the
     * socket is still open. The input side remains available
     * for receiving server messages.
     *
     * <p>If reading console input or shutting down output throws
     * an IOException, logs the error and attempts to close the socket.
     * The console reader and System.in are not closed by this method.
     *
     * @param socket the connection whose output is shut down
     * @param serverWriter the writer used to send lines to the server
     */
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
