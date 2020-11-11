package com.fiberhome.dbserver.common.exception;

/**
 * The base class of all exceptions
 *
 * @author xiajunsheng
 * @date 2020/10/28
 * @since 1.0.0
 */
public class DBException extends Exception
{
    private static final long serialVersionUID = 6646883591588721475L;

    /**
     * 错误码
     */
    private DBServerExceptionCode errorCode;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 构造函数
     */
    public DBException()
    {
        super();
    }

    /**
     * 构造函数
     *
     * @param message 异常信息
     */
    public DBException(String message)
    {
        super(message);
    }

    /**
     * 构造函数
     *
     * @param cause 可抛出异常
     */
    public DBException(Throwable cause)
    {
        super(cause);
    }

    /**
     * 构造函数
     *
     * @param message 异常信息
     * @param cause   可抛出异常
     */
    public DBException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误码，不能为空。
     * @param message 异常信息
     */
    public DBException(DBServerExceptionCode errorCode, String message)
    {
        if (errorCode == null)
        {
            throw new IllegalArgumentException("errorCode is null");
        }
        this.errorCode = errorCode;
        this.errorMsg = message;
    }

    /**
     * 构造函数
     *
     * @param errorCode 异常码
     * @param message 异常信息
     * @param cause   可抛出异常
     */
    public DBException(DBServerExceptionCode errorCode, String message, Throwable cause)
    {
        super(message, cause);
    }

    public DBServerExceptionCode getErrorCode()
    {
        return errorCode;
    }

    public String getErrorMsg()
    {
        return errorMsg;
    }

    @Override
    public String toString()
    {
        return "DBException{" + "errorCode=" + errorCode + ", errorMsg='" + errorMsg + '\'' + '}';
    }
}
