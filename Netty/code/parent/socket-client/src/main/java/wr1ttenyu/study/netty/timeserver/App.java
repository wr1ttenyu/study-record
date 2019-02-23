package wr1ttenyu.study.netty.timeserver;

import java.util.ArrayList;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        System.out.println("Hello World!");
        ArrayList test = new ArrayList();
        test.add("123");
        test.add("123");
        String[] a = new String[2];
        test.toArray(a);
        for (int i = 0; i < a.length; i++) {
            System.out.println(a.length);
            String s = a[i];
            System.out.println(s);
        }

        ArrayList test2 = null;

            test2.get(0);
        /*try {
        } catch (Exception e) {
            System.out.println(e);
            System.out.println(e.getStackTrace());
        }*/


    }
}
