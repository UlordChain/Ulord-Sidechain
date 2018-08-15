package org.ethereum.rpc.exception;

public class JsonRpcInvalidParamException extends UscJsonRpcRequestException{

    public static final Integer ERROR_CODE = -32602;

    public JsonRpcInvalidParamException(String message, Exception e) {
        super(ERROR_CODE, message, e);
    }

    public JsonRpcInvalidParamException(String message) {
        super(ERROR_CODE, message);
    }
}
