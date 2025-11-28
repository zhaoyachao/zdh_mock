package com.zyc.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zyc.mock.util.DbUtils;
import com.zyc.mock.schedule.LoadData2Memory;
import com.zyc.mock.schedule.InsertLog2Db;
import com.zyc.mock.netty.NettyServer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MockServer {
    public static Logger logger= LoggerFactory.getLogger(MockServer.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        String conf_path = MockServer.class.getClassLoader().getResource("application.properties").getPath();

        Properties properties = new Properties();
        InputStream inputStream=MockServer.class.getClassLoader().getResourceAsStream("application.properties");
        properties.load(inputStream);
        File confFile = new File("conf/application.properties");
        if(confFile.exists()){
            conf_path = confFile.getPath();
            inputStream = new FileInputStream(confFile);
            properties.load(inputStream);
        }

        logger.info("加载配置文件路径:{}", conf_path);
        DbUtils.init(properties);

        // 使用 ScheduledExecutorService 优化定时任务
        ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true); // 设置为守护线程
            thread.setName("mock-scheduler-" + thread.getId());
            return thread;
        });

        // 定时加载数据到内存，每20秒执行一次
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                LoadData2Memory loadData2Memory = new LoadData2Memory();
                loadData2Memory.load(properties);
            } catch (Exception e) {
                logger.error("定时加载数据到内存失败", e);
            }
        }, 0, 20, TimeUnit.SECONDS);

        // 单线程执行器处理日志插入
        ExecutorService logExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("log-executor-" + thread.getId());
            return thread;
        });

        logExecutor.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    InsertLog2Db insertLog2Db = new InsertLog2Db();
                    insertLog2Db.insert();
                } catch (Exception e) {
                    logger.error("日志插入失败", e);
                    try {
                        Thread.sleep(1000); // 出错时暂停1秒
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });

        NettyServer nettyServer=new NettyServer();
        nettyServer.start(properties);

    }
}
