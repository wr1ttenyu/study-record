package writtenyu.study.javase.enumstudy;

/**
 * 
 * @author wr1ttenyu
 *
 */
public class EnumCase {

    public static void main(String[] args) {
        TrafficLight.change(ColorEnum.YELLOW);

        ColorWithMethodEnum[] colorWithMethods = ColorWithMethodEnum.values();
        for (ColorWithMethodEnum colorWithMethod : colorWithMethods) {
            System.out.println("color name -> " + colorWithMethod.getName());
            System.out.println("color index -> " + colorWithMethod.getIndex());
            System.out.println("color ordinal -> " + colorWithMethod.ordinal());
        }

        for (Food.DessertEnum dessertEnum : Food.DessertEnum.values()) {
            System.out.print(dessertEnum + "  ");
        }
        System.out.println();
        // 搞个实现接口，来组织枚举，简单讲，就是分类吧。如果大量使用枚举的话，这么干，在写代码的时候，就很方便调用啦。
        // 还有就是个“多态”的功能吧，
        Food food = Food.DessertEnum.CAKE;
        System.out.println(food);
        food = Food.CoffeeEnum.BLACK_COFFEE;
        System.out.println(food);
    }
}

// 用法一
// 作为常量使用，可用于switch
enum ColorEnum {
    RED, GREEN, YELLOW
}

// 用法二
// JDK1.6之前的switch语句只支持int,char,enum类型
// 使用枚举，能让我们的代码可读性更强。
class TrafficLight {
    public static void change(ColorEnum color) {
        switch (color) {
        case RED:
            System.out.println("Stop!");
            break;
        case YELLOW:
            System.out.println("Slow!");
            break;
        case GREEN:
            System.out.println("GoGo!");
            break;
        default:
            System.out.println(4);
        }
    }
}

// 用法三
// 向枚举中添加新方法
// 如果打算自定义自己的方法，那么必须在enum实例序列的最后添加一个分号。而且 Java 要求必须先定义 enum 实例。
enum ColorWithMethodEnum {
    RED("红色", 1), GREEN("绿色", 2), BLANK("白色", 3), YELLO("黄色", 4);
    // 成员变量
    private String name;
    private int index;

    // 构造方法
    private ColorWithMethodEnum(String name, int index) {
        this.name = name;
        this.index = index;
    }

    // 普通方法
    public static String getName(int index) {
        for (ColorWithMethodEnum c : ColorWithMethodEnum.values()) {
            if (c.getIndex() == index) {
                return c.name;
            }
        }
        return null;
    }

    // get set 方法
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}

// 用法四
// 覆盖枚举的方法
enum ColorOfCoverMethodEnum {
    RED("红色", 1), GREEN("绿色", 2), BLANK("白色", 3), YELLO("黄色", 4);
    // 成员变量
    private String name;
    private int index;

    // 构造方法
    private ColorOfCoverMethodEnum(String name, int index) {
        this.name = name;
        this.index = index;
    }

    // 覆盖方法
    @Override
    public String toString() {
        return this.index + "_" + this.name;
    }
}

// 用法五
// 实现接口
// 所有的枚举都继承自java.lang.Enum类。由于Java 不支持多继承，所以枚举对象不能再继承其他类。
interface Behaviour {
    void print();

    String getInfo();
}

enum ColorImpInterfaceEnum implements Behaviour {
    RED("红色", 1), GREEN("绿色", 2), BLANK("白色", 3), YELLO("黄色", 4);
    // 成员变量
    private String name;
    private int index;

    // 构造方法
    private ColorImpInterfaceEnum(String name, int index) {
        this.name = name;
        this.index = index;
    }

    // 接口方法
    @Override
    public String getInfo() {
        return this.name;
    }

    // 接口方法
    @Override
    public void print() {
        System.out.println(this.index + ":" + this.name);
    }
}

// 用法六
// 使用接口组织枚举
interface Food {
    enum CoffeeEnum implements Food {
        BLACK_COFFEE, DECAF_COFFEE, LATTE, CAPPUCCINO
    }

    enum DessertEnum implements Food {
        FRUIT, CAKE, GELATO
    }
}

// 用法七
// 关于枚举集合的使用
// java.util.EnumSet和java.util.EnumMap是两个枚举集合。EnumSet保证集合中的元素不重复；EnumMap中的
// key是enum类型，而value则可以是任意类型。关于这个两个集合的使用就不在这里赘述，可以参考JDK文档。
// 关于枚举的实现细节和原理请参考：
// 参考资料：《ThinkingInJava》第四版