package directory.robert.spotify;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.InetSocketAddress;
import directory.robert.commands.constants;

public class SpotifyAuthServer extends Thread{
    private SecretKey key;

    public SpotifyAuthServer(SecretKey key) {
        this.key = key;
    }

    public void run() {
        try {
            startServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(constants.serverPort), 0);
        server.createContext("/callback", new SpotifyAuthServer.CallbackHandler());
        server.setExecutor(null);
        server.start();
        System.err.println("AUTH SERVER STARTED ON PORT " + constants.serverPort);
    }
     class CallbackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String[] params = query.split("&", 2);
            String code = null;
            String id_encrypted = null;
            // get the code for linking from the url
            if (params[0].startsWith("code=")) {
                code = params[0].split("=")[1];
            }
            // get the encrypted user id from the url
            if (params[1].startsWith("state=")) {
                    id_encrypted = params[1].split("=")[1];
                }

            String id = null;
            try {
                id = AES.decrypt(id_encrypted, key);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            SpotifyAuth spotifyAuth = new SpotifyAuth();
            if (code != null) {
                String response = "Authorization successful! You can close this window.";
                exchange.sendResponseHeaders(200, response.length());
                spotifyAuth.success(code, id, key);
            } else {
                String response = "Authorization failed!";
                exchange.sendResponseHeaders(400, response.length());
                spotifyAuth.failure(id);
            }
        }
    }
}
