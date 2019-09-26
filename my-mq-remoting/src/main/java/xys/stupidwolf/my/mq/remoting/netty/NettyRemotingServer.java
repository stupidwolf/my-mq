package xys.stupidwolf.my.mq.remoting.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xys.stupidwolf.my.mq.remoting.RemotingServer;
import xys.stupidwolf.my.mq.remoting.protocol.RemotingCommand;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class NettyRemotingServer implements RemotingServer {
    private static final Logger logger = LoggerFactory.getLogger(NettyRemotingServer.class);
    private NettyServerConfig nettyServerConfig;
    private ServerBootstrap serverBootstrap;

    /**
     * 处理 server channel 的accept事件.
     * server channel 代表服务器自身的已绑定到本地端口正在监听的套接字
     */
    private final EventLoopGroup bossEventLoopGroup;
    /**
     * 处理已经建立的连接的io
     */
    private final EventLoopGroup workerEventLoopGroup;

//    private EventLoopGroup eventLoopGroup;

    // handlers
    private NettyDecode nettyDecode;
    private NettyEncode nettyEncode;
    private NettyConnectManageHandler connectManageHandler;
    private NettyServerHandler serverHandler;

    public NettyRemotingServer(NettyServerConfig nettyServerConfig) {
        this.nettyServerConfig = nettyServerConfig;
        this.serverBootstrap = new ServerBootstrap();
//        this.eventLoopGroup = new NioEventLoopGroup();
        // handlers
        nettyDecode = new NettyDecode();
        nettyEncode = new NettyEncode();
        this.connectManageHandler = new NettyConnectManageHandler();
        this.serverHandler = new NettyServerHandler();

        // event loop groups
        this.bossEventLoopGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("NettyServerBoss"));
        this.workerEventLoopGroup = new NioEventLoopGroup(nettyServerConfig.getServerWorkerThreads(), new DefaultThreadFactory("NettyServerWorker"));
    }

    @Override
    public void start() throws InterruptedException {
        // bootstrap server
        this.serverBootstrap.group(this.bossEventLoopGroup, this.workerEventLoopGroup)
                .channel(NioServerSocketChannel.class)
                // SO_BACKLOG: tcp三次握手成功,由于服务端是顺序处理客户端请求的,每次只能处理一个请求,用于临时存放已完成三次握手的请求的队列的最大长度
                // sync queue: /proc/sys/net/ipv4/tcp_max_syn_backlog
                // accept queue: /proc/sys/net/core/somaxconn
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_KEEPALIVE, false)
                // 将小的数据包组装为更大的帧然后进行发送
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_SNDBUF, nettyServerConfig.getServerSocketSndBufSize())
                .childOption(ChannelOption.SO_RCVBUF, nettyServerConfig.getServerSocketRcvBufSize())
                .localAddress(nettyServerConfig.getListenPort())
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline()
                                .addLast(nettyEncode)
                                .addLast(new NettyDecode())
                                .addLast(new IdleStateHandler(0, 0, nettyServerConfig.getTimeoutMills(), TimeUnit.MILLISECONDS))
                                .addLast(connectManageHandler)
                                .addLast(serverHandler)
                        ;
                    }
                });

        ChannelFuture sync = this.serverBootstrap.bind().sync();
        InetSocketAddress addr = (InetSocketAddress) sync.channel().localAddress();
        logger.info("netty server has started, address@{}:{}",addr.getHostName(), addr.getPort());
    }

    @Override
    public void shutdown() throws InterruptedException {
//        eventLoopGroup.shutdownGracefully().sync();
        this.bossEventLoopGroup.shutdownGracefully();
        this.workerEventLoopGroup.shutdownGracefully();
        logger.info("bossGroup, workerGroup has shutdown.");
    }

    @ChannelHandler.Sharable
    static class NettyConnectManageHandler extends ChannelDuplexHandler {
        private final ByteBuf HEART_BEAT_SEQUENCE = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(
                "HEART_BEAT", StandardCharsets.UTF_8
        ));

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            logger.info("NETTY SERVER PIPELINE: channelRegistered {}", ctx.channel().remoteAddress());
            super.channelRegistered(ctx);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            logger.info("NETTY SERVER PIPELINE: channelUnregistered, the channel[{}]", ctx.channel().remoteAddress());
            super.channelUnregistered(ctx);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            logger.info("NETTY SERVER PIPELINE: channelActive, the channel[{}]", ctx.channel().remoteAddress());
            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            logger.info("NETTY SERVER PIPELINE: channelInactive, the channel[{}]", ctx.channel().remoteAddress());
            super.channelInactive(ctx);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            // close channel when timeout
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state().equals(IdleState.ALL_IDLE)) {
                    logger.warn("NETTY SERVER PIPELINE: IDLE exception [{}]", ctx.channel().remoteAddress());
                    NettyUtil.closeChannel(ctx.channel());
                }
            }

            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.warn("NETTY SERVER PIPELINE: exceptionCaught {}", ctx.channel().remoteAddress());
            logger.warn("NETTY SERVER PIPELINE: exceptionCaught exception.", cause);
            ctx.close();
        }
    }

    @ChannelHandler.Sharable
    static class NettyServerHandler extends SimpleChannelInboundHandler<RemotingCommand> {

        @Override
        public void channelRead0(ChannelHandlerContext ctx, RemotingCommand remotingCommand) throws Exception {
            // TODO
            logger.info("handler msg, received: {}", remotingCommand.toString());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        NettyRemotingServer server = new NettyRemotingServer(new NettyServerConfig());
        server.start();
    }
}
