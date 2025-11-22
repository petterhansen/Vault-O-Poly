package game.network; // Or 'util' package

import java.util.StringTokenizer;

/**
 * Encodes and Decodes IP addresses into user-friendly, short Session IDs.
 * (NOW WITH DEBUG PRINTS)
 */
public class SessionCodec {

    private static final String SESSION_PREFIX = "VAULT-";
    private static final int RADIX = 36;

    public static String encodeIp(String ipAddress) throws Exception {
        long ipAsLong = 0;

        // --- MODIFIED LOGIC FOR CLARITY ---
        String[] parts = ipAddress.split("\\."); // Split on literal dot
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid IP address format: " + ipAddress);
        }

        long byte1 = Long.parseLong(parts[0]);
        long byte2 = Long.parseLong(parts[1]);
        long byte3 = Long.parseLong(parts[2]);
        long byte4 = Long.parseLong(parts[3]);

        // Pack the 4 bytes into a single long
        ipAsLong = (byte1 << 24) | (byte2 << 16) | (byte3 << 8) | byte4;

        // --- DEBUG ---
        System.out.println("[Debug Codec] Enc: IP '" + ipAddress + "' -> long '" + ipAsLong + "'");
        // --- END DEBUG ---

        // Convert the long to a Base-36 string (0-9, A-Z)
        String encoded = Long.toString(ipAsLong, RADIX).toUpperCase();

        // --- DEBUG ---
        System.out.println("[Debug Codec] Enc: long '" + ipAsLong + "' -> base36 '" + encoded + "'");
        // --- END DEBUG ---

        return SESSION_PREFIX + encoded;
    }

    public static String decodeSessionId(String sessionId) throws Exception {
        if (sessionId == null || !sessionId.startsWith(SESSION_PREFIX)) {
            throw new IllegalArgumentException("Invalid Session ID format.");
        }

        String encoded = sessionId.substring(SESSION_PREFIX.length());

        // --- DEBUG ---
        System.out.println("[Debug Codec] Dec: ID '" + sessionId + "' -> base36 '" + encoded + "'");
        // --- END DEBUG ---

        // Convert the Base-36 string back to a long
        long ipAsLong;
        try {
            ipAsLong = Long.parseLong(encoded, RADIX);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid Session ID content.");
        }

        // --- DEBUG ---
        System.out.println("[Debug Codec] Dec: base36 '" + encoded + "' -> long '" + ipAsLong + "'");
        // --- END DEBUG ---

        // --- MODIFIED LOGIC FOR CLARITY ---
        // Unpack the 4 bytes from the long
        StringBuilder ipAddress = new StringBuilder();
        ipAddress.append((ipAsLong >> 24) & 0xFF); // 4th byte (MSB)
        ipAddress.append(".");
        ipAddress.append((ipAsLong >> 16) & 0xFF); // 3rd byte
        ipAddress.append(".");
        ipAddress.append((ipAsLong >> 8) & 0xFF);  // 2nd byte
        ipAddress.append(".");
        ipAddress.append(ipAsLong & 0xFF);         // 1st byte (LSB)

        String decodedIp = ipAddress.toString();

        // --- DEBUG ---
        System.out.println("[Debug Codec] Dec: long '" + ipAsLong + "' -> IP '" + decodedIp + "'");
        // --- END DEBUG ---

        return decodedIp;
    }
}