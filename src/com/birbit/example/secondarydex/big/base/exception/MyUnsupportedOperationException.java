package com.birbit.example.secondarydex.big.base.exception;

public class MyUnsupportedOperationException extends RuntimeException {
    public MyUnsupportedOperationException(Throwable t)  {
        super(t.getMessage());
    }
}
