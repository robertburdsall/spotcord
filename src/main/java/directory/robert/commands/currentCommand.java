package directory.robert.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import se.michaelthelin.spotify.requests.data.player.GetUsersCurrentlyPlayingTrackRequest;
import se.michaelthelin.spotify.model_objects.specification.Track;


public class currentCommand {
    SpotifyApi spotifyApi;
    public currentCommand(SpotifyApi spotifyApi) {
        this.spotifyApi = spotifyApi;
    }

    public void current(SlashCommandInteractionEvent event) {

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
                        String progress = "";
                        if (current_progress > 0) {
                            progress = constants.progressbar[current_progress - 1];
                        } else {
                            progress = constants.progressbar[0];
                        }

                        eb.addField(progress, current_time + " / " + total_time, false);
                        eb.setColor(constants.primaryColor);
                        event.replyEmbeds(eb.build()).queue();
                    } else {
                        event.reply("You are not currently listening to a song!").queue();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    event.reply("Something went wrong!").setEphemeral(true).queue();

            }
        }

    }
