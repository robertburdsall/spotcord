package directory.robert.spotify;

import net.dv8tion.jda.api.audio.AudioSendHandler;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpotifyStream {
    private Process librespotProcess;
    private Process ffmpegProcess;
    private InputStream ffmpegOut;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Start a Spotify stream using librespot (with OAuth token) and FFmpeg for Discord.
     * @param accessToken OAuth access token from your bot
     * @param trackUri Spotify track URI (optional, for naming/logging)
     * @throws IOException
     */
    public SpotifyStream(String accessToken, String trackUri) {
        // --- 1. Start librespot ---
        ProcessBuilder librespotPb = new ProcessBuilder(
                "librespot.exe",
                "--name", "SpotifyBot-" + trackUri,
                "--backend", "pipe",
                "--device", "pipe:1",
                "--onevent", "playback",
                "--spotify-token", accessToken
        );
        librespotPb.redirectErrorStream(true);
        try {
            librespotProcess = librespotPb.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        InputStream librespotOut = librespotProcess.getInputStream();

        // --- 2. Start FFmpeg to convert 44.1kHz PCM -> 48kHz PCM ---
        ProcessBuilder ffmpegPb = new ProcessBuilder(
                "ffmpeg",
                "-f", "s16le",
                "-ar", "44100",
                "-ac", "2",
                "-i", "pipe:0",
                "-f", "s16le",
                "-ar", "48000",
                "-ac", "2",
                "pipe:1"
        );
        ffmpegPb.redirectErrorStream(true);
        try {
            ffmpegProcess = ffmpegPb.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ffmpegOut = ffmpegProcess.getInputStream();

        // --- 3. Pipe librespot -> FFmpeg ---
        executor.submit(() -> {
            try (OutputStream ffmpegIn = ffmpegProcess.getOutputStream()) {
                librespotOut.transferTo(ffmpegIn);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Get an AudioSendHandler for JDA voice channels.
     */
    public AudioSendHandler getAudioSendHandler() {
        return new AudioSendHandler() {
            @Override
            public boolean canProvide() { return true; }

            @Override
            public ByteBuffer provide20MsAudio() {
                byte[] buffer = new byte[3840]; // 20ms stereo 48kHz PCM
                try {
                    int read = ffmpegOut.read(buffer);
                    if (read <= 0) return null;
                    return ByteBuffer.wrap(buffer, 0, read);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public boolean isOpus() { return false; }
        };
    }

    /**
     * Stop the stream and clean up processes.
     */
    public void stop() {
        if (librespotProcess != null) librespotProcess.destroy();
        if (ffmpegProcess != null) ffmpegProcess.destroy();
        executor.shutdownNow();
    }
}
