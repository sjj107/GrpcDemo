package com.fiberhome.dbserver.common.exception;


/**
 * 常用异常码
 *
 * @author xiajunsheng
 * @date 2020/10/28
 * @since 1.0.0
 */
public enum DBServerExceptionCode
{
    /**
     * 参数错误
     */
    INVALID_PARAMETER,

    /**
     * 非法操作
     */
    INVALID_OPERATION,

    /**
     * 不支持的操作
     */
    UNSUPPORTED_OPERATION,

    /**
     * 解析错误
     */
    PARSER_EXCEPTION,

    /**
     * 网络异常
     */
    NETWORK_EXCEPTION,

    /**
     * IO异常
     */
    IO_EXCEPTION,

    /**
     * 远端异常
     */
    REMOTE_EXCEPTION,

    /**
     * 内部异常
     */
    INTERNAL_EXCEPTION
}
