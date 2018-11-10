package wr1ttenyu.study.spring.springboot.bean;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Email;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * {@link ConfigurationProperties} 将配置文件的值，映射到该类当中
 * {@link ConfigurationProperties#prefix()} 指定配置文件中哪个属性下面的值进行映射
 */
@Component
/*@Validated*/
@PropertySource(value = {"classpath:person.properties"})
@ConfigurationProperties(prefix = "person")
public class Person {

    /*@Email*/
    private String lastName;
    private Integer age;
    private Boolean boss;
    private Date birth;

    private Map<String, String> mapInfo;
    private List<String> lstInfo;
    private Dog dog;

    @Override
    public String toString() {
        return "Person{" +
                "lastName='" + lastName + '\'' +
                ", age=" + age +
                ", boss=" + boss +
                ", birth=" + birth +
                ", mapInfo=" + mapInfo +
                ", lstInfo=" + lstInfo +
                ", dog=" + dog +
                '}';
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Boolean getBoss() {
        return boss;
    }

    public void setBoss(Boolean boss) {
        this.boss = boss;
    }

    public Date getBirth() {
        return birth;
    }

    public void setBirth(Date birth) {
        this.birth = birth;
    }

    public Map<String, String> getMapInfo() {
        return mapInfo;
    }

    public void setMapInfo(Map<String, String> mapInfo) {
        this.mapInfo = mapInfo;
    }

    public List<String> getLstInfo() {
        return lstInfo;
    }

    public void setLstInfo(List<String> lstInfo) {
        this.lstInfo = lstInfo;
    }

    public Dog getDog() {
        return dog;
    }

    public void setDog(Dog dog) {
        this.dog = dog;
    }
}
