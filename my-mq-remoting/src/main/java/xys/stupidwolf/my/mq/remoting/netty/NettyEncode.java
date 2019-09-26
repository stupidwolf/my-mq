package xys.stupidwolf.my.mq.remoting.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xys.stupidwolf.my.mq.remoting.protocol.RemotingCommand;

@ChannelHandler.Sharable
public class NettyEncode extends MessageToByteEncoder<RemotingCommand> {
    private static final Logger logger = LoggerFactory.getLogger(NettyEncode.class);
    @Override
    public void encode(ChannelHandlerContext ctx, RemotingCommand remotingCommand, ByteBuf out) throws Exception {
        try {
            out.writeBytes(remotingCommand.encode());
        } catch (Exception e) {
            logger.error("encode exception, " + ctx.channel().remoteAddress(), e);
            if (remotingCommand != null) {
                logger.error(remotingCommand.toString());
            }
            NettyUtil.closeChannel(ctx.channel());
        }
    }
}
