package xys.stupidwolf.my.mq.remoting.protocol;

import com.alibaba.fastjson.JSON;

import java.nio.charset.StandardCharsets;

public class JsonCodec{
    public static RemotingCommand decode(byte[] bytes) {
        return JSON.parseObject(new String(bytes, StandardCharsets.UTF_8), RemotingCommand.class);
    }

    public static byte[] encode(RemotingCommand remotingCommand) {
        String json = JSON.toJSONString(remotingCommand);
        return json.getBytes(StandardCharsets.UTF_8);
    }
}
