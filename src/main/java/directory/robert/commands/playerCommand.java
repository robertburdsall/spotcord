package directory.robert.commands;

import directory.robert.DiscordBot;
import directory.robert.spotify.AES;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.data.player.GetUsersCurrentlyPlayingTrackRequest;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class playerCommand {
    private SpotifyApi spotifyApi;
    private SecretKey key;

    public playerCommand(SpotifyApi spotifyApi, SecretKey key) {
        this.spotifyApi = spotifyApi;
        this.key = key;
    }

    public void player (SlashCommandInteractionEvent event) {

        final GetUsersCurrentlyPlayingTrackRequest getCurrentTrackRequest = spotifyApi.getUsersCurrentlyPlayingTrack()
        .build();

            try {
                final CurrentlyPlaying currentlyPlaying = getCurrentTrackRequest.execute();
                if (currentlyPlaying != null && currentlyPlaying.getItem() != null) {
                    Track track = (Track) currentlyPlaying.getItem();
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setThumbnail(track.getAlbum().getImages()[0].getUrl());
                    eb.addField(currentlyPlaying.getItem().getName(), track.getArtists()[0].getName(), false);

                    // timestamp math
                    String current_time = constants.formatTime(Long.valueOf(currentlyPlaying.getProgress_ms()));
                    long seconds = currentlyPlaying.getProgress_ms() / 1000;

                    String total_time = constants.formatTime(Long.valueOf(track.getDurationMs()));
                    long total_seconds = track.getDurationMs() / 1000;

                    // progress bar string maths

                    long brackets = total_seconds / 10;
                    int current_progress = (int) (seconds / brackets);
                    String progress;
                    if (current_progress > 0) {
                        progress = constants.progressbar[current_progress - 1];
                    } else {
                        progress = constants.progressbar[0];
                    }

                    eb.addField(progress,  current_time + " / " + total_time, false);
                    eb.setColor(constants.primaryColor);
                    eb.build();

                    // encrypted identifier for buttonids
                    String id = AES.encrypt(event.getUser().getId(), key);
                    event.replyEmbeds(eb.build())
                            .setActionRow(
                                    Button.primary(id + "&" + "skipback" + "&" + "player", "|◁"),
                                    Button.success(id + "&" + "rewind" + "&" + "player", "↻"),
                                    Button.primary(id + "&" + "pauseplay" + "&" + "player", "II"),
                                    Button.success(id + "&" + "fastforward" + "&" + "player", "↻"),
                                    Button.primary(id + "&" + "skipforward" + "&" + "player", "▷|")
                            ).queue(interactionHook -> {
                                interactionHook.retrieveOriginal().queue(originalMessage -> {
                                    String channelId = originalMessage.getChannelId();
                                    String messageId = originalMessage.getId();
                                    startUpdatingMessage(channelId, messageId);
                                });

                            });
                } else event.reply("You are not currently listening to a song!").queue();
            } catch (Exception e) {
                e.printStackTrace();
                event.reply("Something went wrong!").setEphemeral(true).queue();
        }
    }

    private void startUpdatingMessage(String channelId, String messageId) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            final GetUsersCurrentlyPlayingTrackRequest getCurrentTrackRequest = spotifyApi.getUsersCurrentlyPlayingTrack()
                        .build();
                final CurrentlyPlaying currentlyPlaying;
                try {
                    currentlyPlaying = getCurrentTrackRequest.execute();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (SpotifyWebApiException e) {
                    throw new RuntimeException(e);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                if (currentlyPlaying != null && currentlyPlaying.getItem() != null) {
                    Track track = (Track) currentlyPlaying.getItem();
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setThumbnail(track.getAlbum().getImages()[0].getUrl());
                    eb.addField(currentlyPlaying.getItem().getName(), track.getArtists()[0].getName(), false);

                    // timestamp math
                    String current_time = constants.formatTime(Long.valueOf(currentlyPlaying.getProgress_ms()));
                    long seconds = currentlyPlaying.getProgress_ms() / 1000;

                    String total_time = constants.formatTime(Long.valueOf(track.getDurationMs()));
                    long total_seconds = track.getDurationMs() / 1000;

                    // progress bar string maths

                    long brackets = total_seconds / 10;
                    int current_progress = (int) (seconds / brackets);
                    String progress;
                    if (current_progress > 0) {
                        progress = constants.progressbar[current_progress - 1];
                    } else {
                        progress = constants.progressbar[0];
                    }

                    eb.addField(progress,  current_time + " / " + total_time, false);
                    eb.setColor(constants.primaryColor);

                    MessageEmbed updatedEmbed = eb.build();

                    TextChannel channel = DiscordBot.jda.getTextChannelById(channelId);
                    if (channel != null) {
                        channel.editMessageEmbedsById(messageId, updatedEmbed).queue();
                    }

                }
        }, 5, 5, TimeUnit.SECONDS);
    }

}
