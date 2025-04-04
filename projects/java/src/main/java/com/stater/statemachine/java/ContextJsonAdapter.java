package com.stater.statemachine.java;

public interface ContextJsonAdapter<C extends Context> {
    String toJson(C context);

    C fromJson(String json);
}
