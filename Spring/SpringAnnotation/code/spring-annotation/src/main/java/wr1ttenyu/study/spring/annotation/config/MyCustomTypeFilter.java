package wr1ttenyu.study.spring.annotation.config;

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

public class MyCustomTypeFilter implements TypeFilter {

    @Override
    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
            throws IOException {
        // 获取当前类的注解信息
        metadataReader.getAnnotationMetadata();
        // 获取当前正在扫描的类的类信息
        ClassMetadata classMetadata = metadataReader.getClassMetadata();
        // 获取当前类资源（类文件路径）
        Resource resource = metadataReader.getResource();
        System.out.println(resource);
        String targetClassName = classMetadata.getClassName();
        System.out.println("targetClassName -->" + targetClassName);
        return false;
    }

}
