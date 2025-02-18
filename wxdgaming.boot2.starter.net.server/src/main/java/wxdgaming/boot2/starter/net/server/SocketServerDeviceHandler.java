package wxdgaming.boot2.starter.net.server;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.starter.net.ChannelUtil;
import wxdgaming.boot2.starter.net.SocketDeviceHandler;
import wxdgaming.boot2.starter.net.SocketSession;

/**
 * socket server 驱动
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2024-08-16 09:05
 **/
@Slf4j
@ChannelHandler.Sharable
public class SocketServerDeviceHandler extends SocketDeviceHandler {

    final SocketServerConfig socketServerConfig;

    public SocketServerDeviceHandler(SocketServerConfig socketServerConfig) {
        this.socketServerConfig = socketServerConfig;
    }

    @Override public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        log.debug(
                "channel open {}",
                ChannelUtil.getLocalAddress(ctx.channel()) + " : " + ChannelUtil.getRemoteAddress(ctx.channel())
        );
        /*TODO 构造函数自动注册*/
        SocketSession socketSession = new SocketSession(
                SocketSession.Type.server,
                ctx.channel(),
                ChannelUtil.attr(ctx.channel(), ChannelUtil.WEB_SOCKET_SESSION_KEY)
        );
        socketSession.setMaxFrameBytes(socketServerConfig.getMaxFrameBytes());
        socketSession.setMaxFrameLength(socketServerConfig.getMaxFrameLength());
    }

}
