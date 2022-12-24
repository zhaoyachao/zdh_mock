package com.zyc.service;

import com.zyc.entity.MockLogInfo;
import com.zyc.util.DbUtils;

public class InsertLogServiceImpl {
    private DbUtils dbUtils;

    public DbUtils getDbUtils() {
        return dbUtils;
    }

    public void setDbUtils(DbUtils dbUtils) {
        this.dbUtils = dbUtils;
    }

    public void insert(MockLogInfo mockLogInfo){
        String[] r = dbUtils.CUD("insert into zdh_logs(job_id,level,msg,task_logs_id) values(?,?,?,?)", new Object[]{
                mockLogInfo.getJob_id(), mockLogInfo.getLevel(),mockLogInfo.getMsg(), mockLogInfo.getTask_logs_id()
        });
    }
}
