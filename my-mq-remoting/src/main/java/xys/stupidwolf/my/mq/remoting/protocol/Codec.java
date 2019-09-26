package xys.stupidwolf.my.mq.remoting.protocol;

import io.netty.buffer.ByteBuf;

public interface Codec {
    RemotingCommand decode(byte[] bytes);
    byte[] encode(RemotingCommand remotingCommand);
}
