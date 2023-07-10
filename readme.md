# ZDH MOCK服务
        zdh mock是一个轻量级http类型的mock服务,提供http mock请求,使用zdh mock服务,需要提前安装zdh_web服务,web服务提供可视化的http规则配置,
    mock服务是基于ntty的http协议实现,借助nginx可实现分布式使用
        zdh mock 依赖zdh_web项目做可视化配置github: https://github.com/zhaoyachao/zdh_web
# 依赖项目        
   [zdh_web](https://github.com/zhaoyachao/zdh_web)
    

## 打包
    sh build.sh

## 启动

    linux:
        cd xx-SNAPSHOT
        sh bin/start.sh 或者 sh bin/zdh_mock.sh start
        
    windows:
        windows下必须在xx-SNAPSHOT目录下执行启动脚本
        cd xx-SNAPSHOT
        bin\start.bat


