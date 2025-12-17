package com.zyc.mock.entity;

public class ShortInfo {
    public static String SHORT_TYPE_CONTENT = "content";
    public static String SHORT_TYPE_URL = "url";
    private String short_url;
    private String short_type;// 短链类型, content: 内容短链, url: URL短链
    private String content; // 内容
    private long create_time; // 创建时间
    private long update_time; // 更新时间

    public String getShort_url() {
        return short_url;
    }

    public void setShort_url(String short_url) {
        this.short_url = short_url;
    }

    public String getShort_type() {
        return short_type;
    }

    public void setShort_type(String short_type) {
        this.short_type = short_type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getCreate_time() {
        return create_time;
    }

    public void setCreate_time(long create_time) {
        this.create_time = create_time;
    }

    public long getUpdate_time() {
        return update_time;
    }

    public void setUpdate_time(long update_time) {
        this.update_time = update_time;
    }
}
