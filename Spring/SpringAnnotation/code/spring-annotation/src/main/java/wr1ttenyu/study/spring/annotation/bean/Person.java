package wr1ttenyu.study.spring.annotation.bean;

import org.springframework.beans.factory.annotation.Value;

public class Person {

    /**
     * 可以使用@Value来给属性赋值
     * 1. 基本数值
     * 2. 可以写Spel,#{}
     * 3. 可以写${},取出配置文件中的值(运行环境中保存的变量的值)
     */
    
    @Value("wr1tenyu")
	private String name;

    @Value("#{20 - 2}")
	private Integer age;
    
    @Value("${person.sex}")
    private String sex;

	public Person() {
        super();
    }

    public Person(String name, Integer age) {
        super();
        this.name = name;
        this.age = age;
    }

    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}
	
    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    @Override
    public String toString() {
        return "Person [name=" + name + ", age=" + age + ", sex=" + sex + "]";
    }
}