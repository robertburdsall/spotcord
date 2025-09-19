package directory.robert.spotify;

import com.fasterxml.jackson.databind.ObjectMapper;
import directory.robert.commands.constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.User;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import directory.robert.DiscordBot;

import javax.crypto.BadPaddingException;
import javax.crypto.SecretKey;

public class SpotifyAuth {

    private static final String CLIENT_ID = "a1ecfe01e99345eebef3e30989a95471";
    private static final String CLIENT_SECRET = "5ea8b82ca5ea478b93586454df7f7fb4";
    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    public static Map<String, String> REFRESH_TOKENS = new HashMap<>();
    // <Discord ID : Authentication code>
    public static HashMap<String, SpotifyApi> SPOTIFY_API_MAP = new HashMap<>();
    public static ArrayList<String> validating_links = new ArrayList<>();
    public static ArrayList<String> validating_unlinks = new ArrayList<>();
    // <Discord ID : SpotifyApi object with refresh & access token
    // this hashmap is not dumped or saved as the object cannot be sterilized - local to bot online

    public SpotifyAuth () {
        REFRESH_TOKENS = DB.getHashMap("REFRESH_TOKENS");
    }

    public static void storeRefreshToken(String code, String CURRENT_USER_ID){
        REFRESH_TOKENS.put(CURRENT_USER_ID, code);
        DB.insertOrUpdateHashMap("REFRESH_TOKENS", REFRESH_TOKENS);
    }

    public static void removeRefreshToken(String CURRENT_USER_ID){
        REFRESH_TOKENS.remove(CURRENT_USER_ID);
        DB.insertOrUpdateHashMap("REFRESH_TOKENS", REFRESH_TOKENS);
    }



    public String getAuthURL(String id, SecretKey key) {
        String REDIRECT_URI = constants.serverAddress;
        String identification_token = null;
        try {
            identification_token = AES.encrypt(id, key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String authUrl = AUTH_URL + "?client_id=" + CLIENT_ID +
                 "&response_type=code" +
                 "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                 "&scope=" + URLEncoder.encode("user-read-playback-state user-modify-playback-state user-read-currently-playing streaming user-read-email", StandardCharsets.UTF_8) +
                "&state=" + URLEncoder.encode(identification_token, StandardCharsets.UTF_8);


        return authUrl;
    }
    public void success(String code, String id, SecretKey key) {
        // gets token & refresh code and stores them in SPOTIFY_API_MAP - not stored / serialized!
        initSession(id, code);
        String encryptedid = "";
        try {
            encryptedid = AES.encrypt(id, key);
        } catch (BadPaddingException e) {
            EmbedBuilder embed = new EmbedBuilder();
            embed.addField("Error!", "decryption failure", false);
            embed.appendDescription("This usually happens when the bot goes offline after a player is posted, but also protects against shady protection violations - if you believe this is an error, contact your system administrator.");
            embed.setColor(constants.errorColor);
            DiscordBot.jda.getUserById(id).openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(embed.build()).queue());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        String email = "";
        String displayName = "";
        try {
            User profile = SPOTIFY_API_MAP.get(id).getCurrentUsersProfile().build().execute();
            email = profile.getEmail();
            displayName = profile.getDisplayName();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SpotifyWebApiException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(constants.primaryColor);

        embed.addField("Confirming linkage", "hi, just checking in to confirm that you are attempting to link the following Spotify account to this Discord account! \n" + "Email: "+ email + "\nDisplay Name: " + displayName, false);
        String finalEncryptedid = encryptedid;
        DiscordBot.jda.getUserById(id).openPrivateChannel().queue(privateChannel -> {
                 privateChannel.sendMessageEmbeds(embed.build())
                         .setActionRow(
                                 Button.success(finalEncryptedid + "&" + "success" + "&" + "link", "That's me!"),
                                 Button.danger(finalEncryptedid + "&" + "failure" + "&" + "link", "Nope...")
                         ).queue();
             });
        validating_links.add(id);
    }

    public void failure(String id) {
                    DiscordBot.jda.getUserById(id).openPrivateChannel().queue(channel -> channel.sendMessage("AUTHENTICATION FAILURE!").queue());
    }

    public void initSession(String id, String code) {
        SpotifyApi apiobject = getAccessRefresh(code);
        SPOTIFY_API_MAP.put(id, apiobject);
        storeRefreshToken(apiobject.getRefreshToken(), id);

    }
    public SpotifyApi getAccessRefresh(String authcode) {

         final SpotifyApi spotifyApi = new SpotifyApi.Builder()
                .setClientId("a1ecfe01e99345eebef3e30989a95471")
                .setClientSecret("5ea8b82ca5ea478b93586454df7f7fb4")
                .setRedirectUri(SpotifyHttpManager.makeUri("http://10.137.67.175:23349/callback"))
                .build();

         final AuthorizationCodeRequest authorizationCodeRequest = spotifyApi.authorizationCode(authcode)
                 .build();

         try {
              final AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRequest.execute();

              // Set access and refresh token for further "spotifyApi" object usage
              spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
              spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());

    } catch (Exception e) {
          System.out.println("Error: " + e.getMessage());
    }
         return spotifyApi;
    }

    public SpotifyApi refreshAccessToken(String refreshToken, String userID) {
        final SpotifyApi spotifyApi = new SpotifyApi.Builder()
                .setClientId("a1ecfe01e99345eebef3e30989a95471")
                .setClientSecret("5ea8b82ca5ea478b93586454df7f7fb4")
                .setRefreshToken(refreshToken)
                .build();

        AuthorizationCodeRefreshRequest authorizationCodeRefreshRequest = spotifyApi.authorizationCodeRefresh()
                .build();

            try {
              final AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRefreshRequest.execute();

              spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());

            } catch (Exception e) {
                  System.out.println("Error: " + e.getMessage());
            }
            // adds to the session hashmap
        SPOTIFY_API_MAP.put(userID, spotifyApi);
        return spotifyApi;
    }


    public static String toString(InputStream input) throws IOException {
        return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }



}
