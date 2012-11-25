package com.birbit.example.secondarydex.big.base.exception;

public class MyIllegalArgumentException extends RuntimeException {
    public MyIllegalArgumentException(Throwable t)  {
        super(t.getMessage());
    }
}
