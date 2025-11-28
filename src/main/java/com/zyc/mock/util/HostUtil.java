package com.zyc.mock.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

public class HostUtil {
    public static Logger logger= LoggerFactory.getLogger(HostUtil.class);
    public static List<String> getIpAddress() {
        List<String> list = new LinkedList<>();
        try{
            Enumeration enumeration = NetworkInterface.getNetworkInterfaces();
            while (enumeration.hasMoreElements()) {
                NetworkInterface network = (NetworkInterface) enumeration.nextElement();
                if (network.isVirtual() || !network.isUp()) {
                    continue;
                } else {
                    Enumeration addresses = network.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress address = (InetAddress) addresses.nextElement();
                        if (address != null && (address instanceof Inet4Address)) {
                            list.add(address.getHostAddress());
                        }
                    }
                }
            }
        }catch (Exception e){
            logger.error("获取本机IP失败",e);
        }
        return list;
    }
}
