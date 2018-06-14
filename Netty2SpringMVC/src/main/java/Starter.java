import com.jianhua.netty.NettyServer;

/**
 * @author lijianhua
 */
public class Starter {

    public static void main(String[] args) throws Exception {
        new NettyServer().port(443).enableSSL(true).start();
    }
}