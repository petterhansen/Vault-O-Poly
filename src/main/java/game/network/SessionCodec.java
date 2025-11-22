package game.network;

/**
 * Encodes and Decodes IP addresses into user-friendly, short Session IDs.
 */
public class SessionCodec {

    private static final String SESSION_PREFIX = "VAULT-";
    private static final int RADIX = 36;

    public static String encodeIp(String ipAddress) throws Exception {
        long ipAsLong = 0;

        String[] parts = ipAddress.split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid IP address format: " + ipAddress);
        }

        long byte1 = Long.parseLong(parts[0]);
        long byte2 = Long.parseLong(parts[1]);
        long byte3 = Long.parseLong(parts[2]);
        long byte4 = Long.parseLong(parts[3]);

        ipAsLong = (byte1 << 24) | (byte2 << 16) | (byte3 << 8) | byte4;

        String encoded = Long.toString(ipAsLong, RADIX).toUpperCase();

        return SESSION_PREFIX + encoded;
    }

    public static String decodeSessionId(String sessionId) throws Exception {
        if (sessionId == null || !sessionId.startsWith(SESSION_PREFIX)) {
            throw new IllegalArgumentException("Invalid Session ID format.");
        }

        String encoded = sessionId.substring(SESSION_PREFIX.length());

        long ipAsLong;
        try {
            ipAsLong = Long.parseLong(encoded, RADIX);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid Session ID content.");
        }

        StringBuilder ipAddress = new StringBuilder();
        ipAddress.append((ipAsLong >> 24) & 0xFF);
        ipAddress.append(".");
        ipAddress.append((ipAsLong >> 16) & 0xFF);
        ipAddress.append(".");
        ipAddress.append((ipAsLong >> 8) & 0xFF);
        ipAddress.append(".");
        ipAddress.append(ipAsLong & 0xFF);

        return ipAddress.toString();
    }
}