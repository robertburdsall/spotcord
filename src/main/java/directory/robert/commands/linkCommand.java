package directory.robert.commands;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import directory.robert.spotify.*;

import javax.crypto.SecretKey;


public class linkCommand {
    private SecretKey key;
    private SlashCommandInteractionEvent event;

    public linkCommand(SecretKey key) {
        this.key = key;
    }

    boolean AUTH_STATUS = false;

    public void link(SlashCommandInteractionEvent event) {
        SpotifyAuth auth = new SpotifyAuth();
        this.event = event;
        AUTH_STATUS = SpotifyAuth.REFRESH_TOKENS.containsKey(event.getUser().getId());
        if (!(AUTH_STATUS)) {
            String authURL = auth.getAuthURL(event.getUser().getId(), key);
            event.reply("Link your Discord & Spotify accounts by pressing on the following link! \n[Authentication Link]" + "(" + authURL + ")").setEphemeral(true).queue();
        } else {
            event.reply("Your account has already been linked!").queue();
        }
    }

}
