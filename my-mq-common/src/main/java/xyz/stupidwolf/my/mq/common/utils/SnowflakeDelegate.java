package xyz.stupidwolf.my.mq.common.utils;

import java.lang.management.ManagementFactory;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Enumeration;

public class SnowflakeDelegate implements IDGenerateDelegate {
    /**
     * note
     *  0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000 - 00000 - 000000000000
     */

    private final static long START_TIMESTAMP  = 1546272000000L;
    private final static int SYMBOL_BIT = 1;
    private final static int TIME_STAMP_BIT = 41;
    private final static int DATA_CENTER_BIT = 5;
    private final static int MACHINE_BIT = 5;
    private final static int SEQUENCE_BIT = 12;

    private int dataCenterId;
    private int machineId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    private final static int MAX_SEQUENCE = ~(-1 << SEQUENCE_BIT);
    private final static int MAX_MACHINED = ~(-1 << MACHINE_BIT);
    private final static int MAX_DATA_CENTER_ID = ~(-1 << DATA_CENTER_BIT);

    public SnowflakeDelegate() {
        byte[] ipv4 = getIpv4();
        this.dataCenterId = getPid();
        this.machineId = getPid();
    }

    public SnowflakeDelegate(int dataCenterId, int machineId) {
        this.dataCenterId = dataCenterId;
        this.machineId = machineId;
    }

    public synchronized String generateMsgId() {
        long currentTimeStamp = System.currentTimeMillis();
        if (currentTimeStamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards.Refusing to generate id.");
        }

        if (currentTimeStamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;

            long temp = System.currentTimeMillis();
            while (temp == currentTimeStamp) {
                temp = System.currentTimeMillis();
            }
            currentTimeStamp = temp;
        } else {
            sequence = 0;
        }

        lastTimestamp = currentTimeStamp;
        return "" + ((currentTimeStamp - START_TIMESTAMP) << (DATA_CENTER_BIT + MACHINE_BIT + SEQUENCE_BIT) |
                dataCenterId << (DATA_CENTER_BIT + SEQUENCE_BIT) |
                machineId << MACHINE_BIT |
                sequence);
    }

    private static byte[] getIpv4() {
        try {
            Enumeration allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress ip = null;
            byte[] internalIP = null;
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
                Enumeration addresses = netInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    ip = (InetAddress) addresses.nextElement();
                    if (ip instanceof Inet4Address) {
                        byte[] ipByte = ip.getAddress();
                        if (ipByte.length == 4) {
                            if (ipv4Check(ipByte)) {
                                if (!isInternalIP(ipByte)) {
                                    return ipByte;
                                } else if (internalIP == null) {
                                    internalIP = ipByte;
                                }
                            }
                        }
                    }
                }
            }
            if (internalIP != null) {
                return internalIP;
            } else {
                throw new RuntimeException("Can not get local ip");
            }
        } catch (Exception e) {
            throw new RuntimeException("Can not get local ip", e);
        }
    }

    private static boolean ipv4Check(byte[] ip) {
        if (ip.length != 4) {
            throw new RuntimeException("illegal ipv4 bytes");
        }

//        if (ip[0] == (byte)30 && ip[1] == (byte)10 && ip[2] == (byte)163 && ip[3] == (byte)120) {
//        }

        if (ip[0] >= (byte) 1 && ip[0] <= (byte) 126) {
            if (ip[1] == (byte) 1 && ip[2] == (byte) 1 && ip[3] == (byte) 1) {
                return false;
            }
            return ip[1] != (byte) 0 || ip[2] != (byte) 0 || ip[3] != (byte) 0;
        } else if (ip[0] >= (byte) 128 && ip[0] <= (byte) 191) {
            if (ip[2] == (byte) 1 && ip[3] == (byte) 1) {
                return false;
            }
            if (ip[2] == (byte) 0 && ip[3] == (byte) 0) {
                return false;
            }
            return true;
        } else if (ip[0] >= (byte) 192 && ip[0] <= (byte) 223) {
            if (ip[3] == (byte) 1) {
                return false;
            }
            if (ip[3] == (byte) 0) {
                return false;
            }
            return true;
        }
        return false;
    }

    static boolean isInternalIP(byte[] ip) {
        if (ip.length != 4) {
            throw new RuntimeException("illegal ipv4 bytes");
        }

        //10.0.0.0~10.255.255.255
        //172.16.0.0~172.31.255.255
        //192.168.0.0~192.168.255.255
        if (ip[0] == (byte) 10) {

            return true;
        } else if (ip[0] == (byte) 172) {
            if (ip[1] >= (byte) 16 && ip[1] <= (byte) 31) {
                return true;
            }
        } else if (ip[0] == (byte) 192) {
            if (ip[1] == (byte) 168) {
                return true;
            }
        }
        return false;
    }


    private static int getPid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return Integer.parseInt(name.split("@")[0]);
    }

    public static void main(String[] args) {
        System.out.println(getPid());
        System.out.println(Arrays.toString(getIpv4()));
//        System.out.println(~(-1L << SEQUENCE_BIT));
//        System.out.println(Integer.toBinaryString(-1));
//        System.out.println(Integer.toBinaryString((-1 << SEQUENCE_BIT)));
        SnowflakeDelegate app = new SnowflakeDelegate(102, 21);
        for (int i = 0; i < 10; i ++) {
            System.out.println(app.generateMsgId());
        }
    }
}
