package wr1ttenyu.study.spring.annotation.bean;

import org.springframework.beans.factory.FactoryBean;

public class ColorFactoryBean implements FactoryBean<Color>{

    @Override
    public Color getObject() throws Exception {
        return new Color();
    }

    @Override
    public Class<?> getObjectType() {
        return Color.class;
    }
    
    // 是否单实例
    @Override
    public boolean isSingleton() {
        return true;
    }

}
