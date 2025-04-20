package directory.robert.spotify;

public class TokenRefresh extends Thread{
    SpotifyAuth spotifyAuth = new SpotifyAuth();
    public void run() {

        while (true) {
            try {
                Thread.sleep(2400000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        if(SpotifyAuth.SPOTIFY_API_MAP.isEmpty()){
            continue;
        }
            SpotifyAuth.SPOTIFY_API_MAP.replaceAll((i, v) -> spotifyAuth.refreshAccessToken(SpotifyAuth.SPOTIFY_API_MAP.get(i).getRefreshToken(), i));
        }
    }
}
