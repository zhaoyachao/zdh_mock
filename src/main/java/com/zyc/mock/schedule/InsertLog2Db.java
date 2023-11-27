package com.zyc.mock.schedule;

import com.zyc.mock.entity.MockLogInfo;
import com.zyc.mock.service.InsertLogServiceImpl;
import com.zyc.mock.util.DbUtils;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class InsertLog2Db {

    public static BlockingDeque<MockLogInfo> blockingDeque = new LinkedBlockingDeque<MockLogInfo>();


    public void insert(){

        try {
            MockLogInfo mockLogInfo = blockingDeque.take();
            DbUtils dbUtils=new DbUtils();
            InsertLogServiceImpl insertLogService=new InsertLogServiceImpl();
            insertLogService.setDbUtils(dbUtils);
            insertLogService.insert(mockLogInfo);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
