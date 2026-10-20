import cn.hutool.crypto.digest.BCrypt;

public class HashGen {
    public static void main(String[] args) {
        String hash = BCrypt.hashpw("PingPong2026!!", BCrypt.gensalt());
        System.out.println(hash);
    }
}
