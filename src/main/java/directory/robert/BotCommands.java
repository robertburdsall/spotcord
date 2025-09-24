package directory.robert;

import directory.robert.spotify.AES;
import directory.robert.spotify.SpotifyAuth;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import directory.robert.commands.*;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.enums.CurrentlyPlayingType;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import se.michaelthelin.spotify.model_objects.specification.Track;

import javax.crypto.BadPaddingException;
import javax.crypto.SecretKey;
import java.io.IOException;

public class BotCommands extends ListenerAdapter {
    SecretKey key;

    public BotCommands (SecretKey key) {
        this.key = key;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String name = event.getName();
        SpotifyApi spotifyApi = null;

        if (!(name.equals("link"))) {
            authChecker authChecker = new authChecker(event);
            boolean auth = authChecker.checkAuth();
            boolean sessionAuth = authChecker.checkSessionAuth();
            if (!(auth)) {
                event.reply("Your account is not currently linked! Run **/link** to link your Spotify & Discord accounts!").setEphemeral(true).queue();
                return;
            } else if (!sessionAuth) {
                SpotifyAuth spotifyAuth = new SpotifyAuth();
                spotifyAuth.refreshAccessToken(SpotifyAuth.REFRESH_TOKENS.get(event.getUser().getId()), event.getUser().getId());
                spotifyApi = SpotifyAuth.SPOTIFY_API_MAP.get(event.getUser().getId());
            } else {
                spotifyApi = SpotifyAuth.SPOTIFY_API_MAP.get(event.getUser().getId());
                }
                    switch (name) {
                        case "current":
                            currentCommand current = new currentCommand(spotifyApi);
                            current.current(event);
                            break;
                        case "player":
                            playerCommand player = new playerCommand(spotifyApi, key);
                            player.player(event);
                            break;
                        case "unlink":
                            unlinkCommand unlink = new unlinkCommand(key);
                            unlink.unlink(event);
                            break;
                        case "voice":
                            voiceCommand voice = new voiceCommand(key);
                            voice.voice(event);
                        case "play":
                            playCommand play = new playCommand(spotifyApi, key);
                            play.play(event);
                    }
            } else {
            linkCommand link = new linkCommand(key);
            link.link(event);
        }
    }

    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getButton().getId();
        String[] split = buttonId.split("&");
        String name = split[1];
        String id = "";
        String handler = split[2];
        try {
            id = AES.decrypt(split[0], key);
        } catch (BadPaddingException e) {
            EmbedBuilder embed = new EmbedBuilder();
            embed.addField("Error!", "decryption failure", false);
            embed.appendDescription("This usually happens when the bot goes offline after a player is posted, but also protects against shady protection violations - if you believe this is an error, contact your system administrator.");
            embed.setColor(constants.errorColor);
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            return;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        switch (handler) {
            case "player":
                        SpotifyApi spotifyApi = SpotifyAuth.SPOTIFY_API_MAP.get(id);
            switch (name) {
                case "pauseplay":
                    try {
                        final CurrentlyPlaying currentsong = spotifyApi.getUsersCurrentlyPlayingTrack().build().execute();
                        if (currentsong.getIs_playing()) {
                            spotifyApi.pauseUsersPlayback().build().execute();
                            event.reply("Paused your song!").setEphemeral(true).queue();
                            event.getButton().withLabel("►");
                        } else {
                            spotifyApi.startResumeUsersPlayback().build().execute();
                            event.reply("Resumed your song!").setEphemeral(true).queue();
                        }
                    } catch (IOException | SpotifyWebApiException | ParseException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "rewind":
                    try {
                        final CurrentlyPlaying currentsong = spotifyApi.getUsersCurrentlyPlayingTrack().build().execute();
                        int current_position = currentsong.getProgress_ms();
                        int seek_position = current_position - 30000;
                        if (seek_position > 0) {
                            spotifyApi.seekToPositionInCurrentlyPlayingTrack(seek_position).build().execute();
                        } else {
                            seek_position = 0;
                            spotifyApi.seekToPositionInCurrentlyPlayingTrack(seek_position).build().execute();
                        }
                        event.reply("Rewinded your song 30 seconds!").setEphemeral(true).queue();
                    } catch (IOException | SpotifyWebApiException | ParseException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "skipback":
                    try {
                        spotifyApi.skipUsersPlaybackToPreviousTrack().build().execute();
                        event.reply("Skipped to your last song!").setEphemeral(true).queue();
                    } catch (IOException | SpotifyWebApiException | ParseException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "fastforward":
                    try {
                        final CurrentlyPlaying currentsong = spotifyApi.getUsersCurrentlyPlayingTrack().build().execute();
                        Track track = (Track) currentsong.getItem();
                        int current_position = currentsong.getProgress_ms();
                        int seek_position = current_position + 30000;
                        if (seek_position >= track.getDurationMs()) {
                            spotifyApi.skipUsersPlaybackToNextTrack().build().execute();
                        } else {
                            spotifyApi.seekToPositionInCurrentlyPlayingTrack(seek_position).build().execute();
                        }
                        event.reply("Fast forwarded your song 30 seconds!").setEphemeral(true).queue();
                    } catch (IOException | SpotifyWebApiException | ParseException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "skipforward":
                    try {
                        spotifyApi.skipUsersPlaybackToNextTrack().build().execute();
                        event.reply("Skipped to your next song!").setEphemeral(true).queue();
                    } catch (IOException | SpotifyWebApiException | ParseException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                }
            case "link":
                switch (name) {
            // /link authentication verification cases start here
                    case "success":
                        if (SpotifyAuth.validating_links.contains(id)) {
                            event.reply("Successfully confirmed your verification!").queue();
                            SpotifyAuth.validating_links.remove(id);
                        } else {
                            event.reply("This verification has already been confirmed by you. If you believe this is an error or would like to unlink your account, run the command **/unlink** to unlink your accounts!").setEphemeral(true).queue();
                        }
                        break;
                    case "failure":
                        if (SpotifyAuth.validating_links.contains(id)) {
                            SpotifyAuth.removeRefreshToken(id);
                            SpotifyAuth.SPOTIFY_API_MAP.remove(id);
                            event.reply("The linkage between these accounts has been terminated. \nBetween the time of initial verification and this removal, your " +
                                    "discord account has been able to access your spotify account in its entirety. \nIf you did not validate this linkage, **CHANGE YOUR SPOTIFY ACCOUNT PASSWORD**").queue();
                            SpotifyAuth.validating_links.remove(id);

                        } else {
                            event.reply("This verification has already been confirmed by you. If you believe this is an error or would like to unlink your account, run the command **/unlink** to unlink your accounts!").setEphemeral(true).queue();
                        }
                        break;
                        }
            case "unlink":
                switch (name) {
                        case "yes":
                            if (SpotifyAuth.validating_unlinks.contains(id)) {
                                SpotifyAuth.removeRefreshToken(id);
                                SpotifyAuth.SPOTIFY_API_MAP.remove(id);
                                event.reply("Successfully unlinked your Spotify & Discord accounts!").setEphemeral(true).queue();
                                SpotifyAuth.validating_unlinks.remove(id);
                            } else {
                                event.reply("This unlink session has already ended. Try running the command again!").setEphemeral(true).queue();
                            }
                            break;

                        case "no":
                            if (SpotifyAuth.validating_unlinks.contains(id)) {
                                event.reply("The unlinking of your Spotify & Discord accounts has been cancelled. You can still use Spotcord as usual!").setEphemeral(true).queue();
                                SpotifyAuth.validating_unlinks.remove(id);
                            } else {
                                event.reply("This unlink session has already ended. Try running the command again!").setEphemeral(true).queue();
                            }
                            break;
                        }
        }
    }
}
