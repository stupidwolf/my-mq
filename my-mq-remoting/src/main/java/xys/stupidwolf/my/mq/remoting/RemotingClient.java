package xys.stupidwolf.my.mq.remoting;

import xys.stupidwolf.my.mq.remoting.protocol.RemotingCommand;

public interface RemotingClient extends RemotingService {
    void invokeSync(RemotingCommand remotingCommand, long timeoutMillis) throws InterruptedException;
}
