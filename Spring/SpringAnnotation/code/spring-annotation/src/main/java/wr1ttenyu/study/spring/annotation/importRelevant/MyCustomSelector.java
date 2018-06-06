package wr1ttenyu.study.spring.annotation.importRelevant;

import java.util.Set;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

public class MyCustomSelector implements ImportSelector {

    /**
     * 返回需要注册的类的全类名数组 
     * AnnotationMetadata: 标注有@Import的类的所有注解信息
     */
    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        // TODO Auto-generated method stub
        Set<String> annotationTypes = importingClassMetadata.getAnnotationTypes();
        annotationTypes.forEach(annotationType -> System.out
                .println("class MyCustomSelector out: annotationType -->" + annotationType.getClass()));
        // 返回null值报错
        return new String[] {"wr1ttenyu.study.spring.annotation.bean.Car"};
    }

}
