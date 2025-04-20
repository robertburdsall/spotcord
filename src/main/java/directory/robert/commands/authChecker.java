package directory.robert.commands;

import directory.robert.spotify.SpotifyAuth;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class authChecker {
    SlashCommandInteractionEvent event;
    public authChecker (SlashCommandInteractionEvent event) {
        this.event = event;
    }

    public boolean checkAuth() {
        return SpotifyAuth.REFRESH_TOKENS.containsKey(event.getUser().getId());

    }

    public boolean checkSessionAuth() {
        return SpotifyAuth.SPOTIFY_API_MAP.containsKey(event.getUser().getId());
    }

}
