package game.network;

import game.GameController;
import players.Player;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * A thread on the server that handles communication with a single client.
 */
public class ClientHandler extends Thread {
    private Socket socket;
    private GameController controller;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Player player;

    public ClientHandler(Socket socket, GameController controller) {
        this.socket = socket;
        this.controller = controller;
    }

    public void run() {
        try {
            // Server waits for client's header
            in = new ObjectInputStream(socket.getInputStream());

            // Server creates its output stream, sending its own header
            out = new ObjectOutputStream(socket.getOutputStream());

            // Force the header out of the buffer and over the network NOW.
            out.flush();

            // Now, we can safely wait for the client's first message
            NetworkMessage helloMsg = (NetworkMessage) in.readObject();
            String username = "Wanderer"; // Default
            if (helloMsg.getType() == NetworkMessage.MessageType.CLIENT_INFO) {
                username = (String) helloMsg.getPayload();
            }

            // Add this client to the controller
            controller.addClient(this, username);

            // Listen for all future messages
            while (true) {
                NetworkMessage msg = (NetworkMessage) in.readObject();
                controller.handleNetworkMessage(msg, this);
            }
        } catch (Exception e) {
            // Client disconnected
            System.out.println("Client disconnected: " + socket.getInetAddress());
            controller.removeClient(this);
        }
    }

    /**
     * Sends a message from the server to this specific client.
     * SYNCHRONIZED: Prevents "stream active" errors when Heartbeat and GIF Upload
     * try to write to the same client simultaneously.
     */
    public synchronized void sendMessage(NetworkMessage msg) {
        try {
            if (socket != null && socket.isConnected() && out != null) {
                out.writeObject(msg);
                out.flush();
                out.reset(); // Prevent memory leaks by clearing object cache
            }
        } catch (Exception e) {
            // e.printStackTrace(); // Suppress noise on disconnect
            System.out.println("Error sending to " + (player != null ? player.getName() : "Unknown") + ": " + e.getMessage());
        }
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public ObjectOutputStream getNetworkOut() {
        return out;
    }
}