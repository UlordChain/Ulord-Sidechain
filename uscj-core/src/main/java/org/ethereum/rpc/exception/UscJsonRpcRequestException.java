package org.ethereum.rpc.exception;

/**
 * Created by mario on 17/10/2016.
 */
public class UscJsonRpcRequestException extends RuntimeException{

    private final Integer code;

    protected UscJsonRpcRequestException(Integer code, String message, Exception e) {
        super(message, e);
        this.code = code;
    }

    public UscJsonRpcRequestException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

}
