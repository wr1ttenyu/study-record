package wr1ttenyu.study.javase;

import org.junit.Test;
import writtenyu.study.javase.bean.Inter1;
import writtenyu.study.javase.bean.Inter2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DubboTest {

    @Test
    public void testNesting() {
        Inter2 last = new Inter2() {
            @Override
            public void test() {
                System.out.println("我是真正的 invoker 不是 filter");
            }
        };
        List<Inter1> inter1List = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            int j = i;
            inter1List.add(new Inter1() {
                @Override
                public void test(Inter2 inter2) {
                    System.out.println("我是 filter：" + j);
                    inter2.test();
                }
            });
        }

        for (int i = 0; i < 5; i++) {
            final Inter2 next = last;
            Inter1 inter1 = inter1List.get(i);
            last = new Inter2() {
                @Override
                public void test() {
                    inter1.test(next);
                }
            };
        }

        last.test();
    }

}
