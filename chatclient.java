import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatClient {
    private final int port;
    private final String hostAddress;
    private Socket socket = null;
//    username character patterns
    private static final String USERNAME_PATTERN =
            "^[a-zA-Z0-9]([._-](?![._-])|[a-zA-Z0-9]){3,18}[a-zA-Z0-9]$";

    private static final Pattern pattern = Pattern.compile(USERNAME_PATTERN);

    private BufferedReader input;

    // create 2 constructors with different parameters (one with default pot and address, other with user inputs)
    public ChatClient() {
        port = 3000;
        hostAddress = "localhost";

        while (socket == null) {
            try {
                socket = new Socket(hostAddress, port);
            } catch (IOException e) {
                System.out.println("Invalid Server or host address, try again: ");
            }
        }

    }


    public ChatClient(int port, String hostAddress) {
        this.port = port;
        this.hostAddress = hostAddress;

        while (socket == null) {
            try {
                socket = new Socket(hostAddress, port);
            } catch (IOException e) {
                System.out.println("Invalid Server or host address, try again: ");
            }
        }

    }

    public void start() {

        try {
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            Scanner in = new Scanner(System.in);

            // ask for username until you get one correct

            System.out.println("Enter your nickname: ");
            String username;
            do {
                username = in.nextLine();
            } while (!validateUsername(username));


            String currentTime = LocalTime.now().toString();

            // get welcome message and encrypt username, so that you can split it afterwards in ClientLogic
            String welcomeMessage = currentTime + ": Connection accepted " + hostAddress + "/127.0.0.1:" + port;

            username = currentTime + "$" + welcomeMessage + "$" + username;
            // send username to validate
            out.println(username);

            // create thread
            Thread reader = new Thread() {
                @Override
                public void run() {
                    int checker = 0;  // checker for program closure

                    try {
                        input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                        String message = input.readLine(); // take input
                        while (message != null) {
                            if (message.equals("port=" + socket.getPort()) || message.equals("localport=" + socket.getLocalPort())) {  // check for closure
                                checker++;

                                if (checker == 2) {
                                    interrupt();
                                }
                            } else {
                                System.out.println(message);
                            }


                            message = input.readLine(); //read until you can
                        }
                    } catch (SocketException I) {
                        interrupt();
                    } catch (IOException e) {
                        System.out.println("Connection shattered by exception, or maxed out server");
                        interrupt();
                    }
                }
            };

            reader.start();

            while (true) {
                if (reader.isInterrupted()) return; // if thread was interrupted finish the program
                String message = in.nextLine();
                out.println(message);
            }

        } catch (IOException e) {
            System.out.println("I/O Exception occurred!");
        }

    }


// validate username characters using regEx
    public static boolean validateUsername(final String username) {
        Matcher matcher = pattern.matcher(username);
        return matcher.matches();
    }

    public static void main(String[] args) {
//        create client instance, and start the program
        ChatClient client;
        if (args.length == 0)
            client = new ChatClient();
        else if (args.length == 2)
            client = new ChatClient(Integer.parseInt(args[0]), args[1]);
        else
            throw new IllegalStateException("Invalid number of parameters, input just host address and port");
        client.start();
    }
}
