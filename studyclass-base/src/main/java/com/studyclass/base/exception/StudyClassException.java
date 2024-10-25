package com.studyclass.base.exception;

/**
 * 自定义异常类型
 */
public class StudyClassException extends RuntimeException{

    private String errMessage;

    public StudyClassException() {
        super();
    }

    public StudyClassException(String message) {
        super(message);
        this.errMessage = message;
    }

    public static void cast(String message){
        throw new StudyClassException(message);
    }

}
