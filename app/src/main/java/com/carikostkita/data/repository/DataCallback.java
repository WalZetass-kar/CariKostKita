package com.carikostkita.data.repository;

public interface DataCallback<T> {
    void onSuccess(T data);
    void onError(String message);
}
