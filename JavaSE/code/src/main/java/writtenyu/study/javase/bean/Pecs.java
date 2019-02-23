package writtenyu.study.javase.bean;

import java.util.ArrayList;
import java.util.List;

public class Pecs<T extends Object> {

    List<Object> test = new ArrayList<>();

    public void push(T e) {
        test.add(e);
    }

    public void pushAll(Iterable<T> src){
        for(T e : src)
            push(e);
    }

}
