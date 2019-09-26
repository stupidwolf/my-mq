package xys.stupidwolf.my.mq.remoting.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xys.stupidwolf.my.mq.remoting.RemotingClient;
import xys.stupidwolf.my.mq.remoting.protocol.RemotingCommand;
import xys.stupidwolf.my.mq.remoting.protocol.RequestCode;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class NettyRemotingClient implements RemotingClient {
    private static final Logger logger = LoggerFactory.getLogger(NettyRemotingServer.class);
    private NettyClientConfig nettyClientConfig;
    private Bootstrap bootstrap;
    private EventLoopGroup eventLoopGroup;

    // TODO
    private ChannelFuture future;

    // handlers
    private NettyDecode nettyDecode;
    private NettyEncode nettyEncode;
    private NettyConnectManageHandler connectManageHandler;
    private NettyClientHandler clientHandler;

    public NettyRemotingClient(NettyClientConfig nettyClientConfig) {
        this.nettyClientConfig = nettyClientConfig;
        bootstrap = new Bootstrap();
        eventLoopGroup = new NioEventLoopGroup();

        nettyDecode = new NettyDecode();
        nettyEncode = new NettyEncode();
        this.connectManageHandler = new NettyConnectManageHandler();
        clientHandler = new NettyClientHandler();
    }

    @Override
    public void start() throws InterruptedException {
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8000))
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new IdleStateHandler(0, 0, 5, TimeUnit.SECONDS))
                                .addLast(nettyDecode)
                                .addLast(nettyEncode)
                                .addLast(connectManageHandler)
                                .addLast(clientHandler)
                        ;
                    }
                });
        future = bootstrap.connect();
        logger.info("netty client has started.");
    }

    @Override
    public void shutdown() {
        this.eventLoopGroup.shutdownGracefully();
    }

    @Override
    public void invokeSync(RemotingCommand remotingCommand, long timeoutMillis) throws InterruptedException {
        this.future.awaitUninterruptibly();
        if (this.future.channel().isActive()) {
            this.future.channel().writeAndFlush(remotingCommand);
        }
    }

    static class NettyConnectManageHandler extends ChannelDuplexHandler {
        @Override
        public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress,
                            ChannelPromise promise) throws Exception {
            logger.info("NETTY CLIENT PIPELINE: CONNECT  {} => {}", localAddress, remoteAddress);
            super.connect(ctx, remoteAddress, localAddress, promise);
        }

        @Override
        public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            logger.info("NETTY CLIENT PIPELINE: DISCONNECT {}", ctx.channel().remoteAddress());
            super.disconnect(ctx, promise);
        }

        @Override
        public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            logger.info("NETTY CLIENT PIPELINE: CLOSE {}", ctx.channel().remoteAddress());
            super.close(ctx, promise);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state().equals(IdleState.ALL_IDLE)) {
                    logger.warn("NETTY CLIENT PIPELINE: IDLE exception [{}]", ctx.channel().remoteAddress());
                    NettyUtil.closeChannel(ctx.channel());
                }
            }

            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.warn("NETTY CLIENT PIPELINE: exceptionCaught {}", ctx.channel().remoteAddress());
            logger.warn("NETTY CLIENT PIPELINE: exceptionCaught exception.", cause);
        }
    }

    static class NettyClientHandler extends SimpleChannelInboundHandler<RemotingCommand> {

        @Override
        public void channelRead0(ChannelHandlerContext ctx, RemotingCommand remotingCommand) throws Exception {
            // TODO
            logger.info("client received: {}, handler msg...", remotingCommand.toString());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Scanner in = new Scanner(System.in);
        NettyRemotingClient client = new NettyRemotingClient(null);
        client.start();
        for (int i = 0; i < 10; i ++) {
            client.invokeSync(RemotingCommand.createRequestCommand(RequestCode.SEND_MESSAGE), 3000);
//            Thread.sleep(6 * 1000);
            logger.info("request command has been sent.");
        }
        client.shutdown();
//        in.nextLine();
    }
}
