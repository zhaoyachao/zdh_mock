package com.zyc;

import com.zyc.netty.NettyServer;
import com.zyc.schedule.LoadData2Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class MockServer {
    public static Logger logger= LoggerFactory.getLogger(MockServer.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        String conf_path = MockServer.class.getClassLoader().getResource("application.properties").getPath();

        Properties properties = new Properties();
        properties.load(MockServer.class.getClassLoader().getResourceAsStream("application.properties"));
        File confFile = new File("conf/application.properties");
        if(confFile.exists()){
            conf_path = confFile.getPath();
            FileInputStream fis=new FileInputStream(confFile);
            properties.load(fis);
        }
        logger.info("加载配置文件路径:{}", conf_path);
        new Thread(new Runnable() {
            @Override
            public void run() {

                while (true){
                    try {
                        LoadData2Memory loadData2Memory=new LoadData2Memory();
                        loadData2Memory.load(properties);
                        Thread.sleep(1000*20);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        }).start();

        NettyServer nettyServer=new NettyServer();
        nettyServer.start(properties);

    }

}
