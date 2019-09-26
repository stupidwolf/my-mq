package xys.stupidwolf.my.mq.remoting.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyUtil {
    private static final Logger logger = LoggerFactory.getLogger(NettyUtil.class);
    public static void closeChannel(Channel channel) {
        channel.close().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                logger.info("closeChannel: close the connection to remote address[{}] result: {}", channel.remoteAddress(),
                        future.isSuccess());
            }
        });
    }
}
