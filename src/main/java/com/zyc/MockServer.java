package com.zyc;

import com.zyc.netty.NettyServer;
import com.zyc.schedule.InsertLog2Db;
import com.zyc.schedule.LoadData2Memory;
import com.zyc.util.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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

        new Thread(new Runnable() {
            @Override
            public void run() {

                while (true){
                    try {
                        InsertLog2Db insertLog2Db=new InsertLog2Db();
                        insertLog2Db.insert();
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
