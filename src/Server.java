
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;

    public Server() {

        connections = new ArrayList<>();
        done = false;

    }

    @Override
    public void run() {

        try {

            ServerSocket server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();

            while (!done)
            {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (Exception e) {
            shutdown();
        }

    }

    public void broadcast (String message) {
        for (ConnectionHandler ch : connections) {
            if (ch != null) {
                ch.sendMessage(message);
            }
        }
    }

    public void shutdown() {
        done = true;
        try {
            if (!server.isClosed()) {
                server.close();
            }
            for (ConnectionHandler ch : connections) {
                ch.shutdown();
            }
        } catch (IOException ignore) {
            //ignore exception field
        }
    }

    class ConnectionHandler implements Runnable{
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;


        public ConnectionHandler(Socket client) {
            this.client = client;
        }
        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Enter your nickname to begin:");
                nickname = in.readLine();
                System.out.println(nickname + " has joined!");
                broadcast(nickname + " has joined the chat!");
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/nick ")) {
                        String[] messageSplit = message.split(" ", 2);
                        if (messageSplit.length == 2) {
                            broadcast(nickname + " has changed their nickname to: " + messageSplit[1]);
                            System.out.println(nickname + " has changed their nickname to: " + messageSplit[1]);
                            nickname = messageSplit[1];
                            System.out.println("Successfully changed nickname to: [" + nickname + "]");

                        } else {
                            out.println("no nickname provided.");
                        }
                    } else if (message.startsWith("/quit ") || message.startsWith("/quit")) {
                        broadcast("[" + nickname + "] has left the chat!");
                        System.out.println(nickname + """ 
                               has left that chat.
                               -----------------------------
                               Every message, command, or
                               anything else onwards will not
                               be shared publicly with others
                               in the group chat.
                               -----------------------------
                               You can create a new client
                               to begin messaging again. You
                               may even use the same nickname
                               you had before.
                               -----------------------------
                               
                               
                               """);
                    } else {
                        broadcast("[" + nickname + "]: " + message);
                    }
                }
            } catch (IOException e) {
                shutdown();
            }
        }
        public void sendMessage (String message) {
            out.println(message);
        }

        public void shutdown() {
            try {
                in.close();
                out.close();

                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e)
            {
                //ignore
            }
        }

    }

    public static void main(String[] args)
    {
        Server server = new Server();
        server.run();
    }


}
