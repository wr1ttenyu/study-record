package writtenyu.study.javase.juc;

import writtenyu.study.javase.bean.User;

public class DoubleCheck {

    private static volatile User user = null;

    public static void main(String[] args) {

    }

    private User createUser() {

        // User user = new User();
        // 被拆分成两个步骤
        // 1. 在内存中分配空间，将 user 指向内存分配的空间
        // 2. 在分配的内存空间中 实例化 user
        // 这会导致一个问题：
        // 一个线程在执行完 User user = new User(); 但是此时 user 并未实例化
        // 另一个线程进入，此时 user 已经指向一个明确的内存空间
        // 之后 返回的 user 其实并没有被完全实例化 导致后续程序运行出现意外

        // 而 volatile 可以保证 User user = new User(); 被编译为
        // 1. 在内存中分配空间，在分配的内存空间中 实例化 user
        // 2. 将 user 指向内存分配的空间

        if(user == null) {
            synchronized (DoubleCheck.class) {
                if(user == null) {
                    user = new User();
                }
            }
        }
        return user;
    }

}
