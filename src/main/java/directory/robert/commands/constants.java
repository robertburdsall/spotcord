package directory.robert.commands;

import java.awt.*;

public class constants {

    public static final Color primaryColor = new Color(0x7289da);
    public static final Color errorColor = new Color(0xFAA0A0);
    public static final String serverAddress = "http://10.137.68.63:23349/callback";
    public static final String localAddress = "http://localhost:8888/callback";
    public static final int localPort = 8888;
    public static final int serverPort = 23349;

    public static final String[] progressbar = new String[] {"╞▰═════════╡", "╞═▰════════╡", "╞══▰═══════╡",
    "╞═══▰══════╡", "╞════▰═════╡", "╞═════▰════╡", "╞══════▰═══╡", "╞═══════▰══╡", "╞════════▰═╡", "╞═════════▰╡"};

    public static final String resources_path = "C:/Users/rober/Documents/Programming/sigma";
    public static final String librespot_path = "C:/Users/rober/documents/programming/spotcord/librespot/target/release/librespot.exe";


    public static String formatTime (Long milliseconds) {
        long totalSeconds = milliseconds / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        return String.format("%d:%02d", minutes, seconds);
    }

}
