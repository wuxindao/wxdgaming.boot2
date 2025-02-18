package wxdgaming.boot2.starter.net.client;

import io.netty.channel.ChannelHandler;
import wxdgaming.boot2.starter.net.MessageEncode;
import wxdgaming.boot2.starter.net.pojo.ProtoListenerFactory;

/**
 * 服务
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-17 15:08
 **/
@ChannelHandler.Sharable
public class ClientMessageEncode extends MessageEncode {

    public ClientMessageEncode(ProtoListenerFactory protoListenerFactory) {
        super(protoListenerFactory);
    }

}
