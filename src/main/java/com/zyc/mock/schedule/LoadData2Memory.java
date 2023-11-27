package com.zyc.mock.schedule;

import com.zyc.mock.entity.MockDataInfo;
import com.zyc.mock.service.MockServiceImpl;
import com.zyc.mock.util.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class LoadData2Memory {
    Logger logger= LoggerFactory.getLogger(LoadData2Memory.class);
    public static Map<String, MockDataInfo> mockDataInfos=new ConcurrentHashMap<>();

    public void load(Properties properties) throws Exception {

        DbUtils dbUtils=new DbUtils();

        MockServiceImpl mockService=new MockServiceImpl();
        mockService.setDbUtils(dbUtils);

        List<MockDataInfo> mockDataInfos = mockService.selectAll();

        Map<String, MockDataInfo> tmp=new ConcurrentHashMap<>();
        if(mockDataInfos != null){
            for (MockDataInfo mockDataInfo:mockDataInfos){
                tmp.put(mockDataInfo.getUrl(), mockDataInfo);
            }
        }
        logger.info("完成加载mock信息到内存, 总量:{}", tmp.size());
        this.mockDataInfos=tmp;
    }

}
