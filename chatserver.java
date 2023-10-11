import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map.Entry;

public class ChatServer {
    private final HashMap<String, PrintWriter> clients = new HashMap<>();
    private final HashMap<String, String> clientStartDates = new HashMap<>();
    private final int port;
    private ServerSocket server;


    // create 2 constructors with different parameters
    public ChatServer() {
        port = 3000;

        server = null;
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("Server failed to start!");
            e.printStackTrace();
        }
    }

    public ChatServer(int port) {
        this.port = port;

        server = null;
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("Server failed to start!");
            e.printStackTrace();
        }
    }

    public synchronized void connect(String username, String startDate, PrintWriter clientOut, Socket socket) throws IOException {
        if (clients.size() > 48) { // check for errors and don't let to enter
            clientOut.println("Server is already maxed out, try again later");
            socket.getOutputStream().close();
            socket.getInputStream().close();
            socket.close();
            return;
        }
        if (clients.containsKey(username)) {
            clientOut.println("Sorry, you connected, but user with such name already exists");
            socket.getOutputStream().close();
            socket.getInputStream().close();
            socket.close();
            return;
        }
        clients.put(username, clientOut); // put their startDates and streams into maps
        clientStartDates.put(username, startDate);
        sendMessageExceptSender("*** " + username + " has joined the chat room. ***", username); //send this message to everyone except one who connects
    }

    public synchronized void disconnect(String username) { // disconnect by remove this user from list
        clients.remove(username);
        sendMessageToAll("*** " + username + " has disconnected from the chat room. ***");
    }

    public synchronized void sendMessageToAll(String message) { // send message to everyone
        System.out.println(message);
        for (PrintWriter out : clients.values()) {
            out.println(message);
        }
    }

    public synchronized void sendMessageExceptSender(String message, String username) {
        String newMessage = LocalTime.now() + username + ": " + message;
        System.out.println(newMessage);
        for (Entry<String, PrintWriter> entry : clients.entrySet()) {
            if (entry.getKey().equals(username)) continue;
            entry.getValue().println(newMessage);
        }
    }

    public synchronized void sendMessageToDM(String message, String senderUsername, String receiverUsername) { // send DMs, but check to not text yourself
        if (senderUsername.equals(receiverUsername))
            return;

        String newMessage = LocalTime.now() + senderUsername + ": " + message;
        boolean userWasFound = false;

        for (Entry<String, PrintWriter> entry : clients.entrySet()) {
            if (entry.getKey().equals(receiverUsername)) {
                entry.getValue().println(newMessage);
                userWasFound = true;
                break;
            }
        }

        if (!userWasFound)
            failedDM(senderUsername);
    }

    public synchronized void failedDM(String senderUsername) {  // if DM failed, notify the user
        String newMessage = "No such user exists";

        for (Entry<String, PrintWriter> entry : clients.entrySet()) {
            if (entry.getKey().equals(senderUsername)) {
                entry.getValue().println(newMessage);
                break;
            }
        }
    }

    public synchronized void sendInfo(String receiverUsername) { // send info on penguins
        for (Entry<String, PrintWriter> entry : clients.entrySet()) {
            if (entry.getKey().equals(receiverUsername)) {
                for (Entry<String, String> entry1 : clientStartDates.entrySet()) {
                    String newMessage = entry1.getKey() + " since " + entry1.getValue();
                    entry.getValue().println(newMessage);
                }
                break;
            }
        }
    }

    public void start() {
        try {
            // for an active server start
            while (ServerIsActive()) {
                System.out.println("Server is waiting on port " + port);

                Socket clientSocket = server.accept();

                // check size of clients and don't let anyone in after threshold
                if (clients.size() > 48) {
                    clientSocket.getOutputStream().close();
                    clientSocket.getInputStream().close();
                    clientSocket.close();
                    return;
                }

                // create and run client thread
                Thread clientThread = new Thread(new ClientLogic(clientSocket, this));

                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // check if server is active
    public boolean ServerIsActive() {
        return server != null;
    }


    public static void main(String[] args){
        // create server instance
        ChatServer server;
        if (args.length == 0) {
            server = new ChatServer();
        } else if (args.length == 1) {
            server = new ChatServer(Integer.parseInt(args[0]));
        } else
            throw new IllegalStateException("Invalid number of parameters, input just port");

        server.start();

    }


}
