package directory.robert.commands;

import directory.robert.spotify.AES;
import directory.robert.spotify.SpotifyAuth;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Widget;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.managers.AudioManager;
import se.michaelthelin.spotify.SpotifyApi;

import javax.crypto.SecretKey;
import java.awt.*;

public class voiceCommand {
    private SecretKey key;

    public voiceCommand(SecretKey key) {
        this.key = key;
    }

    public void voice(SlashCommandInteractionEvent event) {


        OptionMapping channelOption = event.getOption("channel");
        if (channelOption == null) {
            event.reply("You must select a voice channel!").setEphemeral(true).queue();
            return;
        }
        GuildChannel selectedChannel = channelOption.getAsChannel();

        if (selectedChannel.getType() == ChannelType.VOICE) {
            AudioChannel voiceChannel = (AudioChannel) selectedChannel;
            AudioManager audioManager = event.getGuild().getAudioManager();
            audioManager.openAudioConnection(voiceChannel);
            audioManager.setSendingHandler();

            event.reply("Joined voice channel: " + voiceChannel.getName()).queue();
        } else {
            event.reply("You must select a voice channel!").setEphemeral(true).queue();
        }



    }

}
