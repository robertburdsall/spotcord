package directory.robert.commands;

import com.google.gson.JsonParser;
import directory.robert.spotify.SpotifyAuth;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.data.player.StartResumeUsersPlaybackRequest;
import se.michaelthelin.spotify.requests.data.search.simplified.SearchTracksRequest;

import javax.crypto.SecretKey;

public class playCommand {
        private SecretKey key;
        private SpotifyApi spotifyApi;
    private SlashCommandInteractionEvent event;

    public playCommand(SpotifyApi spotifyApi, SecretKey key) {
        this.key = key;
        this.spotifyApi = spotifyApi;
    }

    public void play(SlashCommandInteractionEvent event) {
        String songName = event.getOption("title").toString();

        try {
             SearchTracksRequest searchRequest = spotifyApi.searchTracks(songName)
                     .limit(1)
                     .build();

            Paging<Track> trackPaging = searchRequest.execute();
            if (trackPaging.getItems().length > 0) {

                StartResumeUsersPlaybackRequest playbackRequest = spotifyApi
                        .startResumeUsersPlayback()
                        .uris(JsonParser.parseString("[\"" + trackPaging.getItems()[0].getUri() + "\"]").getAsJsonArray())
                        .build();
                playbackRequest.execute();
            } else {
                event.reply("no such song was found!").setEphemeral(true).queue();
            }
        } catch (Exception e) {
            event.reply("Something went wrong!").setEphemeral(true).queue();
        }
    }


}
