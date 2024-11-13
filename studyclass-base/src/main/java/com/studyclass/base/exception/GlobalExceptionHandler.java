package com.studyclass.base.exception;

import io.swagger.annotations.ResponseHeader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

/**
 * 异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 项目自定义异常
     * @param e
     * @return
     */
    @ExceptionHandler(StudyClassException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse customException(StudyClassException e){
        log.error("系统异常:{}",e.getMessage(),e);
        //解析异常信息
        String message = e.getMessage();
        RestErrorResponse restErrorResponse = new RestErrorResponse(message);
        return restErrorResponse;
    }

    /**
     * 其他异常
     * @param e
     * @return
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse customException(Exception e){
        log.error("系统异常:{}",e.getMessage(),e);
        if (e.getMessage().equals("不允许访问")){
            return new RestErrorResponse("没有操作此功能的权限");
        }
        //解析异常信息
        RestErrorResponse restErrorResponse = new RestErrorResponse(CommonError.UNKOWN_ERROR.getErrMessage());
        return restErrorResponse;
    }
    /**
     * 其他异常
     * @param e
     * @return
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse methodArgumentNoValidException(MethodArgumentNotValidException e){
        BindingResult bindingResult = e.getBindingResult();
        List<String> errosList = new ArrayList<>();
        bindingResult.getFieldErrors().stream().forEach(item->{
            errosList.add(item.getDefaultMessage());
        });
        //将list信息拼接
        String errMessage = StringUtils.join(errosList, ",");
        log.error("系统异常:{}",e.getMessage(),errMessage);
        //解析异常信息
        RestErrorResponse restErrorResponse = new RestErrorResponse(errMessage);
        return restErrorResponse;
    }
}
