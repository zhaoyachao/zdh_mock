package com.zyc.entity;

import java.sql.Timestamp;

public class MockDataInfo {

    private String id;

    private String wemock_context;

    /**
     * 请求类型
     */
    private String req_type;

    /**
     * url
     */
    private String url;

    /**
     * 解析类型,0:静态，1:动态
     */
    private String resolve_type;

    /**
     * 账号
     */
    private String owner;

    /**
     * 是否删除,0:未删除,1:删除
     */
    private String is_delete;

    /**
     * 创建时间
     */
    private Timestamp create_time;

    /**
     * 更新时间
     */
    private Timestamp update_time;

    /**
     * 产品code
     */
    private String product_code;

    /**
     * mock tree id
     */
    private String mock_tree_id;

    /**
     * 响应信息
     */
    private String header;

    /**
     * 内容
     */
    private String resp_context;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWemock_context() {
        return wemock_context;
    }

    public void setWemock_context(String wemock_context) {
        this.wemock_context = wemock_context;
    }

    public String getReq_type() {
        return req_type;
    }

    public void setReq_type(String req_type) {
        this.req_type = req_type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getResolve_type() {
        return resolve_type;
    }

    public void setResolve_type(String resolve_type) {
        this.resolve_type = resolve_type;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getIs_delete() {
        return is_delete;
    }

    public void setIs_delete(String is_delete) {
        this.is_delete = is_delete;
    }

    public Timestamp getCreate_time() {
        return create_time;
    }

    public void setCreate_time(Timestamp create_time) {
        this.create_time = create_time;
    }

    public Timestamp getUpdate_time() {
        return update_time;
    }

    public void setUpdate_time(Timestamp update_time) {
        this.update_time = update_time;
    }

    public String getProduct_code() {
        return product_code;
    }

    public void setProduct_code(String product_code) {
        this.product_code = product_code;
    }

    public String getMock_tree_id() {
        return mock_tree_id;
    }

    public void setMock_tree_id(String mock_tree_id) {
        this.mock_tree_id = mock_tree_id;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getResp_context() {
        return resp_context;
    }

    public void setResp_context(String resp_context) {
        this.resp_context = resp_context;
    }
}
