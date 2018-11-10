package wr1ttenyu.study.springboot.demo.curd.component;

public enum RedisdbNode {

    DB0(0),
    DB1(1),
    DB2(2),
    DB3(3),
    DB4(4),
    DB5(5),
    DB6(6),
    DB7(7),
    DB8(8),
    DB9(9),
    DB10(10),
    DB11(11),
    DB12(12),
    DB13(13),
    DB14(14),
    DB15(15);

    private int node;

    private RedisdbNode(int node) {
        this.node = node;
    }

    public int getNode() {
        return this.node;
    }
}
