# 一、Spring Boot 入门

## 1. Spring Boot 简介

> 简化Spring应用开发的一个框架；
>
> 整个Spring技术栈的一个大整合；
>
> J2EE开发的一站式解决方案



## 2. 微服务

2014，martin fowler

微服务：架构风格（服务微化）

一个应用应该是一组小型服务；可以通过HTTP的方式进行互通；

每一个功能元素最终都是一个可独立替换和独立升级的软件单元；

[微服务概念详细参照](https://martinfowler.com/articles/microservices.html)



## 3. Spring Boot HelloWorld

[spring boot Demo 骨架生成](http://start.spring.io/)

1. 添加controller（主应用程序包及子包下面）

2. 启动主应用程序

3. 可执行jar包，打包方式

   Create an Executable JAR with Maven

   The `spring-boot-maven-plugin` can be used to create an executable “fat” JAR. If you use the `spring-boot-starter-parent` POM, you can declare the plugin and your jars are repackaged as follows:

```
<build>
	<plugins>
		<plugin>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-maven-plugin</artifactId>
		</plugin>
	</plugins>
</build>
```