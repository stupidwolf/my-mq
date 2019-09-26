package xys.stupidwolf.my.mq.remoting.protocol;

import java.nio.ByteBuffer;

/**
 * codec_type: 8 (编码方式)
 * body_bytes: 报文体内容
 */
public class RemotingCommand {
    private final static int DECODE_TYPE_BIT = 4;
    private int code;

    private String command = "hello,world";

    public static RemotingCommand decode(final ByteBuffer byteBuffer) {
//        int length = byteBuffer.getInt();
//        int bodyLength = length - 4;
//        byte[] bodys = new byte[bodyLength];
        byte[] bodys = new byte[byteBuffer.limit()];
        byteBuffer.get(bodys);
        return JsonCodec.decode(bodys);
    }

    public ByteBuffer encode() {
        byte[] bodys = JsonCodec.encode(this);
        ByteBuffer result = ByteBuffer.allocate(4 + bodys.length);
        result.putInt(bodys.length)
                .put(bodys);
        result.flip();
        return result;
    }

    public static RemotingCommand createRequestCommand(int code) {
        RemotingCommand cmd = new RemotingCommand();
        cmd.setCode(code);
        return cmd;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }


    @Override
    public String toString() {
        return "RemotingCommand{" +
                "code=" + code +
                ", command='" + command + '\'' +
                '}';
    }
}
