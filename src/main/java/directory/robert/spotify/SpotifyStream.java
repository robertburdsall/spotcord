package directory.robert.spotify;

import directory.robert.commands.constants;
import net.dv8tion.jda.api.audio.AudioSendHandler;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SpotifyStream {

    private Process librespotProcess;
    private Process ffmpegProcess;
    private final BlockingQueue<ByteBuffer> frameQueue = new LinkedBlockingQueue<>(20);
    private volatile boolean running = true;

    // 20ms stereo 48kHz PCM = 960 samples * 2 channels * 2 bytes = 3840
    private static final int PCM_FRAME_SIZE = 3840;

    public SpotifyStream(String accessToken, String trackUri) {
        try {
            // -----------------------------
            // 1️⃣ Start librespot
            // -----------------------------
            ProcessBuilder librespotPb = new ProcessBuilder(
                    constants.librespot_path,
                    "--name", "SpotifyBot-" + trackUri,
                    "--backend", "pipe",
                    "--access-token", accessToken,
                    "--bitrate", "160",
                    "--volume-ctrl", "linear"
            );
            librespotProcess = librespotPb.start();
            startLogger(librespotProcess.getErrorStream(), "librespot");
            InputStream librespotOut = librespotProcess.getInputStream();

            // -----------------------------
            // 2️⃣ Start FFmpeg (PCM -> Opus)
            // -----------------------------
            ProcessBuilder ffmpegPb = new ProcessBuilder(
                    "ffmpeg",
                    "-re",
                    "-f", "s16le",      // PCM signed 16-bit little-endian
                    "-ar", "44100",     // input sample rate (librespot default)
                    "-ac", "2",         // input channels
                    "-i", "pipe:0",     // input from librespot
                    "-f", "s16be",       // output format
                    "-ar", "48000",     // output sample rate (Discord requirement)
                    "-ac", "2",         // output channels
                    "-af", "aresample=resampler=soxr",
                    "pipe:1"            // pipe output
            );
            ffmpegProcess = ffmpegPb.start();
            startLogger(ffmpegProcess.getErrorStream(), "ffmpeg");
            InputStream ffmpegOut = ffmpegProcess.getInputStream();

            // -----------------------------
            // 3️⃣ Pipe librespot -> FFmpeg stdin
            // -----------------------------
            Thread pipeThread = new Thread(() -> {
                try (OutputStream ffmpegIn = ffmpegProcess.getOutputStream()) {
                    librespotOut.transferTo(ffmpegIn);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, "librespot-to-ffmpeg");
            pipeThread.setDaemon(true);
            pipeThread.start();

            // -----------------------------
            // 4️⃣ Read Opus packets and feed frameQueue
            // -----------------------------
            Thread readerThread = new Thread(() -> {
                try (BufferedInputStream bis = new BufferedInputStream(ffmpegOut)) {
                    byte[] audioBuffer = new byte[PCM_FRAME_SIZE * 10]; // Buffer multiple frames to reduce overhead
                    int bufferPos = 0;
                    while (running) {
                        int read = bis.read(audioBuffer, bufferPos, audioBuffer.length - bufferPos);
                        if (read == -1) break;
                        bufferPos += read;

                        while (bufferPos >= PCM_FRAME_SIZE) {
                            byte[] frameBytes = new byte[PCM_FRAME_SIZE];
                            System.arraycopy(audioBuffer, 0, frameBytes, 0, PCM_FRAME_SIZE);
                            frameQueue.put(ByteBuffer.wrap(frameBytes));

                            System.arraycopy(audioBuffer, PCM_FRAME_SIZE, audioBuffer, 0, bufferPos - PCM_FRAME_SIZE);
                            bufferPos -= PCM_FRAME_SIZE;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, "ffmpeg-reader");
            readerThread.setDaemon(true);
            readerThread.start();

        } catch (IOException e) {
            throw new RuntimeException("Failed to start SpotifyStream", e);
        }
    }

    // -----------------------------
    // JDA AudioSendHandler
    // -----------------------------
    public AudioSendHandler getAudioSendHandler() {
        return new AudioSendHandler() {
            @Override
            public boolean canProvide() {
                return !frameQueue.isEmpty();
            }

            @Override
            public ByteBuffer provide20MsAudio() {
                ByteBuffer frame = frameQueue.poll();
                if (frame != null) {
                    return frame;
                } else {
                    return ByteBuffer.allocate(PCM_FRAME_SIZE); // silence
                }
            }

            @Override
            public boolean isOpus() {
                return false;
            }
        };
    }

    public void stop() {
        running = false;
        if (librespotProcess != null) librespotProcess.destroyForcibly();
        if (ffmpegProcess != null) ffmpegProcess.destroyForcibly();
        frameQueue.clear();
    }

    private void startLogger(InputStream stream, String name) {
        Thread t = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.err.println("[" + name + "] " + line);
                }
            } catch (IOException ignored) {}
        }, name + "-stderr");
        t.setDaemon(true);
        t.start();
    }
}
