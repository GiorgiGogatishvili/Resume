import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


public class ClientLogic extends Thread {

    private final Socket socket;
    private String username;
    private final ChatServer chatServer;

    protected ClientLogic(Socket client, ChatServer chatServer) {
        this.chatServer = chatServer;
        this.socket = client;
    }

    @Override  // runnable class for client thrads
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            String firstInput = reader.readLine().trim();  // read through first input and split it into, username startDate, printwriter and socket

            int divisorIndex = firstInput.indexOf("$");
            int welcomeIndex = firstInput.indexOf("$", divisorIndex + 1);

            username = firstInput.substring(welcomeIndex + 1);

            String welcomeMessage = firstInput.substring(divisorIndex + 1, welcomeIndex);

            String startDate = firstInput.substring(0, divisorIndex);

            chatServer.connect(username, startDate, writer, socket);

            writer.println(welcomeMessage);

            String message = reader.readLine();
            String DM = null;

            if (message != null) {
                message = message.trim();
            }

            while (message != null) {
                int spaceIndex = message.indexOf(" ");

                if (message.charAt(0) == '@' && spaceIndex != -1) // detect if user wants to DM with someone
                    DM = message.substring(1, spaceIndex);

                if (DM != null) {
                    chatServer.sendMessageToDM(message, username, DM);
                    DM = null;
                } else {
                    String factAboutPenguins = "The black and white 'tuxedo' look donned by most penguin species is a clever camouflage called countershading.";
                    switch (message) { // check for special commands and work towards it
                        case "WHOIS" -> chatServer.sendInfo(username);
                        case "PINGU" -> chatServer.sendMessageToAll(factAboutPenguins);
                        case "LOGOUT" -> {
                            writer.println("port=" + socket.getLocalPort());
                            writer.println("localport=" + socket.getPort());
                            socket.getInputStream().close();
                            socket.getOutputStream().close();
                            socket.close();
                        }
                        default -> chatServer.sendMessageExceptSender(message, username);
                    }
                }

                message = reader.readLine();

                if (message != null)
                    message = message.trim();
            }
        } catch (IOException ignored) {

        } finally {
            chatServer.disconnect(username);
        }
    }

}
