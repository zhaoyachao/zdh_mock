package com.zyc.mock.netty;


import cn.hutool.core.text.StrFormatter;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hubspot.jinjava.Jinjava;
import com.zyc.mock.entity.MockDataInfo;
import com.zyc.mock.entity.MockLogInfo;
import com.zyc.mock.schedule.InsertLog2Db;
import com.zyc.mock.schedule.LoadData2Memory;
import com.zyc.mock.util.RocksDBUtil;
import com.zyc.mock.util.ShortUrlUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class HttpServerHandler extends HttpBaseHandler{
    Logger logger= LoggerFactory.getLogger(HttpServerHandler.class);

    //单线程线程池，同一时间只会有一个线程在运行,保证加载顺序
    private ThreadPoolExecutor threadpool = new ThreadPoolExecutor(
            1, // core pool size
            1, // max pool size
            500, // keep alive time
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>()
            );

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        FullHttpRequest request = (FullHttpRequest)msg;
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        HttpResponse response = diapathcer(request);
        if (keepAlive) {
            response.headers().set(Connection, KeepAlive);
            ctx.writeAndFlush(response);
        } else {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.writeAndFlush(defaultResponse(serverErr)).addListener(ChannelFutureListener.CLOSE);
    }


    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    private Map<String,Object> getBody(String content){
        return JSON.parseObject(content, Map.class);
    }

    private Map<String,Object> getParam(String uri) throws UnsupportedEncodingException {
        Map<String, Object> map = new HashMap<>();
        String path = URLDecoder.decode(uri, chartSet);
        String cont = path.substring(path.lastIndexOf("?") + 1);
        if (cont.contains("=")){
            List<String> params = Arrays.stream(cont.split("&|&&")).map(e -> e.trim()).collect(Collectors.toList());
            for (String param : params) {
                String[] kv = param.split("=", 2);
                map.put(kv[0], kv[1]);
            }
        }
        return map;
    }

    private Map<String,Object> getReqContent(FullHttpRequest request) throws UnsupportedEncodingException {

        if(request.method().name().equalsIgnoreCase(HttpMethod.GET.name())){
            return getParam(request.uri());
        }else if(request.method().name().equalsIgnoreCase(HttpMethod.POST.name())){
            Map getMap = getParam(request.uri());
            Map postMap = getBody(request.content().toString(CharsetUtil.UTF_8));
            postMap.putAll(getMap);
            return postMap;
        }else if(request.method().name().equalsIgnoreCase(HttpMethod.PUT.name())){
            Map<String,Object> map = getParam(request.uri());
            map.putAll(getBody(request.content().toString(CharsetUtil.UTF_8)));
            return map;
        }else if(request.method().name().equalsIgnoreCase(HttpMethod.DELETE.name())){
            Map<String,Object> map = getParam(request.uri());
            map.putAll(getBody(request.content().toString(CharsetUtil.UTF_8)));
            return map;
        }else if(request.method().name().equalsIgnoreCase(HttpMethod.PATCH.name())){
            Map<String,Object> map = getParam(request.uri());
            map.putAll(getBody(request.content().toString(CharsetUtil.UTF_8)));
            return map;
        }
        return null;
    }


    public HttpResponse diapathcer(FullHttpRequest request) throws UnsupportedEncodingException {
        //生成请求ID
        String request_id = UUID.randomUUID().toString();
        String uri = URLDecoder.decode(request.uri(), chartSet);
        String method = request.method().name();
        List<Map.Entry<String, String>> headers = request.headers().entries();
        logger.info("request:{}, 接收到请求:{}, 请求类型:{}", request_id, uri, method);
        MockLogInfo mockLogInfo=new MockLogInfo();
        try{
            String server_context = NettyServer.properties.getProperty("short.server", "/d/");
            //解析参数
            Map<String,Object> param = getReqContent(request);
            String resp = "";
            //根据uri 匹配数据库中mock数据
            String url=uri.split("\\?")[0];
            if(url.equalsIgnoreCase("/api/short/generator")){
                return shortUrlGenerator(param);
            }else if(url.startsWith(server_context)){
                return shortUrlCallBack(url);
            }
            if(LoadData2Memory.mockDataInfos.containsKey(url)){
                MockDataInfo mockDataInfo= LoadData2Memory.mockDataInfos.get(url);
                String job_id = mockDataInfo.getId();
                mockLogInfo.setTask_logs_id(request_id);
                if(!mockDataInfo.getReq_type().equalsIgnoreCase(method)){
                    mockLogInfo = mockLogInfo(job_id, request_id, "ERROR",StrFormatter.format("request:{}, uri:{}, request method:{}, but allow method:{}", request_id, uri, method, mockDataInfo.getReq_type()));
                    InsertLog2Db.blockingDeque.add(mockLogInfo);
                    logger.error("request:{}, uri:{}, request method:{}, but allow method:{}", request_id, uri, method, mockDataInfo.getReq_type());
                    DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                            HttpVersion.HTTP_1_1,
                            HttpResponseStatus.METHOD_NOT_ALLOWED,
                            Unpooled.wrappedBuffer(resp.getBytes(Charset.forName(mockDataInfo.getResp_encode())))
                    );
                    response.headers().setInt(ContentLength, response.content().readableBytes());

                    return response;
                }

                Callable<DefaultHttpResponse> task = new Callable<DefaultHttpResponse>(){
                    @Override
                    public DefaultHttpResponse call() throws Exception {
                        //返回编码,解析类型, 返回header,返回类型,返回内容
                        Map<String,Object> header=new HashMap<>();
                        String[] headers = mockDataInfo.getHeader().split("\r\n|\n");
                        for (String headerStr:headers){
                            String[] kv = headerStr.split(":",2);
                            if(kv.length!=2){
                                continue;
                            }
                            header.put(kv[0], kv[1]);
                        }

                        header.put("Content-Type", mockDataInfo.getResp_content_type()+";charset:"+mockDataInfo.getResp_encode());

                        logger.info("request:{}, uri:{}, header:{}", request_id, uri, header);

                        MockLogInfo mockLogInfo1 = mockLogInfo(job_id, request_id, "INFO",StrFormatter.format("request:{}, uri:{}, header:{}", request_id, uri, header));
                        InsertLog2Db.blockingDeque.add(mockLogInfo1);
                        String template = StringUtils.isEmpty(mockDataInfo.getResp_context())?"":mockDataInfo.getResp_context();
                        logger.info("request:{}, uri:{}, param:{}", request_id, uri, JSON.toJSONString(param));
                        mockLogInfo1 = mockLogInfo(job_id, request_id, "INFO",StrFormatter.format("request:{}, uri:{}, param:{}", request_id, uri, JSON.toJSONString(param)));
                        InsertLog2Db.blockingDeque.add(mockLogInfo1);
                        //判断是否动态解析,static:静态,dynamics:动态
                        if(mockDataInfo.getResolve_type().equalsIgnoreCase("dynamics")){
                            //jinjava解析模板
                            Jinjava jinjava=new Jinjava();
                            template = jinjava.render(template, param);
                        }
                        String resp = template;
                        logger.info("request:{}, uri:{}, resp:{}", request_id, uri, resp);
                        mockLogInfo1 = mockLogInfo(job_id, request_id, "INFO",StrFormatter.format("request:{}, uri:{}, resp:{}", request_id, uri, resp));
                        InsertLog2Db.blockingDeque.add(mockLogInfo1);
                        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                                HttpVersion.HTTP_1_1,
                                HttpResponseStatus.OK,
                                Unpooled.wrappedBuffer(resp.getBytes(Charset.forName(mockDataInfo.getResp_encode())))
                        );
                        for (String key:header.keySet()){
                            response.headers().set(key, header.get(key));
                        }
                        response.headers().setInt(ContentLength, response.content().readableBytes());
                        return response;
                    }
                };
                Future<DefaultHttpResponse> future = threadpool.submit(task);
                try{
                    return future.get(Long.parseLong(mockDataInfo.getReq_timeout()), TimeUnit.SECONDS);
                }catch (TimeoutException e){
                    logger.error("request:{}, uri:{}, resp:{}", request_id, uri, "请求超时");
                    MockLogInfo mockLogInfo1 = mockLogInfo(job_id, request_id, "ERROR",StrFormatter.format("request:{}, uri:{}, resp:{}", request_id, uri, "请求超时"));
                    InsertLog2Db.blockingDeque.add(mockLogInfo1);
                    DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                            HttpVersion.HTTP_1_1,
                            HttpResponseStatus.REQUEST_TIMEOUT,
                            Unpooled.wrappedBuffer("请求超时".getBytes("utf-8"))
                    );
                    response.headers().setInt(ContentLength, response.content().readableBytes());
                    return response;
                }
            }else{
                logger.error("request:{}, uri:{}, request method:{}, not found uri", request_id, uri, method);
                DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.NOT_FOUND,
                        Unpooled.wrappedBuffer(resp.getBytes())
                );
                response.headers().setInt(ContentLength, response.content().readableBytes());

                return response;
            }
        }catch (Exception e){
            logger.error("request:{}, uri:{}, request method:{}, error:{} ", request_id, uri, method, e.getMessage());
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.EXPECTATION_FAILED,
                    Unpooled.wrappedBuffer(e.getMessage().getBytes())
            );
            response.headers().setInt(ContentLength, response.content().readableBytes());

            return response;
        }

    }

    private HttpResponse shortUrlGenerator(Map<String,Object> param){
        try{
            String path = NettyServer.properties.getProperty("short.path", "./data/short");
            String long_path = NettyServer.properties.getProperty("long.path", "./data/long");
            String host = NettyServer.properties.getProperty("short.host", "http://127.0.0.1:9001");
            String server_context = NettyServer.properties.getProperty("short.server", "/d/");


            String remote_url = param.get("url").toString();
            String use_cache = param.getOrDefault("use_cache", "false").toString();
            String short_url = "";
            if(use_cache.equalsIgnoreCase("true")){
                String cache_short_url = RocksDBUtil.get(long_path, remote_url);
                if(!StringUtils.isEmpty(cache_short_url)){
                    short_url = cache_short_url;
                }
            }

            if(StringUtils.isEmpty(short_url)){
                short_url = server_context+ShortUrlUtil.generateShortLink(remote_url);
            }

            RocksDBUtil.put(path, short_url, remote_url);
            //记录长链接
            RocksDBUtil.put(long_path, remote_url, short_url);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("short_url", host+short_url);
            String resp = jsonObject.toJSONString();
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.wrappedBuffer(resp.getBytes(Charset.forName("utf-8")))
            );
            response.headers().setInt(ContentLength, response.content().readableBytes());
            return response;
        }catch (Exception e){
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.EXPECTATION_FAILED,
                    Unpooled.wrappedBuffer(e.getMessage().getBytes())
            );
            response.headers().setInt(ContentLength, response.content().readableBytes());

            return response;
        }

    }

    private HttpResponse shortUrlCallBack(String url){
        try{
            String path = NettyServer.properties.getProperty("short.path", "./data/short");
            String resp = RocksDBUtil.get(path, url);

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.FOUND
            );
            response.headers().set("Location",resp);
            response.headers().setInt(ContentLength, response.content().readableBytes());
            return response;
        }catch (Exception e){
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.EXPECTATION_FAILED,
                    Unpooled.wrappedBuffer(e.getMessage().getBytes())
            );
            response.headers().setInt(ContentLength, response.content().readableBytes());

            return response;
        }
    }

    public MockLogInfo mockLogInfo(String job_id, String request_id, String level, String msg){
        MockLogInfo mockLogInfo=new MockLogInfo();
        mockLogInfo.setJob_id(job_id);
        mockLogInfo.setTask_logs_id(request_id);
        mockLogInfo.setLevel(level);
        mockLogInfo.setMsg(msg);

        return mockLogInfo;
    }
}
