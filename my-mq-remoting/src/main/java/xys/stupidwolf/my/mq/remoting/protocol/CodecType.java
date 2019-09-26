package xys.stupidwolf.my.mq.remoting.protocol;

public enum CodecType {
    JSON((byte)0);

    private byte code;

    CodecType(byte code) {
        this.code = code;
    }

    public static CodecType valueOf(byte code) {
        for (CodecType codecType : CodecType.values()) {
            if (codecType.getCode() == code) {
                return codecType;
            }
        }
        return null;
    }

    public byte getCode() {
        return code;
    }
}
