package xys.stupidwolf.my.mq.remoting.netty;

public class NettySystemConfig {
    private static final String REMOTING_NETTY_POOLED_BYTE_BUF_ALLOCATOR_ENABLE =
            "remoting.nettyPooledByteBufAllocatorEnable";
    private static final String REMOTING_SOCKET_SNDBUF_SIZE =
            "remoting.socket.sndbuf.size";
    private static final String REMOTING_SOCKET_RCVBUF_SIZE =
            "remoting.socket.rcvbuf.size";
    private static final String REMOTING_CLIENT_ASYNC_SEMAPHORE_VALUE =
            "remoting.clientAsyncSemaphoreValue";
    private static final String REMOTING_CLIENT_ONEWAY_SEMAPHORE_VALUE =
            "remoting.clientOnewaySemaphoreValue";

    public static final boolean NETTY_POOLED_BYTE_BUF_ALLOCATOR_ENABLE = //
            Boolean.parseBoolean(System.getProperty(REMOTING_NETTY_POOLED_BYTE_BUF_ALLOCATOR_ENABLE, "false"));
    public static final int CLIENT_ASYNC_SEMAPHORE_VALUE = //
            Integer.parseInt(System.getProperty(REMOTING_CLIENT_ASYNC_SEMAPHORE_VALUE, "65535"));
    public static final int CLIENT_ONEWAY_SEMAPHORE_VALUE =
            Integer.parseInt(System.getProperty(REMOTING_CLIENT_ONEWAY_SEMAPHORE_VALUE, "65535"));
    public static int socketSndbufSize =
            Integer.parseInt(System.getProperty(REMOTING_SOCKET_SNDBUF_SIZE, "65535"));
    public static int socketRcvbufSize =
            Integer.parseInt(System.getProperty(REMOTING_SOCKET_RCVBUF_SIZE, "65535"));
}
