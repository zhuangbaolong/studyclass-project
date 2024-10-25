package com.studyclass.base.exception;

import java.io.Serializable;

/**
 * 返回的异常信息类
 */
public class RestErrorResponse implements Serializable {

    //约定返回的属性
    private String errMessage;

    public RestErrorResponse(String errMessage){
        this.errMessage= errMessage;
    }

    public String getErrMessage() {
        return errMessage;
    }

    public void setErrMessage(String errMessage) {
        this.errMessage = errMessage;
    }
}
