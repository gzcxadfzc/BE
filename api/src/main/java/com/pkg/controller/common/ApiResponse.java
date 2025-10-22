package com.pkg.controller.common;


public class ApiResponse<T> {

    private final ResultType result;
    private final T data;
    private final ApiError error;

    private ApiResponse(ResultType result, T data, ApiError error) {
        this.result = result;
        this.data = data;
        this.error = error;
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(ResultType.SUCCESS, null, null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResultType.SUCCESS, data, null);
    }

    public static ApiResponse<Void> error(ApiError error) {
        return new ApiResponse<>(ResultType.ERROR, null, error);
    }
//
//    public static ApiResponse<Void> error(ApiError error, String errorData) {
//        return new ApiResponse<>(ResultType.ERROR, null, new CoreApiErrorMessage(error, errorData));
//    }
//
//    public static ApiResponse<Void> error(String errorCode, String errorMessage) {
//        return new ApiResponse<>(ResultType.ERROR, null, new CoreApiErrorMessage(errorCode, errorMessage, null));
//    }
//
//    public static ApiResponse<Object> error(String errorCode, String errorMessage, Object errorData) {
//        return new ApiResponse<>(ResultType.ERROR, errorData, new CoreApiErrorMessage(errorCode, errorMessage, errorData));
//    }

    public ResultType getResult() {
        return result;
    }

    public Object getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }

    enum ResultType {
        SUCCESS,
        ERROR,
        ;
    }
}

