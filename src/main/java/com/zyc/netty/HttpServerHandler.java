package com.zyc.netty;


import com.alibaba.fastjson.JSON;
import com.hubspot.jinjava.Jinjava;
import com.zyc.entity.MockDataInfo;
import com.zyc.schedule.LoadData2Memory;
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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
            return getBody(request.content().toString(CharsetUtil.UTF_8));
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
        String uri = request.uri();
        String method = request.method().name();
        logger.info("request:{}, 接收到请求:{}, 请求类型:{}", request_id, uri, method);
        try{
            //解析参数
            Map<String,Object> param = getReqContent(request);
            String resp = "";
            //根据uri 匹配数据库中mock数据
            String url=uri.split("\\?")[0];
            if(LoadData2Memory.mockDataInfos.containsKey(url)){
                MockDataInfo mockDataInfo= LoadData2Memory.mockDataInfos.get(url);
                if(!mockDataInfo.getReq_type().equalsIgnoreCase(method)){
                    logger.error("request:{}, uri:{}, request method:{}, but allow method:{}", request_id, uri, method, mockDataInfo.getReq_type());
                    DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                            HttpVersion.HTTP_1_1,
                            HttpResponseStatus.METHOD_NOT_ALLOWED,
                            Unpooled.wrappedBuffer(resp.getBytes(Charset.forName("utf-8")))
                    );
                    response.headers().setInt(ContentLength, response.content().readableBytes());

                    return response;
                }

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
                logger.info("request:{}, uri:{}, header:{}", request_id, uri, mockDataInfo.getHeader());
                String template = StringUtils.isEmpty(mockDataInfo.getResp_context())?"":mockDataInfo.getResp_context();
                logger.info("request:{}, uri:{}, param:{}", request_id, uri, JSON.toJSONString(param));
                //判断是否动态解析,static:静态,dynamics:动态
                if(mockDataInfo.getResolve_type().equalsIgnoreCase("dynamics")){
                    //jinjava解析模板
                    Jinjava jinjava=new Jinjava();
                    template = jinjava.render(template, param);
                }
                resp = template;
                logger.info("request:{}, uri:{}, resp:{}", request_id, uri, resp);
                DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK,
                        Unpooled.wrappedBuffer(resp.getBytes(Charset.forName("utf-8")))
                );
                for (String key:header.keySet()){
                    response.headers().set(key, header.get(key));
                }
                response.headers().setInt(ContentLength, response.content().readableBytes());

                return response;
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
}
