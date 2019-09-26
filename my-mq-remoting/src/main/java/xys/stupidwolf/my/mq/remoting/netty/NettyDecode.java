package xys.stupidwolf.my.mq.remoting.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xys.stupidwolf.my.mq.remoting.protocol.RemotingCommand;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class NettyDecode extends LengthFieldBasedFrameDecoder {
    private static final Logger logger = LoggerFactory.getLogger(NettyDecode.class);
    private static final int FRAME_MAX_LENGTH = Math.min(1 << 30,
            Integer.parseInt(System.getProperty("remoting.frameMaxLength", "16777216")));


    public NettyDecode() {
        super(FRAME_MAX_LENGTH, 0, 4, 0, 4);
    }

    @Override
    public Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = null;
        try {
            frame = (ByteBuf) super.decode(ctx, in);
            if (null == frame) {
                return null;
            }
            System.out.println(frame.toString(StandardCharsets.UTF_8));
            ByteBuffer byteBuffer = frame.nioBuffer();
            return RemotingCommand.decode(byteBuffer);
        } catch (Exception e) {
            logger.error("decode exception, " + ctx.channel().remoteAddress(), e);
            NettyUtil.closeChannel(ctx.channel());
        } finally {
            if (null != frame) {
                frame.release();
            }
        }
        return null;
    }
}
