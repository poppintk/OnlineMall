package com.atguigu.common.exception;

public enum BizCodeEnum {

    SMS_CODE_EXCEPTION(002,"验证码获取频率太高请稍后再试"),
    UNKNOW_EXCEPTION(10000, "系统未知异常"),
    VALID_EXCEPTION(10001, "参数格式效验失败"),
    PRODUCT_UP_EXCEPTION(11000, "商品上架异常");

    private int code;
    private String msg;
    BizCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
