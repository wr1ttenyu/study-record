package wr1ttenyu.study.javase;

import org.junit.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class DemoApplicationTests {

    @Test
    public void contextLoads() {
        byte[] bytesbuffer4 = new byte[]{11};
        byte[] bytesEachCache = new byte[]{123};
        System.arraycopy(bytesbuffer4, 0, bytesEachCache, bytesEachCache.length, bytesbuffer4.length);
        System.out.println(bytesEachCache);
    }

    @Test
    public void testIntegerTransmit() {
        Integer test = new Integer(10000);
        handleInteger(test);
        System.out.println(test);
    }

    private void handleInteger(Integer test) {
        test += 1;
        System.out.println(test);
    }

    @Test
    public void testTryFinally() {
        try {
            testTryFinally2();
        } catch (Exception e) {
            System.out.println("收到异常");
        }
        System.out.println("lalala");
    }


    @Test
    public void testHashMap() {
        System.out.println(comparableClassFor(new A()));    // null,A does not implement Comparable.
        System.out.println(comparableClassFor(new B()));    // null,B implements Comparable, compare to Object.
        System.out.println(comparableClassFor(new C()));    // class Demo$C,C implements Comparable, compare to itself.
        System.out.println(comparableClassFor(new D()));    // null,D implements Comparable, compare to its sub type.
        System.out.println(comparableClassFor(new F()));    // null,F is C's sub type.
    }

    static class A {
    }

    static class B implements Comparable<Object> {
        @Override
        public int compareTo(Object o) {
            return 0;
        }
    }

    static class C implements Comparable<C> {
        @Override
        public int compareTo(C o) {
            return 0;
        }

    }

    static class D implements Comparable<E> {
        @Override
        public int compareTo(E o) {
            return 0;
        }
    }

    static class E {
    }

    static class F extends C {
    }

    /**
     * Returns x's Class if it is of the form "class C implements
     * Comparable<C>", else null.
     */
    static Class<?> comparableClassFor(Object x) {
        if (x instanceof Comparable) {
            Class<?> c;
            Type[] ts, as;
            Type t;
            ParameterizedType p;
            if ((c = x.getClass()) == String.class) // bypass checks
                return c;
            if ((ts = c.getGenericInterfaces()) != null) {
                for (int i = 0; i < ts.length; ++i) {
                    if (((t = ts[i]) instanceof ParameterizedType) &&
                            ((p = (ParameterizedType) t).getRawType() ==
                                    Comparable.class) &&
                            (as = p.getActualTypeArguments()) != null &&
                            as.length == 1 && as[0] == c) // type arg is c
                        return c;
                }
            }
        }
        return null;
    }

    private void testTryFinally2() {
        try {
            int i = 1 / 0;
        } /*catch (Exception e) {
            throw e;
        } */ finally {
            /*return 2;*/
        }
    }
}