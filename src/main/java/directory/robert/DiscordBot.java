package directory.robert;

import directory.robert.commands.constants;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import directory.robert.spotify.*;
import io.github.cdimascio.dotenv.Dotenv;

import javax.crypto.SecretKey;


public class DiscordBot extends ListenerAdapter {
    public static JDA jda;

    public static void main(String[] args) {

        /*
        HashMap<String, String> envVariables = new HashMap<>();

        try {
            Files.lines(Paths.get("/home/container/resources/.env"))
                    .forEach(line -> {
                        String[] split = line.split("=", 2);
                        if (split.length == 2) {
                            envVariables.put(split[0].trim(), split[1].trim());
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
        String BOT_TOKEN = envVariables.get("BOT_TOKEN");

         */
        Dotenv dotenv = Dotenv.configure()
                .filename(".env")
                .directory(constants.resources_path)
                .load();

        String BOT_TOKEN = dotenv.get("BOT_TOKEN");

        SecretKey key = AES.generateKey();

        // create the databases if they weren't already
        DB.main();

        JDABuilder builder = JDABuilder.createDefault(BOT_TOKEN)
                .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGE_REACTIONS)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .addEventListeners(new BotCommands(key))
                .setActivity(Activity.listening("urmom"));
        jda = builder.build();



        // runs auth server on its own thread
        SpotifyAuthServer server = new SpotifyAuthServer(key);
        server.start();

        // runs token refresh on its own thread
        TokenRefresh tokenRefresh = new TokenRefresh();
        tokenRefresh.start();


        // register commands here
        // comment out if no updates are being made
        jda.updateCommands().addCommands(
                Commands.slash("link", "Link your discord & Spotify accounts!"),
                Commands.slash("current", "Get the current song you are listening too!"),
                Commands.slash("player", "Get the player to control your songs!"),
                Commands.slash("unlink", "Unlink your discord and spotify accounts!")
                /*
                Commands.slash("dubs", "is a w command")
                        .addOptions(
                        new OptionData(OptionType.STRING, "type", "type of W you want")
                                .addChoice("Skibbity Fortnite", "skibbity fortnite")
                                .addChoice("Kai Cenat", "Kai Cenat")
                                .addChoice("Edging Streak", "edging streak")
                        )

                 */

        ).queue();
    }
/*
        private static File createTempEnvFile(String resourcePath) {
        try (InputStream inputStream = DiscordBot.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                System.out.println("Resource not found: " + resourcePath);
                return null;
            }

            // Create a temporary file to store the .env content
            File tempFile = File.createTempFile("temp", ".env");
            tempFile.deleteOnExit();

            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            return tempFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }


    }
    */
}