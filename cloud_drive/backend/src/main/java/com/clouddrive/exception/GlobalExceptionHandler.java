package com.clouddrive.exception;

import com.clouddrive.model.vo.ResultVO;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultVO<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String errorMsg = fieldErrors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResultVO.fail(HttpStatus.BAD_REQUEST.value(), errorMsg);
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultVO<Void> handleBindException(BindException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String errorMsg = fieldErrors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResultVO.fail(HttpStatus.BAD_REQUEST.value(), errorMsg);
    }

    /**
     * 处理认证异常
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResultVO<Void> handleBadCredentialsException(BadCredentialsException e) {
        return ResultVO.fail(HttpStatus.UNAUTHORIZED.value(), "用户名或密码错误");
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultVO<Void> handleBusinessException(BusinessException e) {
        return ResultVO.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理栈溢出错误
     */
    @ExceptionHandler(StackOverflowError.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResultVO<Void> handleStackOverflowError(StackOverflowError e) {
        e.printStackTrace(); // 打印异常堆栈信息
        return ResultVO.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "系统发生栈溢出错误，可能存在循环依赖");
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResultVO<Void> handleException(Exception e) {
        e.printStackTrace(); // 打印异常堆栈信息
        return ResultVO.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器内部错误");
    }
} 