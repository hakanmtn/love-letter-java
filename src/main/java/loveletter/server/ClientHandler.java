package loveletter.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Handles nickname registration and line-based communication
 * for a single chat client.
 *
 * <p>Delegates commands and message broadcasts to {@link ChatServer}.
 * Implements {@link Runnable} so the connection can be handled
 * by a dedicated thread.
 */
public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final ChatServer server;
    private PrintWriter writer;
    private String nickname;



    /**
     * Creates a handler for a client connection.
     *
     * <p>Communication begins when {@link #run()} is executed.
     *
     * @param clientSocket the connected client socket used by run
     * @param server the server managing clients, nicknames and commands
     */
    public ClientHandler(Socket clientSocket, ChatServer server) {

        this.clientSocket = clientSocket;
        this.server = server;
    }

    /**
     * Processes the client connection until communication ends.
     *
     * <p>Requests a nickname, registers the client and announces
     * their arrival. Blank messages are ignored, commands are
     * delegated to the server, and ordinary chat messages are
     * broadcast with the sender's nickname.
     *
     * <p>A trimmed, case-insensitive "bye" message or the end of
     * the input stream ends the receive loop.
     * I/O errors are logged.
     *
     * <p>The socket and streams are closed by try-with-resources.
     * Client registration is cleaned up in the finally block.
     */
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
                if(message.isBlank()){
                    continue;
                }

                if("bye".equalsIgnoreCase(message.trim())){
                    break;
                }

                if(server.handleCommand(this,message)){
                    continue;
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

    /**
     * Requests a nickname until the server accepts it or input ends.
     *
     * <p>Trims surrounding whitespace and rejects empty names.
     * Nickname availability is checked by the server.
     *
     * @param reader the reader providing the client's input
     * @return the accepted and registered nickname,
     *         or null if input ends before registration succeeds
     * @throws IOException if reading from the client fails
     */
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

    /**
     * Removes this handler from the server.
     *
     * <p>If a nickname was accepted, releases it, broadcasts
     * the departure message and logs the disconnection.
     * Any game-related removal behavior is handled by the server.
     *
     * <p>This method does not close the socket or streams.
     */
    private void disconnectClient(){
        server.removeClient(this);

        if(nickname!= null){
            server.unregisterNickname(nickname);
            server.broadcast(nickname + " left the chat.");

            System.out.println("Client disconnected: " + nickname);
        }
    }

    /**
     * Writes a message followed by a line terminator to the client
     * and flushes the writer.
     *
     * <p>Does nothing if the writer has not been initialized.
     * Calls to this method are synchronized on this handler.
     *
     * <p>This method does not confirm delivery to the client
     * or check the PrintWriter error state.
     *
     * @param message the message to write
     */
    public synchronized void sendMessage(String message) {
        if (writer != null){
            writer.println(message);
        }
    }

    /**
     * Returns the nickname accepted for this connection.
     *
     * @return the accepted nickname, or null if no nickname
     *         has been accepted yet
     */
    public String getNickname(){
        return nickname;
    }


}
