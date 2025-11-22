package game.network; // Or a 'util' package

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A utility to fetch the user's public IP address from an external service.
 * This is necessary for the host to generate a Session ID.
 */
public class IpUtil {

    /**
     * Fetches the public IP address of this machine.
     * This makes an external web request, so it should not be run on the main UI thread.
     *
     * @return The public IP address as a string.
     * @throws Exception if the service is unreachable or fails.
     */
    public static String getPublicIp() throws Exception {
        // We use Amazon's service, but 'http://ifconfig.me/ip' also works.
        URL url = new URL("http://checkip.amazonaws.com");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(3000); // 3-second timeout
        con.setReadTimeout(3000);

        int status = con.getResponseCode();
        if (status == 200) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String ip = in.readLine();
                return ip.trim(); // Return the trimmed IP
            }
        } else {
            throw new RuntimeException("Failed to get public IP. HTTP Status: " + status);
        }
    }
}