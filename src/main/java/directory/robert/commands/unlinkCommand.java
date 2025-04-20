package directory.robert.commands;

import directory.robert.spotify.AES;
import directory.robert.spotify.SpotifyAuth;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import se.michaelthelin.spotify.SpotifyApi;

import javax.crypto.SecretKey;
import java.awt.*;

public class unlinkCommand {
    private SecretKey key;

    public unlinkCommand(SecretKey key) {
        this.key = key;
    }

    public void unlink(SlashCommandInteractionEvent event) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(constants.primaryColor);
        embedBuilder.setTitle("Unlink your Spotify & Discord!");
        embedBuilder.setDescription("Are you sure you would like to unlink your Spotify & Discord bots? You will have to run **/link** again to use Spotcord!");

        String encrypted_id = "";

        try {
            encrypted_id = AES.encrypt(event.getUser().getId(), key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        SpotifyAuth.validating_unlinks.add(event.getUser().getId());

            event.replyEmbeds(embedBuilder.build()).setEphemeral(true)
                .setActionRow(
                        Button.success(encrypted_id + "&" + "yes" + "&" + "unlink", "Yup!"),
                        Button.danger(encrypted_id + "&" + "no" + "&" + "unlink", "Nah...")
                ).queue();
    }

}
