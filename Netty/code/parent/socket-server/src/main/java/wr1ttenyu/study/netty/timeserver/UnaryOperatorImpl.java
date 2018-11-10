package wr1ttenyu.study.netty.timeserver;

import java.util.function.UnaryOperator;

public class UnaryOperatorImpl implements UnaryOperator<String> {

    @Override
    public String apply(String s) {
        return s;
    }
}

