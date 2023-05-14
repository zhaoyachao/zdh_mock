# ZDH MOCK服务
        zdh mock是一个轻量级http类型的mock服务,提供http mock请求,使用zdh mock服务,需要提前安装zdh_web服务,web服务提供可视化的http规则配置,
    mock服务是基于ntty的http协议实现,借助nginx可实现分布式使用
    

## 打包
    sh build.sh

## 启动

    linux:
        cd xx-SNAPSHOT
        sh bin/start.sh
        
    windows:
        windows下必须在xx-SNAPSHOT目录下执行启动脚本
        cd xx-SNAPSHOT
        bin\start.bat


