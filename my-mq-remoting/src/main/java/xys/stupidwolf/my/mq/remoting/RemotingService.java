package xys.stupidwolf.my.mq.remoting;

public interface RemotingService {
    void start() throws InterruptedException;
    void shutdown() throws InterruptedException;
}
