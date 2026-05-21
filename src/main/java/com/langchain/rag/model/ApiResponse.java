package com.langchain.rag.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse(
        boolean success,
        String message,
        String error
) {

    public static ApiResponse ok(String message){
        return new ApiResponse(true, message, null);
    }

    public static ApiResponse error(String error){
        return new ApiResponse(false, null, error);
    }
}
