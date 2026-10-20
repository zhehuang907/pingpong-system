import cn.hutool.crypto.digest.BCrypt;
import org.junit.jupiter.api.Test;

public class PasswordTest {
    @Test
    public void generateHash() {
        String hash = BCrypt.hashpw("PingPong2026!!", BCrypt.gensalt());
        System.out.println("BCRYPT_HASH_START:" + hash + ":BCRYPT_HASH_END");
    }
}
