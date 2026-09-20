package loveletter.client;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;


/**
 * Provides line-based communication with the server.
 *
 * <p>Connect, read and send operations must run outside the
 * JavaFX application thread because they may block.
 * Use a new instance for each connection attempt.
 */
public class ServerConnection implements AutoCloseable{

    private final Socket socket = new Socket();
    private BufferedReader reader;
    private PrintWriter writer;

    /**
     * Creates a connection that is not yet connected.
     */
    public ServerConnection(){

    }

    /**
     * Connects to the server and initializes the streams.
     *
     * @param host the server hostname
     * @param port the server port
     * @throws IOException if connecting or initializing streams fails
     */
    public void connect(String host, int port) throws IOException {
        try{
            socket.connect(new InetSocketAddress(host,port), 5000);

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            writer = new PrintWriter(socket.getOutputStream(), true);
        }catch(IOException exception){
            try{
                socket.close();
            }catch(IOException closeException){
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }

    /**
     * Reads the next line from the server.
     *
     * @return the received line, or null if the server ends its output
     * @throws IOException if reading fails
     * @throws IllegalStateException if streams have not been initialized
     */
    public String readMessage() throws IOException{
        if(reader == null){
            throw new IllegalStateException("Not connected.");
        }
        return reader.readLine();
    }

    /**
     * Sends one line to the server.
     *
     * @param message the non-null message without line breaks
     * @throws IllegalArgumentException if the message is null
     *         or contains a line break
     * @throws IllegalStateException if streams have not been initialized
     * @throws IOException if writing fails
     */
    public void sendMessage(String message) throws IOException{
        if(message == null
                ||message.contains("\n")
                || message.contains("\r")) {
            throw new IllegalArgumentException("Message must be a single non-null line.");
        }

        if(writer == null){
            throw new IllegalStateException("Not connected.");
        }

        writer.println(message);

        if(writer.checkError()){
            throw new IOException("Message could not be sent.");
        }

    }

    /**
     * Closes the socket and its associated streams.
     * Also releases a thread waiting for incoming socket data.
     *
     * @throws IOException if closing the socket fails
     */
    @Override
    public void close() throws IOException{
        socket.close();
    }

}
