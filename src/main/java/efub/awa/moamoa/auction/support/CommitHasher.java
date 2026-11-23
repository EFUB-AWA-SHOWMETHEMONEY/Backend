package efub.awa.moamoa.auction.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class CommitHasher {
    private CommitHasher() {}

    public static String hash(Long amount, String salt) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256")
                    .digest((amount + ":" + salt).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean matches (Long amount, String salt, String expectedHash) {
        return MessageDigest.isEqual(hash(amount, salt).getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8));
    }
}
