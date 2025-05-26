package com.clouddrive.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultVO<T> {

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 消息
     */
    private String message;

    /**
     * 数据
     */
    private T data;

    /**
     * 成功返回结果
     */
    public static <T> ResultVO<T> success(T data) {
        return new ResultVO<>(200, "success", data);
    }

    /**
     * 成功返回结果，带自定义消息
     */
    public static <T> ResultVO<T> success(String message, T data) {
        return new ResultVO<>(200, message, data);
    }

    /**
     * 失败返回结果
     */
    public static <T> ResultVO<T> fail(String message) {
        return new ResultVO<>(400, message, null);
    }

    /**
     * 失败返回结果，带自定义状态码
     */
    public static <T> ResultVO<T> fail(Integer code, String message) {
        return new ResultVO<>(code, message, null);
    }
} 