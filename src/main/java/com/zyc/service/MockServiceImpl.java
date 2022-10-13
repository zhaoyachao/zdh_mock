package com.zyc.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zyc.entity.MockDataInfo;
import com.zyc.util.DbUtils;

import java.util.List;
import java.util.Map;

public class MockServiceImpl {

    private DbUtils dbUtils;

    public DbUtils getDbUtils() {
        return dbUtils;
    }

    public void setDbUtils(DbUtils dbUtils) {
        this.dbUtils = dbUtils;
    }

    public List<MockDataInfo> selectAll() throws Exception {
        List<Map<String,Object>> r = dbUtils.R("select a.* from we_mock_data_info a");

        String rs = JSONObject.toJSONString(r);

        List<MockDataInfo> d =JSON.parseArray(rs, MockDataInfo.class);
        return d;
    }

}
