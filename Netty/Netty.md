# Netty
## 第一章 - Java I/O 演进之路

### 1.1 -- I/O 基础入门

---

java 1.4之前，java I/O 缺陷：

1.  没有数据缓冲区
2.  没有Channel概念
3.  同步阻塞式 I/O
4.  支持的字符集有限，可移植性不好

####1.1.1 --- Linux 网络 I/O 模型简介

​	Linux 的内核将所有外部设备都看做一个文件来操作，对一个文件的读写操作会调用内核提供的系统命令，返回一个file descriptor。对 socket 的读写同样会有一个 socketfd (socket file descriptor)， 描述符就是一个数字，它指向内核中的一个结构体 (文件路径，数据区等一些属性)。

**UNIX 五种 I/O 模型**

1. 阻塞 I/O 模型：进程从调用 recvfrom 开始到它返回的整段时间内都是被阻塞的。

   ![阻塞IO模型](img\阻塞IO模型.png)

2. 非阻塞 I/O 模型：recvfrom 对应的缓冲区如果没有数据的话，就直接返回一个错误，后续进行轮询检查这个状态，看内核是不是有数据到来。

   ![非阻塞IO模型](img\非阻塞IO模型.png)

3. I/O 复用模型：提供select/poll，进程将一个或多个 fd 传递给 select 或 poll , 阻塞在 select 操作上， select 帮我们顺序扫描多个 fd 是否就绪，fd 的数量有限，因此它的使用受到了制约。还提供 epoll , 它基于事件驱动代替轮询扫描，因此性能更高。有 fd 就绪时，就回调 rollback。

   ![IO复用模型](img\IO复用模型.png)

4. 信号驱动 I/O 模型：首先开启套接信号驱动 I/O 功能，并通过系统调用 sigaction 执行一个信号处理函数（此系统调用立即返回，进程继续工作，是非阻塞的）。当数据准备就绪时，为该进程发送一个信号，通知该进程来读取和处理数据。

   ![信号驱动IO模型](img\信号驱动IO模型.png)

5. 异步 I/O：告知内核启动某个操作，并让内核在完成操作后告知我们。与信号驱动 I/O 的区别在于：异步 I/O 会将数据从内核复制到用户自己的缓冲区，相当于是告知我们 I/O 操作已经完成，而信号驱动 I/O 是告知我们何时可以去内核读取数据，需要自己把数据从内核复制到自己的缓冲区。

![异步IO模型](img\异步IO模型.png)

#### 1.1.2 I/O 多路复用技术

​	即 1.1.1 章节中描述的 I/O 复用模型，Linux 使用 epoll 来代替 select ，相比于 select 来说，其改进如下：

1. 支持一个进程打开的socket描述符（FD）不受限制（仅受限于操作系统的最大文件句柄数）。
2. I/O 效率不会随着 FD 数据的增加而线性下降，因为 epoll , 它基于事件驱动代替轮询扫描，因此性能更高。
3. 使用 mmap 加速内核与用户空间的消息传递。（epoll 是通过内核和用户空间 mmap 同用一块内存来实现的，避免了不必要的内存复制）
4. epoll 的 API 更加简单。



## 第二章 NIO 入门

### 2.1 传统的 BIO 编程

---

​	在传统的同步阻塞模型开发中，ServerSocket 负责绑定 IP 地址，启动监听端口；Socket 负责发起连接操作。连接成功后，双方通过输入和输出流进行同步阻塞试通信。

#### 2.1.1 BIO 通信模型图

![同步阻塞服务端通信IO模型](img\同步阻塞服务端通信IO模型.png)

Acceptor 线程负责监听客户端的连接，然后创建一个新的线程来进行链路处理，处理完成后，线程销毁。

**最大缺陷：**缺乏弹性伸缩能力，请求量过大之后，服务端线程数量激增，系统性能急剧下降。

####2.1.2 同步阻塞式 I/O 创建的 TimeServer 源码分析

> 见代码 
>
> wr1ttenyu.study.netty.blockio.demo.TimeServer
>
> wr1ttenyu.study.netty.blockio.TimeSocketClient



### 2.2 伪异步 I/O 编程

---

####2.2.1 伪异步 I/O 模型图

​	采用线程池和任务队列来实现

![伪异步IO通信模型图](img\伪异步IO通信模型图.png)

#### 2.2.2 源码分析

> 见代码 
>
> wr1ttenyu.study.netty.blockio.demo.PseudoAsynchronousTimeServer

#### 2.2.3 伪异步 I/O 弊端分析

​	首先看 java 同步 I/O 的 API 说明

```java
/**
  * Reads some number of bytes from the input stream and stores them into
  * the buffer array <code>b</code>. The number of bytes actually read is
  * returned as an integer. This method blocks until input data is
  * available, end of file is detected, or an exception is thrown.
  * 阻塞 直到 data is available, end of file is detected, or an exception is thrown
  */
public int read(byte b[]) throws IOException {
    return read(b, 0, b.length);
}
```

```java
/**
  * 阻塞 直到 所有要发送的字节全部写入完毕，或者发生异常。
  * TCP/IP 中，当消息的接收方处理缓慢时，将不能及时地从TCP缓冲区读取数据
  * 将导致发送方的 TCP window size 不断减小，直到为0，这时在同步阻塞I/O下
  * write 操作将会被无限期阻塞 直到TCP window size 大于0 或者 出现异常
  */
public void write(byte b[]) throws IOException {
    write(b, 0, b.length);
}
```

在这种情况下，伪异步 I/O 弊端有如下（包含但不限于）：

1. 接收方数据处理缓慢，将导致服务端处理缓慢

2. 服务端读取故障节点，输入流阻塞，正在处理的线程也会阻塞

3. 所有线程都被故障节点阻塞，导致后续所有请求都阻塞

4. 采用阻塞队列，队列积满之后，后续入队列操作将被阻塞

5. 由于前段只有一个 Acceptor 线程接收客户端请求，他被阻塞在同步队列之外，新的客户端请求消息将被拒绝，客户端产生大量连接超时。

6. 由于几乎所有的连接都超时，调用者会认为系统已经崩溃，无法接收新的请求。


###2.3 NIO 编程

---

​	与 Socket 类和 ServerSocket 相对应，NIO 提供了 SocketChannel 和 ServerSocketChannel。这两种通道都支持阻塞和非阻塞两种模式。

#### 2.3.1 NIO 类库简介

1.  `缓冲区 Buffer`

   缓冲区包含一些要写入或者要读出的数据。在 NIO 类库中加入 Buffer，体现了与原 I/O 的一个重要区别。NIO 可以是面向块的 I/O，而 BIO 只能是面向流的。

   **在 NIO 中所有数据都是用缓冲区处理的。任何时候都是通过缓冲区来进行数据操作。**

   缓冲区不仅仅是一个数组，来存储数据，还提供了对数据的结构化访问以及维护读写位置等信息。

![Buffer继承关系图](img\Buffer继承关系图.png)

​	每一个 Buffer 类都是 Buffer 接口的一个子实例。除了 ByteBuffer，每一个 Buffer 类都有完全一样的操作，因为大多数标准 I/O 操作都使用 ByteBuffer，所以他在具有一般的缓冲区操作之外，还提供了一些特殊的操作，以方便网络读写。

2. `通道 Channel`

   通过与流的不同之处在于通道是双向的，而流只是一个方向移动的，而通道可以用于读、写或者二者同时进行。

   UNIX 网络编程模型中，底层操作系统的通道也是全双工的，因此 Channel 可以更好的映射底层操作系统的 API。

![Channel继承关系图](img\Channel继承关系图.png)

​	Channel 可以分为两大类：用于网络读写的 SelectableChannel 和 用于文件操作的 FileChannel。

3. `多路复用器 Selector`

   它是 java NIO 编程的基础，Selector 会不断的轮询注册在其上的 Channel，如果某个 Channel 上面发生的读或者写事件已经完成了，这个 Channel 就会处于就绪状态，会被 Selector 轮询出来，然后通过 SelectionKey 可以获取这个就绪的 Channel 的集合，进行后续的 I/O 操作。

   JDK 使用 epoll() 来代替传统的 select 实现，其好处可见  `1.1.2 I/O 多路复用技术` 小节

#### 2.3.2 NIO 服务端序列图

![NIO服务端通信序列图](img\NIO服务端通信序列图.png)

#### 2.3.3 NIO 创建的 TimeServer 源码分析

> 见代码 wr1ttenyu.study.netty.nio.demo.NIOTimeServer

#### 2.3.4 NIO 客户端序列图

![NIO客户端创建序列图](img\NIO客户端创建序列图.png)

####2.3.5 NIO 创建的 TimeClient 源码分析

> 见代码 wr1ttenyu.study.netty.nio.NIOTimeClien



### 2.4 AIO 编程

---

JDK1.7 升级了 NIO 类库，升级后的类库被称为 NIO2.0。

NIO2.0 引入了新的异步通道的概念，并提供了异步文件通道和异步套接字通道的实现。

异步通道提供两种方式获取操作结果：

- 通过 `java.util.concurrent.Future` 类来表示异步操作结果
- 在执行异步操作的时候传入一个`java.nio.channels`

`CompletionHandler`接口的实现类作为操作完成的回调

#### 2.4.1 AIO 创建的 TimeServer 源码分析

> 见代码 wr1ttenyu.study.netty.aio.demo.AIOTimeServer

#### 2.4.2 AIO 创建的 TimeClient 源码分析

> 见代码 wr1ttenyu.study.netty.aio.AIOTimeClient



### 2.5 4种 I/O 的对比

---

#### 2.5.1 概念澄清

1. 异步非阻塞 I/O

   非阻塞：我的理解是在去内核读取数据时（数据可能是通过网络或读取本地文件而来），在数据没有到达内核之前，读取操作不会一直阻塞在那里等待数据准备就绪，而是直接返回一个无数据的标志位，后续通过轮询或事件通知或其他方式在数据准备好之后获取。

   异步：我的理解是我们的程序不用关心数据有没有准备好，而且数据在内核中准备好之后，我们也不用操心去读取，而是指定一个回调，在内核数据准备好之后，操作系统回去调用我们回调函数。

2. 多路复用器

   Selector	多数中文书都翻译成 `选择器`，Selector 的核心是通过轮询注册在其上的 Channel，当发现有准备就绪的一个或多个 Channel，就返回 Channel 的选择键集合，进行后续 I/O 操作。

3. 伪异步 I/O

   当连接建立成功后，后续的业务操作放入的线程池中进行操作，用于解决一连接一线程的问题，转而用线程池和队列的方式，达到一个或多个线程处理N个请求的目的。

#### 2.5.2 不同 I/O 模型的对比

![不同IO模型对比(阻塞)](img\不同IO模型对比(阻塞).png)

![不同IO模型对比(同步)](img\不同IO模型对比(同步).png)

I/O 类型的阻塞是指：在内核准备数据的过程中，读取线程也一直同步阻塞直到数据准备完毕

I/O 类型的同步是指：在内核数据准备好之后，需要我们去同步数据（同步数据也就是读取数据），而异步则是操作系统回调我们指定的函数，来完成数据操作。



### 2.6 选择 Netty 的理由

---

​	从可维护性的角度来看，由于NIO采用了异步非阻塞编程模型，而且是一个 I/O 线程处理多条链路，他的调试和跟踪非常麻烦，生产环境问题中的问题，无法进行有效的调试和跟踪，往往只能靠一些日志来辅助分析，定位难度很大。

#### 2.6.1 不选择 Java原生 NIO 编程的原因

1.  NIO 类库和 API 繁杂，使用麻烦
2. 需要具备其他的额外既能做铺垫，例如熟悉 Java 多线程编程。这是因为 NIO 编程设计到 Reactor 模式，你必须对多线程和网络编程非常熟悉，才能编写出高质量的 NIO 程序。
3. 可靠性能力补齐，工作量和难度都非常大。例如客户端闪断重连，网络闪断等
4. JDK NIO 的 BUG，例如臭名昭著的 epoll bug，他会导致 Selector 空轮询，最终导致 CPU 100%。

#### 2.6.2 为什么选择 Netty

​	Netty 是业界最流行的 NIO 框架之一，他的健壮性，功能，性能，可定制性和可扩展性在同类框架中都是首屈一指的。

​	其优点总结如下：

- API 使用简单，开发门槛低
- 功能强大，预置了多种编解码功能，支持多种主流协议
- 定制能力强，可以通过 ChannelHandler 对通信框架进行灵活地扩展
- 成熟，稳定，Netty 修复了已经发现的所有 JDK NIO BUG
- 社区活跃，版本迭代周期短
- 经历了大规模的商业应用考验，质量得到验证



## 第三章 Netty 应用入门

### 3.1 Netty 开发环境的搭建

---

引入 netty jar 包

```xml
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-all</artifactId>
    <version>5.0.0.Alpha1</version>
</dependency>
```



### 3.2 Netty 服务端开发

---

> netty 开发 TimeServer 
>
> 见代码 wr1ttenyu.study.netty.timeserver.netty.NettyTimeServer



### 3.3 Netty 客户端开发

---

> netty 开发 TimeServer 
>
> 见代码 wr1ttenyu.study.netty.timeclient.netty.NettyTimeClient



## 第四章 TCP 粘包/拆包问题的解决之道

### 4.1 TCP 粘包/拆包

---

​	TCP 是个"流"协议，所谓流，就是没有界限的一串数据。可以想象成河里面的流水，他们是连成一片的，其间并没有分界线。TCP 底层并不了解上层业务数据的具体含义，他会根据 TCP 缓冲区的实际情况进行包的划分，所以在业务上认为，一个完整的包可能会被 TCP 拆分成多个包进行发送，也有可能把多个小的包封装成一个大的数据包发送，这就是所谓的 TCP 粘包和拆包问题。

#### 4.1.1 TCP 粘包/拆包问题说明 

客户端发送时，可能出现粘包和拆包

服务端接收时，也可能出现粘包和拆包

#### 4.1.2  TCP 粘包/拆包发生的原因

如：

- 应用程序 write 写入的字节大小大于套接口发送缓冲区大小

#### 4.1.3 粘包问题的解决策略

​	在底层无法保证数据包不被拆分和重组，所以只能通过上层的应用协议栈设计来解决，业界主流协议的解决方案，可以归纳如下：

1. 消息定长，例如每个报文的大小为固定长度200字节，如果不够，空位补齐；
2. 在包尾增加回车换行符进行分割，例如FTP协议；
3. 将消息分为消息头和消息体，消息头中包含标识消息总长度（或者消息体长度）的字段，通常设计思路为消息头的第一个字段使用 i nt32 来表示消息的总长度；
4. 更复杂的应用层协议。



### 4.2 未考虑  TCP 粘包导致功能异常案例

---

> 多次发送的消息被合并发送 
>
> 代码 
>
> wr1ttenyu.study.netty.timeclient.netty.HalfPackageTimeClientHandler
>
> wr1ttenyu.study.netty.timeclient.netty.HalfPackageTimeServerHandler
>
> 运行 可见 



### 4.3 利用 LineBasedFrameDecoder 解决 TCP 粘包问题

---

​	Netty 默认提供了多种编解码器用于处理半包，只要能熟练掌握这些类库的使用，TCP 粘包问题就能很容易解决。

#### 4.3.1 支持 TCP 粘包的 TimeServer

> 见代码 wr1ttenyu.study.netty.timeclient.netty.SolveHalfNettyTimeServer

#### 4.3.2 支持 TCP 粘包的 TimeClient

> 见代码 wr1ttenyu.study.netty.timeclient.netty.SolveHalfNettyTimeClient

#### 4.3.3 运行支持 TCP 粘包的时间服务器程序

运行 4.3.1 和 4.3.2 代码  结果符合预期

####4.3.4 LineBasedFrameDecoder 和 StringDecoder 的原理分析

​	LineBasedFrameDecoder 的工作原理就是它依次遍历 ByteBuf 中的可读字节，判断看是否有 "\n" 或者 "\r\n"，如果有，就以此位置为结束位置，从可读索引到结束位置区间的字节就组成了一行。他是以换行符为结束标志的解码器，支持携带结束符或者不携带结束符两种解码方式，同事支持配置单行最大长度。如果连续读取到最大长度后仍然没有发现换行符，就会抛出异常，同时忽略掉之前读取到的异常码流。

​	StringDecoder 的功能非常简单，就是将接受到的对象转换成字符串，然后继续调用后面的 Handler。LineBasedFrameDecoder + StringDecoder 组合就是按行动切换的文本解码器，它被设计用来支持 TCP 的粘包和拆包。

​	Netty 提供了多种支持 TCP 粘包/拆包的解码器，用来满足用户的不同诉求。 



## 第五章 分隔符和定长解码器的应用

​	TCP 以流的方式传输数据，上层应用协议为了对消息进行区分，往往采用如下4中方式：

1. 消息长度固定，累计读取到长度总和为定长的消息后，视为收到一个完整的消息，然后重置计数器，重新开始读取下一条消息；
2. 将回车换行符作为消息结束符，例如 FTP 协议；
3. 将特殊的分隔符作为消息的结束标志；
4. 通过在消息头中定义长度字段来标识消息的总长度。

Netty 对上面4种应用做了统一的抽象，提供了4种解码器来解决对应的问题，用户不需要考虑解码和 TCP 粘包拆包的问题，非常方便。

​	

### 5.1 DelimiterBasedFrameDecoder 应用开发

---

​	DelimiterBasedFrameDecoder 可以自动完成以指定分隔符做结束标志的消息的解码。

####5.1.1 DelimiterBasedFrameDecoder 服务端开发

> 见代码wr1ttenyu.study.netty.timeclient.netty.decoderDemo.DelimiterBasedFrameDecoderServer
>
####5.1.2 DelimiterBasedFrameDecoder 客户端开发

> 见代码wr1ttenyu.study.netty.timeclient.netty.decoderDemo.DelimiterBasedFrameDecoderClient
>

#### 5.1.3 DelimiterBaseFrameDecoder 运行结果

 先运行 5.1.1 小节代码 后运行 5.1.2 小节代码 ，可达到预期效果。



### 5.2 FixedLengthFrameDecoder 应用开发

---

#### 5.2.1 FixedLengthFrameDecoder 服务端开发

> 见代码 wr1ttenyu.study.netty.timeclient.netty.decoderDemo.FixedLengthFrameDecoderServer

####5.2.2 利用 telnet 命令行测试 FixedLengthFrameDecoder 服务端

```xml
telnet localhost 8080
```



## 第六章 编解码技术

### 6.1 Java 序列化的缺点

---

- 无法跨语言，由于java序列化是java语言内部的私有协议，其他语言并不支持；
- 序列化后的码流太大
- 序列化性能太低



###6.2 业界主流的编解码框架

---

#### 6.2.1 Google 的 Protobuf 介绍

![Protobuf编解码和其他几种序列化框架的响应时间对比](img\Protobuf编解码和其他几种序列化框架的响应时间对比.png)

![Protobuf和其他几种序列化框架的字节数对比](img\Protobuf和其他几种序列化框架的字节数对比.png)



## 第七章 MessagePack 编解码

### 7.1 MessagePack 介绍

MessagePack 的特点如下:

- 编解码高效
- 序列化之后的码流小
- 支持跨语言

#### 7.1.1 MessagePack 多语言支持

​	官方支持 Java，Python，Go，C 等

#### 7.1.2 MessagePack Java API 介绍

```java
List<String> src = new ArrayList<String>();
src.add("123");
src.add("4123");
MessagePack msgPack = new MessagePack();
byte[] raw = msgPack.write(arc);
List<String> dst1 = msgPack.read(raw, Template.tList(Templates.TString));
System.out.pringln(dst1.get(0));
```



### 7.2 MessagePack 编码器和解码器开发

---

​	Netty 预集成了几种常用的编解码框架，利用 Netty 的编解码框架可以非常方便的集成第三方序列化框架。

####7.2.1 MessagePack 编码器开发

> 见代码 wr1ttenyu.study.netty.timeclient.msgpack.MsgpackEncoder

####7.2.2 MessagePack 解码器开发

> 见代码 wr1ttenyu.study.netty.timeclient.msgpack.MsgPackDecoder

#### 7.2.3 功能测试

FIXME 测试失败



### 7.3 粘包/半包支持

---

​	最常用的粘包/半包的解决策略就是在消息头中新增报文长度字段，利用该字段进行半包的编解码。

​	利用 Netty 提供的 LengthFieldPrepender 和 LengthFieldBasedFrameDecoder，结合前面的 MessagePack 编解码框架，实现对 TCP 粘包/半包的支持。

> 见代码 wr1ttenyu.study.netty.timeclient.netty.msgpack.SolveHalfMsgPackClient
>

> 见代码 wr1ttenyu.study.netty.timeclient.msgpack.SolveHalfMsgPackEchoServer
>



##第八章 Google Protobuf 编解码

Protobuf 的优点：

- 产品成熟度高
- 跨语言
- 码流小
- 性能高
- 支持不同协议版本的前向兼容
- 支持定义可选和必选字段



### 8.1 Protobuf  入门

---

​	支持数据结构的 前向兼容（以前的版本支持现在的数据结构）

#### 8.1.1 Protobuf 环境搭建

下载 Protobuf 代码生成工具 <https://github.com/google/protobuf/releases>

#### 8.1.2 Protobuf 编解码开发

> 见代码 wr1ttenyu.study.netty.timeserver.TestSubscribeReqProto

#### 8.1.3 运行 Protobuf 例程

运行 8.1.2 小节代码



### 8.2 Netty 的 protobuf 服务开发

---

#### 8.2.1 Protobuf 服务端开发

> 见代码 wr1ttenyu.study.netty.timeserver.protocol.SubReqServer

#### 8.2.2 Protobuf 客户端开发

> 见代码 wr1ttenyu.study.netty.timeserver.protobuf.SubReqClient

#### 8.2.3 protobuf 服务开发测试

运行 8.2.1 和 8.2.2 小节的代码



### 8.3 Protobuf 的使用注意事项

---

​	ProtobufDecoder 仅负责解码，在 ProtobufDecoder 前面，一定要有能够处理读半包的解码器。



## 第九章 JBoss Marshalling 编解码

暂时跳过



##第十章 HTTP 协议开发应用

 	HTTP 协议是建立在 TCP 传输协议之上的应用层协议，HTTP 是一个属于应用层的面向对象的协议，由于其简捷，快速的方式，适用于分布式超媒体信息系统。

​	由于 HTTP 协议是目前 Web 开发的主流协议，Netty 的 	HTTP 协议栈是基于 Netty 的 NIO 通信框架开发的，因此，Netty 的HTTP 协议也是异步非阻塞的。



### 10.1 HTTP 协议介绍

---

​	HTTP 是一个属于应用层的面向对象的协议，由于其简洁、快速的方式，适用于分布式超媒体信息系统。

​	HTTP 协议的主要特点如下：

- 支持 Client/Server 模式
- 简单 —— 客户向服务器请求服务时，只需指定服务的 URL，携带必要的请求参数或消息体
- 灵活 —— HTTP 允许传输任意类型的数据对象
- 无状态 —— HTTP 协议是无状态的协议

#### 10.1.1 HTTP 协议的 URL

格式：`http://host[":"port][abs_path]`

abs_path 即 URI：如果没有 URI，必须以 "/" 的形式给出 

#### 10.1.2 HTTP 请求消息（HttpRequest)

HTTP 请求由三部分组成，具体如下：

- HTTP 请求行
- HTTP 消息头
- HTTP 请求正文

1. 请求行以一个方法符开头，以空格分开，后面跟着请求的 URI 和协议的版本，格式为：

Method    Request-URI    HTTP-Version   CRLF

| **Method**       | 请求方法                                                     |
| :--------------- | ------------------------------------------------------------ |
| **Request-URI**  | 统一资源标识符                                               |
| **HTTP-Version** | HTTP 协议版本                                                |
| **CRLF**         | 表示回车和换行（除了作为结尾的CRLF外，不允许出现单独的 CR 或 LF 字符） |

2. 请求报头允许客户端向服务端传递请求的附加信息以及客户端自身的信息

![HTTP部分请求消息头列表](img\HTTP部分请求消息头列表.png)

3. HTTP  请求消息体是可选的

#### 10.1.3 HTTP 响应消息（HttpResponse)

​	处理完 HTTP 客户端的请求之后，HTTP 服务端返回响应消息给客户端，HTTP 响应也是由三个部分组成，分别是：状态行、消息报头、响应正文。

1. 状态行 格式：HTTP-Version 空格 Status-Code 空格 Reason-Phrase CRLF

   HTTP- Version表示HTTP版本，例如为HTTP/1.1。

   Status- Code是结果代码，用三个数字表示。

   Reason-Phrase是个简单的文本描述，解释Status-Code的具体原因。

   Status-Code用于机器自动识别，Reason-Phrase用于人工理解。

HTTP 协议详解可参考：

https://www.cnblogs.com/EricaMIN1987_IT/p/3837436.html



### 10.2 Netty HTTP 服务端入门开发

---

​	Netty 天生是异步事件驱动的架构，因此基于 NIO TCP 协议栈开发的 HTTP 协议栈也是异步非阻塞的。

​	Netty 的 HTTP 协议栈无论在性能还是可靠性上，都表现优异，非常适合在非 Web 容器的场景下应用，相比于传统的 Tomcat、Jetty 等 Web 容器，它更加轻量和小巧，灵活性和定制性也更好。

#### 10.2.1 HTTP 服务端例程场景描述

​	开发文件服务器

#### 10.2.2 HTTP 服务端开发

> 见代码 wr1ttenyu.study.netty.timeserver.protocol.http.HttpFilesServer



### 10.3 Netty HTTP + XML 协议栈开发

---

​	由于 HTTP 协议的通用性，很多异构系统间的通信交互采用 HTTP 协议，如 HTTP + XML 或者 RESTful + JSON。

​	很多基于 HTTP 的应用都是后台应用，HTTP 仅仅是承载数据交换的一个通道，是一个载体而不是 Web 容器，在这种场景下，一般不需要类似 Tomcat 这样的重量型 Web 容器。

​	重量级的 Web 容器功能繁杂，在网络安全日益严峻的今天，会存在很多安全漏洞，这意味着你需要为 Web 容器做很多安全加固工作，然而你并没有用到这些功能，在这种场景下，一个更加轻量级的 HTTP 协议栈是个更好的选择。

#### 10.3.1 开发场景介绍

 	模拟一个简单的用户订购系统，请求消息放在 HTTP 消息体中，以 XML 承载，即采用 HTTP + XML 的方式进行通信。

#### 10.3.2 HTTP + XML 协议栈设计

![商品订购流程图](img\商品订购流程图.png)

​	通过流程图分析可得：

1. 需要一套通用、高性能的 xml 序列化框架，灵活的实现 POJO-XML 的互相转换；
2. POJO-XML 对象的映射关系应该非常灵活，支持命名空间和自定义标签；
3. 提供 HTTP + XML 请求消息 及 响应消息 的编解码器；
4. 协议栈使用者不需要关心 HTTP + XML 的编解码，对上层业务零侵入，业务只需要对上层的业务 POJO 对象进行编排。

#### 10.3.4 编解码框架开发

> 服务端见代码 wr1ttenyu.study.netty.timeserver.protocol.http.HttpXmlServer

> 客户端见代码 wr1ttenyu.study.netty.timeserver.protocol.HttpXmlClient



##第十一章 WebSocket 协议开发

### 11.1 HTTP 协议的弊端

---

- HTTP 协议为半双工协议，而且是由客户端控制的一请求一应答的模式
- HTTP 消息冗长而繁琐
- 可以针对服务器推送进行黑客攻击。例如长时间的轮询



### 11.2 WebSocket 入门

---

WebSocket 的特点：

- 单一的 TCP 连接，采用全双工模式通信
- 无头部信息、Cookie 和身份验证
- 无安全开销
- 通过 "ping/pong" 帧保持链路激活
- 服务器可以主动传递消息给客户端，不在需要客户端轮询



## 第十二章  私有协议栈开发



## 第十三章 服务端创建

 	Netty 服务端创建需要的必备知识如下：

- 熟悉 JDK NIO 主要类库的使用，例如 ByteBuffer、Selector、ServerSocketChannel 等
- 熟悉 JDK 的多线程编程
- 了解 Reactor 模式

###13.1 原生 NIO 类库的复杂性

---

​	开发高质量的 NIO 程序并不是一件简单的事情，除去 NIO 类库的固有复杂性和BUG，作为 NIO 服务端，需要能够处理网络的闪断、消息编解码、半包处理等。如果没有足够的 NIO 编程经验积累，自研 NIO 框架是非常困难的。



### 13.2 Netty 服务端创建源码分析

---

####13.2.1 Netty 服务端创建时序图	

![Netty 服务端创建时序图](img\Netty 服务端创建时序图.png)

​	步骤1：创建 ServerBootstrap 实例。ServerBootstrap 是 Netty 服务端的启动辅助类，提供一系列的方法用于设置服务器端启动相关的参数。通过门面模式，对底层 API 进行抽象和封装出各种能力，降低用户开发难度。

​	ServerBootstrap只有一个无参的构造器，他需要与多个其他组件或者类交互。ServerBootstrap 构造函数没有参数的根本原因是因为它的参数太多，而且未来也可能会发生变化，为了解决这个问题，就需要引入 Builder 模式。

​	步骤2：设置并绑定 Reactor 线程池。Netty 的 Reactor 线程池是 EventLoopGroup，他实际就是 EventLoop 的数组。EventLoop 的职责是处理所有注册到本线程多路复用器 Selector 上的 Channel，Selector 的轮询操作是由绑定的 EventLoop 线程 run 方法驱动，在一个循环体内循环执行。值得说明的是，EventLoop 的职责不仅仅是处理网络 I/O 事件，用户自定义的 Task 和定时任务 Task 也统一由 EventLoop 负责处理，这样线程模型就实现了统一。从调度层面看，也不存在从 EventLoop 线程中再启动其他类型的线程用于异步执行另外的任务，这样就避免了多线程并发操作和锁竞争，提升了 I/O 线程的处理和调度性能。

​	步骤3：设置并绑定服务器端 Channel。作为 NIO 服务端，需要创建 ServerSocketChannel，Netty 对原生的 NIO 类库进行了封装，对应实现是 NioServerSocketChannel。对于用户而言，不需要关心服务器端 Channel 的底层实现细节和工作原理，只需要指定具体使用哪种服务端 Channel 即可。因此，Netty 的 ServerBootstrap 方法提供了 channel 方法用于指定服务端 Channel 的类型。Netty 通过工厂类，利用反射创建 NioServerSocketChannel 对象。由于服务端监听端口往往只需要在系统启动时才会调用，因此反射对性能的影响并不大。

​	步骤4：链路建立的时候创建并初始化 ChannelPipeline。ChannelPipeline 并不是 NIO 服务端必需的，它本质就是一个负责处理网络事件的职责链，负责管理和执行 ChannelHandler。网络事件以事件流的形式在 ChannelPipeline 中流转，由 ChannelPipeline 根据  ChannelHandler 的执行策略调度 ChannelHandler 的执行。典型的网络事件如下：

- 链路注册、激活、断开
- 接收到请求消息
- 请求消息接收并处理完毕
- 发送应答消息
- 链路发生异常
- 发生用户自定义事件

​	步骤5：初始化 ChannelPipeline 完成之后，添加并设置 ChannelHandler。ChannelHandler 是Netty 提供给用户定制和扩展的关键接口。利用 ChannelHandler 用户可以完成大多数的功能定制，例如消息编解码、心跳、安全认证、流量控制和流量整形等。Netty 同时也提供大量的系统 ChannelHandler 供用户使用，比较实用的系统 ChannelHandler 总结如下。

- 系统编解码框架 —— ByteToMessageCodec;
- 通用基于长度的半包解码器 —— LengthFieldBasedFrameDecoder;
- 码流日志打印 Handler —— LoggingHandler;
- 流量整形 Handler —— ChannelTrafficShapingHandler 等

​	步骤6：绑定并启动监听端口。在绑定监听端口之前系统会做一系列的初始化和检测工作，完成之后，会启动监听端口，并将 ServerSocketChannel 注册到 Selector 上监听客户端连接。

​	步骤7：Selector 轮询。由 Reactor 线程 NioEventLoop 负责调度和执行 Selector 轮询操作，选择准备就绪的 Channel 集合。

​	步骤8：当轮询到准备就绪的 Channel 之后，就由 Reactor 线程 NioEventLoop 执行 ChannelPipeline 的相关方法，最终调度并执行 ChannelHandler。

​	步骤9：执行 Netty 系统 ChannelHandler 和用户添加定制的 ChannelHandler。ChannelPipeline 根据网络事件的类型，调度并执行 ChannelHandler。

####13.2.2 Netty 服务端创建源码分析

​	首先通过无参构造器创建 ServerBootstrap 实例，随后，通常会创建两个 EventLoopGroup （并不是必须要创建两个不同的 EventLoopGroup，也可以只创建一个并共享），代码如下：

```java
EventLoopGroup bossGroup = new NioEventLoopGroup();
EventLoopGroup workerGroup = new NioEventLoopGroup();
```

​	NioEventLoopGroup 实际就是 Reactor 线程池，负责调度和执行客户端的接入、网络读写事件的处理、用户自定义任务和定时任务的执行。通过 ServerBootstrap 的 group 方法将两个 EventLoopGroup 实例传入，代码如下：

```java
public ServerBootstrap group(EventLoopGroup parentGroup, EventLoopGroup childGroup) {
    super.group(parentGroup);
    if (childGroup == null) {
        throw new NullPointerException("childGroup");
    }
    if (this.childGroup != null) {
        throw new IllegalStateException("childGroup set already");
    }
    this.childGroup = childGroup;
    return this;
}
```

​	其中父 NioEventLoopGroup 被传入了父类构造函数中，代码如下：

```java
public B group(EventLoopGroup group) {
    if (group == null) {
        throw new NullPointerException("group");
    }
    if (this.group != null) {
        throw new IllegalStateException("group set already");
    }
    this.group = group;
    return (B) this;
}
```

​	该方法会被客户端和服务端共用，用于设置工作 I/O 线程，执行和调度网络事件的读写。

​	线程组和线程类型设置完成后，需要设置服务端 Channel 用于端口监听和客户端链路接入。Netty 通过 Channel 工厂类来创建不同类型的 Channel，对于服务端，需要创建 NioServerSocketChannel。所以，通过指定 Channel 类型的方式创建 Channel 工厂。ServerBootstrapChannelFactory 是 ServerBootstrap 的内部静态类，职责是根据 Channel 的类型通过反射创建 Channel 的实例，服务端需要创建的是 NioServerSocketChannel 实例，代码如下：

```java
public T newChannel(EventLoop eventLoop, EventLoopGroup childGroup) {
    try {
        Constructor<? extends T> constructor = clazz.getConstructor(EventLoop.class, EventLoopGroup.class);
        return constructor.newInstance(eventLoop, childGroup);
    } catch (Throwable t) {
        throw new ChannelException("Unable to create Channel from class " + clazz, t);
    }
}
```

​	指定 NioServerSocketChannel 后，需要设置 TCP 的一些参数，作为服务端，主要是要设置 TCP 的 backlog 参数。

​	backlog 指定了内核为此套接口排队的最大连接个数，对于给定的监听接口，内核要维护两个队列：未连接队列和已连接队列，根据 TCP 三次握手过程中三个分节来分隔这两个队列。服务器处于 listen 状态时，收到客户端 syn 分节（connect）时在未完成队列中创建一个新的条目，然后用三次握手的第二个分节即服务器的 syn 响应客户端，此条目在第三个分节到达前（客户端对服务器 syn 的 ack）一直保留在未完成连接队列中，如果三次握手完成，该条目将从未完成连接队列搬到已完成连接队列尾部。当进程调用accept时，从已完成队列中的头部取出一个条目给进程，当已完成队列为空时进程将睡眠，直到有条目在已完成连接队列中才唤醒。backlog 被规定为两个队列总和的最大值，Netty 默认的 backlog 为100，用户可以根据实际场景和网络状况进行灵活配置。

​	TCP 参数设置完成后，用户可以为启动辅助类和其父类分别指定 Handler。两类 Handler 的用途不同：

1. 子类中的 Handler 是 NioServerSocketChannel 对应的 ChannelPipeline 的 Handler；

2. 父类中的 Handler 是客户端新接入的连接 SocketChannel 对应的 ChannelPipeline 的 Handler。

   ![ServerBootstrap的Handler模型](img\ServerBootstrap的Handler模型.png)

   ​	本质区别就是：ServerBootstrap 中的 Handler 是 NioServerSocketChannel  使用的，所有连接该监听端口的客户端都会执行它；父类 AbstractBootstrap 中的 Handler 是个工厂类，它为每个新接入的客户端都创建一个新的 Handler。

   ​	服务端启动的最后一步，就是绑定本地接口，启动服务，代码如下：

   ```java
   private ChannelFuture doBind(final SocketAddress localAddress) {
       // initAndRegister()
       // 首先创建 Channel，createChannel 由子类 ServerBootstrap 实现
       // 然后对 创建成功后的 NioServerSocketChannel 进行初始化 分三个步骤
       // 1.设置 Socket 参数 和 NioServerSocketChannel 的附加属性
       // 2.将 AbstractBootstrap 的 Handler 添加到 NioServerSocketChannel 的 ChannelPipeline 中
       // 3.将用于服务端注册的 Handler 添加到 ServerBootstrapAcceptor 的 ChannelPipeline 中
       final ChannelFuture regFuture = initAndRegister();
       final Channel channel = regFuture.channel();
       if (regFuture.cause() != null) {
           return regFuture;
       }
   
       final ChannelPromise promise;
       if (regFuture.isDone()) {
           promise = channel.newPromise();
           doBind0(regFuture, channel, localAddress, promise);
       } else {
           // Registration future is almost always fulfilled already, but just in case it's not.
           promise = new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE);
           regFuture.addListener(new ChannelFutureListener() {
               @Override
               public void operationComplete(ChannelFuture future) throws Exception {
                   doBind0(regFuture, channel, localAddress, promise);
               }
           });
       }
   
       return promise;
   }
   ```

​	Netty 服务端监听的相关资源已经初始化完毕之后，就剩下最后一步 —— 注册 NioServerSocketChannel 到 Reactor 线程的多路复用器上，然后轮询客户端连接事件。在分析注册代码之前，先通过下图看看目前 NioServerSocketChannel 的 ChannelPipeline 的组成。

![NioServerSocketChannel的ChannelPipeline](img\NioServerSocketChannel的ChannelPipeline.png)

​	最后，看下 NioServerSocketChannel 的注册。当 NioServerSocketChannel 初始化完成之后，需要将他注册到 Reactor 线程的多路复用器上监听新客户端的接入，代码如下：

```java
public final void register(final ChannelPromise promise) {
    if (eventLoop.inEventLoop()) {
        register0(promise);
    } else {
        try {
            eventLoop.execute(new Runnable() {
                @Override
                public void run() {
                    register0(promise);
                }
            });
        } catch (Throwable t) {
            logger.warn(
                "Force-closing a channel whose registration task was not accepted by an event loop: {}",
                AbstractChannel.this, t);
            closeForcibly();
            closeFuture.setClosed();
            promise.setFailure(t);
        }
    }
}
```

​	首先判断是否是 NioEventLoop 自身发起的操作。如果是，则不存在并发操作，直接执行 Channel 注册；如果由其他线程发起，则封装成一个 Task 放入消息队列中异步执行。此处，由于是 ServerBootstrap 所在的线程执行的注册操作，所以会将其封装成 Task 投递到 NioEventLoop 中执行，代码如下：

```java
private void register0(ChannelPromise promise) {
    try {
        // check if the channel is still open as it could be closed in the mean time when the register
        // call was outside of the eventLoop
        if (!ensureOpen(promise)) {
            return;
        }
        doRegister();
        registered = true;
        promise.setSuccess();
        pipeline.fireChannelRegistered();
        if (isActive()) {
            pipeline.fireChannelActive();
        }
    } catch (Throwable t) {
        // 代码省略...
    }
}
```

​	将 NioServerSocketChannel 注册到 NioEventLoop 的 Selector 上，代码如下：

```java
protected void doRegister() throws Exception {
    boolean selected = false;
    for (;;) {
        try {
            selectionKey = javaChannel().register(eventLoop().selector, 0, this);
            return;
        } catch (CancelledKeyException e) {
            if (!selected) {
                // Force the Selector to select now as the "canceled" SelectionKey may still be
                // cached and not removed because no Select.select(..) operation was called yet.
                eventLoop().selectNow();
                selected = true;
            } else {
                // We forced a select operation on the selector before but the SelectionKey is still cached
                // for whatever reason. JDK bug ?
                throw e;
            }
        }
    }
}
```

`selectionKey = javaChannel().register(eventLoop().selector, 0, this);`上述代码中的这段，大家可能会很诧异，应该注册 OP_ACCEPT（16）到多路复用器上，怎么注册 0 呢？0 表示只注册，不监听任何网络操作。这样做的原因如下：

1. 注册方法是多态的，它既可以被 NioServerSocketChannel 用来监听客户端的连接接入，也可以注册 SocketChannel 用来监听网络读或者写操作；

2. 通过 SelectionKey 的 interestOps(int ops) 方法可以方便地修改监听操作位。所以，此处注册需要获取 SelectionKey 并给 AbstractNioChannel 的成员变量 selectionKey 赋值。

   注册成功之后，触发 ChannelRegistered 事件，方法如下：

   ```java
   promise.setSuccess();
   pipeline.fireChannelRegistered();
   ```

   当 ChannelRegistered 事件传递到 TailHandler 后结束，TailHandler  也不关心 ChannelRegistered 事件，因此是空实现，代码如下：

   ```java
   io.netty.channel.DefaultChannelPipeline.TailHandler
   @Override
   public void channelRegistered(ChannelHandlerContext ctx) throws Exception { }
   ```

   ChannelRegistered 事件传递完成后，判断 ServerSocketChannel 监听是否成功，如果成功，需要触发 NioServerSocketChannel 的 ChannelActive 事件，代码如下：

   ```java
   if (isActive()) {
       pipeline.fireChannelActive();
   }
   ```

   isActive() 也是个多态方法。如果是服务端，判断监听是否启动；如果是客户端，判断 TCP 连接是否完成。ChannelActive 事件在 ChannelPipeline 中传递，完成之后根据配置决定是否自动触发 Channel 的读操作，代码如下：

   ```java
   public ChannelPipeline fireChannelActive() {
       head.fireChannelActive();
   
       if (channel.config().isAutoRead()) {
           channel.read();
       }
   
       return this;
   }
   ```

   AbstractChannel 的读操作触发 ChannelPipeline 的读操作，最终调用到 HeadHandler 的读方法，代码如下：

   ```java
   public void read(ChannelHandlerContext ctx) {
       unsafe.beginRead();
   }
   ```

   继续看 AbstractUnsafe 的 beginRead 方法，代码如下：

   ```java
   public void beginRead() {
       if (!isActive()) {
           return;
       }
   
       try {
           doBeginRead();
       } catch (final Exception e) {
           invokeLater(new Runnable() {
               @Override
               public void run() {
                   pipeline.fireExceptionCaught(e);
               }
           });
           close(voidPromise());
       }
   }
   ```

   由于不同类型的 Channel 对读操作的准备工作不同，因此，beginRead 也是个多态方法，对于 NIO 通信，无论是客户端还是服务端，都是要修改网络监听操作位为自身感兴趣的，对于 NioServerSocketChannel 感兴趣的操作是 OP_ACCEPT(16)，于是重新修改注册的操作位为 OP_ACCEPT，代码如下：

   ```java
   protected void doBeginRead() throws Exception {
       if (inputShutdown) {
           return;
       }
   
       final SelectionKey selectionKey = this.selectionKey;
       if (!selectionKey.isValid()) {
           return;
       }
   
       final int interestOps = selectionKey.interestOps();
       if ((interestOps & readInterestOp) == 0) {
           selectionKey.interestOps(interestOps | readInterestOp);
       }
   }
   ```

   在某些场景下，当前监听的操作类型和 Channel 关心的网络事件是一致的，不需要重复注册，所以增加了&操作的判断，只有两者不一致，才需要重新注册操作位。

   JDK SelectionKey 有 4 种操作类型，分别为：

1. OP_READ = 1<< 0;
2. OP_WRITE = 1<< 2;
3. OP_CONNECT = 1<< 3;
4. OP_ACCEPT = 1<< 4。

​	由于只有 4 种网络操作类型，所以用 4 bit 就可以表示所有网络操作位，由于 Java 语言没有 bit 类型，所以使用了整型来表示，每个操作位代表一种网络操作类型，分别为：0001、0010、0100、1000，这样做的好处是可以非常方便地通过位操作来进行网络操作位的状态判断和状态修改，从来提升操作性能。

​	由于创建 NioServerSocketChannel 将 readInterestOps 设置成了 OP_ACCEPT，所以，在服务端链路注册成功之后重新将操作位设置为监听客户端的网络连接操作，初始化 NioServerSocketChannel 的代码如下：

```java
public NioServerSocketChannel(EventLoop eventLoop, EventLoopGroup childGroup) {
    super(null, eventLoop, childGroup, newSocket(), SelectionKey.OP_ACCEPT);
    config = new DefaultServerSocketChannelConfig(this, javaChannel().socket());
}
```



### 13.3 客户端接入源码分析

---

​	负责处理网络读写、连接和客户端请求接入的 Reactor 线程就是 NioEventLoop，下面我们分析下 NioEventLoop 是如何处理新的客户端连接接入的。当多路复用器检测到新的准备就绪的 Channel 时，默认执行 processSelectedKeysOptimized 方法，代码如下。

```java
if (selectedKeys != null) {
    processSelectedKeysOptimized(selectedKeys.flip());
} else {
    processSelectedKeysPlain(selector.selectedKeys());
}
```

​	由于 Channel 的 Attachment 是 NioServerSocketChannel，所以执行 processSelectedKey 方法，根据就绪的操作位，执行不同的操作。此处，由于监听的是连接操作，所以执行 unsafe.read() 方法。由于不同的 Channel 执行不同的操作，所以 NioUnsafe 被设计成接口，由不同的 Channel 内部的 Unsafe 实现类负责具体实现。我们发现 read() 方法的实现有两个，分别是 NioByteUnsafe 和 NioMessageUnsafe。对于 NioServerSocketChannel，它使用的是 NioMessageUnsafe，它的 read 方法代码如下：

```java
public void read() {
	assert eventLoop().inEventLoop();
	if (!config().isAutoRead()) {
		removeReadOp();
	}

	final ChannelConfig config = config();
	final int maxMessagesPerRead = config.getMaxMessagesPerRead();
	final boolean autoRead = config.isAutoRead();
	final ChannelPipeline pipeline = pipeline();
	boolean closed = false;
	Throwable exception = null;
	try {
		for (;;) {
			int localRead = doReadMessages(readBuf);
			if (localRead == 0) {
				break;
			}
			if (localRead < 0) {
				closed = true;
				break;
			}

			if (readBuf.size() >= maxMessagesPerRead | !autoRead) {
				break;
			}
		}
	
	// 代码省略...
}
```

​	对 doReadMessage 方法进行分析，发现他实际就是接受新的客户端连接并创建 NioSocketChannel，代码如下：

```java
protected int doReadMessages(List<Object> buf) throws Exception {
    SocketChannel ch = javaChannel().accept();

    try {
        if (ch != null) {
            buf.add(new NioSocketChannel(this, childEventLoopGroup().next(), ch));
            return 1;
        }
    } catch (Throwable t) {
    // 后续代码省略...
}
```

​	接受到新的客户端连接后，触发 ChannelPipeline 的 ChannelRead 方法，代码如下：

```java
int size = readBuf.size();
for (int i = 0; i < size; i ++) {
    pipeline.fireChannelRead(readBuf.get(i));
}
```

​	执行 headChannelHandlerContext 的 fireChannelRead 方法，事件在 ChannelPipeline 中传递，执行 ServerBootstrapAcceptor 的 channelRead 方法，代码如下：

```java
public void channelRead(ChannelHandlerContext ctx, Object msg) {
    Channel child = (Channel) msg;

    child.pipeline().addLast(childHandler);

    for (Entry<ChannelOption<?>, Object> e: childOptions) {
       try {
           if (!child.config().setOption((ChannelOption<Object>) e.getKey(), e.getValue())) {
               logger.warn("Unknown channel option: " + e);
           }
       } catch (Throwable t) {
           logger.warn("Failed to set a channel option: " + child, t);
       }
    }

    for (Entry<AttributeKey<?>, Object> e: childAttrs) {
        child.attr((AttributeKey<Object>) e.getKey()).set(e.getValue());
    }

    child.unsafe().register(child.newPromise());
}
```

​	该方法主要分为如下三个步骤：

1. 将启动时传入的 childHandler 加入到客户端 SocketChannel 的 ChannelPipeline 中；
2. 设置客户端 SocketChannel 的 TCP 参数；
3. 注册 SocketChannel 到多路复用器上；

​	以上三个步骤执行完成之后，下面我们展开看下 NioSocketChannel 的 register 方法，发现它和 NioServerSocketChannel 的 register 方法是同一个。也是将 Channel 注册到 Reactor 线程的多路复用器上。由于注册的操作位是 0，所以，此时 NioSocketChannel 还不能读取客户端发送的消息。

​	执行完注册操作之后，紧接着会触发 ChannelReadComplete 事件。 ChannelReadComplete 在 ChannelPipeline 中的处理流程：Netty 的 Header 和 Tail 本身不关注 ChannelReadComplete 事件就直接透传，执行完 ChannelReadComplete 后，接着执行 PipeLine 的 read() 方法，最终执行 HeadHandler 的 read() 方法。

​	HeadHandler read() 方法的代码已经在之前的小节介绍过了，用来将网络操作位修改为自身感兴趣的操作。创建 NioSocketChannel 的时候已经将 AbstractNioChannel 的 readInterestOp 设置为 OP_READ，这样，执行 selectionKey.interestOps(interestOps | readInterestOps) 操作时就会把操作位设置为 OP_READ。

`io.netty.channel.socket.nio.NioServerSocketChannel#doReadMessages`

```java
protected int doReadMessages(List<Object> buf) throws Exception {
	SocketChannel ch = javaChannel().accept();

	try {
		if (ch != null) {
            // 这里 new NioSocketChannel 调用父类构造器
            // super(parent, eventLoop, ch, SelectionKey.OP_READ); 将兴趣位置为 SelectionKey.OP_READ
			buf.add(new NioSocketChannel(this, childEventLoopGroup().next(), ch));
			return 1;
		}
	} catch (Throwable t) {
	// 代码省略...
}
```



##第十四章 客户端创建

​	相对于服务端，Netty 客户端的创建更加复杂，除了要考虑线程模型、异步连接、客户端连接超时等因素外，还需要对连接过程中的各种异常进行考虑。



### 14.1 Netty 客户端创建流程分析

---

####14.1.1 Netty 客户端创建时序图

![Netty 客户端创建时序图](img\Netty 客户端创建时序图.png)

#### 14.1.2 Netty 客户端创建流程分析

​	步骤1：用户线程创建 Bootstrap 实例，通过 API 设置创建客户端相关参数，异步发起客户端连接；

​	步骤2：创建处理客户端连接、I/O 读写的 Reactor 线程组 NioEventLoopGroup。可以通过构造函数指定 I/O 线程的个数，默认为 CPU 内核数的 2 倍；

​	步骤3：通过 Bootstrap 的 ChannelFactory 和用户指定的 Channel 类型，创建用于客户端连接的 NioSocketChannel，它的功能类似于 JDK NIO 类库提供的 SocketChannel;

​	步骤4：创建默认的 Channel Handler Pipeline，用于调度和执行网络事件；

​	步骤5：异步发起 TCP 连接，判断连接是否成功。如果成功，则直接将 NioSocketChannel 注册到多路复用器上，监听读操作位，用于数据报读取和消息发送；如果没有立即连接成功，则注册连接监听位到多路复用器，等待连接结果；

​	步骤6：注册对应的网络监听状态位到多路复用器；

​	步骤7：由多路复用器在 I/O 线程中轮询各 Channel，处理连接结果；

​	步骤8：如果连接成功，设置 Future 结果，发送连接成功事件，触发 ChannelPipeline 执行；

​	步骤9：由 ChannelPipeline 调度执行系统和用户的 ChannelHandler，执行业务逻辑。



### 14.2 Netty 客户端创建源码分析

---

#### 14.2.1 客户端连接辅助类 Bootstrap

​	Bootstrap 是 Netty  提供的客户端连接工具类，主要用于简化客户端的创建。

​	设置 I/O 线程组：在前面的章节我们介绍过，非阻塞 I/O 的特点就是一个多路复用器可以同时处理成百上千条链路，这就意味着使用一个线程可以处理多个 TCP 连接。考虑到 I/O 线程的处理性能，大多数 NIO  框架都采用线程池的方式处理 I/O 读写，Netty 也不例外。客户端相对于服务器，只需要一个处理 I/O 读写的线程组即可，Bootstrap 提供了设置 I/O 线程组的接口，代码如下：

FIXME 客户端 为什么只需要一个处理 I/O 读写的线程组即可

```java
public B group(EventLoopGroup group) {
    if (group == null) {
        throw new NullPointerException("group");
    }
    if (this.group != null) {
        throw new IllegalStateException("group set already");
    }
    this.group = group;
    return (B) this;
}
```

​	由于 Netty 的 NIO 线程组默认采用 EventLoopGroup 接口，因此线程组参数使用 EventLoopGroup。

​	TCP 参数设置接口：无论是异步 NIO，还是同步 BIO，创建客户端套接字的时候通常都会设置连接参数，例如接收和发送缓冲区大小等。BootStrap 也提供了客户端 TCP 参数设置接口，代码如下：

```java
public <T> B option(ChannelOption<T> option, T value) {
    if (option == null) {
        throw new NullPointerException("option");
    }
    if (value == null) {
        synchronized (options) {
            options.remove(option);
        }
    } else {
        synchronized (options) {
            options.put(option, value);
        }
    }
    return (B) this;
}
```

​	Netty 提供的主要 TCP 参数如下：

- SO_TIMEOUT：控制读取操作将阻塞多少毫秒。如果返回值为0，计时器就被禁止了，该线程将无限期阻塞；FIXME 这他妈什么意思啊
- SO_SNDBUF：套接字使用的发送缓冲区大小；
- SO_RCVBUF：套接字使用的接收缓冲区大小；
- SO_REUSEADDR：用于决定如果网络上仍然有数据向旧的 ServerSocket 传输数据，是否允许新的 ServerSocket 绑定到与旧的 ServerSocket 同样的端口上。SO_REUSEADDR 选项的默认值与操作系统有关，在某些操作系统中，允许重用端口，而某些不允许；
- CONNECT_TIMEOUT_MILLIS：客户端连接超时时间，由于 NIO 原生的客户端并不提供设置连接超时的接口，因此，Netty 采用的是自定义连接超时定时器负责监测和超时控制；
- TCP_NODELAY：激活或禁止 TCP_NODELAY 套接字选项，它决定是否使用 Nagle 算法。如果是延时敏感型的应用，建议关闭 Nagle 算法。

​	channel 接口：用于指定客户端使用的 channel 接口，对于 TCP 客户端连接，默认使用 NioSocketChannel，代码如下：

```java
public Bootstrap channel(Class<? extends Channel> channelClass) {
    if (channelClass == null) {
        throw new NullPointerException("channelClass");
    }
    return channelFactory(new BootstrapChannelFactory<Channel>(channelClass));
}
```

​	BootstrapChannelFactory 利用 channelClass 类型信息，通过反射机制创建 NioSocketChannel 对象。

​	设置 Handler 接口：Bootstrap 为了简化 Handler 的编排，提供了 ChannelInitializer，它继承了 ChannelHandlerAdapter，当 TCP 链路注册成功之后，调用 initChannel 接口，用于设置用户 ChannelHandler。代码如下：

```java
public final void channelRegistered(ChannelHandlerContext ctx) throws Exception {
    ChannelPipeline pipeline = ctx.pipeline();
    boolean success = false;
    try {
        initChannel((C) ctx.channel());
        pipeline.remove(this);
        ctx.fireChannelRegistered();
        success = true;
    } catch (Throwable t) {
        logger.warn("Failed to initialize a channel. Closing: " + ctx.channel(), t);
    } finally {
        if (pipeline.context(this) != null) {
            pipeline.remove(this);
        }
        if (!success) {
            ctx.close();
        }
    }
}
```

​	其中 initChannel 为抽象接口，用户可以在此方法中设置 ChannelHandler，代码如下：

```java
.handler(new ChannelInitializer<SocketChannel>() {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline().addLast(new HalfPackageTimeClientHandler());
    }
});
```

最后一个比较重要的接口就是发起客户端连接，代码如下：

```java
ChannelFuture f = b.connect(host, port).sync();
```

客户端连接方法比较复杂，下小节对此进行详细解析。

####14.2.2 客户端连接操作

​	首先要创建和初始化 NioSocketChannel，代码如下：

```java
private ChannelFuture doConnect(final SocketAddress remoteAddress, final SocketAddress localAddress) {
    final ChannelFuture regFuture = initAndRegister();
    final Channel channel = regFuture.channel();
    if (regFuture.cause() != null) {
        return regFuture;
    }

    final ChannelPromise promise = channel.newPromise();
    if (regFuture.isDone()) {
        doConnect0(regFuture, channel, remoteAddress, localAddress, promise);
    } else {
        regFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                doConnect0(regFuture, channel, remoteAddress, localAddress, promise);
            }
        });
    }

    return promise;
}
```

​	从 NioEventLoopGroup 中获取 NioEventLoop，然后使用其作为参数创建 NioSocketChannel，代码如下：

```java
Channel createChannel() {
    EventLoop eventLoop = group().next();
    return channelFactory().newChannel(eventLoop);
}
```

​	初始化 Channel 之后，将其注册到 Selector 上，代码如下：

```java
ChannelPromise regFuture = channel.newPromise();
channel.unsafe().register(regFuture);
```

​	链路创建成功之后，发起异步的 TCP 连接，代码如下：

```java
private static void doConnect0(
    final ChannelFuture regFuture, final Channel channel,
    final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelPromise promise) {

    // This method is invoked before channelRegistered() is triggered.  Give user handlers a chance to set up
    // the pipeline in its channelRegistered() implementation.
    channel.eventLoop().execute(new Runnable() {
        @Override
        public void run() {
            if (regFuture.isSuccess()) {
                if (localAddress == null) {
                    channel.connect(remoteAddress, promise);
                } else {
                    channel.connect(remoteAddress, localAddress, promise);
                }
                promise.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            } else {
                promise.setFailure(regFuture.cause());
            }
        }
    });
}
```

​	由上述代码可以看出，从 doConnect() 操作开始，连接操作切换到 Netty 的 NIO 线程 NioEventLoop 中进行，此时客户端返回，连接操作异步执行。

​	doConnect() 最终调用 HeadHandler 的 connect 方法，代码如下：

```java
public void connect(
    ChannelHandlerContext ctx,
    SocketAddress remoteAddress, SocketAddress localAddress,
    ChannelPromise promise) throws Exception {
    unsafe.connect(remoteAddress, localAddress, promise);
}
```

​	AbstractNioUnsafe 的 connect 操作如下：

```java
if (doConnect(remoteAddress, localAddress)) {
    fulfillConnectPromise(promise, wasActive);
} else {
    // 后续代码省略...
```

​	需要注意的是，SocketChannel 执行 connect() 操作后有以下三种结果：

1. 连接成功，返回true；
2. 暂时没有连接上，服务端没有返回 ACK 应答，连接结果不确定，返回 false；
3. 连接失败，直接抛出 I/O 异常。

​	如果是第二种结果，需要将 NioSocketChannel 中的 selectionKey 设置为 OP_CONNECT，监听连接结果。

​	异步连接返回之后，需要判断连接结果，如果连接成功，则触发 ChannelActive 事件，代码如下：

```java
if (!wasActive && isActive()) {
    pipeline().fireChannelActive();
}
```

​	ChannelActive 事件处理在前面的章节已经详细说明过，最终会将 NioSocketChannel 中的 selectionKey 设置为 SelectionKey.OP_READ，用于监听网络读操作。

​	如果没有立即连接上服务端，则注册 SelectionKey.OP_CONNECT  到多路复用器，代码如下：

```java
boolean success = false;
try {
    boolean connected = javaChannel().connect(remoteAddress);
    if (!connected) {
        selectionKey().interestOps(SelectionKey.OP_CONNECT);
    }
    success = true;
    return connected;
} finally {
    // 如果连接过程发生异常，则关闭链路，进入连接失败处理流程
    if (!success) {
        doClose();
    }
}
```



#### 14.2.3 异步连接结果通知

​	NioEventLoop 的 Selector 轮询客户端连接 Channel，当服务端返回握手应答之后，对连接结果进行判断，代码如下：

```java
if ((readyOps & SelectionKey.OP_CONNECT) != 0) {
    // remove OP_CONNECT as otherwise Selector.select(..) will always return without blocking
    // See https://github.com/netty/netty/issues/924
    int ops = k.interestOps();
    ops &= ~SelectionKey.OP_CONNECT;
    k.interestOps(ops);

    unsafe.finishConnect();
}
```

​	下面对 finishConnect 方法进行分析，代码如下：

```java
try {
    boolean wasActive = isActive();
    doFinishConnect();
    fulfillConnectPromise(connectPromise, wasActive);
}
```

​	doFinishConnect  用于判断 JDK 的 SocketChannel 的连接结果，如果返回 true 表示连接成功，其他值或者发生异常表示连接失败。

```java
protected void doFinishConnect() throws Exception {
    if (!javaChannel().finishConnect()) {
        throw new Error();
    }
}
```

​	连接成功之后，调用 fulfillConnectPromise 方法，触发链路激活事件，该事件由 Channel Pipeline 进行传播，代码如下：

```java
private void fulfillConnectPromise(ChannelPromise promise, boolean wasActive) {
    // trySuccess() will return false if a user cancelled the connection attempt.
    boolean promiseSet = promise.trySuccess();

    // Regardless if the connection attempt was cancelled, channelActive() event should be triggered,
    // because what happened is what happened.
    if (!wasActive && isActive()) {
        pipeline().fireChannelActive();
    }

    // If a user cancelled the connection attempt, close the channel, which is followed by channelInactive().
    if (!promiseSet) {
        close(voidPromise());
    }
}
```

​	前面章节已经对 fireChannelActive 方法进行过讲解，主要用于修改网络监听位为读操作。

#### 14.2.4 客户端连接超时机制

​	对于 SocketChannel 接口，JDK 并没有提供链接超时机制，需要 NIO 框架或者用户自己扩展实现。Netty 利用定时器提供了客户端连接超时控制功能。

​	首先，用户在创建 Netty 客户端的时候，可以通过 ChannelOption.CONNECT_TIMEOUT_MILLIS 配置项设置连接超时时间，代码如下：

```java
b.group(group).channel(NioSocketChannel.class)
    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 8000)
```

​	发起连接的同时，启动连接超时检测定时器，代码如下：

```java
// Schedule connect timeout.
int connectTimeoutMillis = config().getConnectTimeoutMillis();
if (connectTimeoutMillis > 0) {
    connectTimeoutFuture = eventLoop().schedule(new Runnable() {
        @Override
        public void run() {
            ChannelPromise connectPromise = AbstractNioChannel.this.connectPromise;
            ConnectTimeoutException cause =
                    new ConnectTimeoutException("connection timed out: " + remoteAddress);
            if (connectPromise != null && connectPromise.tryFailure(cause)) {
                close(voidPromise());
            }
        }
    }, connectTimeoutMillis, TimeUnit.MILLISECONDS);
}
```

​	一旦超时定时器执行，说明客户端连接超时，构造连接超时异常，将异常结果设置到 connectPromise 中，同时关闭客户端连接，释放句柄。

​	如果在连接超时之前获取到连接结果，则删除连接超时定时器，防止其被触发，代码如下：

​	`io.netty.channel.nio.AbstractNioChannel.AbstractNioUnsafe#finishConnect`

```java 
finally {
    // Check for null as the connectTimeoutFuture is only created if a connectTimeoutMillis > 0 is used
    // See https://github.com/netty/netty/issues/1770
    if (connectTimeoutFuture != null) {
        connectTimeoutFuture.cancel(false);
    }
    connectPromise = null;
}
```

​	无论连接是否成功，只要获取到连接结果，之后就删除连接超时定时器。



## 第十五章 ByteBuf 和相关辅助类

### 15.1 ByteBuf 功能说明

---

​	实际上，7种基础类型 (Boolean除外) 都有自己的缓冲区实现。对于 NIO 编程而言，我们主要使用的是 ByteBuffer。从功能角度而言，ByteBuffer 完全可以满足 NIO 编程的需要，但是由于 NIO 编程的复杂性，ByteBuffer 也有其局限性，他的主要确定如下：

- ByteBuffer 长度固定，一旦分配完成，他的容量不能动态扩展和收缩，当需要编码的 POJO 对象大于 ByteBuffer 的容量时，会发生索引越界操作；

- ByteBuffer 只有一个标识位置的指针 position，读写的时候需要手工调用 flip() 和 rewind() 等，使用者必须小心敬慎地处理这些 API，否则很容易导致程序处理失败；

- ByteBuffer 的 API 功能有限，一些高级和使用的特性他不支持，需要使用者自己编程实现。

  为了弥补这些不足，Netty 提供了自己的 ByteBuffer 实现 —— ByteBuf

#### 15.1.1 ByteBuf 的工作原理

​	首先，ByteBuf 依然是个 Byte 数组的缓冲区，他的基本功能应该与 JDK 的 ByteBuffer 一致，提供一下几种基本功能。

- 7 种 Java 基础类型、byte 数组、ByteBuffer（ByteBuf）等的读写；
- 缓冲区自身的 copy 和 slice 等；
- 设置网络字节序；
- 构造缓冲区实例；
- 操作位置指针等方法。

​	由于 JDK 的 ByteBuffer 已经提供了这些基础能力的实现，因此，Netty ByteBuf 的实现可以有两种策略。

1. 参考 JDK ByteBuffer 的实现，增加额外的功能，解决原 ByteBuffer 的缺点；
2. 聚合 JDK ByteBuffer，通过 Facade 模式对其进行包装，可以减少自身的代码量，降低实现成本。

​	JDK ByteBuffer 由于只有一个位置指针用于处理读写操作，因此每次读写的时候都需要额外调用 flip() 和 clear() 等方法，否则功能将出错，它的典型用法如下：

```java
ByteBuffer buffer = ByteBuffer.allocate(88);
String value = "Netty 权威指南"；
buffer.put(value.getBytes());
buffer.flip();
byte[] vArray = new byte[buffer.remaining()];
buffer.get(vArray);
String decodeValue = new String(vArray);
```

​	对于 ByteBuffer，掌握其三个属性的作用即可：position、limit、capacity

​	ByteBuf 通过两个位置指针来协助缓冲区的读写操作，读操作使用 readerIndex，写操作使用 writerIndex。

​	readerIndex 和 writerIndex 的取值一开始都是0，数据写入会使 writerIndex  增加，数据读取会使 readerIndex 增加，但是它不会超过 writerIndex。在读取之后，0 ~ readerIndex 就被视为 discard 的，调用discardReadBytes 方法，可以释放这部分空间，他的作用类似 ByteBuffer 的 compact 方法。

​	ByteBuf 是如何实现动态扩容的。通常情况下，当我们对 ByteBuffer 进行 put 操作的时候，如果缓冲区剩余可写空间不够，就会发生 BufferOverflowException 异常。为了避免发生这个问题，通常在进行 put 操作的时候会对剩余可用空间进行校验。如果剩余空间不足，需要重新创建一个新的 ByteBuffer，并将之前的 ByteBuffer 复制到新创建的 ByteBuffer 中，最后释放老的 ByteBuffer，代码如下：

```java
if(buffer.remaining() < needSize) {
    int toBeExtSize = needSize > 128 ? needSize : 128;
    ByteBUffer tmpBuffer = ByteBuffer.allocate(buffer.capacity() + toBeExtSize);
    buffer.flip();
    tmpBuffer.put(buffer);
    buffer = tmpBuffer;
}
```

​	从示例代码可以看出，为了防止 ByteBuffer 溢出，每进行一次put操作，都需要进行空间校验，代码冗余，且使用容易出错。而 ByteBuf 对 write 操作进行了封装，由 ByteBuf 的 write 操作负责进行剩余可用空间的校验，如果缓冲区不足，ByteBuf 会自动进行动态扩展。对于使用者而言，不需要关系这些细节。代码如下：

```java
@Override
public ByteBuf writeByte(int value) {
    ensureWritable(1);
    setByte(writerIndex++, value);
    return this;
}
```

​	由于 NIO 的 Channel 读写的参数都是 ByteBuffer，因此，Netty 的 ByteBuf 接口必须提供 API，以方便的将 ByteBuf 转换成 ByteBuffer，或者将 ByteBuffer 包装成 ByteBuf。考虑到性能，应该尽量避免缓冲区的复制，内部实现的时候可以考虑聚合一个 ByteBuffer 的私有指针用来代表 ByteBuffer。

```java
@Override
public ByteBuf ensureWritable(int minWritableBytes) {
    if (minWritableBytes < 0) {
        throw new IllegalArgumentException(String.format(
            "minWritableBytes: %d (expected: >= 0)", minWritableBytes));
    }

    if (minWritableBytes <= writableBytes()) {
        return this;
    }

    if (minWritableBytes > maxCapacity - writerIndex) {
        throw new IndexOutOfBoundsException(String.format(
            "writerIndex(%d) + minWritableBytes(%d) exceeds maxCapacity(%d): %s",
            writerIndex, minWritableBytes, maxCapacity, this));
    }

    // Normalize the current capacity to the power of 2.
    int newCapacity = calculateNewCapacity(writerIndex + minWritableBytes);

    // Adjust to the new capacity.
    capacity(newCapacity);
    return this;
}
```

#### 15.1.2 ByteBuf 的功能介绍

​	对 ByteBuf 的常用 API 进行分类说明

1. 顺序读操作 (read)

   ByteBuf 的 read 操作类似于 ByteBuffer 的 get 操作，主要的 API 功能说明如下表：

![readApi1](img\readApi1.png)

![readApi2](img\readApi2.png)

![readApi3](img\readApi3.png)

![readApi4](img\readApi4.png)

2. 顺序写操作 (write)

   ByteBuf 的 write 操作类似于 ByteBuffer 的 put 操作，主要的 API 说明如下表：

   ![writeApi1](img\writeApi1.png)

![writeApi2](img\writeApi2.png)

![writeApi3](img\writeApi3.png)

![writeApi4](img\writeApi4.png)

3. readerIndex 和 writerIndex

   Netty 提供了两个指针变量用于支持顺序读取和写入操作：readerIndex 用于标识读取索引，writerIndex 用于标识写入索引。两个位置指针将 ByteBuf 缓冲区分割成三个区域，如下图：

   ![ByteBuf三个区域](img\ByteBuf三个区域.png)

4. Discardable bytes

   相比于其他的 Java 对象，缓冲区的分配和释放是个耗时的操作，因此，我们需要尽量重用他们。由于缓冲区的动态扩张需要进行字节数组的复制，是个耗时的操作，因此为了最大程度地提升性能，往往需要尽最大努力提升缓冲区的重用率。

   ByteBuf 的 discardReadBytes 操作效果图如下：

![discardReadBytes效果图](img\discardReadBytes效果图.png)

​	需要指出的是，调用 discardReadBytes 会发生字节数组的内存复制，所以，频						繁调用将会导致性能下降，调用 discardReadBytes 牺牲性能来换取更多的可用内存。

​	调用 discardReadBytes 操作之后的 writable bytes 内容处理策略跟 ByteBuf 接口的具体实现有关。

```java
public ByteBuf discardReadBytes() {
    ensureAccessible();
    if (readerIndex == 0) {
        return this;
    }

    if (readerIndex != writerIndex) {
        setBytes(0, this, readerIndex, writerIndex - readerIndex);
        writerIndex -= readerIndex;
        adjustMarkers(readerIndex);
        readerIndex = 0;
    } else {
        adjustMarkers(readerIndex);
        writerIndex = readerIndex = 0;
    }
    return this;
}
```

5. Readable bytes 和 Writable bytes

   可读空间段是数据实际存储的区域，以 read 或者 skip 开头的任何操作都会从 	readerIndex 开始读取或者跳过指定的数据，操作完成之后 readIndex 增加了读取或者跳过的字节数长度。如果读取的字节数长度大于实际可读的字节数，则抛出 IndexOutOfBoundsException。

   可写空间段是尚未被使用可以填充的空闲空间，任何以 write 开头的操作都会从 writerIndex 开始向空闲空间写入字节，操作完成之后 writerIndex 增加了写入的字节数长度。

6. Clear 操作

   Clear 操作主要作用是来操作位置指针，即 readerIndex 和 writerIndex，将他们还原为初始分配值。

7. Mark 和 Reset

   当对缓冲区进行读操作时，由于某种原因，可能需要对之前的操作进行回滚。读操作并不会改变缓冲区的内容，回滚操作主要就是重新设置索引信息。

   Netty 的 ByteBuf 也有类似的 reset 和 mark 接口，因为 ByteBuf 有读索引和写索引，因此，它总共有 4 个相关的方法：

   - markReaderIndex
   - resetReaderIndex
   - markWriterIndex
   - resetWriterIndex

8. 查找操作

   很多时候，需要从 ByteBuf 中查找某个字符，例如通过 ”\r\n" 作为文本字符串的换行符，利用 "NUL(0x00)" 作为分隔符。

   ByteBuf 提供了多种查找方法用于满足不同的应用场景，详细分类如下：

   - indexOf(int fromIndex, int toIndex, byte value)：从 fromIndex 开始，最大到 toIndex，定位 value 首次出现的位置，没有返回 -1
   - bytesBefore(byte value)：从 readerIndex 开始，最大到 writerIndex，定位 value 首次出现的位置，没有返回 -1
   - bytesBefore(int length, byte value)：从 readerIndex 开始，最大到 readerIndex + length，定位 value 首次出现的位置，没有返回 -1
   - bytesBefore(int index, int length, byte value)：从 index 开始，最大到 index + length，定位 value 首次出现的位置，没有返回 -1
   - forEachByte(ByteBufProcessor processor)：遍历当前 ByteBuf 的可读自己数组，与 ByteBufProcessor 设置的查找条件进行对比，如果满足条件，则返回位置索引，否则返回 -1。
   - forEachByteDesc(int index, int length, ByteProcessor processor)：从 index 开始，最大到 index + length 进行遍历，与 ByteBufProcessor 设置的查找条件进行对比，如果满足条件，则返回位置索引，否则返回 -1。

​	对于查找的字节而言，存在一些常用值，例如回车换行符、常用的分隔符等，Netty 为了减少业务的重复定义，在 ByteBufProcessor 接口中对这些常用的查找字节进行了抽象，常用定义如下：

- FIND_NUL：NUL (0x00)
- FIND_CR：CR ('\r')
- FIND_LF：LF ('\n')
- FIND_CRLF：CR ('\r') 或者 LF ('\n')
- FIND_LINEAR_WHITESPACE：' ' or '\t'

9. Derived buffers

   类似于数据库的视图，ByteBuf 提供了多个接口用于创建某个 ByteBuf 的视图或者复制 ByteBuf，具体方法如下：

   1. duplicate：返回当前 ByteBuf 的复制对象，复制后返回的 ByteBuf 与操作的 ByteBuf 共享缓冲区内容，但是维护自己独立的索引。当修改复制后的 ByteBuf 内容后，之前原 ByteBuf 的内容也随之改变，双方持有的是同一个内容指针引用。
   2. copy：复制一个新的 ByteBuf 对象，他的内容和索引都是独立的，复制操作本身并不修改原 ByteBuf 的读写索引。
   3. copy(int index, int length)：从指定的索引开始复制，复制的字节长度为 length，复制后的 ByteBuf 内容和读写索引都与之前的独立。
   4. slice：返回当前 ByteBuf 的可读子缓冲区，起始位置从 readIndex 到 writerIndex，返回后的 ByteBuf 与原 ByteBuf 共享内容，但是读写索引独立维护。该操作并不修改原 ByteBuf 的 readerIndex 和 writerIndex。
   5. slice(int index, int length)：返回当前 ByteBuf 的可读子缓冲区，起始位置从 index 到 index + length，返回后的 ByteBuf 与原 ByteBuf 共享内容，但是读写索引独立维护。该操作并不修改原 ByteBuf 的 readerIndex 和 writerIndex。

10. 转换成标准的 ByteBuffer

    当通过 NIO 的 SocketChannel 进行网络读写时，操作的对象是 JDK 标准的 java.nio.ByteBuffer，由于 Netty 统一使用 ByteBuf 替代 JDK 原生的 java.nio.ByteBuffer，多以必须从接口层面支持两者的互相转换，下面就一起看下如何将 ByteBuf 转换成 java.nio.ByteBuffer。

    - ByteBuffer nioBuffer()：将当前 ByteBuf 可读的缓冲区转换成 ByteBuffer，两者共享同一个缓冲区内容引用，对 ByteBuffer 的读写操作并不会修改原 ByteBuf 的读写索引。需要指出的是，返回后的 ByteBuffer 无法感知原 ByteBuf 的动态扩展操作。
    - ByteBuffer nioBuffer(int index, int length)：将当前 ByteBuf 从 index 开始长度为 length 的可读的缓冲区转换成 ByteBuffer，两者共享同一个缓冲区内容引用，对 ByteBuffer 的读写操作并不会修改原 ByteBuf 的读写索引。需要指出的是，返回后的 ByteBuffer 无法感知原 ByteBuf 的动态扩展操作。

11. 随机读写( set 和 get)

    除了顺序读写之外，ByteBuf 还支持随机读写，他与顺序读写的最大差别在于可以随机指定读写的索引位置。

    读取操作的 API 列表

![随机读操作API](img\随机读操作API.png)

​	随机写操作的 API 列表

![随机写操作API](img\随机写操作API.png)

​	无论是 get 还是 set 操作，ByteBuf 都会对其索引和长度等进行合法性校验，与顺序读写一直。但是，set 操作与 write 操作不同的是，它不支持动态扩展缓冲区，所以使用者必须保证当前的缓冲区可写的字节数大于需要写入的字节长度，否则会抛出异常。



### 15.2 ByteBuf 源码分析

---

​	由于 ByteBuf 的实现非常繁杂，所以只挑选 ByteBuf 的主要接口实现类和主要方法分析说明，举一反三。

#### 15.2.1 ByteBuf 的主要类继承关系

![ByteBuf类实现继承关系与](img\ByteBuf类实现继承关系与.png)

​	从内存分配角度来看，ByteBuf 可以分为两类：

1. 堆内存 (HeapByteBuf) 字节缓冲区：特点是内存的分配和回收速度快，可以被 JVM自动回收；缺点是如果进行 Socket 的 I/O 读写，需要额外做一次内存复制，将堆内存对应的缓冲区复制到内核 Channel 中，性能会有一定程度的下降。
2. 直接内存 (DirectByteBuf) 字节缓冲区：非堆内存，它在堆外进行内存分配，相比于堆内存，它的分配和回收速度会慢一些，但是将它写入或者从 Socket Channel 中读取时，由于少了一次内存复制，速度比堆内存快。

​	正是因为各有利弊，所以 Netty 提供了多种 ByteBuf 供开发者使用，经验表明，ByteBuf 的最佳实践是在 I/O 通信线程的读写缓冲区使用 DirectByteBuf，后端业务消息的编解码模块使用 HeapByteBuf，这样组合可以达到新能最优。

​	从内存回收角度看，ByteBuf 也分为两类：基于对象池的 ByteBuf 和普通 ByteBuf。两者的主要区别就是基于对象池的 ByteBuf 可以重用 ByteBuf 对象，他自己维护了一个内存池，可以循环利用创建的 ByteBuf，提升内存的使用效率，降低由于高负载导致的频繁 GC。测试表明使用内存池后的 Netty 在高负载、大并发的冲击下内存和 GC 更加平稳。

​	尽管推荐使用基于内存池的 ByteBuf，但是内存池的管理和维护更加复杂，使用起来也需要更加谨慎，因此，Netty 提供了灵活的策略供使用者选择。

#### 15.2.2 AbstractByteBuf 源码分析

​	AbstractByteBuf 继承自 ByteBuf，ByteBuf 的一些公共属性和功能会在 AbstractByteBuf 中实现。

1. 主要成员变量

```java
static final ResourceLeakDetector<ByteBuf> leakDetector = new ResourceLeakDetector<ByteBuf>(ByteBuf.class);

int readerIndex;
private int writerIndex;
private int markedReaderIndex;
private int markedWriterIndex;

private int maxCapacity;

private SwappedByteBuf swappedBuf;
```

​	首先，像读索引、写索引、mark、最大容量等公共属性需要定义。

​	重点关注下 leakDetector，他被定义为 static，意味着所有的 ByteBuf 实例共享同一个 ResourceLeakDetector 对象。ResourceLeakDetector 用于检测对象是否泄漏，后面有专门章节进行讲解。

​	在 AbstactByteBuf 中并没有定义 ByteBuf 的缓冲区实现，例如 byte 数组或者 DirectByteBuf。原因是因为 AbstractByteBuf 并不清楚子类到底是基于堆内存还是直接内存，因此无法以前定义。

2. 读操作簇

​	无论子类如何实现 ByteBuf，例如 UnpooledHeapByteBuf 使用 byte 数组表示字节缓冲区，UnpooledDirectByteBuf 直接使用 ByteBuffer，它们的功能都是相同的，操作的结果是等价的。

​	因此，读操作以及其他的一些公共功能都是由父类实现，差异化的功能由子类实现，这就是抽象和继承的价值所在。

3. 写操作簇

   与读取操作类似，写操作的公共行为在 AbstractByteBuf 中实现。

   Netty 的 ByteBuf 可以动态扩展，为了保证安全性，允许使用者指定最大的容量，在容量范围内，可以先分配个较小的初始容量，后面不够用再动态扩展，这样可以达到功能和性能的最优组合。

4. 操作索引

   与索引相关的操作主要涉及设置读写索引、mark 和 reset 等

   ![ByteBuf索引相关操作](img\ByteBuf索引相关操作.png)

5. 重用缓冲区

   对 discardReadBytes 和 discardSomeReadBytes 进行分析，discardReadBytes 源码如下：

```java
public ByteBuf discardReadBytes() {
    ensureAccessible();
    if (readerIndex == 0) {
        return this;
    }

    if (readerIndex != writerIndex) {
        setBytes(0, this, readerIndex, writerIndex - readerIndex);
        writerIndex -= readerIndex;
        // 调整 marker 的位置
        adjustMarkers(readerIndex);
        readerIndex = 0;
    } else {
        // readerIndex == writerIndex 不需要进行复制
        adjustMarkers(readerIndex);
        writerIndex = readerIndex = 0;
    }
    return this;
}
```

​	首先对读索引进行判断，如果为 0 则说明没有可重用的缓冲区，直接返回。如果读索引大于 0 且读索引不等于写索引，说明缓冲区中既有已经读取过的被丢弃的缓冲区，也有尚未读取的可读缓冲区。调用 setBytes(0, this, readerIndex, writeIndex - readerIndex) 方法进行字节数组复制。接着调整 readerIndex 和 writeIndex 的值，在设置读写索引的同时，需要同时调整 markedReaderIndex 和 markerWriterIndex。

​	markedReaderIndex 和 markerWriterIndex 都减少 discard 的长度，如果discard 的长度大于 mark 的标记位置，则将 markedReaderIndex 或 markerWriterIndex 置为 0 。

6. skipBytes

​	在解码的时候，有时候需要丢弃非法的数据报，或者跳跃过不需要读取的字节或字节数组，此时，使用 skipBytes 方法就非常方便。

```java
public ByteBuf skipBytes(int length) {
    checkReadableBytes(length);

    int newReaderIndex = readerIndex + length;
    if (newReaderIndex > writerIndex) {
        throw new IndexOutOfBoundsException(String.format(
            "length: %d (expected: readerIndex(%d) + length <= writerIndex(%d))",
            length, readerIndex, writerIndex));
    }
    readerIndex = newReaderIndex;
    return this;
}
```

#### 15.2.3 AbstractReferenceCountedByteBuf 源码分析

​	从类的名字就可以看出该类主要是对引用进行计数，类似于 JVM 内存回收的对象引用计数器，用于跟踪对象的分配和销毁，做自动内存回收。

1. 成员变量

   AbstractReferenceCountedByteBuf 成员变量列表

```java
private static final AtomicIntegerFieldUpdater<AbstractReferenceCountedByteBuf> refCntUpdater =
            AtomicIntegerFieldUpdater.newUpdater(AbstractReferenceCountedByteBuf.class, "refCnt");

// 它用于标识 refCnt 字段在 AbstractReferenceCountedByteBuf 中的内存地址。
// 该内存地址的获取是 JDK 实现强相关的，如果使用 SUN 的 JDK，它通过 sun.misc.Unsafe 的 objectFieldOffset 接口来获得，ByteBuf 的实现子类 UnpooledUnsafeDirectByteBuf 和 PooledUnsafeDirectByteBuf 会使用到这个偏移量
private static final long REFCNT_FIELD_OFFSET;

static {
    long refCntFieldOffset = -1;
    try {
        if (PlatformDependent.hasUnsafe()) {
            refCntFieldOffset = PlatformDependent.objectFieldOffset(
                AbstractReferenceCountedByteBuf.class.getDeclaredField("refCnt"));
        }
    } catch (Throwable t) {
        // Ignored
    }

    REFCNT_FIELD_OFFSET = refCntFieldOffset;
}
// 用于跟踪对象的引用次数 使用 volatile 是为了解决多线程并发访问的可见性问题
@SuppressWarnings("FieldMayBeFinal")
private volatile int refCnt = 1;
```

​	首先看第一个字段 refCntUpdater，它是 AtomicIntegerFieldUpdater 类型变量，通过原子的方式对成员变量进行更新等操作，以实现线程安全，消除锁。

2. 对象引用计数器

   每调用一次 retain 方法，引用计数器就会加 1，由于可能存在多线程并发调用，所以他的累加操作必须是线程安全的，下面看下它的具体实现细节：

```java
public ByteBuf retain() {
    for (;;) {
        int refCnt = this.refCnt;
        if (refCnt == 0) {
            throw new IllegalReferenceCountException(0, 1);
        }
        if (refCnt == Integer.MAX_VALUE) {
            throw new IllegalReferenceCountException(Integer.MAX_VALUE, 1);
        }
        if (refCntUpdater.compareAndSet(this, refCnt, refCnt + 1)) {
            break;
        }
    }
    return this;
}
```

​	通过自旋(自旋锁)对引用计数器进行加一操作，由于引用计数器的初始值为 1，如果申请和释放操作能够保证正确使用，则它的最小值为1。当被释放和被申请的次数相等时，就调用回收方法回收当前的 ByteBuf 对象。如果为 0，说明对象被意外、错误地引用，抛出 IllegalReferenceCountException。如果引用计数器达到整型数的最大值，就抛出越界异常 IllegalReferenceCountException。最后通过 compareAndSet 进行原子更新（CAS） 

​	释放计数器的方法：

```java
public final boolean release() {
    for (;;) {
        int refCnt = this.refCnt;
        if (refCnt == 0) {
            throw new IllegalReferenceCountException(0, -1);
        }

        if (refCntUpdater.compareAndSet(this, refCnt, refCnt - 1)) {
            if (refCnt == 1) {
                deallocate();
                return true;
            }
            return false;
        }
    }
}
```

​	与 retain 方法类似，它也是一个自旋循环里面进行判断和更新的。需要注意的是：当 refCnt == 1 时意味着申请和释放相等，说明对象引用已经不可达，该对象需要被释放和垃圾回收掉，则通过调用 deallocate 方法来释放 ByteBuf 对象。


#### 15.2.4 UnpooledHeapByteBuf 源码分析

---

​	UnpooledHeapByteBuf 是基于堆内存进行内存分配的字节缓冲区，它没有基于对象池技术实现，这就意味着每次 I/O 的读写都会创建一个新的 UnpooledHeapByteBuf，频繁进行大块内存的分配和回收对性能会造成一定影响，但是相比于堆外内存的申请和释放，它的成本还是会低一些。

​	相比于 PooledHeapByteBuf，UnpooledHeapByteBuf 的实现原理更加简单，也不容易出现内存管理方面的问题，因此在满足性能的情况下，推荐使用 UnpooledHeapByteBuf 。

 	UnpooledHeapByteBuf 代码实现：

1. 成员变量

```java
// 聚合一个 ByteBufAllocator 用于 UnpooledHeapByteBuf 的内存分配
private final ByteBufAllocator alloc;
// byte 数组作为缓冲区
private byte[] array;
// 用于实现 Netty ByteBuf 到 JDK NIO ByteBuffer 的转换
private ByteBuffer tmpNioBuf;
```

  	事实上，如果使用 JDK 的 ByteBuffer 替换 byte 数组也是可行的，直接使用 byte 数组的根本原因就是提升性能和更加便捷地进行位操作。JDK 的 ByteBuffer 底层实现也是 byte 数组

2. 动态扩展缓冲区

​	介绍 AbstractByteBuf 的时候，提到 ByteBuf 的实现类在最大容量范内能够实现自动扩张，下面看看 UnpooledHeapByteBuf 是如何实现的

```java
public ByteBuf capacity(int newCapacity) {
    // 保证 buffers content 没有被释放
    ensureAccessible();
    // 判断 容量大小 的合法性
    if (newCapacity < 0 || newCapacity > maxCapacity()) {
        throw new IllegalArgumentException("newCapacity: " + newCapacity);
    }

    int oldCapacity = array.length;
    if (newCapacity > oldCapacity) {
        byte[] newArray = new byte[newCapacity];
        System.arraycopy(array, 0, newArray, 0, array.length);
        setArray(newArray);
    } else if (newCapacity < oldCapacity) {
        byte[] newArray = new byte[newCapacity];
        int readerIndex = readerIndex();
        if (readerIndex < newCapacity) {
            int writerIndex = writerIndex();
            if (writerIndex > newCapacity) {
                writerIndex(writerIndex = newCapacity);
            }
            System.arraycopy(array, readerIndex, newArray, readerIndex, writerIndex - readerIndex);
        } else {
            setIndex(newCapacity, newCapacity);
        }
        // 类成员变量 array 指向 newArray
        // 类成员变量 tmpNioBuf 指向 null
        setArray(newArray);
    }
    return this;
}
```

3. 字节数组复制

   字节数组的复制，也是由子类实现，下面看看 UnpooledHeapByteBuf 的实现：

```java
public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length) {
    // 合法性校验
    checkSrcIndex(index, length, srcIndex, src.length);
    System.arraycopy(src, srcIndex, array, index, length);
    return this;
}
```

​	需要指出的是，ByteBuf 以 set 和 get 开头读写缓冲区的方法并不会修改读写索引。

4. 转换成 JDK ByteBuffer

​	ByteBuffer 提供了 wrap 方法，可以将 byte 数组转换成 ByteBuffer 对象，而 ByteBuf 正是基于 byte 数组实现。

```java
// ByteBuffer wrap 方法
public static ByteBuffer wrap(byte[] array,
       int offset, int length){
    try {
        return new HeapByteBuffer(array, offset, length);
    } catch (IllegalArgumentException x) {
        throw new IndexOutOfBoundsException();
    }
}
```

```java
// UnpooledHeapByteBuf 转 ByteBuffer
public ByteBuffer nioBuffer(int index, int length) {
    ensureAccessible();
    return ByteBuffer.wrap(array, index, length).slice();
}
```

#### 15.2.5 PooledByteBuf 内存池原理分析

​	由于 ByteBuf 内存池的实现涉及到的类和数据结构非常多，不能一一说明，只能从涉及原理角度来讲解内存池的实现：

 	1. PoolArena

​	Arena 本身是指一块区域，在内存管理中，Memory Arena 是指内存中的一大块连续的区域，PoolArena 就是 Netty 的内存池实现类。

​	为了集中管理内存的分配和释放，同时提高分配和释放内存时候的性能，就预先申请一大块内存，然后集中管理分配和释放。在这种设计思路下，预先申请的那一大块内存就被称为 Memory Arena。

​	不同的框架，Memory Arena 的实现不同，Netty 的  PoolArena 是由多个 Chunk 组成的大块内存区域，而每个 Chunk 则由一个或者多个 Page 组成，因此，对内存的组织和管理也就主要集中在如何管理和组织 Chunk 和 Page 了。

2. PoolChunk

​	Chunk 主要用来组织和管理多个 Page 的内存分配和释放，在 Netty 中，Chunk 中的 Page 被构建成一颗二叉树。

![Chunk中的Page结构](img\Chunk中的Page结构.png)

3. PoolSubPage

​	对于小于一个 Page 的内存，Netty 在 Page 中完成分配。每个 Page 会被切分成大小相等的多个存储块，存储块的大小由第一次申请的内存块大小决定。

#### 15.2.6 PooledDirectByteBuf 源码分析

​	PooledDirectByteBuf 基于内存池实现，与 UnPooledDirectHeapByteBuf 的唯一不同就是缓冲区的分配是销毁策略不同，其他功能都是等同的。

1. 创建字节缓冲区实例

​	由于采用内存池实现，所以新创建 PooledDirectByteBuf 对象时不能直接 new 一个实例，而是从内存池中获取，然后设置引用计数器的值。

```java
static PooledDirectByteBuf newInstance(int maxCapacity) {
    PooledDirectByteBuf buf = RECYCLER.get();
    buf.setRefCnt(1);
    buf.maxCapacity(maxCapacity);
    return buf;
}
```

2. 复制新的字节缓冲区实例

​	如果使用者确实需要复制一个新的实例，与原来的 PooledDirectByteBuf 独立，则调用它的 copy (int index, int length)



### 15.3 ByteBuf 相关的辅助类功能介绍

---

#### 15.3.1 ByteBufHolder

​	ByteBufHolder 是 ByteBuf 的容器，在 Netty 中，它非常有用。例如 HTTP 协议的请求消息和应答消息都可以携带消息体，这个消息体在 NIO ByteBuffer 中就是个 ByteBuffer 对象，在 Netty 中就是 ByteBuf 对象。由于不同的协议消息体可以包含不同的协议字段和功能，因此，需要对 ByteBuf 进行包装和抽象，不同的子类可以有不同的实现。

​	为了满足这些定制化的需求，Netty 抽象出了 ByteBufHolder 对象，它包含了一个 ByteBuf，另外还提供了一些其他实用的方法，使用者继承 ByteBufHolder 接口后可以按需封装自己的实现。

#### 15.3.2 ByteBufAllocator

​	ByteBufAllocator 是字节缓冲区分配器，按照 Netty 的缓冲区实现不同，共有两种不同的分配器：

- 基于内存池的字节缓冲区分配器
- 普通的字节缓冲区分配器

![ByteBufAllocator的API](img\ByteBufAllocator的API.png)

#### 15.3.3 CompositeByteBuf

​	CompositeByteBuf 允许将多个 ByteBuf 的实例组装到一起，形成一个统一的视图，有点类似于数据库将多个表的字段组装到一起统一用视图展示。

​	CompositeByteBuf 在一些场景下非常有用，例如某个协议的 POJO 对象包含两部分：消息头和消息体，他们都是 ByteBuf 对象。

#### 15.3.4 ByteBufUtil

​	ByteBufUtil 是一个非常有用的工具类，它提供了一系列静态方法用于操作 ByteBuf 对象。其中最有用的方法就是对字符串的编码和解码：

1. encodeString(ByteBufAllocator alloc, CharBuffer src, Charset charset)：对需要编码的字符串 src 按照指定的字符集 charset 进行编码，利用指定的 ByteBufAllocator 生成一个新的 ByteBuf；
2. decodeString(ByteBuffer src, Charset charset)：使用指定的 ByteBuffer 和 charset 进行对 Byteset 进行解码，获取解码后的字符串。

​	还有一个 非常有用的方法就是 hexDump，它能够将参数 ByteBuf 的内容以十六进制字符串的方式打印出来，用于输出日志或者打印码流，方便问题定位，提升系统的可维护性。



# 第十六章 Channel 和 Unsafe

### 16.1 Channel 功能说明

---

​	io.netty.channel.Channel 是 Netty 网络操作抽象类，它聚合了一组功能，包括但不限于网络的读、写，客户端发起连接，主动关闭连接，链路关闭，获取通信双方的网络地址等。它也包含了 Netty 框架相关的一些功能，包括获取该 Channel 的 EventLoop，获取缓冲分配器 ByteBufAllocator 和 pipeline 等。

#### 16.1.1 Channel 的工作原理

​	Channel 是 Netty 抽象出来的网络 I/O 读写相关的接口，为什么不使用 JDK NIO 原生的 Channel 而要另起炉灶，原因如下：

- JDK 的 SocketChannel 和 ServerSocketChannel 没有统一的 Channel 接口供业务开发者使用，对于用户而言，没有统一的操作视图，使用起来并不方便。
- JDK 的 SocketChannel 和 ServerSocketChannel 的主要职责就是网络 I/O 操作，由于他们是 SPI 类接口，由具体的虚拟机厂家来提供，所以通过继承 SPI 功能类来扩展其功能的难度很大；
- Netty 的 Channel 需要能够跟 Netty 的整体架构融合在一起，例如 I/O 模型、基于 ChannelPipeline 的定制模型，以及基于元数据描述配置化的 TCP 参数等，这些 JDK 的 SocketChannel 和 ServerSocketChannel 都没有提供
- 自定义的 Channel，功能实现更加灵活。

​	基于上述 4 个原因，Netty 重新设计了 Channel 接口，并且给予了很多不同的实现。它的设计原理比较简单，但是功能缺比较繁杂，主要的设计理念如下：

- 在 Channel 接口层，采用 Facade 模式进行统一封装，将网络 I/O 操作以及相关联的其他操作封装起来，统一对外提供；
- Channel 接口的定义尽量大而全，为 SocketChannel 和 ServerSocketChannel 提供统一的视图，由不同子类实现不同的功能，公共功能在抽象父类中实现，最大程度地实现功能和接口的重用；
- **具体实现采用聚合而非包含的方式，将相关的功能类聚合在 Channel 中，由 Channel 统一负责分配和调度，功能实现更加灵活；**

####16.1.2 Channel 的功能介绍

​	Channel 的功能比较繁杂，我们通过分类的方式对它的主要功能进行介绍。

1. 网络 I/O 操作

   Channel  网络 I/O 相关的方法定义如图：

   ![Channel网络 IO 相关的方法定义](img\Channel网络 IO 相关的方法定义.png)

   下面我们对这些 API 的功能进行分类说明，读写相关的 API 列表：

   - Channel read()：从当前的 Channel 中读取数据到第一个 inbound 缓冲区中，如果数据被成功读取，触发 ChannelHandler.channelRead(ChannelHandlerContext, Object) 事件。读取操作 API 调用完成之后，紧接着会触发 ChannelHandler.channelReadComplete(Channel HandlerContext) 事件，这样业务的 ChannelHandler 可以决定是否需要继续读取数据。如果已经有读操作请求被挂起，则后续的读操作会被忽略。
   - ChannelFuture write(Object msg)：请求将当前的 msg 通过 ChannelPipeline 写入到目标 Channel 中。注意，write 操作只是将消息存入到消息发送环形数组中，并没有真正被发送，只有调用 flush 操作才会被写入到 Channel 中，发送给对方。
   - ChannelFuture write(Object msg, ChannelPromise promise)：功能与 write(Object msg) 相同，但是携带了 ChannelPromise 参数负责设置写入操作的结果。
   - ChannelFuture writeAndFlush(Object msg, ChannelPromise promise)：与上一个功能类似，不同之处在于它会将消息写入 Channel 中发送，等价于单独调用 write 和 flush 操作的组合。
   - Channel flush()：将之前写入到发送环形数组中的消息全部写入到目标 Channel 中，发送给通信对方。
   - ChannelFuture close(ChannelPromise promise)：主动关闭当前连接，通过 ChannelPromise 设置操作结果并进行结果通知，无论操作是否成功，都可以通过 ChannelPromise 获取操作结果。该操作会级联触发 ChannelPipeline 中所有 ChannelHandler 的 ChannelHandler.close(ChannelHandlerContext, ChannelPromise promise) 事件。
   - ChannelFuture disconnect(ChannelPromise promise)：请求断开与远程通信对端的连接并使用 ChannelPromise 来获取操作结果的通知消息。该方法会级联触发 ChannelHandler.disconnect(ChannelHandlerContext, ChannelPromise) 事件。
   - ChannelFuture connect(SocketAddress remoteAddress)：客户端使用指定的服务端地址 remoteAddress 发起连接请求，如果连接因为应答超时而失败，ChannelFuture 中的操作结果就是 ConnectTimeoutException 异常；如果连接被拒绝，操作结果为 ConnectException。该方法会级联触发 ChannelHandler.connect(ChannelHandlerContext, SocketAddress, SocketAddress, ChannelPromise) 事件。
   - ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress)：与上一个方法类似，唯一不同就是先绑定指定的本地地址 localAddress，然后再连接服务端。
   - ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise)：与上上个方法类似，唯一不同的就是携带了 ChannelPromise 参数用于写入操作结果。
   - ChannelFuture bind(SocketAddress localAddress)：绑定指定的本地 Socket 地址 localAddress，该方法会级联触发 ChannelHandler.bind(ChanneHandlerContext, SocketAddress, ChannelPromise) 事件。
   - ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise)：与上一个功能类似，多携带了一个 ChannelPromise 用于写入操作结果。
   - ChannelConfig config()：获取当前 Channel 的配置信息，例如 CONNECT_TIMEOUT_MILLIS
   - boolean isOpen()：判断当前 Channel 是否已经打开。
   - boolean isRegistered()：判断当前 Channel 是否已经注册到 EventLoop 上
   - boolean isActive()：判断当前 Channel 是否已经处于激活状态
   - ChannelMetadata metadate()：获取当前 Channel 的元数据描述信息，包括 TCP 参数配置等
   - SocketAddress localAddress()：获取当前 Channel 的本地绑定地址
   - SocketAddress remoteAddress()：获取当前 Channel 通信的远程 Socket 地址

2. 其他常用的 API 功能说明
   - eventLoop()：Channel 需要注册到 EventLoop 的多路复用器上，用于处理 I/O 事件，通过 eventLoop() 方法可以获取到 Channel 注册的 EventLoop。EventLoop 本质上就是处理网络读写事件的 Reactor 线程。在 Netty 中，它不仅仅用来处理网络事件，也可以用来执行定时任务和用户自定义 NioTask 等任务
   - metadate()：在 TCP 协议中，当创建 Socket 的时候需要指定 TCP 参数，例如接收和发送的 TCP 缓冲区大小、TCP的超时时间、是否重用地址等。在 Netty 中，每个 Channel 对应一个物理连接，每个连接都有自己的TCP 参数配置。所以，Channel 会聚合一个 ChannelMetadate 用来对 TCP 参数提供元数据描述信息，通过 metadata() 方法就可以获取当前 Channel 的 TCP 参数配置。
   - parent()：对于服务端 Channel 而言，它的父 Channel 为空；对于客户端 Channel，它的父 Channel 就是创建他的 ServerSocketChannel。
   - id()：它返回 ChannelId 对象，ChannelId 是 Channel 的唯一标识，它的生成策略有多种。

### 16.2 Channel 源码分析

---

​	Channe 的实现子类非常多，继承关系复杂，从学习的角度抽取两个最重要的：

- io.netty.channel.socket.nio.NioServerSocketChannel

- io.netty.channel.socket.nio.NioSocketChannel

  服务端 NioServerSocketChannel 的继承关系类图：

  ![服务端 NioServerSocketChannel 的继承关系类图](img\服务端 NioServerSocketChannel 的继承关系类图.png)

  客户端 NioSocketChannel 的继承关系类图：

![客户端 NioSocketChannel 的继承关系类图](img\客户端 NioSocketChannel 的继承关系类图.png)

#### 16.2.2 AbstractChannel 源码分析

1. 成员变量定义

   在分析 AbstractChannel 源码之前，我们先看下它的成员变量定义：

   ```java
   static final ClosedChannelException CLOSED_CHANNEL_EXCEPTION = new ClosedChannelException();
   static final NotYetConnectedException NOT_YET_CONNECTED_EXCEPTION = new NotYetConnectedException();
   ```

   根据之前的 Channel 原理分析，AbstractChannel 采用聚合的方式封装各种功能，从成员变量的定义可以看出，他聚合了以下内容：

   ```java
   // 代表父类 Channel
   private final Channel parent;
   // 采用默认方式生成的全局唯一 ID
   private final ChannelId id = DefaultChannelId.newInstance();
   // Unsafe 实例
   private final Unsafe unsafe;
   // 当前 Channel 对应的 DefaultChannelPipeline
   private final DefaultChannelPipeline pipeline;
   private final ChannelFuture succeededFuture = new SucceededChannelFuture(this, null);
   private final VoidChannelPromise voidPromise = new VoidChannelPromise(this, true);
   private final VoidChannelPromise unsafeVoidPromise = new VoidChannelPromise(this, false);
   private final CloseFuture closeFuture = new CloseFuture(this);
   
   private volatile SocketAddress localAddress;
   private volatile SocketAddress remoteAddress;
   // 当前 Channel 注册的 EventLoop
   private final EventLoop eventLoop;
   private volatile boolean registered;
   
   /** Cache for the string representation of this channel */
   private boolean strValActive;
   private String strVal;
   ```

​	AbstractChannel 聚合了所有 Channel 使用到的能力对象，由 AbstractChannel 提供初始化和统一封装，如果功能和子类强相关，则定义成抽象方法由子类具体实现。

2. 核心 API 源码分析

​	首先看下网络读写操作，前面介绍网络 I/O 操作时讲到，他会触发 ChannelPipeline 中对应的事件方法。Netty 基于事件驱动，我们也可以理解为当 Channel 进行 I/O 操作时会产生对应的 I/O 事件，然后驱动事件在 ChannelPipeline 中传播，由对应的 ChannelHandler 对事件进行拦截和处理，不关心的事件可以直接忽略。采用事件驱动的方式可以非常轻松地通过事件定义来划分事件拦截切面，方便业务的定制和功能扩展，相比于 AOP，其性能更高，但是功能却基本等价。

​	网络 I/O 操作直接调用 DefaultChannelPipeline 的相关方法，由 DefaultChannelPipeline 中对应的 ChannelHandler 进行具体的逻辑处理

```java
@Override
public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
    return pipeline.connect(remoteAddress, localAddress);
}

@Override
public ChannelFuture disconnect() {
    return pipeline.disconnect();
}

@Override
public ChannelFuture close() {
    return pipeline.close();
}

@Override
public Channel flush() {
    pipeline.flush();
    return this;
}
```

​	AbstractChannel 也提供了一些公共 API 的具体实现，例如 localAddress() 和 remoteAddress() 方法，源码如下：

```java
public SocketAddress remoteAddress() {
    // 先从缓存的成员变量中获取
    SocketAddress remoteAddress = this.remoteAddress;
    // 如果第一次调用为空 需要通过 unsafe 的 remoteAddress 获取
    if (remoteAddress == null) {
        try {
            // unsafe().remoteAddress() 是个抽象方法，由对应的 Channel 子类实现
            this.remoteAddress = remoteAddress = unsafe().remoteAddress();
        } catch (Throwable t) {
            // Sometimes fails on a closed socket in Windows.
            return null;
        }
    }
    return remoteAddress;
}
```

#### 16.2.3 AbstractNioChannel 源码分析

1. 成员变量定义

```java
// NIO Channel、NioSocketChannel、NioServerSocketChannel 共用，定义一个 SocketChannel 和 ServerSocketChannel 的公共父类 SelectableChannel，用于设置 SelectableChannel 参数和进行 I/O 操作
private final SelectableChannel ch;
// 代表 JDK SelectionKey 的 OP_READ
protected final int readInterestOp;
// SelectionKey 是 Channel 注册到 EventLoop 后返回的选择键，由于 Channel 会面临多个业务线程的并发写操作，所以使用 volatile 修饰，保证修改对于其他业务线程的可见性
private volatile SelectionKey selectionKey;
private volatile boolean inputShutdown;

/**
  * The future of the current connection attempt.  If not null, subsequent
  * connection attempts will fail.
  */
// 代表连接操作结果
private ChannelPromise connectPromise;
// 连接超时定时器
private ScheduledFuture<?> connectTimeoutFuture;
// 请求的通信地址信息
private SocketAddress requestedRemoteAddress;
```

2. 核心 API 源码分析

- Channel 的注册：

```java
protected void doRegister() throws Exception {
    // 标识注册是否成功
    boolean selected = false;
    for (;;) {
        try {
            // 将当前的 Channel 注册到 EventLoop 的多路复用器上
            // 注册 0 说明 对任何事件都不感兴趣 仅仅完成注册操作
            // 注册的时候可以指定附件，后续 Channel 接收到网络事件通知时可以从 SelectionKey 中重新获取之前的附件进行处理，此处将 AbstractNioChannel 的实现子类自身当作附件注册。如果 Channel 注册成功，则返回 selectionKey，通过 selectionKey 可以从多路复用器中获取 Channel 对象。
            // 当前注册返回的 selectionKey 已经被取消，则抛出 CancelledKeyException，如果是第一次处理该异常，调用多路复用器的 selectNow() 方法将已经取消的 selectionKey 从多路复用器中删除掉。
            selectionKey = javaChannel().register(eventLoop().selector, 0, this);
            return;
        } catch (CancelledKeyException e) {
            if (!selected) {
                // Force the Selector to select now as the "canceled" SelectionKey may still be
                // cached and not removed because no Select.select(..) operation was called yet.
                eventLoop().selectNow();
                selected = true;
            } else {
                // 下一次注册操作 如果仍然发生 CancelledKeyException，说明我们无法删除已经被取消的 selectionKey，如果仍然发生 CancelledKeyException，说明可能是 NIO 的相关类库存在不可恢复的 BUG，直接向上层抛出该异常进行统一处理。
                // We forced a select operation on the selector before but the SelectionKey is still cached
                // for whatever reason. JDK bug ?
                throw e;
            }
        }
    }
}
```
- doBeginRead()：

```java
protected void doBeginRead() throws Exception {
    // 先判断下 Channel 是否关闭，如果处于关闭中，则直接返回。
    if (inputShutdown) {
        return;
    }
	//获取当前的 SelectionKey 进行判断，如果可用，说明 Channel 当前状态正常
    final SelectionKey selectionKey = this.selectionKey;
    if (!selectionKey.isValid()) {
        return;
    }
	// 获取 Channel 当前的操作位
    final int interestOps = selectionKey.interestOps();
    // 与读操作位比较 不一致则置为读操作位
    if ((interestOps & readInterestOp) == 0) {
        selectionKey.interestOps(interestOps | readInterestOp);
    }
}
```

#### 16.2.4 AbstractNioByteChannel 源码分析

1. 成员变量

```java
// 负责继续写半包消息
private Runnable flushTask;
```

2. 核心 API 源码分析

- doWrite(ChannelOutboundBuffer in)

```java
protected void doWrite(ChannelOutboundBuffer in) throws Exception {
    int writeSpinCount = -1;
    for (;;) {
        // 从 ChannelOutboundBuffer 中弹出一条消息
        Object msg = in.current(true);
        // 如果为空，说明消息发送数组中的所有待发送的消息都已经发送完成
        if (msg == null) {
            // Wrote all messages.
            // 清除写标识 退出循环
            clearOpWrite();
            break;
        }
        // 消息不为空 继续处理
        if (msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) msg;
            int readableBytes = buf.readableBytes();
            if (readableBytes == 0) {
                in.remove();
                continue;
            }
			// 写半包标识
            boolean setOpWrite = false;
            //消息是否全部发送
            boolean done = false;
            // 发送的总消息字节数
            long flushedAmount = 0;
            // writeSpinCount 写半包最大循环次数
            if (writeSpinCount == -1) {
                writeSpinCount = config().getWriteSpinCount();
            }
            // 设置写半包最大循环次数的原因是：当循环发送的时候 I/O 线程会一直尝试进行写操作，此时 I/O 线程无法处理其他 I/O 操作，例如读新的消息或者执行定时任务和 NioTask 等，如果网络 I/O 阻塞或者对方接受消息太慢，可能会导致线程假死
            for (int i = writeSpinCount - 1; i >= 0; i --) {
                // 调用 doWriteBytes 进行消息发送，不同的 Channel 子类有不同的实现，因此它是抽象方法。如果本次发送的字节数为 0，说明发送TCP缓冲区已满，发生了 ZERO_WINDOW。此时再次发送仍然可能出现写 0 字节，空循环会占用 CPU 的资源，导致 I/O 线程无法处理其他 I/O 操作，所以将写半包标识 setOpWrite 设置为 true，退出循环，释放 I/O 线程。
                int localFlushedAmount = doWriteBytes(buf);
                if (localFlushedAmount == 0) {
                    setOpWrite = true;
                    break;
                }
				// 如果发送的字节数大于0，则对发送总数进行计数
                flushedAmount += localFlushedAmount;
                // 判断当前消息是否已经发送成功(缓冲区没有可读字节)
                if (!buf.isReadable()) {
                    done = true;
                    break;
                }
            }
            // 更新发送进度信息
            in.progress(flushedAmount);
			// 如果发送成功 则将已经发送的消息从发送数组中删除
            if (done) {
                in.remove();
            } else {
                // 处理半包发送任务
                // 将SelectionKey 设置为 OP_WRITE，多路复用器会不断轮询对应的 Channel，用于处理没有发送完成的半包消息，直到清除 SelectionKey 的 OP_WRITE 操作位。 如果有数据写入的TCP的缓冲区，则启动独立的 Runnable 来负责发送半包消息
                incompleteWrite(setOpWrite);
                break;
            }
        } else if (msg instanceof FileRegion) {
            // 文件传输分支
            FileRegion region = (FileRegion) msg;
            boolean setOpWrite = false;
            boolean done = false;
            long flushedAmount = 0;
            if (writeSpinCount == -1) {
                writeSpinCount = config().getWriteSpinCount();
            }
            for (int i = writeSpinCount - 1; i >= 0; i --) {
                long localFlushedAmount = doWriteFileRegion(region);
                if (localFlushedAmount == 0) {
                    setOpWrite = true;
                    break;
                }

                flushedAmount += localFlushedAmount;
                if (region.transfered() >= region.count()) {
                    done = true;
                    break;
                }
            }

            in.progress(flushedAmount);

            if (done) {
                in.remove();
            } else {
                incompleteWrite(setOpWrite);
                break;
            }
        } else {
            throw new UnsupportedOperationException("unsupported message type: " + StringUtil.simpleClassName(msg));
        }
    }
}
```

#### 16.2.5 AbstractNioMessageChannel 源码分析

​	AbstractNioMessageChannel 没有成员变量，它的主要实现方法只有一个：doWrite(ChannelOutboundBuffer in)，下面首先看下它的源码：

```java
protected void doWrite(ChannelOutboundBuffer in) throws Exception {
    final SelectionKey key = selectionKey();
    final int interestOps = key.interestOps();

    for (;;) {
        // 弹出一条消息进行处理
        Object msg = in.current();
        if (msg == null) {
            // Wrote all messages.
            if ((interestOps & SelectionKey.OP_WRITE) != 0) {
                // 清除写标识
                key.interestOps(interestOps & ~SelectionKey.OP_WRITE);
            }
            break;
        }

        boolean done = false;
        for (int i = config().getWriteSpinCount() - 1; i >= 0; i --) {
            // 判断消息是否发送成功
            if (doWriteMessage(msg, in)) {
                done = true;
                break;
            }
        }

        if (done) {
            in.remove();
        } else {
            // Did not write all messages.
            // 设置写半包标识
            if ((interestOps & SelectionKey.OP_WRITE) == 0) {
                key.interestOps(interestOps | SelectionKey.OP_WRITE);
            }
            break;
        }
    }
}
```

​	AbstractNioMessageChannel 和 AbstractNioByteChannel 的消息发送实现比较类似，不同之处在于：一个发送的是 ByteBuf 或者 FileRegion，他们可以直接被发送：另一个发送的则是 POJO 对象。

####16.2.6 AbstractNioMessageServerChannel 源码分析

​	AbstractNioMessageServerChannel 的实现非常简单，他定义了一个 EventLoopGroup 类型的 childGroup，用于给新接入的客户端 NioSocketChannel 分配 EventLoop，它的源码实现：

```java
public abstract class AbstractNioMessageServerChannel extends AbstractNioMessageChannel implements ServerChannel {

    private final EventLoopGroup childGroup;

    protected AbstractNioMessageServerChannel(
            Channel parent, EventLoop eventLoop, EventLoopGroup childGroup, SelectableChannel ch, int readInterestOp) {
        super(parent, eventLoop, ch, readInterestOp);
        this.childGroup = childGroup;
    }

    /**
     * 每当服务端接入一个新的客户端连接 NioSocketChannel 时，都会调用 childEventLoopGroup 方法获取 EventLoopGroup 线程组，用于给 NioSocketChannel 分配 Reactor 线程 EventLoop，相关分配代码见后续代码
     */
    @Override
    public EventLoopGroup childEventLoopGroup() {
        return childGroup;
    }
}
```

```java
// 调用 childEventLoopGroup 方法获取 EventLoopGroup 线程组，用于给 NioSocketChannel 分配 Reactor 线程 EventLoop
protected int doReadMessages(List<Object> buf) throws Exception {
    SocketChannel ch = javaChannel().accept();

    try {
        if (ch != null) {
            buf.add(new NioSocketChannel(this, childEventLoopGroup().next(), ch));
       ...
```

#### 16.2.7 NioServerSocketChannel 源码分析

1. 成员变量定义和静态方法：

```java
private static final ChannelMetadata METADATA = new ChannelMetadata(false);

private static final InternalLogger logger = InternalLoggerFactory.getInstance(NioServerSocketChannel.class);

private static ServerSocketChannel newSocket() {
    try {
        // 打开新的 ServerSocketChannel 通道
        return ServerSocketChannel.open();
    } catch (IOException e) {
        throw new ChannelException(
                "Failed to open a server socket.", e);
    }
}
// 用于配置 ServerSocketChannel 的 TCP 参数
private final ServerSocketChannelConfig config;
```

2. 相关的接口实现：

```java
@Override
public boolean isActive() {
    // 判断服务端监听端口是否处于绑定状态，他的 remoteAddress 为空
    return javaChannel().socket().isBound();
}

@Override
public InetSocketAddress remoteAddress() {
    return null;
}

@Override
protected ServerSocketChannel javaChannel() {
    return (ServerSocketChannel) super.javaChannel();
}

@Override
protected SocketAddress localAddress0() {
    return javaChannel().socket().getLocalSocketAddress();
}

@Override
protected void doBind(SocketAddress localAddress) throws Exception {
    // backlog：允许客户端排队的最大长度
    javaChannel().socket().bind(localAddress, config.getBacklog());
}
```

3. doReadMessage(List<Object> buf)

```java
protected int doReadMessages(List<Object> buf) throws Exception {
    // 接收新的客户端连接
    SocketChannel ch = javaChannel().accept();

    try {
        if (ch != null) {
            // 创建新的 NioSocketChannel，加入到List<Object> buf 中，最后返回 1，表示服务端消息读取成功
            // 对于 NioServerSocketChannel，它的读取操作就是接收客户端的连接，创建 NioSocketChannel 对象
            buf.add(new NioSocketChannel(this, childEventLoopGroup().next(), ch));
            return 1;
        }
    } catch (Throwable t) {
        logger.warn("Failed to create a new channel from an accepted socket.", t);

        try {
            ch.close();
        } catch (Throwable t2) {
            logger.warn("Failed to close a socket.", t2);
        }
    }

    return 0;
}
```

4. 无关接口实现

```java
// Unnecessary stuff
// 与服务端 Channel 无关的接口定义，这些方法是客户端 Channel 相关的，服务端Channel 无须实现，如果这些方法被误调，则返回 UnsupportedOperationException
@Override
protected boolean doConnect(
    SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
    throw new UnsupportedOperationException();
}

@Override
protected void doFinishConnect() throws Exception {
    throw new UnsupportedOperationException();
}

@Override
protected SocketAddress remoteAddress0() {
    return null;
}

@Override
protected void doDisconnect() throws Exception {
    throw new UnsupportedOperationException();
}

@Override
protected boolean doWriteMessage(Object msg, ChannelOutboundBuffer in) throws Exception {
    throw new UnsupportedOperationException();
}
```

####16.2.8 NioSocketChannel 源码分析

1. 连接方法实现

```java
@Override
protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
    // 绑定本地地址
    if (localAddress != null) {
        javaChannel().socket().bind(localAddress);
    }

    boolean success = false;
    try {
        // 发起 TCP 连接有三种结果
        // 1.连接成功 2.暂时没有连接上，结果不确定，返回false 3.连接失败，抛出异常
        boolean connected = javaChannel().connect(remoteAddress);
        if (!connected) {
            selectionKey().interestOps(SelectionKey.OP_CONNECT);
        }
        success = true;
        return connected;
    } finally {
        if (!success) {
            // 连接失败 有异常 需要关闭客户端连接
            doClose();
        }
    }
}
```

2. 写半包

   写操作的实现比较复杂

```java
protected void doWrite(ChannelOutboundBuffer in) throws Exception {
    for (;;) {
        // Do non-gathering write for a single buffer case.
        final int msgCount = in.size();
        if (msgCount <= 1) {
            super.doWrite(in);
            return;
        }

        // Ensure the pending writes are made of ByteBufs only.
        ByteBuffer[] nioBuffers = in.nioBuffers();
        if (nioBuffers == null) {
            super.doWrite(in);
            return;
        }
		// 批量发送缓冲区消息之前，先对一系列的局部变量进行赋值
        // 需要发送的 ByteBuffer 数组个数
        int nioBufferCnt = in.nioBufferCount();
        // 需要发送的总字节数
        long expectedWrittenBytes = in.nioBufferSize();
		// 获取 NIO 的 SocketChannel
        final SocketChannel ch = javaChannel();
        long writtenBytes = 0;
        // 是否发送完成
        boolean done = false;
        // 是否有写半包标识设置
        boolean setOpWrite = false;
        for (int i = config().getWriteSpinCount() - 1; i >= 0; i --) {
            // NIO SocketChannel write 三个参数：
            // 1.需要发送的 ByteBuffer 数组 2.数组的偏移量 3.发送 ByteBuffer 个数
            // 返回值是写入 SocketChannel 的字节个数
            final long localWrittenBytes = ch.write(nioBuffers, 0, nioBufferCnt);
            // 如果写入的字节数为0，很可能是 TCP 发送缓冲区已满
            if (localWrittenBytes == 0) {
                // 设置写半包标识为 true 然后退出循环
                // 其实这个标识 是在 incompleteWrite(setOpWrite);方法中
                // 来标识是否需要去flush，因为如果localWrittenBytes == 0 的话 是不需要去flush的
                setOpWrite = true;
                break;
            }
            // 需要发送的字节数更新
            expectedWrittenBytes -= localWrittenBytes;
            // 已经发送的字节数更新
            writtenBytes += localWrittenBytes;
            // 判断消息是否全部发送完成
            if (expectedWrittenBytes == 0) {
                done = true;
                break;
            }
        }

        if (done) {
            // Release all buffers
            for (int i = msgCount; i > 0; i --) {
                in.remove();
            }

            // Finish the write loop if no new messages were flushed by in.remove().
            if (in.isEmpty()) {
                // 清除写半包标识
                clearOpWrite();
                break;
            }
        } else {
            // Did not write all buffers completely.
            // Release the fully written buffers and update the indexes of the partially written buffer.
			// 当缓冲区中的消息没有发送完成，甚至某个 ByteBuf 只发送了几个字节，出现“写半包”时
            for (int i = msgCount; i > 0; i --) {
                final ByteBuf buf = (ByteBuf) in.current();
                final int readerIndex = buf.readerIndex();
                // 获取 ByteBuf 的可读字节数
                final int readableBytes = buf.writerIndex() - readerIndex;
				// 如果已经发送的字节数 大于 当前 ByteBuf 的可读字节数，说明该 ByteBuf 已经全部发送完成了
                if (readableBytes < writtenBytes) {
                    // 更新发送进度
                    in.progress(readableBytes);
                    // 移除已完成发送的 ByteBuf，释放资源
                    in.remove();
                    // 更新已发送字节数，在下次比较时，需要减去这次比较中移除的 ByteBuf 字节数
                    writtenBytes -= readableBytes;
                } else if (readableBytes > writtenBytes) {
                    // 可读字节数大于已发送字节数 表明该 ByteBuf 没有完全发送 也就是出现了“写半包”问题
                    // 更新该 ByteBuf 的可读索引
                    buf.readerIndex(readerIndex + (int) writtenBytes);
                    // 更新发送进度
                    in.progress(writtenBytes);
                    break;
                } else { 
                    // readableBytes == writtenBytes
                    // 最后一组ByteBuf 整包全部发送 更新发送进度
                    in.progress(readableBytes);
                    // 移除 该ByteBuf
                    in.remove();
                    break;
                }
            }
			// 循环发送操作完成之后，根据发送结果，更新 SocketChannel 的操作位，如果setOpWrite为true，则说明没有全部发送完成，设置写半包标识，等待下一次Reactor线程轮询到时，再次发送
            incompleteWrite(setOpWrite);
            break;
        }
    }
}
```

3. 读写操作

   NioSocketChannel 的读写操作实际上是基于 NIO 的 SocketChannel 和 Netty 的 ByteBuf 封装而成

```java
@Override
protected int doReadBytes(ByteBuf byteBuf) throws Exception {
    return byteBuf.writeBytes(javaChannel(), byteBuf.writableBytes());
}

@Override
public int writeBytes(ScatteringByteChannel in, int length) throws IOException {
    ensureWritable(length);
    int writtenBytes = setBytes(writerIndex, in, length);
    if (writtenBytes > 0) {
        writerIndex += writtenBytes;
    }
    return writtenBytes;
}

@Override
public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException {
    ensureAccessible();
    try {
        // 从 SocketChannel 中读取字节数组到缓冲区 java.nio.ByteBuffer 中
        return in.read((ByteBuffer) internalNioBuffer().clear().position(index).limit(index + length));
    } catch (ClosedChannelException e) {
        return -1;
    }
}
```



### 16.3 Unsafe 功能说明

---

​	Unsafe 接口实际上是 Channel 接口的辅助接口，它不应该被用户代码直接调用。实际的 I/O 读写操作都是由 Unsafe 接口负责完成的。

![Unsafe-API1](img\Unsafe-API1.png)

![Unsafe-API2](img\Unsafe-API2.png)



### 16.4 Unsafe 源码分析

---

​	实际的网络 I/O 操作基本都是由 Unsafe 功能类负责实现的

#### 16.4.1 Unsafe 继承关系类图

![Unsafe 继承关系图](img\Unsafe 继承关系图.png)

####16.4.2 AbstractUnsafe 源码分析

1. register() 方法

```java
/**
 * register 方法主要用于将当前 Unsafe 对应的 Channel 注册到 EventLoop 的多路复用器上，然后调用 DefaultChannelPipeline 的 fireChannelRegister 方法。
 * 如果 Channel 被激活，则调用 DefaultChannelPipeline 的 fireChannelActive 方法
 */
@Override
public final void register(final ChannelPromise promise) {
    // 判断当前所在的线程是否是 Channel 对应的 NioEventLoop 线程，如果是同一个线程，则不存在多线程并发操作问题，直接调用 register() 进行注册
    if (eventLoop.inEventLoop()) {
        register0(promise);
    } else {
        // 如果是用户线程或者其他线程发起的注册操作，则将注册操作封装成 Runnable，放到 NioEventLoop 任务队列中执行。如果直接执行 register0 方法，会存在多线程并发操作 Channel 的问题
        try {
            eventLoop.execute(new Runnable() {
                @Override
                public void run() {
                    register0(promise);
                }
            });
        } catch (Throwable t) {
            logger.warn(
                "Force-closing a channel whose registration task was not accepted by an event loop: {}",
                AbstractChannel.this, t);
            closeForcibly();
            closeFuture.setClosed();
            promise.setFailure(t);
        }
    }
}
```

​	register0() 方法

```java
private void register0(ChannelPromise promise) {
    try {
        // check if the channel is still open as it could be closed in the mean time when the register
        // call was outside of the eventLoop
        if (!ensureOpen(promise)) {
            return;
        }
        // 如果 doRegister() 没有抛出异常，则说明注册成功
        doRegister();
        registered = true;
        // 将 ChannelPromise 的结果设置为成功
        promise.setSuccess();
        pipeline.fireChannelRegistered();
        // 判断当前的 Channel 是否已经被激活
        if (isActive()) {
            // 已激活 调用 fireChannelActive()
            pipeline.fireChannelActive();
        }
    } catch (Throwable t) {
        // Close the channel directly to avoid FD leak.
        closeForcibly();
        closeFuture.setClosed();
        if (!promise.tryFailure(t)) {
            logger.warn(
                "Tried to fail the registration promise, but it is complete already. " +
                "Swallowing the cause of the registration failure:", t);
        }
    }
}
```
​	doRegister() 方法

```java
protected void doRegister() throws Exception {
    boolean selected = false;
    for (;;) {
        try {
            selectionKey = javaChannel().register(eventLoop().selector, 0, this);
            return;
        } catch (CancelledKeyException e) {
            if (!selected) {
                // Force the Selector to select now as the "canceled" SelectionKey may still be
                // cached and not removed because no Select.select(..) operation was called yet.
                eventLoop().selectNow();
                selected = true;
            } else {
                // We forced a select operation on the selector before but the SelectionKey is still cached
                // for whatever reason. JDK bug ?
                throw e;
            }
        }
    }
}
```

2. bind 方法

   bind 方法主要用于绑定指定的端口，对于服务端，用于绑定监听端口，可以设置 backlog 参数；对于客户端，主要用于指定客户端 Channel 的本地绑定 Socket 地址

```java
@Override
public final void bind(final SocketAddress localAddress, final ChannelPromise promise) {
    if (!ensureOpen(promise)) {
        return;
    }

    // See: https://github.com/netty/netty/issues/576
    if (!PlatformDependent.isWindows() && !PlatformDependent.isRoot() &&
        Boolean.TRUE.equals(config().getOption(ChannelOption.SO_BROADCAST)) &&
        localAddress instanceof InetSocketAddress &&
        !((InetSocketAddress) localAddress).getAddress().isAnyLocalAddress()) {
        // Warn a user about the fact that a non-root user can't receive a
        // broadcast packet on *nix if the socket is bound on non-wildcard address.
        logger.warn(
            "A non-root user can't receive a broadcast packet if the socket " +
            "is not bound to a wildcard address; binding to a non-wildcard " +
            "address (" + localAddress + ") anyway as requested.");
    }

    boolean wasActive = isActive();
    try {
        doBind(localAddress);
    } catch (Throwable t) {
        // 绑定本地端口发生异常，则将异常设置到 ChannelPromise 中用于通知 ChannelFuture，随后调用 closeIfClosed 方法来关闭 Channel
        promise.setFailure(t);
        closeIfClosed();
        return;
    }
    if (!wasActive && isActive()) {
        invokeLater(new Runnable() {
            @Override
            public void run() {
                pipeline.fireChannelActive();
            }
        });
    }
    promise.setSuccess();
}
```

```java
// NioServerSocketChannel doBind 实现
@Override
protected void doBind(SocketAddress localAddress) throws Exception {
    javaChannel().socket().bind(localAddress, config.getBacklog());
}
```

```java
// NioSocketChannel doBind 实现
@Override
protected void doBind(SocketAddress localAddress) throws Exception {
    javaChannel().socket().bind(localAddress);
}
```

3. disconnect 方法

```java
// 用于客户端或者服务端主动关闭连接
@Override
public final void disconnect(final ChannelPromise promise) {
    boolean wasActive = isActive();
    try {
        doDisconnect();
    } catch (Throwable t) {
        promise.setFailure(t);
        closeIfClosed();
        return;
    }
    if (wasActive && !isActive()) {
        invokeLater(new Runnable() {
            @Override
            public void run() {
                pipeline.fireChannelInactive();
            }
        });
    }
    promise.setSuccess();
    closeIfClosed(); // doDisconnect() might have closed the channel
}
```

4. close 方法

```java
@Override
public final void close(final ChannelPromise promise) {
    // 在链路关闭之前先判断是否处于刷新状态
    if (inFlush0) {
        // 处于刷新状态，需要等到所有消息发送完成再关闭链路
        invokeLater(new Runnable() {
            @Override
            public void run() {
                close(promise);
            }
        });
        return;
    }
	// 已经关闭 无需重复关闭
    if (closeFuture.isDone()) {
        // Closed already.
        promise.setSuccess();
        return;
    }

    boolean wasActive = isActive();
     // 将消息发送缓冲区设置为空，通知 JVM 进行内存回收
    ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
    this.outboundBuffer = null; // Disallow adding any messages and flushes to outboundBuffer.

    try {
        // 执行关闭操作
        doClose();
        closeFuture.setClosed();
        promise.setSuccess();
    } catch (Throwable t) {
        closeFuture.setClosed();
        promise.setFailure(t);
    }

    // Fail all the queued messages
    try {
        // 设置 failFlushed 原因
        outboundBuffer.failFlushed(CLOSED_CHANNEL_EXCEPTION);
        // 释放缓冲区的消息
        outboundBuffer.close(CLOSED_CHANNEL_EXCEPTION);
    } finally {
		// 构造 链路关闭 通知 Runnable 放到 NioEventLoop 中执行
        if (wasActive && !isActive()) {
            invokeLater(new Runnable() {
                @Override
                public void run() {
                    pipeline.fireChannelInactive();
                }
            });
        }
		// 将 Channel 从多路复用器上取消注册
        deregister();
    }
}
```

5. write 方法

```java
/**
 * write方法实际上将消息添加到环形发送数组中，并不是真正的写 Channl
 */
@Override
public void write(Object msg, ChannelPromise promise) {
    // Channel 没有处于激活状态，说明 TCP 链路还没有真正建立成功
    if (!isActive()) {
        // Mark the write request as failure if the channel is inactive.
        // Channel 打开，但是TCP链路没有尚未建立
        if (isOpen()) {
            promise.tryFailure(NOT_YET_CONNECTED_EXCEPTION);
        } else {
            // Channel已经关闭
            promise.tryFailure(CLOSED_CHANNEL_EXCEPTION);
        }
        // release message now to prevent resource-leak
        ReferenceCountUtil.release(msg);
    } else {
        // 链路状态正常，将需要发送的 msg 和 promise 放入发送缓冲区中
        outboundBuffer.addMessage(msg, promise);
    }
}
```

6. flush 方法

```java
/**
 * flush 方法负责将发送缓冲区中待发送的消息全部写入到 Channel 中，并发送给通信对方
 */
@Override
public void flush() {
    ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
    if (outboundBuffer == null) {
        return;
    }
	// 将发送环形数组的 unflushed 指针修改为 tail，标识本次要发送消息的缓冲区范围
    outboundBuffer.addFlush();
    flush0();
}
```

 	flush0() 方法代码非常简单，重点看 flush0() 方法中调用的 doWrite 方法

```java
@Override
protected void doWrite(ChannelOutboundBuffer in) throws Exception {
    for (;;) {
        // Do non-gathering write for a single buffer case.
        // 首先计算需要发送的消息个数
        final int msgCount = in.size();
        if (msgCount <= 1) {
            // 如果只有1个消息需要发送，则调用父类的写操作
            super.doWrite(in);
            return;
        }

        // Ensure the pending writes are made of ByteBufs only.
        ByteBuffer[] nioBuffers = in.nioBuffers();
        if (nioBuffers == null) {
            super.doWrite(in);
            return;
        }

        int nioBufferCnt = in.nioBufferCount();
        long expectedWrittenBytes = in.nioBufferSize();

        final SocketChannel ch = javaChannel();
        long writtenBytes = 0;
        boolean done = false;
        boolean setOpWrite = false;
        for (int i = config().getWriteSpinCount() - 1; i >= 0; i --) {
            final long localWrittenBytes = ch.write(nioBuffers, 0, nioBufferCnt);
            if (localWrittenBytes == 0) {
                setOpWrite = true;
                break;
            }
            expectedWrittenBytes -= localWrittenBytes;
            writtenBytes += localWrittenBytes;
            if (expectedWrittenBytes == 0) {
                done = true;
                break;
            }
        }

        if (done) {
            // Release all buffers
            for (int i = msgCount; i > 0; i --) {
                in.remove();
            }

            // Finish the write loop if no new messages were flushed by in.remove().
            if (in.isEmpty()) {
                clearOpWrite();
                break;
            }
        } else {
            // Did not write all buffers completely.
            // Release the fully written buffers and update the indexes of the partially written buffer.

            for (int i = msgCount; i > 0; i --) {
                final ByteBuf buf = (ByteBuf) in.current();
                final int readerIndex = buf.readerIndex();
                final int readableBytes = buf.writerIndex() - readerIndex;

                if (readableBytes < writtenBytes) {
                    in.progress(readableBytes);
                    in.remove();
                    writtenBytes -= readableBytes;
                } else if (readableBytes > writtenBytes) {
                    buf.readerIndex(readerIndex + (int) writtenBytes);
                    in.progress(writtenBytes);
                    break;
                } else { // readableBytes == writtenBytes
                    in.progress(readableBytes);
                    in.remove();
                    break;
                }
            }

            incompleteWrite(setOpWrite);
            break;
        }
    }
}
```

​	AbstractNioByteChannel 的 doWrite 方法

```java
@Override
protected void doWrite(ChannelOutboundBuffer in) throws Exception {
    int writeSpinCount = -1;

    for (;;) {
        // 直接从 ChannelOutboundBuffer 中获取当前需要发送的消息
        Object msg = in.current(true);
        if (msg == null) {
            // Wrote all messages.
            clearOpWrite();
            break;
        }

        if (msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) msg;
            int readableBytes = buf.readableBytes();           
            // 已经发送完成，将消息从环形队列中删除
            if (readableBytes == 0) {
                in.remove();
                // 继续循环
                continue;
            }

            boolean setOpWrite = false;
            boolean done = false;
            long flushedAmount = 0;
            if (writeSpinCount == -1) {
                writeSpinCount = config().getWriteSpinCount();
            }
            for (int i = writeSpinCount - 1; i >= 0; i --) {
                // 对写入的字节个数进行判断，如果为 0 说明 TCP 的发送缓冲已满，需要退出并监听写操作
                int localFlushedAmount = doWriteBytes(buf);
                if (localFlushedAmount == 0) {
                    setOpWrite = true;
                    break;
                }

                flushedAmount += localFlushedAmount;
                if (!buf.isReadable()) {
                    done = true;
                    break;
                }
            }

            in.progress(flushedAmount);

            if (done) {
                in.remove();
            } else {
                incompleteWrite(setOpWrite);
                break;
            }
        } else if (msg instanceof FileRegion) {
            FileRegion region = (FileRegion) msg;
            boolean setOpWrite = false;
            boolean done = false;
            long flushedAmount = 0;
            if (writeSpinCount == -1) {
                writeSpinCount = config().getWriteSpinCount();
            }
            for (int i = writeSpinCount - 1; i >= 0; i --) {
                long localFlushedAmount = doWriteFileRegion(region);
                if (localFlushedAmount == 0) {
                    setOpWrite = true;
                    break;
                }

                flushedAmount += localFlushedAmount;
                if (region.transfered() >= region.count()) {
                    done = true;
                    break;
                }
            }

            in.progress(flushedAmount);

            if (done) {
                in.remove();
            } else {
                incompleteWrite(setOpWrite);
                break;
            }
        } else {
            throw new UnsupportedOperationException("unsupported message type: " + StringUtil.simpleClassName(msg));
        }
    }
}
```

#### 16.4.3 AbstractNioUnsafe 源码分析

​	AbstractNioUnsafe 是 AbstractUnsafe 类的 NIO 实现，它主要实现了 connect、finishConnect 等方法

1. connect 方法

```java
@Override
public void connect(
    final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelPromise promise) {
    if (!ensureOpen(promise)) {
        return;
    }

    try {
        if (connectPromise != null) {
            throw new IllegalStateException("connection attempt already made");
        }

        boolean wasActive = isActive();
        // 发起连接操作
        if (doConnect(remoteAddress, localAddress)) {
            // 连接成功 触发 ChannelActive 事件
            fulfillConnectPromise(promise, wasActive);
        } else {
            // 暂时没有连接上，服务端没有返回 ACK 应答，连接结果不确定
            connectPromise = promise;
            requestedRemoteAddress = remoteAddress;

            // Schedule connect timeout.
            int connectTimeoutMillis = config().getConnectTimeoutMillis();
            // 设置定时任务，超时时间达到之后触发
            if (connectTimeoutMillis > 0) {
                connectTimeoutFuture = eventLoop().schedule(new Runnable() {
                    @Override
                    public void run() {
                        // 校验连接如果没有完成，则关闭句柄，释放资源，设置异常堆栈并发起去注册
                        ChannelPromise connectPromise = AbstractNioChannel.this.connectPromise;
                        ConnectTimeoutException cause =
                            new ConnectTimeoutException("connection timed out: " + remoteAddress);
                        if (connectPromise != null && connectPromise.tryFailure(cause)) {
                            close(voidPromise());
                        }
                    }
                }, connectTimeoutMillis, TimeUnit.MILLISECONDS);
            }
			// 设置连接结果监听器
            // 如果接收到连接完成通知 则判断 连接是否被取消，如果被取消则关闭连接句柄，释放资源，发起取消注册操作
            promise.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isCancelled()) {
                        if (connectTimeoutFuture != null) {
                            connectTimeoutFuture.cancel(false);
                        }
                        connectPromise = null;
                        close(voidPromise());
                    }
                }
            });
        }
    } catch (Throwable t) {
        if (t instanceof ConnectException) {
            Throwable newT = new ConnectException(t.getMessage() + ": " + remoteAddress);
            newT.setStackTrace(t.getStackTrace());
            t = newT;
        }
        promise.tryFailure(t);
        closeIfClosed();
    }
}
```

2. finishConnect 方法

```java
/**
 * 客户端接收到服务端的 TCP 握手应答消息，通过 SocketChannel 的 finishConnect 方法对连接结果进行判断
 */
@Override
public void finishConnect() {
    // Note this method is invoked by the event loop only if the connection attempt was
    // neither cancelled nor timed out.

    assert eventLoop().inEventLoop();
    assert connectPromise != null;

    try {
        boolean wasActive = isActive();
		// 连接成功 无任何异常；
        // 连接失败 抛出Error() 调用方执行句柄关闭等资源释放操作
        doFinishConnect();
        // 连接成功 执行fulfillConnectPromise 将 SocketChannel 修改为读操作位
        fulfillConnectPromise(connectPromise, wasActive);
    } catch (Throwable t) {
        if (t instanceof ConnectException) {
            Throwable newT = new ConnectException(t.getMessage() + ": " + requestedRemoteAddress);
            newT.setStackTrace(t.getStackTrace());
            t = newT;
        }

        // Use tryFailure() instead of setFailure() to avoid the race against cancel().
        connectPromise.tryFailure(t);
        closeIfClosed();
    } finally {
        // Check for null as the connectTimeoutFuture is only created if a connectTimeoutMillis > 0 is used
        // See https://github.com/netty/netty/issues/1770
        if (connectTimeoutFuture != null) {
            connectTimeoutFuture.cancel(false);
        }
        connectPromise = null;
    }
}
```

#### 16.4.4 NioByteUnsafe 源码分析

1. read 方法

```java
@Override
public void read() {
    // 用于设置客户端连接的 TCP 参数
    final ChannelConfig config = config();
    final ChannelPipeline pipeline = pipeline();
    final ByteBufAllocator allocator = config.getAllocator();
    final int maxMessagesPerRead = config.getMaxMessagesPerRead();
    RecvByteBufAllocator.Handle allocHandle = this.allocHandle;
    if (allocHandle == null) {
        // 首次调用，从 SocketChannelConfig 的 RecvByteBUfAllocator 中创建 Handle
        // RecvByteBufAllocator 默认有两种实现 AdaptiveRecvByteBufAllocator 和 FixedRecvByteBufAllocator
        this.allocHandle = allocHandle = config.getRecvByteBufAllocator().newHandle();
    }
    if (!config.isAutoRead()) {
        removeReadOp();
    }

    ByteBuf byteBuf = null;
    int messages = 0;
    boolean close = false;
    try {
        // 通过接收缓冲区分配器的 Handler 计算获得下次预分配的缓冲区容量
        int byteBufCapacity = allocHandle.guess();
        int totalReadAmount = 0;
        do {
            // 根据缓冲区容量进行缓冲区分配
            byteBuf = allocator.ioBuffer(byteBufCapacity);
            int writable = byteBuf.writableBytes();
            // 进行消息异步读取
            // 返回0，表示没有就绪的消息可读；返回值大于0，读到消息；返回值-1，表示发生了 I/O 异常，读取失败
            int localReadAmount = doReadBytes(byteBuf);
            if (localReadAmount <= 0) {
                // not was read release the buffer
                // 出现异常，释放缓冲区
                byteBuf.release();
                // 置 close 状态位，用于关闭连接，释放句柄
                close = localReadAmount < 0;
                break;
            }

            pipeline.fireChannelRead(byteBuf);
            byteBuf = null;

            if (totalReadAmount >= Integer.MAX_VALUE - localReadAmount) {
                // Avoid overflow.
                totalReadAmount = Integer.MAX_VALUE;
                break;
            }

            totalReadAmount += localReadAmount;
            // 本次读取的字节数小于缓冲区可写的容量，说明 TCP 缓冲区已经没有就绪的字节可读，读取操作已经完成，需要退出循环
            if (localReadAmount < writable) {
                // Read less than what the buffer can hold,
                // which might mean we drained the recv buffer completely.
                break;
            }
        } while (++ messages < maxMessagesPerRead);
		// 完成本轮读取操作之后，触发 ChannelReadComplete 事件
        pipeline.fireChannelReadComplete();
        // 
        allocHandle.record(totalReadAmount);

        if (close) {
            closeOnRead(pipeline);
            close = false;
        }
    } catch (Throwable t) {
        handleReadException(pipeline, byteBuf, t, close);
    }
}
}
```

​	AdaptiveRecvByteBufAllocator

```java
// 缓冲区大小可以动态调整的 ByteBuf 分配器
public class AdaptiveRecvByteBufAllocator implements RecvByteBufAllocator {
	// 最小缓冲区长度
    static final int DEFAULT_MINIMUM = 64;
    // 初始容量
    static final int DEFAULT_INITIAL = 1024;
    // 最大容量
    static final int DEFAULT_MAXIMUM = 65536;
	// 扩张的步进索引
    private static final int INDEX_INCREMENT = 4;
    // 收缩的步进索引
    private static final int INDEX_DECREMENT = 1;

    private static final int[] SIZE_TABLE;

    static {
        // 长度的向量表初始化
        List<Integer> sizeTable = new ArrayList<Integer>();
        // 前面扩容的步进值较小
        for (int i = 16; i < 512; i += 16) {
            sizeTable.add(i);
        }
		// 超过512是  扩大扩容的步进值
        for (int i = 512; i > 0; i <<= 1) {
            sizeTable.add(i);
        }

        SIZE_TABLE = new int[sizeTable.size()];
        for (int i = 0; i < SIZE_TABLE.length; i ++) {
            SIZE_TABLE[i] = sizeTable.get(i);
        }
    }

    public static final AdaptiveRecvByteBufAllocator DEFAULT = new AdaptiveRecvByteBufAllocator();

    // 根据容量size，查找容量向量表对应的索引 -- 二分查找法
    private static int getSizeTableIndex(final int size) {
        for (int low = 0, high = SIZE_TABLE.length - 1;;) {
            if (high < low) {
                return low;
            }
            if (high == low) {
                return high;
            }

            int mid = low + high >>> 1;
            int a = SIZE_TABLE[mid];
            int b = SIZE_TABLE[mid + 1];
            if (size > b) {
                low = mid + 1;
            } else if (size < a) {
                high = mid - 1;
            } else if (size == a) {
                return mid;
            } else {
                return mid + 1;
            }
        }
    }

    private static final class HandleImpl implements Handle {
        // 最小索引
        private final int minIndex;
        // 最大索引
        private final int maxIndex;
        // 当前索引
        private int index;
        // 下一次预分配的 Buffer 大小
        private int nextReceiveBufferSize;
        // 是否立即执行容量收缩操作
        private boolean decreaseNow;

        HandleImpl(int minIndex, int maxIndex, int initial) {
            this.minIndex = minIndex;
            this.maxIndex = maxIndex;

            index = getSizeTableIndex(initial);
            nextReceiveBufferSize = SIZE_TABLE[index];
        }

        @Override
        public ByteBuf allocate(ByteBufAllocator alloc) {
            return alloc.ioBuffer(nextReceiveBufferSize);
        }

        @Override
        public int guess() {
            return nextReceiveBufferSize;
        }
		
        // 当 NioSocketChannel 执行完读操作后，会计算获得本次轮询读取的总字节数，它就是参数 actualReadBytes，执行 record 方法，根据实际读取的字节数对 ByteBuf 进行动态伸缩和扩张
        // 实际上就是根据本次读取的实际字节数对下次接收缓冲区的容量进行动态调整
        // 使用动态缓冲区分配器的优点
        // 1.根据上次的实际读取的码流大小对下次的接收 Buffer 缓冲区进行预测和调整，能够最大限度地满足不同行业的应用场景
        // 2.性能更高，容量过大会导致内存占用开销增加，后续的 Buffer 处理性能会下降；容量过小时需要频繁地内存扩张来接收大的请求消息，同样会导致性能处理下降
        // 3.更节约内存
        @Override
        public void record(int actualReadBytes) {
            if (actualReadBytes <= SIZE_TABLE[Math.max(0, index - INDEX_DECREMENT - 1)]) {
                if (decreaseNow) {
                    index = Math.max(index - INDEX_DECREMENT, minIndex);
                    nextReceiveBufferSize = SIZE_TABLE[index];
                    decreaseNow = false;
                } else {
                    decreaseNow = true;
                }
            } else if (actualReadBytes >= nextReceiveBufferSize) {
                index = Math.min(index + INDEX_INCREMENT, maxIndex);
                nextReceiveBufferSize = SIZE_TABLE[index];
                decreaseNow = false;
            }
        }
    }

    private final int minIndex;
    private final int maxIndex;
    private final int initial;

    /**
     * Creates a new predictor with the default parameters.  With the default
     * parameters, the expected buffer size starts from {@code 1024}, does not
     * go down below {@code 64}, and does not go up above {@code 65536}.
     */
    private AdaptiveRecvByteBufAllocator() {
        this(DEFAULT_MINIMUM, DEFAULT_INITIAL, DEFAULT_MAXIMUM);
    }

    /**
     * Creates a new predictor with the specified parameters.
     *
     * @param minimum  the inclusive lower bound of the expected buffer size
     * @param initial  the initial buffer size when no feed back was received
     * @param maximum  the inclusive upper bound of the expected buffer size
     */
    public AdaptiveRecvByteBufAllocator(int minimum, int initial, int maximum) {
        if (minimum <= 0) {
            throw new IllegalArgumentException("minimum: " + minimum);
        }
        if (initial < minimum) {
            throw new IllegalArgumentException("initial: " + initial);
        }
        if (maximum < initial) {
            throw new IllegalArgumentException("maximum: " + maximum);
        }

        int minIndex = getSizeTableIndex(minimum);
        if (SIZE_TABLE[minIndex] < minimum) {
            this.minIndex = minIndex + 1;
        } else {
            this.minIndex = minIndex;
        }

        int maxIndex = getSizeTableIndex(maximum);
        if (SIZE_TABLE[maxIndex] > maximum) {
            this.maxIndex = maxIndex - 1;
        } else {
            this.maxIndex = maxIndex;
        }

        this.initial = initial;
    }

    @Override
    public Handle newHandle() {
        return new HandleImpl(minIndex, maxIndex, initial);
    }
}
```



##第十七章 ChannelPipeline 和 ChannelHandler

​	Netty 的 ChannelPipeline 和 ChannelHandler 机制类似于 Servelt 和 Filter 过滤器，这类拦截器实际上是职责链模式的一种变形，主要是为了方便事件的拦截和用户业务逻辑的定制。

​	Netty 的 Channel 过滤器实现原理与 Servelt Filter 机制一致，它将 Channel 的数据管道抽象为 ChannelPipeline，消息在 ChannelPipeline 中流动和传递。ChannelPipeline 持有 I/O 事件拦截器 ChannelHandler 的链表，由 ChannelHandler 对 I/O 事件进行拦截和处理，可以方便地通过新增和删除 ChannelHandler 来实现不同的业务逻辑定制，不需要对已有的 ChannelHandler 进行修改，能够实现对修改封闭和对扩展的支持。

###17.1 ChannelPipeline 功能说明

​	ChannelPipeline 是 ChannelHandler 的容器，它负责 ChannelHandler 的管理和事件拦截与调度。

#### 17.1.1 ChannelPipeline 的事件处理

![ChannelPipeline对事件流的拦截和处理流程](img\ChannelPipeline对事件流的拦截和处理流程.png)

​	上图展示了一个消息被 ChannelPipeline 的 ChannelHandler 链拦截和处理的全过程，消息的读取和发送处理全流程描述如下：

1. 底层的 SocketChannel read()  方法读取 ByteBuf，触发 ChannelRead 事件，由 I/O 线程 NioEventLoop 调用 ChannelPipeline 的 fireChannelRead(Object msg) 方法，将消息(BytebBuf) 传输到 ChannelPipeline 中。
2. 消息依次被 HeadHandler、ChannelHandler1、ChannelHandler2 .....、TailHandler 拦截和处理，在这个过程中，任何 ChannelHandler 都可以中断当前的流程，结束消息的传递。
3. 调用 ChannelHandlerContext 的 write 方法发送消息，消息从 TailHandler 开始，途径 ChannelHandlerN 、...... 、ChannelHandler2、ChannelHandler1，最终被添加到消息发送缓冲区中等待刷新和发送，在此过程中也可以中断消息的传递，例如当编码失败时，就需要中断流程，构造异常的 Future 返回。

​	Netty 中的事件分为 inbound 事件和 outbound 事件。inbound 事件通常由 I/O 线程触发，例如 TCP 链路建立事件、链路关闭事件、读事件、异常通知事件等，它对应上图左半部分。

​	触发 inbound 事件的方法如下：

- ChannelHandlerContext.fireChannelRegister()：Channel 注册事件；

- ChannelHandlerContext.fireChannelActive()：TCP 链路建立成功，Channel 激活事件；

- ChannelHandlerContext.fireChannelRead(Object): 读事件

- ChannelHandlerContext.fireChannelReadComplete()：读操作完成通知事件

- ChannelHandlerContext.fireExceptionCaught(Throwable)：异常通知事件

- ChannelHandlerContext.fireUserEventTriggered(Object)：用户自定义事件

- ChannelHandlerContext.fireChannelWritabilityChanged()：Channel 的可写状态变化通知事件

- ChannelHandlerContext.fireChannelInactive()：TCP 连接关闭，链路不可用通知事件

  Outbound 事件通常是由用户主动发起的网络 I/O 操作，例如用户发起的连接操作、绑定操作、消息发送等操作，它对应上图的右半部分

  触发 outbound 事件的方法如下：

- ChannelHandlerContext.bind(SocketAddress，ChannelPromise)：绑定本地地址事件
- ChannelHandlerContext.connect(SocketAddress，SocketAddress，ChannelPromise)：连接服务端事件
- ChannelHandlerContext.write(Object，ChannelPromise)：发送事件
- ChannelHandlerContext.flush()：刷新事件
- ChannelHandlerContext.read()：读事件
- ChannelHandlerContext.disconnect(ChannelPromise)：断开连接事件
- ChannelHandlerContext.close(ChannelPromise)：关闭当前 Channel 事件

#### 17.1.2 自定义拦截器

​	ChannelPipeline 通过 ChannelPipeline 接口来实现事件的拦截和处理，由于 ChannelHandler 中的事件种类繁多，不同的 ChannelHandler 可能只需要关心其中的某一个或者几个事件，所以，通常 ChannelHandler 只需要继承 ChannelHandlerAdapter 类覆盖自己关心的方法即可。

#### 17.1.3 构建 pipeline

​	事实上，用户不需要自己创建 pipeline，因为使用 ServerBootstrap 或者 BootStrap 启动服务端或者客户端时，Netty 会为每个 Channel 连接创建一个独立的 pipeline，对于使用者而言，只需要将自定义的拦截器加入到 pipeline 中即可。

```java
pipeline = ch.pipeline();
pipeline.addLast("decoder", new MyProtocolDecoder());
pipeline.addLast("encoder", new MyProtocolEncoder());
```

​	对于类似编解码这样的 ChannelHandler，它存在先后顺序，例如 MessageToMessageDecoder，在他之前往往需要有 ByteToMessageDecoder 将 ByteBuf 解码为对象，然后对对象做二次解码得到最终的 POJO 对象。Pipeline 支持指定位置添加或者删除拦截器，相关接口定义如下图：

![顺序添加ChannelHandler](img\顺序添加ChannelHandler.png)

#### 17.1.4 ChannelPipeline 的主要特征

​	ChannelPipeline 支持运行态动态的添加或者删除 ChannelHandler，在某些场景下这个特性非常实用。例如当业务高峰期需要对系统做拥塞保护时，就可以根据当前的系统时间进行判断，如果处于业务高峰期，则动态地将系统拥塞保护 ChannelHandler 添加到当前的 ChannelPipeline 中，当高峰期过去之后，就可以动态删除拥塞保护 ChannelHandler 了。



### 17.2 ChannelPipeline 源码分析

---

​	ChannelPipelin 的代码相对比较简单，他实际上是一个 ChannelHandler 的容器，内部维护了一个 ChannelHandler 的链表和迭代器，可以方便地实现 ChannelHandler 查找、添加、替换和删除。

####17.2.1 ChannelPipeline 的类继承关系图

![ChannelPipeline类继承关系图](img\ChannelPipeline类继承关系图.png)

####17.2.2 ChannelPipeline 对 ChannelHandler 的管理

​	ChannelPipeline 是 ChannelHandler 的管理容器，负责 ChannelHandler 的查询、添加、替换和删除。由于它与 Map 等容器的实现非常类似，所以我们只简单抽取新增接口进行源码分析，其他方法读者可以自行阅读分析。在 ChannelPipeline 中添加 ChannelHandler 方法如下：

```java
@Override
public ChannelPipeline addBefore(String baseName, String name, ChannelHandler handler) {
    return addBefore((ChannelHandlerInvoker) null, baseName, name, handler);
}

// 由于 ChannelPipeline 支持运行期动态修改，因此存在两种潜在的多线程并发访问场景
// 1.I/O 线程和用户业务线程的并发访问
// 2.用户多个线程之间的并发访问
@Override
public ChannelPipeline addBefore(
    ChannelHandlerInvoker invoker, String baseName, final String name, ChannelHandler handler) {
    // 使用 synchronize 关键字，保证操作的原子性
    synchronized (this) {
        // 根据 beseName 获取它对应的 DefaultChannelHandlerContext，ChannelPipeline 维护了 ChannelHandler 名和 ChannelHandlerContext 实例的映射关系
        DefaultChannelHandlerContext ctx = getContextOrDie(baseName);
		// 对新增的 ChannelHandler 名进行重复性校验
        checkDuplicateName(name);
		// 创建新的 DefaultChannelHandlerContext
        DefaultChannelHandlerContext newCtx =
            new DefaultChannelHandlerContext(this, invoker, name, handler);
		// 将 DefaultChannelHandlerContext 添加到 pipeline 中
        addBefore0(name, ctx, newCtx);
    }
    return this;
}
```

#### 17.2.3 ChannelPipeline 的 inbound 事件

​	当发生某个 I/O 事件的时候，例如链路建立、链路关闭、读取操作完成等，都会产生一个事件，事件在 pipeline 中得到传播和处理，它是事件处理的总入口。由于网络 I/O 相关的事情有限，因此 Netty 对这些事件进行了统一抽象，Netty 自身和用户的 ChannelHandler 会对感兴趣的事件进行拦截和处理。

​	pipeline 中以 fireXXX 命名的方法都是从 I/O 线程流向用户业务 Handler 的 inbound 事件，他们的实现因功能而异，但是处理步骤类似，总结如下：

1. 调用 HeadHandler 对应的 fireXXX 方法；
2. 执行事件相关的逻辑操作。

​	以 fireChannelActive 方法为例，调用head.fireChannelActive() 之后，判断当前的 Channel 配置是否自动读取，如果为真则调用 Channel 的 read 方法，代码如下：

```java
@Override
public ChannelPipeline fireChannelActive() {
    head.fireChannelActive();

    if (channel.config().isAutoRead()) {
        channel.read();
    }

    return this;
}
```

#### 17.2.4 ChannelPipiline 的 outbound 事件

​	由用户线程或者代码发起的 I/O 操作被称为 outbound 事件，事实上 inbound 和 outbound 是 Netty 自身根据事件在 pipeline 中的流向抽象出来的术语，在其他 NIO 框架中没有这个概念。

​	outbound 事件相关联的操作如下图：

![outbound事件相关联的操作](img\outbound事件相关联的操作.png)

​	Pipeline 本身并不直接进行 I/O 操作，在前面对 Channel 和 Unsafe 的介绍中我们知道最终都是由 Unsafe 和 Channel 来实现真正的 I/O 操作的。Pipeline 负责将 I/O 事件通过 TailHandler 进行调度和传播，最终调用 Unsafe 的 I/O 方法进行 I/O 操作，相关代码实现如下：

```java
@Override
public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
    return tail.connect(remoteAddress, localAddress);
}

// 最终调用到下面方法
@Override
public void connect(
    ChannelHandlerContext ctx,
    SocketAddress remoteAddress, SocketAddress localAddress,
    ChannelPromise promise) throws Exception {
    // 最终由 HeadHandler 调用 Unsafe 的 connect 方法发起真正的连接，pipeline 仅仅负责事件的调度
    unsafe.connect(remoteAddress, localAddress, promise);
}
```



### 17.3 ChannelHandler 功能说明

---

​	ChannelHandler 类似于 Servlet 的 Filter 过滤器，负责对 I/O 事件或者 I/O 操作进行拦截和处理，它可以选择性地拦截和处理自己感兴趣的事件，也可以透传和终止事件的传递。

​	基于 ChannelHandler 接口，用户可以方便地进行业务逻辑定制，例如打印日志、统一封装异常信息、性能统计和消息编解码等。

​	ChannelHandler 支持注解，目前支持的注解有两种：

- Sharable：多个 ChannelPipeline 共用同一个 ChannelHandler；
- Skip：被 Skip 注解的方法不会被调用，直接被忽略。

#### 17.3.1 ChannelHandlerAdapter 功能说明

​	对于大多数的 ChannelHandler 会选择性地拦截和处理某个或者某些事件，其他的事件会忽略，由下一个 ChannelHandler 进行拦截和处理。这就会导致一个问题：用户 ChannelHandler 必须要实现 ChannelHandler 的所有接口，包括它不关心的那些事件处理接口，这会导致用户代码的冗余和臃肿，代码的可维护性也会变差。

​	为了解决这个问题，Netty 提供了 ChannelHandlerAdapter 基类，他的所有接口实现都是事件透传，如果用户 ChannelHandler 关心某个事件，只需要覆盖实现对应的方法即可，不关心的直接继承使用父类的，这样子类的代码就会非常简洁和清晰。/

```java
// ChannelHandlerAdapter 相关代码实现
// @Skip 在执行过程中会被忽略，直接跳到下一个 ChannelHandler 中执行对应的方法
@Skip
@Override
public void read(ChannelHandlerContext ctx) throws Exception {
    ctx.read();
}

@Skip
@Override
public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    ctx.write(msg, promise);
}
```

#### 17.3.2 ByteToMessageDecoder 功能说明

​	利用 NIO 进行网络编程时，往往需要将读取到的字节数组或者字节缓冲区解码为业务可以使用的 POJO 对象。为了方便业务将 ByteBuf 解码成业务 POJO 对象，Netty 提供了ByteToMessageDecoder 抽象工具解码类。

​	用户的解码器继承 ByteToMessageDecoder，只需要实现 decode 方法即可，由于 ByteToMessageDecoder 并没有考虑 TCP 粘包和组包等场景，所以继承另外一些更高级的解码器来屏蔽半包的处理。

#### 17.3.3 MessageToMessageDecoder 功能说明

​	MessageToMessgeDecoder 实际上是 Netty 的二次解码器，他的职责是将一个对象二次解码为其他对象。

​	MessageToMessageDecoder 在 ByteToMessageDecoder 之后，所以称之为二次解码器。二次解码器在实际的商业项目中非常有用，以 HTTP + XML 协议栈为例，第一次解码往往是将字节数组解码成 HttpRequest 对象，然后对 HttpRequest 消息中的消息体字符串进行二次解码为 POJO 对象，这就是用到了二次解码器。

​	使用多个 MessageToMessageDecoder 组合的方式来实现消息的解码，更有利于功能的扩展和维护，符合对修改关闭，对扩展开发的原则。

#### 17.3.4 LengthFieldBasedFrameDecoder 功能说明

​	在编解码章节我们讲过 TCP 的粘包导致解码的时候需要考虑如何处理半包的问题，前面介绍了 Netty 提供的半包解码器 LineBasedFrameDecoder 和 DelimiterBasedFrameDecoder，现在来看第三种最通用的半包解码器 ------ LengthFieldBasedFrameDecoder。

​	如何区分一个整包消息，通常有如下 4 种做法：

- 固定长度，例如每 120 个字节代表一个整包消息，不足的前面补零
- 通过回车换行符区分消息
- 通过分隔符区分整包消息
- 通过指定长度来标识整包消息

​	如果消息是通过长度进行区分的，LengthFieldBasedFrameDecoder 都可以自动处理粘包和半包问题，只需要传入正确的参数。

​	使用以下四个参数组合进行解码：

- lengthFieldOffset：消息长度字段在消息头中的偏移量
- lengthFieldLength：消息长度字段的长度
- lengthAdjustment：当消息长度值包含消息头的长度时，使用该这段修正长度值，所以该字段值为负值
- initialBytesToStrip：抓取消息体时的偏移量（偏移掉消息头的长度）

#### 17.3.5 MessageToByteEncoder

​	MessageToByteEncoder 负责将 POJO 对象编码成 ByteBuf，用户的编码器继承 MessageToByteEncoder，实现 encode 接口即可。

```java
public class IntegerEncoder extends MessageToByteEncoder<Integer> {
    @Override
    public void encode(ChannelHandlerContext ctx, Integer msg, ByteBuf out) throws Exception {
        out.writeInt(msg);
    }
}
```

####17.3.6 MessageToByteEncoder 功能说明

​	将一个 POJO 对象编码成另一个对象，往往复杂协议需要经历多次编码，为了便于功能扩展，可以通过多个编码器组合来实现相关功能。代码示例如下：

```java
public class IntegerToStringEncoder extends MessageToMessageEncoder<Integer> {
    @Override
    public void encode(ChannelHandlerContext ctx, Integer message，List<Object> out) throws Exception {
        out.add(message.toString());
    }
}
```

####17.3.7 LengthFieldPrepender 功能说明

​	如果协议中的第一个字段为长度字段，Netty 提供了 LengthFieldPrepender 编码器，它可以计算当前待发送消息的二进制字节长度，将该长度添加到 ByteBuf 的缓冲区头中。可以通过设置 LengthFieldPrepender 为 true 或者 false，来指定消息长度是否包含消息长度字段本身占用的字节数。



### 17.4 ChannelHandler 源码分析

---

####17.4.1 ChannelHandler 的类继承关系图

​	相对于 ByteBuf 和 Channel，ChannelHandler 的类继承关系稍微简单些，但是他的子类非常多。由于 ChannelHandler 是 Netty 框架和用户代码的主要扩展和定制点，所以它的子类种类繁多、功能各异，系统 ChannelHandler 主要分类如下：

- ChannelPipeline 的系统 ChannelHandler，用于 I/O 操作和对事件进行预处理，对于用户不可见，这类 ChannelHandler 主要包括 HeadHandler 和 TailHandler；

- 编解码 ChannelHandler

- 其他系统功能性 ChannelHandler，包括流量整形 Handler、读写超时 Handler、日志 Handler 等

  ![1544670648771](C:\Users\wr1ttenyu\AppData\Roaming\Typora\typora-user-images\1544670648771.png)

#### 17.4.2 ByteToMessageDecoder 源码分析

 ```java
@Override
public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    // 判断需要解码的 msg 对象是否是 ByteBuf，是才需要进行解码
    if (msg instanceof ByteBuf) {
        RecyclableArrayList out = RecyclableArrayList.newInstance();
        try {
            ByteBuf data = (ByteBuf) msg;
            // 通过 cumulation 是否为空判断解码器是否缓存了没有解码完成的半包消息，如果为空，说明是首次解码或者最近一次已经处理完了半包消息，没有缓存的半包消息需要处理
            first = cumulation == null;
            if (first) {
                cumulation = data;
            } else {
                // 存在半包消息 需要进行复制操作，将需要解码的 ByteBuf 复制到 cumulation 中
                if (cumulation.writerIndex() > cumulation.maxCapacity() - data.readableBytes()) {
                    // 如果 cumulation 长度可写区域不够 需要扩展
                    // 这个方法里面的扩展算法 可以优化
                    expandCumulation(ctx, data.readableBytes());
                }
                cumulation.writeBytes(data);
                data.release();
            }
            callDecode(ctx, cumulation, out);
        } catch (DecoderException e) {
            throw e;
        } catch (Throwable t) {
            throw new DecoderException(t);
        } finally {
            if (cumulation != null && !cumulation.isReadable()) {
                cumulation.release();
                cumulation = null;
            }
            int size = out.size();
            decodeWasNull = size == 0;

            for (int i = 0; i < size; i ++) {
                ctx.fireChannelRead(out.get(i));
            }
            out.recycle();
        }
    } else {
        ctx.fireChannelRead(msg);
    }
}


protected void callDecode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
    try {
        while (in.isReadable()) {
            int outSize = out.size();
            int oldInputLength = in.readableBytes();
            // 调用用户的子类解码器进行解码
            decode(ctx, in, out);

            // Check if this handler was removed before continuing the loop.
            // If it was removed, it is not safe to continue to operate on the buffer.
            //
            // See https://github.com/netty/netty/issues/1664
            if (ctx.isRemoved()) {
                break;
            }
			// 输出的out列表长度没变化，说明没有解码成功，需要针对以下不同场景进行判断：            
            if (outSize == out.size()) {
                // 1.ByteBuf 没有被消费，则说明是个半包消息，需要由 I/O 线程继续读取后续的数据报，在这种场景下要退出循环
                // 在这种场景下可以看出，业务解码器需要遵守 Netty 的某些契约，解码器才能正常工作，否则可能会导致功能错误，最重要的契约就是：如果业务解码器认为当前的字节缓冲区无法完成业务层的解码，需要将 readIndex 复位，告诉 Netty 解码条件不满足应当退出解码，继续读取数据报。
                if (oldInputLength == in.readableBytes()) {
                    break;
                } else {
                    // 2.ByteBuf 被消费了，说明可以解码，则继续进行
                    continue;
                }
            }

            // 3.如果用户解码器没有消费 ByteBuf，但是却解码出了一个或者多个对象，这种行为被认为是非法的
            if (oldInputLength == in.readableBytes()) {
                throw new DecoderException(
                    StringUtil.simpleClassName(getClass()) +
                    ".decode() did not read anything but decoded a message.");
            }
			// 通过 isSingleDecode 进行判断，如果是单条消息解码器，第一次解码完成之后就退出循环
            if (isSingleDecode()) {
                break;
            }
        }
    } catch (DecoderException e) {
        throw e;
    } catch (Throwable cause) {
        throw new DecoderException(cause);
    }
}
 ```

#### 17.4.3 MessageToMessageDecoder 源码分析

```java
@Override
public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    RecyclableArrayList out = RecyclableArrayList.newInstance();
    try {
        // 先判断消息类型是否是可接收的类型
        if (acceptInboundMessage(msg)) {
            @SuppressWarnings("unchecked")
            I cast = (I) msg;
            try {
                // 调用子类实现的消息解码方法
                decode(ctx, cast, out);
            } finally {
                // 释放被解码的 msg 对象
                ReferenceCountUtil.release(cast);
            }
        } else {
            // 如果需要解码的对象不是当前解码器可以处理的类型，加入到 RecyclableArrayList 中不进行解码
            out.add(msg);
        }
    } catch (DecoderException e) {
        throw e;
    } catch (Exception e) {
        throw new DecoderException(e);
    } finally {
        int size = out.size();
        // 遍历 RecyclableArrayList 不需要解码的对象，调用 fireChannelRead 通知后续的 ChannelHandler 继续进行处理
        for (int i = 0; i < size; i ++) {
            ctx.fireChannelRead(out.get(i));
        }
        // 循环通知完成，释放 RecyclableArrayList
        out.recycle();
    }
}
```

####17.4.4 LengthFieldBaseFrameDecoder 源码分析

```java
protected final void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    Object decoded = decode(ctx, in);
    if (decoded != null) {
        out.add(decoded);
    }
}
```

```java
protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
    // 判断discardingTooLongFrame标识，是否需要丢弃当前可读的字节缓冲区
    if (discardingTooLongFrame) {
        long bytesToDiscard = this.bytesToDiscard;
        int localBytesToDiscard = (int) Math.min(bytesToDiscard, in.readableBytes());
        in.skipBytes(localBytesToDiscard);
        bytesToDiscard -= localBytesToDiscard;
        this.bytesToDiscard = bytesToDiscard;
		// 判断是否已经达到需要忽略的字节数，达到的话对 判断discardingTooLongFrame标识 等进行重置
        failIfNecessary(false);
    }
	// 对当前缓冲区的可读字节数和长度偏移量进行对比，如果小于长度偏移量，说明当前缓冲区的数据包不够，需要返回空，由 I/O 线程继续读取后续的数据报	
    if (in.readableBytes() < lengthFieldEndOffset) {
        return null;
    }
	
    int actualLengthFieldOffset = in.readerIndex() + lengthFieldOffset;
    // 获取消息报文的长度字段值
    long frameLength = getUnadjustedFrameLength(in, actualLengthFieldOffset, lengthFieldLength, byteOrder);

    if (frameLength < 0) {
        in.skipBytes(lengthFieldEndOffset);
        throw new CorruptedFrameException(
            "negative pre-adjustment length field: " + frameLength);
    }
 
    // lengthFieldOffset lengthFieldLength lengthAdjustment initialBytesToStrip 四个字段的含义 可参考 ：17.3.4
    frameLength += lengthAdjustment + lengthFieldEndOffset;
	// 修正后的报文长度小于lengthFieldEndOffset 说明是非法数据
    if (frameLength < lengthFieldEndOffset) {
        in.skipBytes(lengthFieldEndOffset);
        throw new CorruptedFrameException(
            "Adjusted frame length (" + frameLength + ") is less " +
            "than lengthFieldEndOffset: " + lengthFieldEndOffset);
    }
	// 修正后的报文长度大于ByteBuf
    if (frameLength > maxFrameLength) {
        // 需要丢弃的字节数
        long discard = frameLength - in.readableBytes();
        tooLongFrameLength = frameLength;
        if (discard < 0) {
            // buffer contains more bytes then the frameLength so we can discard all now
            in.skipBytes((int) frameLength);
        } else {
            // Enter the discard mode and discard everything received so far.
            discardingTooLongFrame = true;
            bytesToDiscard = discard;
            in.skipBytes(in.readableBytes());
        }
        failIfNecessary(true);
        return null;
    }

    // never overflows because it's less than maxFrameLength
    int frameLengthInt = (int) frameLength;
    if (in.readableBytes() < frameLengthInt) {
        // 半包消息 返回空 由 I/O 线程继续读取后续的数据报 等待下次解码
        return null;
    }

    if (initialBytesToStrip > frameLengthInt) {
        in.skipBytes(frameLengthInt);
        throw new CorruptedFrameException(
            "Adjusted frame length (" + frameLength + ") is less " +
            "than initialBytesToStrip: " + initialBytesToStrip);
    }
    in.skipBytes(initialBytesToStrip);

    // extract frame
    int readerIndex = in.readerIndex();
    int actualFrameLength = frameLengthInt - initialBytesToStrip;
    // 通过 extractFrame 方法获取解码后的整包消息缓冲区
    ByteBuf frame = extractFrame(ctx, in, readerIndex, actualFrameLength);
    in.readerIndex(readerIndex + actualFrameLength);
    return frame;
}
```

#### 17.4.5 MessageToByteEncodre 源码分析

```java
@Override
public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    ByteBuf buf = null;
    try {
        // 判断当前编码器是否支持需要发送的消息，不支持直接透传
        if (acceptOutboundMessage(msg)) {
            @SuppressWarnings("unchecked")
            I cast = (I) msg;
            // 判断缓冲区类型
            if (preferDirect) {
                buf = ctx.alloc().ioBuffer();
            } else {
                buf = ctx.alloc().heapBuffer();
            }
            try {
                // 进行编码
                encode(ctx, cast, buf);
            } finally {
                ReferenceCountUtil.release(cast);
            }
			// 如果缓冲区包含可发送的字节
            if (buf.isReadable()) {
                ctx.write(buf, promise);
            } else {
                buf.release();
                ctx.write(Unpooled.EMPTY_BUFFER, promise);
            }
            buf = null;
        } else {
            ctx.write(msg, promise);
        }
    } catch (EncoderException e) {
        throw e;
    } catch (Throwable e) {
        throw new EncoderException(e);
    } finally {
        if (buf != null) {
            buf.release();
        }
    }
}
```

####17.4.6 MessageToMessageEncoder 源码分析

```java
@Override
public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    RecyclableArrayList out = RecyclableArrayList.newInstance();
    try {
        // 判断是否符合类型要求
        if (acceptInboundMessage(msg)) {
            @SuppressWarnings("unchecked")
            I cast = (I) msg;
            try {
                decode(ctx, cast, out);
            } finally {
                ReferenceCountUtil.release(cast);
            }
        } else {
            out.add(msg);
        }
    } catch (DecoderException e) {
        throw e;
    } catch (Exception e) {
        throw new DecoderException(e);
    } finally {
        // 循环发送解码后得到的对象
        int size = out.size();
        for (int i = 0; i < size; i ++) {
            ctx.fireChannelRead(out.get(i));
        }
        out.recycle();
    }
}
```

####17.4.7 LengthFieldPrepender 源码分析

​	LengthFieldPrepender 负责在待发送的 ByteBuf 消息头中增加一个长度字段来标识消息的长度。

```java

@Override
protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
    int length = msg.readableBytes() + lengthAdjustment;
    if (lengthIncludesLengthFieldLength) {
        length += lengthFieldLength;
    }
	// 如果调整后的消息长度小于0，则抛出参数非法异常
    if (length < 0) {
        throw new IllegalArgumentException(
            "Adjusted frame length (" + length + ") is less than zero");
    }
	// 根据长度字段自身所占的字节数，采取不同的写入方式
    switch (lengthFieldLength) {
        case 1:
            if (length >= 256) {
                throw new IllegalArgumentException(
                    "length does not fit into a byte: " + length);
            }
            out.add(ctx.alloc().buffer(1).writeByte((byte) length));
            break;
        case 2:
            if (length >= 65536) {
                throw new IllegalArgumentException(
                    "length does not fit into a short integer: " + length);
            }
            out.add(ctx.alloc().buffer(2).writeShort((short) length));
            break;
        case 3:
            if (length >= 16777216) {
                throw new IllegalArgumentException(
                    "length does not fit into a medium integer: " + length);
            }
            out.add(ctx.alloc().buffer(3).writeMedium(length));
            break;
        case 4:
            out.add(ctx.alloc().buffer(4).writeInt(length));
            break;
        case 8:
            out.add(ctx.alloc().buffer(8).writeLong(length));
            break;
        default:
            throw new Error("should not reach here");
    }
    out.add(msg.retain());
}
```



##第十八章 EventLoop 和 EventLoopGroup

​	Netty 框架的主要线程就是 I/O 线程，线程模型设计的好坏，决定了系统的吞吐量、并发性和安全性等架构质量属性。

​	Netty 的线程模型被精心地设计，既提升了框架的并发性能，又能在很大程度上避免锁，局部实现了无锁化。

###18.1 Netty 的线程模型

​	不同的 NIO 框架对于 Reactor 模式的实现存在差异，但本质上还是遵循了 Reactor 的基础线程模型。

#### 18.1.1 Reactor 单线程模型

​	Reactor 单线程模型，是指所有的 I/O 操作都在同一个 NIO 线程上面完成。NIO 线程的职责如下：

- 作为 NIO 服务端，接收客户端的 TCP 连接；
- 作为 NIO 客户端，向服务端发起器 TCP 连接；
- 读取通信对端的请求或者应答消息；
- 向通信对端发送消息请求或者应答消息；

缺陷：

- 一个 NIO 线程同时处理成百上千的链路，性能上无法支撑
- NIO 线程负载过重之后，处理速度将变慢，导致大量客户端连接超时，超时之后往往会进行重发，这更加重了 NIO 线程的负载
- 可靠性问题：一旦 NIO 线程意外跑飞，或者进入死循环，会导致整个系统通信模块不可用

#### 18.1.2 Reactor 多线程模型

​	Reactor 多线程模型与单线程模型最大的区别就是有一组 NIO 线程来处理 I/O 操作，Reactor 多线程模型的特点如下：

- 有专门一个 NIO 线程 —— Acceptor 线程用于监听服务端，接收客户端的 TCP 连接请求
- 网络 I/O 操作 —— 读、写等由一个 NIO 线程池负责
- 一个 NIO 线程可以同时处理 N 条链路，但是一个链路只对应一个 NIO 线程，防止发生并发操作问题

​	在绝大多数场景下，Reactor 多线程模型可以满足性能需求。但是，在个别特殊场景中，一个 NIO 线程负责监听和处理所有的客户端连接可能会存在性能问题。例如并发百万客户端连接，或者服务端需要对客户端握手进行安全认证，但是认证本身非常损耗性能。

#### 18.1.3 主从 Reactor 多线程模型

​	主从 Reactor 线程模型的特点是：服务端用于接收客户端连接的不再是一个单独的 NIO 线程，而是一个独立的 NIO 线程池。Acceptor 接收到客户端 TCP 连接请求并处理完成后，将新创建的 SocketChannel 注册到 I/O 线程池的某个 I/O 线程上，由它负责 SocketChannel 的读写和编解码工作。Acceptor 线程池仅仅用于客户端的登录、握手和安全认证，一旦链路建立成功，就将链路注册到后端 subReactor 线程池的 I/O 线程上，由 I/O 线程负责后续的 I/O 操作。                                                        

#### 18.1.4 Netty 的线程模型

​	Netty 的线程模型并不是一成不变的，它实际取决于用户的启动参数配置。通过设置不同的启动参数，Netty 可以同时支持 Reactor 单线程模型、多线程模型和主从 Reactor 多线程模型。

![1545028239953](C:\Users\wr1ttenyu\AppData\Roaming\Typora\typora-user-images\1545028239953.png)

```java
// 服务端启动 创建两个 NioEventLooGroup，他们实际是两个独立的 Reactor 线程池。一个用于接收客户端的 TCP 连接，另一个用于处理 I/O 相关的读写操作、或者执行系统Task、定时任务Task。
EventLoopGroup bossGroup = new NioEventLoopGroup();
EventLoopGroup workerGroup = new NioEventLoopGroup();
try {
    ServerBootstrap b = new ServerBootstrap();
    b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
        .option(ChannelOption.SO_BACKLOG, 1024)
        .handler(new LoggingHandler(LogLevel.TRACE))
        .childHandler(new FixedLengthFrameDecoderServer.ChildChannelHandler());
```

​	Netty 用于接收客户端请求的线程池职责如下：

- 接收客户端 TCP 连接，初始化 Channel 参数；

- 将链路状态变更事件通知给 ChannelPipeline；

  Netty 处理 I/O 操作的 Reactor 线程池职责如下：

- 异步读取通信对端的数据报，发送读事件到 ChannelPipeline；
- 异步发送消息到通信对端，调用 ChannelPipeline 的消息发送接口；
- 执行系统调用  Task；
- 执行定时任务 Task，例如链路空闲状态检测定时任务。

​	通过调整线程池的线程个数，是否共享线程池等方式，Netty 的 Reactor 线程模型可以在单线程、多线程、主从多线程间切换，这种灵活的配置方式可以最大程度地满足不同用户的个性化定制。

​	为了尽可能地提升性能，Netty 在很多地方进行了无锁化的设计，例如在 I/O 线程内部进行了串行操作，避免多线程竞争导致的性能下降问题。表面上看，串行化设计似乎 CPU 利用率不高，并发程度不够。但是，通过调整 NIO 线程池的线程参数，可以同时启动多个串行化的线程并行运行，这种局部无锁化的串行线程设计相比一个队列多个工作线程的模型性能更优。

![Netty Reactor 线程模型](img\Netty Reactor 线程模型.png)

​	Netty 的 NioEventLoop 读取到消息之后，直接调用 ChannelPipeline 的 fireChannelRead(Object msg)。只要用户不主动切换线程，一直都是由 NioEventLoop 调用用户的 Handler，期间不进行线程切换。这种串行化处理方式避免了多线程操作导致的锁的竞争，从性能角度看是最优的。

#### 18.1.5 最佳实践

​	Netty 的多线程编程最佳实践：

1. 创建两个 NioEventLoopGroup，用于逻辑隔离 NIO Acceptor 和 NIO I/O 线程；

2. 尽量不要在 ChannelHandler 中启动用户线程(解码后用于将 POJO 消息派发到后端业务线程除外)；

3. 解码要放在 NIO 线程调用的解码 Handler 中进行，不要切换到用户线程中完成消息的解码；

4. 如果业务逻辑操作非常简单，可以直接在 NIO 线程上完成业务逻辑编排，不需要切换到用户线程；

5. 如果业务逻辑处理复杂，不要在 NIO 线程上完成，建议将解码后的 POJO 消息封装成 Task，派发到业务线程池中由业务线程执行，以保证 NIO 线程尽快被释放，处理其他的 I/O 操作。

   推荐的线程数量计算公式有以下几种：

   - 线程数量 = ( 线程总时间 / 瓶颈资源时间 ) × 瓶颈资源的线程并行数
   - QPS = 1000 / 线程总时间 × 线程数

​	由于用户场景的不同，对于一些复杂的系统，实际上很难计算出最优线程配置，只能是根据测试数据和用户场景，结合公式给出一个相对合理的范围，然后对范围内的数据进行性能测试，选择相对最优值。



### 18.2 NioEventLoop 源码分析

---

#### 18.2.1 NioEventLoop 设计原理

​	Netty 的 NioEventLoop 并不是一个纯粹的 I/O 线程，它除了负责 I/O 的读写之外，还兼顾处理以下两类任务：

- 系统 Task：通过调用 NioEventLoop 的 execute(Runnable task) 方法实现，Netty 有很多系统 Task，创建他们的主要原因是：当 I/O 线程和用户线程同时操作网络资源时，为了防止并发操作导致的锁竞争，将用户线程的操作封装成 Task 放入消息队列中，由 I/O 线程负责执行，这样就实现了局部无锁化。
- 定时任务：通过调用 NioEventLoop 的 schedule(Runnable command, long delay, TimeUnit unit) 方法实现。

​	正是因为 NioEventLoop 具备多种职责，所以它的实现比较特殊，它并不是简单的 Runnable。

![NioEventLoop继承关系](img\NioEventLoop继承关系.png)

​	它实现了 EventLoop 接口、EventExecutorGroup 接口和 ScheduledExecutorService 接口，正是因为这种设计，导致 NioEventLoop 和其父类功能实现非常复杂。

#### 18.2.2 NioEventLoop 继承关系类图

![NioEventLoop继承关系图](img\NioEventLoop继承关系图.png)

#### 18.2.3 NioEventLoop

​	作为 NIO 框架的 Reactor 线程，NioEventLoop 需要处理网络 I/O 读写事件，因此它必须聚合一个多路复用器对象。

```java
/**
 * The NIO {@link Selector}.
 
 */
Selector selector;
private SelectedSelectionKeySet selectedKeys;

private final SelectorProvider provider;
```

​	Selector 的初始化非常简单，直接调用 Selector.open() 方法就能创建并打开一新的 Selector。Netty 对 Selector 的 selectedKeys 进行了优化，用户可以通过 io.netty.noKeySetOptimization 开关决定是否启用该优化项。默认不打开 selectedKeys 优化功能。

```java
private Selector openSelector() {
    final Selector selector;
    try {
        selector = provider.openSelector();
    } catch (IOException e) {
        throw new ChannelException("failed to open a new selector", e);
    }

    if (DISABLE_KEYSET_OPTIMIZATION) {
        return selector;
    }
    ....
}
```

​	如果没有开启 selectedKeys 优化开关，通过 provider.openSelector() 创建并打开多路复用器之后就立即返回。

​	如果开启了优化开关，需要通过反射的方式从 Selector 实例中获取 selectedKeys 和 publicSelectedKeys，将上述两个成员变量设置为可写，通过反射的方式使用 Netty 构造的 selectedKeys 包装类 selectedKeySet 将原 JDK 的 selectedKeys 替换掉。

​	分析完 Selector 的初始化，下面重点看下 run 方法的实现：

```java
@Override
protected void run() {
    // 所有的逻辑操作都在 for 循环体内进行 只有当 NioEventLoop 接收到退出指令的时候，才退出循环，否则一直执行下去，这也是通用的 NIO 线程实现方式
    for (;;) {
        oldWakenUp = wakenUp.getAndSet(false);
        try {
            // 判断当前的消息队列中是否有消息尚未处理
            if (hasTasks()) {
                selectNow();
            } else {
                // 没有消息需要处理，执行 select() 方法，由 Selector 多路复用器轮询，看是否有准备就绪的 Channel
                select();

                if (wakenUp.get()) {
                    selector.wakeup();
                }
            }

            cancelledKeys = 0;

            final long ioStartTime = System.nanoTime();
            needsToSelectAgain = false;
            if (selectedKeys != null) {
               processSelectedKeysOptimized(selectedKeys.flip());
            } else {
               // 默认未开启 selectedKeys 优化功能，进入 processSelectedKeysPlain 分支执行
               processSelectedKeysPlain(selector.selectedKeys());
            }
            final long ioTime = System.nanoTime() - ioStartTime;
			// 处理完 I/O 事件之后，NioEventLoop 需要执行非 I/O 操作的系统 Task 和 定时任务
            final int ioRatio = this.ioRatio;
            // 由于 NioEventLoop 需要同时处理 I/O 事件和非 I/O 任务，为了保证两者都能得到足够的 CPU 时间被执行，Netty提供了 I/O 比例供用户定制
            runAllTasks(ioTime * (100 - ioRatio) / ioRatio);

            // 判断系统是否进入优雅停机状态          
            if (isShuttingDown()) {               
                closeAll();
                if (confirmShutdown()) {
                    break;
                }
            }
        } catch (Throwable t) {
            logger.warn("Unexpected exception in the selector loop.", t);

            // Prevent possible consecutive immediate failures that lead to
            // excessive CPU consumption.
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // Ignore.
            }
        }
    }
}
```

```java
private void select() throws IOException {
    Selector selector = this.selector;
    try {
        int selectCnt = 0;
        long currentTimeNanos = System.nanoTime();
        // 调用 delayNanos() 方法计算获得 NioEventLoop 中的定时任务的触发时间
        long selectDeadLineNanos = currentTimeNanos + delayNanos(currentTimeNanos);
        for (;;) {
            // 计算出下一个将要触发的定时任务的剩余超时时间，将它转换成毫秒，为超时时间增加 0.5 毫秒的调整值
            long timeoutMillis = (selectDeadLineNanos - currentTimeNanos + 500000L) / 1000000L;
            // 对剩余超时时间进行判断，如果需要立即执行或者已经超时，则调用 selector.selectNow() 进行轮询操作
            if (timeoutMillis <= 0) {
                if (selectCnt == 0) {
                    selector.selectNow();
                    selectCnt = 1;
                }
                break;
            }
            // 否则将定时任务剩余的超时时间作为参数进行 select 操作，每完成一次 select 操作，对 select 计数器 selectCnt 加 1
            int selectedKeys = selector.select(timeoutMillis);
            selectCnt ++;
            // Select 操作完成之后，需要对结果进行判断，如果存在下列任意一种情况，则退出当前循环
            // 1.有 Channel 处于就绪状态，selectedKeys 不为0，说明有读写事件需要处理 2.oldWakenUp 为 true 3.系统或者用户调用了 wakeup 操作，唤醒当前的多路复用器 4.消息队列中有新的任务需要处理
            if (selectedKeys != 0 || oldWakenUp || wakenUp.get() || hasTasks()) {
                // Selected something,
                // waken up by user, or
                // the task queue has a pending task.
                break;
            }
            
            // 如果本次 Selector 的轮询结果为空，也没有 wakeup 操作或是新的消息需要处理，则说明是个空轮询，有可能触发 JDK 的 epoll bug，它会导致 Selector 的空轮询，使 I/O 线程一直处于100%状态。
            // 该 Bug 的修复策略：
            // 1.对 Selector 的 select 操作周期进行统计 2.每完成一次空的 select 操作进行一次计数 3.在某个周期内如果连续发生 N 次空轮询，说明触发了该 bug         
            if (SELECTOR_AUTO_REBUILD_THRESHOLD > 0 &&
                selectCnt >= SELECTOR_AUTO_REBUILD_THRESHOLD) {
                // The selector returned prematurely many times in a row.
                // Rebuild the selector to work around the problem.
                logger.warn(
                    "Selector.select() returned prematurely {} times in a row; rebuilding selector.",
                    selectCnt);
				// 检测到 Selector 处于死循环之后，需要通过重建 Selector 的方式让系统恢复正常
                rebuildSelector();
                selector = this.selector;

                // Select again to populate selectedKeys.
                selector.selectNow();
                selectCnt = 1;
                break;
            }
```

```java
public void rebuildSelector() {
    // 先通过 inEventLoop() 方法判断是否是其他线程发起的 rebuildSelector，如果由其他线程发起，为了避免多线程并发操作 Selector 和其他资源，需要将 rebuildSelector 封装成 Task，放到 NioEventLoop 的消息队列中，由 NioEventLoop 线程负责调用，这样就避免了多线程并发操作导致的线程安全问题
    if (!inEventLoop()) {
        execute(new Runnable() {
            @Override
            public void run() {
                rebuildSelector();
            }
        });
        return;
    }

    final Selector oldSelector = selector;
    final Selector newSelector;

    if (oldSelector == null) {
        return;
    }

    try {
        // 打开新的 Selector
        newSelector = openSelector();
    } catch (Exception e) {
        logger.warn("Failed to create a new Selector.", e);
        return;
    }

    // Register all channels to the new Selector.
    int nChannels = 0;
    // 通过循环，将原 Selector 上注册的 SocketChannel 从旧的 Selector 上去注册，重新注册到新的 Selector 上，并将老的 Selector 关闭
    // 通过销毁旧的、有问题的多路复用器，使用新建的 Selector，就可以解决空轮询的 Selector 导致的 I/O 线程 CPU 占用100%的问题
    for (;;) {
        try {
            for (SelectionKey key: oldSelector.keys()) {
                Object a = key.attachment();
                try {
                    if (key.channel().keyFor(newSelector) != null) {
                        continue;
                    }

                    int interestOps = key.interestOps();
                    key.cancel();
                    key.channel().register(newSelector, interestOps, a);
                    nChannels ++;
                } catch (Exception e) {
                    logger.warn("Failed to re-register a Channel to the new Selector.", e);
                    if (a instanceof AbstractNioChannel) {
                        AbstractNioChannel ch = (AbstractNioChannel) a;
                        ch.unsafe().close(ch.unsafe().voidPromise());
                    } else {
                        @SuppressWarnings("unchecked")
                        NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                        invokeChannelUnregistered(task, key, e);
                    }
                }
            }
        } catch (ConcurrentModificationException e) {
            // Probably due to concurrent modification of the key set.
            continue;
        }

        break;
    }

    selector = newSelector;

    try {
        // time to close the old selector as everything else is registered to the new one
        oldSelector.close();
    } catch (Throwable t) {
        if (logger.isWarnEnabled()) {
            logger.warn("Failed to close the old Selector.", t);
        }
    }

    logger.info("Migrated " + nChannels + " channel(s) to the new Selector.");
}
```

```java
private void processSelectedKeysPlain(Set<SelectionKey> selectedKeys) {
    // check if the set is empty and if so just return to not create garbage by
    // creating a new Iterator every time even if there is nothing to process.
    // See https://github.com/netty/netty/issues/597
    if (selectedKeys.isEmpty()) {
        return;
    }
	
    Iterator<SelectionKey> i = selectedKeys.iterator();
    for (;;) {
        final SelectionKey k = i.next();
        final Object a = k.attachment();
        i.remove();
		// 对 SocketChannel 的附件类型进行判断，如果是 AbstractNioChannel 类型，说明它是 NioServerSocketChannel 或者 NioSocketChannel，需要进行 I/O 读写相关操作
        if (a instanceof AbstractNioChannel) {
            processSelectedKey(k, (AbstractNioChannel) a);
        } else {
            // 如果是 NioTask，则对其进行类型转换，调用processSelectedKey 进行处理。Netty 自身没实现 NioTask 接口，都是用户自行注册该 Task 到多路复用器上
            @SuppressWarnings("unchecked")
            NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
            processSelectedKey(k, task);
        }

        if (!i.hasNext()) {
            break;
        }

        if (needsToSelectAgain) {
            selectAgain();
            selectedKeys = selector.selectedKeys();

            // Create the iterator again to avoid ConcurrentModificationException
            if (selectedKeys.isEmpty()) {
                break;
            } else {
                i = selectedKeys.iterator();
            }
        }
    }
}
```

```java
private static void processSelectedKey(SelectionKey k, AbstractNioChannel ch) {
    // 首先从 NioServerSocketChannel 或者 NioSocketChannel 中获取其内部类 Unsafe，判断当前选择键是否可用，如果不可用，调用 Unsafe 的 close 方法，释放连接资源
    final NioUnsafe unsafe = ch.unsafe();
    if (!k.isValid()) {
        // close the channel if the key is not valid anymore
        unsafe.close(unsafe.voidPromise());
        return;
    }

    try {
        // 如果选择键可用，则继续对网络操作位进行判断
        int readyOps = k.readyOps();
        // Also check for readOps of 0 to workaround possible JDK bug which may otherwise lead
        // to a spin loop
        if ((readyOps & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0 || readyOps == 0) {
            // Unsafe 的实现是个多态，对于 NioServerSocketChannel，它的读操作就是接收客户端的 TCP 连接，对于 NioSocketChannel，它的读操作就是从 SocketChannel 中读取 ByteBuf
            unsafe.read();
            if (!ch.isOpen()) {
                // Connection already closed - no need to handle write.
                return;
            }
        }
        if ((readyOps & SelectionKey.OP_WRITE) != 0) {
            // Call forceFlush which will also take care of clear the OP_WRITE once there is nothing left to write
            // 如果操作位为写，则说明有半包消息尚未完成发送
            ch.unsafe().forceFlush();
        }
        if ((readyOps & SelectionKey.OP_CONNECT) != 0) {
            // remove OP_CONNECT as otherwise Selector.select(..) will always return without blocking
            // See https://github.com/netty/netty/issues/924
            int ops = k.interestOps();
            ops &= ~SelectionKey.OP_CONNECT;
            k.interestOps(ops);
			// 如果网络操作位为连接状态，则需要对连接结果进行判断
            unsafe.finishConnect();
        }
    } catch (CancelledKeyException e) {
        unsafe.close(unsafe.voidPromise());
    }
}
```

```java
protected boolean runAllTasks(long timeoutNanos) {
    // 首先从定时任务消息队列中弹出消息进行处理
    fetchFromDelayedQueue();
    Runnable task = pollTask();
    if (task == null) {
        return false;
    }

    final long deadline = ScheduledFutureTask.nanoTime() + timeoutNanos;
    long runTasks = 0;
    long lastExecutionTime;
    for (;;) {
        try {
            task.run();
        } catch (Throwable t) {
            logger.warn("A task raised an exception.", t);
        }

        runTasks ++;

        // Check timeout every 64 tasks because nanoTime() is relatively expensive.
        // XXX: Hard-coded value - will make it configurable if it is really a problem.
        // 由于获取系统纳秒时间是个耗时的操作，每次循环都获取当前系统纳秒时间进行超时判断会降低性能。为了提升性能，每执行60次循环判断一次
        if ((runTasks & 0x3F) == 0) {
            lastExecutionTime = ScheduledFutureTask.nanoTime();
            // 如果当前系统时间已经到了分配给非 I/O 操作的超时时间，则退出循环，防止由于非 I/O 任务过多导致 I/O 操作被长时间阻塞
            if (lastExecutionTime >= deadline) {
                break;
            }
        }

        task = pollTask();
        if (task == null) {
            lastExecutionTime = ScheduledFutureTask.nanoTime();
            break;
        }
    }

    this.lastExecutionTime = lastExecutionTime;
    return true;
}
```

```java
private void fetchFromDelayedQueue() {
    long nanoTime = 0L;    
    for (;;) {
        ScheduledFutureTask<?> delayedTask = delayedTaskQueue.peek();
        // 如果消息队列为空，则退出循环
        if (delayedTask == null) {
            break;
        }

        if (nanoTime == 0L) {
            nanoTime = ScheduledFutureTask.nanoTime();
        }
		// 根据时间戳判断，如果该定时任务已经或者正处于超时状态，则将其加入到 TaskQueue 中，同时从延时队列中删除
        if (delayedTask.deadlineNanos() <= nanoTime) {
            delayedTaskQueue.remove();
            taskQueue.add(delayedTask);
        } else {
            break;
        }
    }
}
```



## 第十九章 Future 和 Promise

### 19.1 Future 功能

​	Future 最早来源于 JDK 的 java.util.concurrent.Future，它用于代表异步操作的结果。

​	可以通过 get 方法获取操作结果，如果操作尚未完成，则会同步阻塞当前调用的线程；可以设置阻塞时间，如果到达超时时间仍然没有完成，则抛出 TimeoutException。

![JDK Future 的 API 列表](img\JDK Future 的 API 列表.png)

​	**ChannelFuture 功能介绍**

​	由于 Netty 的 Future 都是与异步 I/O 操作相关的，因此，命名为 ChannelFuture， 代表它与 Channel 操作相关。

![ChannelFuture 接口列表](img\ChannelFuture 接口列表.png)

 	![ChannelFuture 接口列表续表](C:\Users\wr1ttenyu\学习\study-record\Netty\img\ChannelFuture 接口列表续表.png)

​	在 Netty 中，所有的 I/O 操作都是异步的，这意味着任何 I/O 调用都会立即返回，而不是像传统 BIO 那样同步等待操作完成。那么调用者如何获取异步操作的结果？

​	ChannelFuture 有两种状态：uncompleted 和 completed。当开始一个 I/O 操作时，一个新的 ChannelFuture 被创建，此时它处于 uncompleted 状态 —— 非失败、非成功、非取消，因为 I/O 操作此时还没有完成。一旦 I/O 操作完成，ChannelFuture 将会被设置成 completed，他的结果有如下三种可能：

- 操作完成
- 操作失败
- 操作被取消

​	ChannelFuture 的状态迁移图：

![ChannelFuture 状态迁移图](img\ChannelFuture 状态迁移图.png)

​	ChannelFuture 提供了一系列新的 API，用于获取操作结果、添加事件监听器、取消 I/O 操作、同步等待等。

​	我们重点介绍添加监听器的接口：

![ChannelFuture管理监听器](img\ChannelFuture管理监听器.png)

​	Netty 强烈建议直接通过添加监听器的方式获取 I/O 操作结果，或者进行后续的相关操作。

​	ChannelFuture 可以同时增加一个或者多个 GenericFutureListener，也可以用 remove 方法删除 GenericFutureListener。

​	GenericFutureListener 的接口定义：

![GenericFutureListener接口定义](img\GenericFutureListener接口定义.png)

​	当 I/O 操作完成之后，I/O 线程会回调 ChannelFuture 中 GenericFutureListener 的 operationComplete 方法，并把 ChannelFuture 对象当作方法的入参。如果用户需要做上下文相关的操作，需要将上下文信息保存到对应的 ChannelFuture 中。

​	推荐通过 GenericFutureListener 代替 ChannelFuture 的 get 等方法的原因是：当我们进行异步 I/O 操作时，完成的时间是无法预测的，如果不设置超时时间，他会导致调用线程长时间被阻塞，甚至挂死。而设置超时时间，时间又无法精确预测。利用异步通知机制回调 GenericFutureListener 是最佳的解决方法，他的性能最优。

​	需要注意的是：不要在 ChannelHandler 中调用 ChannelFuture 的 await() 方法，这会导致死锁。原因是发起 I/O 操作之后，由 I/O 线程负责异步通知发起 I/O 操作的用户线程，如果 I/O 线程和用户线程是同一个线程，就会导致 I/O 线程等待自己通知操作完成，这就导致了死锁，这跟经典的两个线程互等死锁不同，属于自己把自己挂死。

```java
// BAD - NEVER DO THIS
public void channelRead(ChannelHandlerContext ctx, GoodByeMessage msg) {
	ChannelFuture future = ctx.channel().close();
    future.awaitUninterruptibly();
}
// GOOD
public void channelRead(ChannelHandlerContext ctx, GoodByeMessage msg) {
	ChannelFuture future = ctx.channel().close();
    future.addListener(new ChannelFutureListener() {
        public void operationComplete(ChannelFuture future) {
            
        }
    })
}
```

​	异步 I/O 操作有两类超时：一个是 TCP 层面的 I/O 超时，另一个是业务逻辑层面的操作超时。两者没有必然的联系，但是通常情况下业务逻辑超时时间应该大于 I/O 超时时间，他们两者是包含的关系。

```java
// GOOD
// I/O 超时时间配置
Bootstrap b = ...;
// configure the connect timeout option
b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
ChannelFuture f = b.connect(...);
f.awaitUninterruptibly();

// Now we are sure the future is completed
assert f.isDone();

if (f.isCancelled()) {
    // Connection attempt cancelled by user
} else if (!f.isSuccess()) {
    f.cause().printStackTrace();
} else {
    // Connection established successfully
}

// ChannelFuture 超时时间配置
Bootstrap b = ...;
ChannelFuture f = b.connect(...);
f.awaitUninterruptibly(10, TimeUnit.SECONDS);
if (f.isCancelled()) {
    // Connection attempt cancelled by user
} else if (!f.isSuccess()) {
    // You might get a NullPointerException here beacuse the future might not be completed yet
    f.cause().printStackTrace();
} else {
    // Connection established successfully
}
```

​	需要指出的是：ChannelFuture 超时并不代表 I/O 超时，这意味着 ChannelFuture 超时后，如果没有关闭连接资源，随后连接依旧可能会成功，这会导致严重的问题。所以通常情况下，必须要考虑究竟是设置 I/O  超时还是 ChannelFuture 超时。



### 19.2 ChannelFuture 源码分析

---

​	ChannelFuture 的接口继承关系：

![ChannelFuture 接口继承关系图](img\ChannelFuture 接口继承关系图.png)

​	**AbstractFuture**

​	AbstractFuture 实现 Future 接口，它不允许 I/O 操作被取消。

​	获取异步操作结果的代码：

```java
public V get() throws InterrupteExceptiuon, ExecutionException {
    // 调用 await() 方法进行无限期阻塞，当 I/O 操作完成后会被 notify()
    await();
    // 检查 I/O 操作是否发生异常
    Throwable cause = cause();
    if (cause == null) {
        // 如果没有异常 获取结果并返回
        return getNow();
    }
    // 如果有异常 包装异常进行返回
    throw new ExecutionException(cause);
}
```



### 19.3 Promise 功能介绍

---

​	Promise 是可写的 Future，Future 自身并没有写操作相关的接口，Netty 通过 Promise 对 Future  进行扩展，用于设置 I/O 操作的结果。

![Netty 的 Future 接口定义](img\Netty 的 Future 接口定义.png)

![Promise 写操作相关的接口定义](img\Promise 写操作相关的接口定义.png)

​	Netty 发起 I/O 操作的时候，会创建一个新的 Promise 对象，例如调用 ChannelHandlerContext 的 write(Object object) 方法时，会创建一个新的 ChannelPromise，相关代码如下：

```java
public ChannelPromise newPromise() {
    return new DefaultChannelPromise(channel(), executor());
}
```

​	当 I/O 操作发生异常或者完成时，设置 Promise 的结果：

```java
public void write(Object msg, ChannelPromise promise) {
    if (!isActive()) {
        // Mark the write request as failure if the channel is inactive 
        if (isOpen()) {
            promise.tryFailure(NOT_YET_CONNECTED_EXCEPTION);
        } else {
            promise.tryFailure(CLOSED_CHANNEL_EXCEPTION);
        }
        // release message now to prevent resource-leak
        ReferenceCountUtil.release(msg);
    } else {
    	outboundBuffer.addMessage(msg, promise);
    }
}
```



### 19.4 Promise 源码分析

---

#### 19.4.1 Promise 继承关系图

​	由于 I/O 操作种类非常多，因此对应的 Promise 子类也非常繁多，它的继承关系如下图：

![Promise继承关系图](img\Promise继承关系图.png)

#### 19.4.2 DefaultPromise

1. setSuccess 方法

```java
public Promise<V> setSuccess(V result) {
    // 调用 setSuccess0 方法并对其操作结果进行判断
    if (setSuccess0(result)) {
        // 操作成功 调用 notifyListeners 方法通知 listener
        notifyListeners();
        return this;
    }
    throw new IllegelStateException("complete already: " + this);
}
```

```java
private boolean setSuccess0(V result) {
    // 首先判断当前 Promise 的操作结果是否已经被设置，如果已经被设置，则不允许重复设置，返回设置失败
    if (isDone()) {
        return false;
    }
	// 由于可能存在 I/O 线程用户和用户线程同时操作 Promise，所以加锁保护
    synchronized (this) {
        // Allow only once.
        // 二次判断(为了提升并发性能的二次判断)
        if (isDone()) {
            return false;
        }
        // 对操作结果 result 进行判断，如果为空，说明仅仅需要 notify 在等待的业务线程，不包含具体的业务逻辑对象
        if (result == null) {
            this.result = SUCCESS;
        } else {
            // 如果操作结果非空，将结果设置为 result
            this.result = result;
        }
        // 如果有正在等待异步 I/O 操作完成的用户线程或者其他系统线程，则调用 notifyAll 方法唤醒所有正在等待的线程
        // 注意：notifyAll 和 wait 方法都必须在同步块内使用
        if (hasWaiters()) {
            notifyAll();
        }
    }
    return true;
}
```

2. await 方法

```java
@Override
public Promise<V> await() throws InterruptedException {
    // 首先判断当前 Promise 的操作结果是否已经被设置，如果已经被设置，则直接返回
    if (isDone()) {
        return this;
    }

    if (Thread.interrupted()) {
        throw new InterruptedException(toString());
    }
	// 通过同步关键字锁定当前的 Promise 对象，使用循环判断对 isDone 结果进行判断，进行循环判断的原因是防止线程被意外唤醒导致的功能异常
    synchronized (this) {
        while (!isDone()) {
            // 由于在 I/O 线程中调用 Promise 的 await 或者 sync 方法会导致死锁，所以在循环体中需要对死锁进行保护性校验，防止 I/O 线程被挂死
            checkDeadLock();
            incWaiters();
            try {
                // 调用 java.lang.Object.wait() 方法进行无限期等待，直到 I/O 线程调用 setSuccess 方法、trySuccess 方法，setFailure 方法或者 tryFailure 方法
                wait();
            } finally {
                decWaiters();
            }
        }
    }
    return this;
}
```

### 19.5  总结

---

 	本章重点介绍了 Future 和 Promise，由于 Netty 中的 I/O 操作种类繁多，所以 Future 和 Promise 的紫子类也非常繁多。尽管这些子类的功能各异，但本质上都是异步 I/O 操作结果的通知回调类。Future-Listener 机制在 JDK 中的应用已经非常广泛，所以本章并没有对这些子类的实现做过多的源码分析，希望读者在本章源码分析的基础上自行学习其他相关子类的实现。

​	**无论 Future 还是 Promise，都强烈建议读者通过增加监听器 Listener 的方式接受异步 I/O 操作结果的通知，而不是调用 wait 或者 sync** **阻塞用户线程**。



## 第二十章 Netty 架构剖析

### 20.1 Netty 逻辑架构

---

​	Netty 采用了典型的三层网络架构进行设计和开发，逻辑架构如下图：

![Netty 逻辑架构图](img\Netty 逻辑架构图.png)

#### 20.1.1 Reactor 通信调度层

​	它由一系列辅助类完成，包括 Reactor 线程 NioEventLoop 及其父类，NioSocketChannel/NioServerSocketChannel 及其父类，ByteBuffer 以及由其衍生出来的各种 Buffer，Unsafe 以及其衍生出的各种内部类等。该层的主要职责就是监听网络的读写和连接操作，负责将网络层的数据读取到内存缓冲区中，然后触发各种网络事件，例如连接创建、连接激活、读事件、写事件等，将这些事件触发到 PipeLine 中，由 Pipeline 管理的职责链来进行后续的处理。

#### 20.1.2 职责链 ChannelPipeline

​	它负责事件在职责链中的有序传播，同时负责动态地编排职责链。职责链可以选择监听和处理自己关心的事件，它可以拦截处理和向后/向前传播事件。不同应用的 Handler 节点的功能也不同，通常情况下，往往会开发编解码 Handler 用于消息的编解码，它可以将外部的协议消息转换成内部的 POJO 对象，这样上层业务则只需要关心处理业务逻辑即可，不需要感知底层的协议差异和线程模型差异，实现了架构层面的分层隔离。

####20.1.3 业务逻辑编排层(Service ChannelHandler)

​	业务逻辑编排层通常有两类：一类是纯粹的业务逻辑编排，还有一类是其他的应用层协议插件，用于特定协议相关的会话和链路管理。例如 CMPP 协议，用于管理和中国移动短信系统的对接。

​	架构的不同层面，需要关心和处理的对象不同，通常情况下，对于业务开发者，只需要关心职责链的拦截和业务 Handler 的编排。因为应用层协议栈往往是开发一次，到处运行，所以实际上对于业务开发者来说，只需要关心服务层的业务逻辑开发即可。各种应用协议以插件的形式提供，只有协议开发人员需要关注协议插件，对于其他业务开发人员来说，只需要关心业务逻辑定制。这种分层的架构设计理念实现了 NIO 框架各层之间的解耦，便于上层业务协议栈的开发和业务逻辑的定制。

​	正是由于 Netty 的分层架构设计非常合理，基于 Netty 的各种应用服务器和协议栈开发才能够如雨后春笋般得到快速发展。



### 20.2 关键结构质量属性

---

#### 20.2.1 高性能

​	影响最终产品的性能因素非常多，其中软件因素如下：

- 架构不合理导致的性能问题

- 编码实现不合理导致的性能问题，例如锁的不恰当使用导致性能瓶颈

  硬件因素如下：

- 服务器硬件配置太低导致的性能问题

- 带宽、磁盘的 IOPS 等限制导致的 I/O 操作性能差

- 测试环境被共用导致被测试的软件产品受到影响

​	尽管影响产品性能的因素非常多，但是架构的性能模型合理与否对性能的影响非常大。如果一个产品的架构设计不好，无论开发如何努力，都很难开发出一个高性能、高可用的软件产品。

​	Netty 的架构设计是如何实现高性能的：

1. 采用异步非阻塞的 I/O 类库，基于 Reactor 模式实现，解决了传统同步阻塞 I/O 模式下一个服务端无法平滑地处理线性增长的客户端的问题；
2. TCP 接收和发送缓冲区使用直接内存代替堆内存，避免了内存复制，提升了 I/O 读取和写入的性能；
3. 支持通过内存池的方式循环利用 ByteBuf，避免了频创建和销毁 ByteBuf 带来的性能损耗；
4. 可配置的 I/O 线程数、TCP 参数等，为不同的用户场景提供定制化的调优参数，满足不同的性能场景；
5. 采用环形数组缓冲区实现无锁化并发编程，代替传统的线程安全容器或者锁；
6. 合理地使用线程安全容器、原子类等，提升系统的并发处理能力；
7. 关键资源的处理使用单线程串行化的方式，避免多线程并发访问带来的锁竞争和额外的 CPU 资源消耗问题；
8. 通过引用计数器及时地申请释放不再被引用的对象，细粒度的内存管理降低了 GC 的频率，减少了频繁 GC 带来的时延增大和 CPU 损耗。

​	无论是 Netty 的官方性能测试数据，还是携带业务实际场景的性能测试，Netty 在各个 NIO 框架中综合性能是最高的。

#### 20.2.2 可靠性

​	作为一个高性能的异步通信框架，架构的可靠性是大家选择的一个重要依据。下面我们探讨 Netty 架构的可靠性设计。

1. 链路有效性检测

​	由于长连接不需要每次发送消息都创建链路，也不需要在消息交互完成时关闭链路，因此相对于短连接性能更高。对于长连接，一旦链路建立成功便一直维系双方之间的链路，直到系统退出。

​	为了保证长连接的链路有效性，往往需要通过心跳机制周期性地进行链路检测。使用周期性心跳的原因是：在系统空闲时，例如凌晨，往往没有业务消息，如果此时链路被防火墙 Hang 住，或者遭遇网络闪断、网络单通等，通信双方无法识别出这类链路异常。等到第二天业务高峰期到来时，瞬间海量业务冲击会导致消息积压无法发送给对方，由于链路的重建需要时间，这期间业务会大量失败(集群或者分布式组网情况会好一些)。为了解决这个问题，需要周期性的心跳对链路进行有效性检测，一旦发生问题，可以及时关闭链路，重建 TCP 连接。

​	当有业务消息时，无须心跳检测，可以由业务消息进行链路可用性检测。所以心跳消息往往是在链路空闲时发送的。

​	为了支持心跳，Netty 提供了如下两种链路空闲检测机制：

- 读空闲超时机制：当连续周期 T 没有消息可读时，触发超时 Handler，用户可以基于读空闲超时发送心跳消息，进行链路检测；如果连续 N 个周期仍然没有读取到心跳消息，可以主动关闭链路。
- 写空闲超时机制：当连续周期 T 没有消息要发送时，触发超时 Handler，用户可以基于写空闲超时发送心跳消息，进行链路检测；如果连续 N 个周期仍然没有接收到对方的心跳消息，可以主动关闭链路。

​	为了满足不同用户场景的心跳定制，Netty 提供了空闲状态检测事件通知机制，用户可以订阅读空闲超时事件、写空闲超时事件、读或者写超时事件，在接收到对应的空闲事件之后，灵活地进行定制

2. 内存保护机制

​	Netty 提供多种机制对内存进行保护，包括以下几个方面：

- 通过对象引用计数器对 Netty 的 ByteBuf 等内置对象进行细粒度的内存申请和释放，对非法的对象引用进行检测和保护；

- 通过内存池来重用 ByteBuf，节省内存；

- 可设置的内存容量上限，包括 ByteBuf、线程池线程数等。

  AbstractReferenceCountedByteBuf 的内存管理方法实现：

```java
// 对象引用
@Override
public ByteBuf retain(int increment) {
    if (increment <= 0) {
        throw new IllegalArgumentException("increment: " + increment + " (expected: > 0)");
    }

    for (;;) {
        int refCnt = this.refCnt;
        if (refCnt == 0) {
            throw new IllegalReferenceCountException(0, increment);
        }
        if (refCnt > Integer.MAX_VALUE - increment) {
            throw new IllegalReferenceCountException(refCnt, increment);
        }
        if (refCntUpdater.compareAndSet(this, refCnt, refCnt + increment)) {
            break;
        }
    }
    return this;
}

// 对象引用释放
@Override
public final boolean release() {
    for (;;) {
        int refCnt = this.refCnt;
        if (refCnt == 0) {
            throw new IllegalReferenceCountException(0, -1);
        }

        if (refCntUpdater.compareAndSet(this, refCnt, refCnt - 1)) {
            if (refCnt == 1) {
                deallocate();
                return true;
            }
            return false;
        }
    }
}
```

​	ByteBuf 的解码保护，防止非法码流导致内存溢出，代码如图：

````java
public LengthFieldBasedFrameDecoder(
            int maxFrameLength, int lengthFieldOffset, int lengthFieldLength,
            int lengthAdjustment, int initialBytesToStrip, boolean failFast) {
    this(
        ByteOrder.BIG_ENDIAN, maxFrameLength, lengthFieldOffset, lengthFieldLength,
        lengthAdjustment, initialBytesToStrip, failFast);
}
````

​	如果长度解码器没有单个消息最大报文长度限制，当解码错位或者读取到畸形码流时，长度值可能是个超大整数值，这很容易导致内存溢出。如果有上限保护，当读取到非法消息长度时，直接抛出解码异常，这样就避免了大内存的分配。

3. 优雅停机

​	相比于 Netty 的早期版本，Netty 5.0 版本的优雅退出功能做得更加完善。优雅停机功能指的是当系统推退出时，JVM 通过注册的 Shutdown Hook 拦截到退出信号，然后执行退出操作，释放相关模块的资源占用，将缓冲区的消息处理完成或者清空，将待刷新的数据持久化到磁盘或者数据库中，等到资源回收和缓冲区消息处理完成的之后，再退出。

​	优雅停机往往需要设置个最大超时时间 T，如果达到 T 后系统仍然没有退出，则通过 Kill -9 pid 强杀当前的进程。

​	Netty 所有涉及到资源回收和释放的地方都增加了优雅退出的方法，他们的相关接口如下表：

![Netty重要资源的优雅退出方法](img\Netty重要资源的优雅退出方法.png)

![Netty重要资源的优雅退出方法续表](img\Netty重要资源的优雅退出方法续表.png)

#### 20.2.3 可定制性

​	Netty 的可定制性主要体现在以下几点：

- 责任链模式：ChannelPipeline 基于责任链模式开发，便于业务逻辑的拦截、定制和扩展；
- 基于接口的开发：关键的类库都提供了接口或者抽象类，如果 Netty 自身的实现无法满足用户的需求，可以由用户自定义实现相关接口；
- 提供了大量工厂类，通过重载这些工厂类可以按需创建出用户实现的对象；
- 提供了大量的系统参数供用户按需设置，增强系统的场景定制性。

#### 20.2.4 可扩展性

​	基于 Netty 的基础 NIO 框架，可以方便地进行应用层协议定制，例如 HTTP 协议栈、Thrift 协议栈、FTP 协议栈等。这些扩展不需要修改 Netty 的源码，直接基于 Netty 的二进制类库即可实现协议的扩展和定制。

​	目前，业界存在大量的基于 Netty 框架开发的协议，例如基于 Netty 的 HTTP 协议、Dubbo 协议、RocketMQ 内部私有协议等。

## 第二十一章 Java 多线程编程在 Netty 中的应用

### 21.1 Java 内存模型与多线程编程

---

#### 21.1.1 硬件的发展和多任务处理

​	随着硬件的发展，多任务处理已经是所有操作系统必备的一向基本功能。让 CPU 的多核特性和高计算能力得到充分使用。

​	Java 提供了很多类库和工具用于降低并发编程的门槛，也有第三方类库提供封装的工具类来方便 Java 开发者，但是无论并发类库设计的如何完美，他都无法涵盖所有用户的需求，所以对于一个 Java 程序员来说，还是要懂得 Java 并发编程原理。

#### 21.1.2 Java 内存模型

​	JVM 规范定义了 Java 内存模型来屏蔽掉各种操作系统、虚拟机实现厂商和硬件的内存访问差异，以确保 Java 程序在所有操作系统和平台上能够实现一次编写、到处运行的效果。

1. 工作内存和主内存

​	Java 内存模型规定所有的变量都存储在主内存中，每个线程有自己独立的工作内存，它保存了被该线程使用的变量的主内存复制。线程对这些变量的操作都在自己的工作内存中进行，不能直接操作主内存和其他工作内存中的变量或者变量副本。线程间的变量访问需要通过主内存来完成。

![Java内存访问模型](img\Java内存访问模型.png)

2. Java 内存交互协议

   Java 内存模型定义了 8 种操作来完成主内存和工作内存的变量访问，具体如下：

- lock：主内存变量，把一个变量表示为某个线程独占的状态；
- unlock：主内存变量，把一个处于锁定状态变量释放出来，被释放后的变量才可以被其他线程锁定；
- read：主内存变量，把一个变量的值从主内存传输到线程的动作内存中，以便随后的 load 动作使用；
- load：工作内存变量，把 read 读取到的主内存中的变量值放入工作内存的变量副本中；
- use：工作内存变量，把工作内存中变量的值传递给 Java 虚拟机执行引擎，每当虚拟机遇到一个需要使用到变量值的字节码指令时，将会执行该操作；
- assign：工作内存变量，把从执行引擎接受到的变量的值赋值给工作变量，每当虚拟机遇到一个给变量复制的字节码时，将会执行该操作；
- store：工作内存变量，把工作内存中一个变量的值传送到主内存中，以便随后的 write 操作使用；
- write：主内存变量，把 store 操作从工作内存中得到的变量值放入主内存的变量中。

3. Java 的线程

​	并发可以通过多种方式来实现，例如：单进程 - 单进程模型，通过在一台服务器上启动多个进程来实现多任务的并行处理。但是在  Java 语言中，通常是通过单进程 - 多线程的模型进行多任务的并发处理。

​	主流的操作系统提供了线程实现，目前实现线程的方式主要有三种：

1. 内核线程实现
2. 用户线程实现
3. 混合实现

​	在 Windows 和 Linux 操作系统上采用了内核线程的实现方式，在 Solaris 版本的 JDK 中，提供了一些专有的虚拟机线程参数，用于设置使用哪种线程模型。



### 21.2 Netty 的并发编程实战

---

#### 21.2.1 对共享的可变数据进行正确的同步

​	关键字 synchronized 可以保证在同一时刻，只有一个线程可以执行某一个方法或者代码块。同步的作用不仅仅是互斥，它的另一个作用就是共享可变性，当某个线程修改了可变数据并释放锁后，其他线程可以获取被修改变量的最新值。如果没有正确的同步，这种修改对其他线程是不可见的。

​	Netty 是如何对并发可变数据进行正确同步的，以 ServerBootstrap 为例进行分析：

```java
public <T> B option(ChannelOption<T> option, T value) {
    // 对 option 和 value 的合法性判断不需要加锁
    // 保证锁的范围尽可能细粒度
    if (option == null) {
        throw new NullPointerException("option");
    }
    if (value == null) {
        // 由于ServerBootstrap是被外部使用者创建和使用的，无法保证他的方法和成员变量不被并发访问，因此，作为成员变量的 options 必须进行正确地同步
        synchronized (options) {
            options.remove(option);
        }
    } else {
        synchronized (options) {
            options.put(option, value);
        }
    }
    return (B) this;
}
```

####21.2.2 正确使用锁

1. 通过 ForkJoinTask，来学习一些多线程同步和写作方面的技巧

```java
private int externalAwaitDone() {
    int s;
    ForkJoinPool cp = ForkJoinPool.common;
    if ((s = status) >= 0) {
        if (cp != null) {
            if (this instanceof CountedCompleter)
                s = cp.externalHelpComplete((CountedCompleter<?>)this, Integer.MAX_VALUE);
            else if (cp.tryExternalUnpush(this))
                s = doExec();
        }
        if (s >= 0 && (s = status) >= 0) {
            boolean interrupted = false;
            // 通过循环检测的方式对状态变量 status 进行判断
            do {
                if (U.compareAndSwapInt(this, STATUS, s, s | SIGNAL)) {
                    synchronized (this) {
                        // 状态大于等于 0 执行 wait(),阻塞当前的调度线程，直到 status 小于 0，唤醒所有被阻塞的线程，继续执行
                        if (status >= 0) {
                            try {
                                // 三个多线程的编程技巧
                                // 1.wait 方法用来使线程等待某个条件，它必须在同步块内部被调用，这个同步块通常会锁定当前对象实例
                                // 2.始终使用 wait 循环来调用 wait 方法，永远不要在循环之外调用 wait 方法。这样做的原因是尽管不满足被唤醒条件，但是由于其他线程调用 notifyAll() 方法会导致被阻塞线程意外被唤醒，此时执行条件并不满足，它将破坏被锁保护的约定关系，导致约束失效，引起意想不到的结果
                                // 3.唤醒线程，应该使用 notify 还是 notifyAll？当你不知道究竟该调用哪个方法时，保守的做法是调用 notifyAll 唤醒所有等待的线程。从优化的角度看，如果处于等到的所有线程都在等待同一个条件，而每次只有一个线程可以从这个条件中被唤醒，那么就应该选择调用 notify
                                wait();
                            } catch (InterruptedException ie) {
                                interrupted = true;
                            }
                        }
                        else
                            notifyAll();
                    }
                }
            } while ((s = status) >= 0);
            if (interrupted)
                Thread.currentThread().interrupt();
        }
    }
    return s;
}
```

​	当多个线程共享同一个变量的时候，每个读或者写数据的操作方法都必须加锁进行同步，如果没有正确的同步，就无法保证一个线程所做的修改被其他线程共享。未能同步共享变量会造成程序的活性失败和安全性失败，这样的失败通常是难以调试和重现的，他们可能间歇性地出现问题，可能随着并发的线程个数增加而失败，也可能在不同的虚拟机或者操作系统上存在不同的失败概率。因此，务必要保证锁的正确使用。

​	下面这个案例，就是典型的错误应用：

```java
int size = 0;
public synchronized void increase() {
    size++;
}
public int current() {
    return size;
}
```

#### 21.2.3 volatile 的正确使用

​	长久以来对于 volatile 的如何正确使用有很多的争议，其实只要理解了 Java 的内存模型和多线程编程的基础知识，那么正确的使用 volatile 是不存在任何问题的。

​	关键字 volatile 是 Java 提供的最轻量级的同步机制，Java 内存模型对 volatile 专门定义了一些特殊访问的规则，规则如下：

​	当一个变量被 volatile 修饰后，它将具备以下两种特性：

- 线程可见性：当一个线程修改了被 volatile 修饰的变量后，无论是否加锁，其他线程都可以立即看到最新的修改，而普通变量却做不到这点
- 禁止指令重排序优化，普通的变量仅仅保证在该方法的执行过程中所有依赖赋值结果的地方都能获取正确的结果，而不能保证变量赋值操作的顺序与程序代码的执行顺序一致。以下面代码为例：

```java
public class ThreadStopExample {
    private static boolean stop;
    public static void main(String args[]) throws InterruptedException {
        java.lang.Thread workThread = new java.lang.Thread(new Runnable() {
            public void run() {
                int i = 0;
                while (!stop) {
                    i++;
                    try {
                        TimeUnit.SECONDS.sleep(1);     
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        workThread.start();
        TimeUnit.SECOND.sleep(3);
        stop = true;
    }
}

// 上面的代码：我们预期程序会在 3s 后停止，但实际上他会一直执行下去，原因就是虚拟机对代码进行了指令重排序和优化，优化后的指令如下：
// if (!stop) {
//     while (true) {
//	   }
// }
// 重排序后的代码是无法发现 stop 被主线程修改的，因此无法停止运行。要解决这个问题，只要将 stop 前增加 volatile 修饰即可
// 再次运行，我们发现 3s 后程序退出，达到预期效果，使用 volatile 解决了如下两个问题：
// 1. main 线程对 stop 的修饰在 workThread 线程中可见，也就是说 workThread 线程立即看到了其他线程对于 stop 变量的修改
// 2. 禁止指令重排序，防止因为重排序导致的并发访问逻辑混乱
```

​	**一些人认为使用 volatile 可以代替传统锁，提升并发性能，这个认识是错误的。**

​	**volatile 仅仅解决了可见性的问题**，但是他并不保证互斥性，也就是说多个线程并发修改某个变量时，依旧会产生多线程问题。因此，不能靠 volatile 来完全代替传统的锁。

​	根据经验总结，volatile 最适合使用的是一个线程写，其他线程读的场合，如果有多个线程并发写操作，仍然需要使用锁或者线程安全的容器或者原子变量来代替。

​	在 Netty 的 NioEventLoop 中，有定义如下成员变量：

```java
// 控制 I/O 操作和其他任务运行比例的 ioRatio
private volatile int ioRatio = 50;
```

​	通过代码分析我们发现，在 NioEventLoop 线程中，ioRatio 并没有被修改，而是提供了重新设置 ioRatio 的公共方法：

```java
public void setIoRatio(int ioRatio) {
    if (ioRatio <= 0 || ioRatio >= 100) {
        throw new IllegalArgumentException("ioRatio: " + ioRatio + " (expected: 0 < ioRatio < 100)");
    }
    this.ioRatio = ioRatio;
}
```

​	NioEventLoop 线程没有调用该方法，说明调整 I/O 执行时间比例是外部发起的操作，通常是由业务的线程调用该方法，重新设置该参数。这样就形成了一个线程写、多个线程读。**根据前面对 volatile 的应用总结，此时可以使用 volatile 来代替传统的 synchronized 关键字提升并发访问的性能。**

#### 21.2.4 CAS 指令和原子类

​	互斥同步最主要的问题就是进行线程阻塞和唤醒所带来的性能的额外损耗，因此这种这种同步被称为阻塞同步，它属于一种悲观的并发策略，我们称之为悲观锁。随着硬件和操作系统指令集的发展和优化，产生了非阻塞同步，被称为乐观锁。简单地说，就是先进行操作，操作完成之后再判断操作是否成功，是否有并发问题，如果有则进行失败补偿，如果没有就算操作成功，这样就从根本上避免了同步锁的弊端。

​	从 JDK 1.5 以后，可以使用 CAS 操作，该操作由 sun.misc.Unsafe 类里的 compareAndSwapInt() 和 compareAndSwapLong() 等方法包装提供。通常情况下 sun.misc.Unsafe 对于开发者是不可见的，因此， JDK 提供了很多 CAS 包装类简化开发者的使用，如 AtomicInteger。

​	通过 Netty ChannelOutboundBuffer 的源码，对原子类的正确使用进行说明，看看如何对发送的总字节数进行计数和更新操作。

```java
// AtomicLongFieldUpdater 保证多线程修改并发安全性
private static final AtomicLongFieldUpdater<ChannelOutboundBuffer> TOTAL_PENDING_SIZE_UPDATER = AtomicLongFieldUpdater.newUpdater(ChannelOutboundBuffer.class, "totalPendingSize");
private volatile long totalPendingSize;
```

```java
// 当执行 write 操作的时候，需要对外发的消息字节数进行统计汇总，由于调用 write 操作的既可以是 I/O 线程，也可以是业务的线程，还可能由业务线程池多个工作线程同时执行发送任务，因此，统计操作是多线程并发的，这也就是为什么要将计数器定义成 volatile 并使用原子更新类进行原子操作
long oldValue = totalPendingSize;
long newWriteBufferSize = oldValue + size;
// 利用 CAS 自旋操作
while(!TOTAL_PENDING_SIZE_UPDATER.compareAndSet(this, oldValue, newWriteBufferSize)) {
    // 操作失败 进入循环 更新oldValue
    oldValue = totalPendingSize;
    // 计算更新值
    newWriteBufferSize = oldValue + size;
}
// 使用 Java 自带的 Atomic 原子类，可以避免同步锁带来的并发访问性能降低问题，减少犯错的机会。因此，Netty 中对于 int、long、boolean 等成员变量大量使用其原子类，减少了锁的应用，从而降低了频繁使用同步锁带来的性能下降。
```

#### 21.2.5 线程安全类的应用

​	在 JDK1.5 的发行版本中，Java 平台新增了 java.util.concurrent，这个包中提供了一系列的线程安全集合、容器和线程池，利用这些新的线程安全类可以极大地降低 Java 多线程的难度，提升开发效率。

​	新的并发编程包中的工具可以分为如下 4 类：

- 线程池 Executor Framework 以及定时任务相关的类库，包括 Timer 等
- 并发集合，包括 List、Queue、Map 和 Set 等
- 新的同步器，例如读写锁 ReadWriteLock 等
- 新的原子包装类，例如 AtomicInteger 等

​	在实际的编码过程中，我们建议通过使用线程池、Task(Runnable/Callable)、原子类和线程安全容器来代替传统的同步锁、wait 和 notify，以提升并发访问的性能、降低多线程编程的难度。

​	下面，针对新的线程并发包在 Netty 中的应用进行分析和说明：

1. NioEventLoop

​	NioEventLoop 是 I/O 线程，负责网络读写操作，同时也执行一些非 I/O 的任务。例如事件通知、定时任务执行等，因此，它需要一个任务队列来缓存这些 Task。

```java
protected Queue<Runnable> newTaskQueue() {
    // This event loop never calls takeTask()
    return new ConcurrentLinkedQueue<Runnable>();
}
```

​	他是一个 ConcurrentLinkedQueue，对它进行读写操作不需要加锁。JDK 的线程安全容器底层采用了 CAS、volatile 和 ReadWriteLock 实现，相比于传统重量级的同步锁，采用了更轻量、细粒度的锁，因此，性能会更高。合理地应用这些线程安全容器，不仅能提升多线程并发访问的性能，还能降低开发难度。

2. SingleThreadEventExecutor

```java
// 定义标准线程池用于执行任务
private final Executor executor;

// 对线程池进行初始化
this.addTaskWakesUp = addTaskWakesUp;
this.executor = executor;
taskQueue = new TaskQueue();

@Override
public void execute(Runnable task) {
    if (task == null) {
        throw new NullPointerException("task");
    }
	
    boolean inEventLoop = inEventLoop();
    // 判断线程是否已经启动循环执行
    // inEventLoop() 方法就是判断当前线程是否就是这个类中的全局变量
    // private volatile Thread thread;
    // FIXME 这里面的逻辑还有待去研究
    if (inEventLoop) {
        addTask(task);
    } else {
        // 这个方法中会把 全局变量中的 thread 赋值为 Thread.currentThread
        startThread();
        addTask(task);
        if (isShutdown() && removeTask(task)) {
            reject();
        }
    }

    if (!addTaskWakesUp) {
        wakeup(inEventLoop);
    }
}
```

```java
// SingleThreadEventExecutor 启动新的线程
private void startThread() {
    synchronized (stateLock) {
        if (state == ST_NOT_STARTED) {
            state = ST_STARTED;
            delayedTaskQueue.add(new ScheduledFutureTask<Void>(
                this, delayedTaskQueue, Executors.<Void>callable(new PurgeTask(), null),
                ScheduledFutureTask.deadlineNanos(SCHEDULE_PURGE_INTERVAL), -SCHEDULE_PURGE_INTERVAL));
            doStartThread();
        }
    }
}
```

```java
// NioEventLoop 按照 I/O 任务比例执行任务 Task
if (selectedKeys != null) {
    processSelectedKeysOptimized(selectedKeys.flip());
} else {
    processSelectedKeysPlain(selector.selectedKeys());
}
final long ioTime = System.nanoTime() - ioStartTime;

final int ioRatio = this.ioRatio;
runAllTasks(ioTime * (100 - ioRatio) / ioRatio);
```

```java
// 循环从任务队列中获取任务 Task 并执行
protected boolean runAllTasks() {
    fetchFromDelayedQueue();
    Runnable task = pollTask();
    if (task == null) {
        return false;
    }

    for (;;) {
        try {
            task.run();
        } catch (Throwable t) {
            logger.warn("A task raised an exception.", t);
        }

        task = pollTask();
        if (task == null) {
            lastExecutionTime = ScheduledFutureTask.nanoTime();
            return true;
        }
    }
}
```

#### 21.2.6 读写锁的应用

​	JDK1.5 新的并发编程工具包中新增了读写锁，它是个轻量级、细粒度的锁，合理地使用读写锁，相比于传统的同步锁，可以提升并发访问的性能和吞吐量，在读多写少的场景下，使用同步锁比同步块性能高一大截。

​	尽管在JDK1.6 之后，随着 JVM 团队对 JIT 即时编译器的不断优化，同步块和读写锁的性能差距缩小了很多，但是，读写锁的应用依然非常广泛。

​	在 Netty 的 HashedWheelTimer 中，读写锁定义如下：

```java
final int mask;
final ReadWriteLock lock = new ReentrantReadWriteLock();
```

```java
// 当新增一个任务时，使用读锁用于感知 wheel 的变化。由于读锁是共享锁，所以当多个线程同时调用 newTimeout 时，并不会互斥，提升并发读的性能
@Override
public Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
    start();

    if (task == null) {
        throw new NullPointerException("task");
    }
    if (unit == null) {
        throw new NullPointerException("unit");
    }

    long deadline = System.nanoTime() + unit.toNanos(delay) - startTime;

    // Add the timeout to the wheel.
    HashedWheelTimeout timeout;
    lock.readLock().lock();
    try {
        timeout = new HashedWheelTimeout(task, deadline);
        if (workerState.get() == WORKER_STATE_SHUTDOWN) {
            throw new IllegalStateException("Cannot enqueue after shutdown");
        }
        // wheel 的读可以并发
        wheel[timeout.stopIndex].add(timeout);
    } finally {
        lock.readLock().unlock();
    }

    return timeout;
}
```

```java
// 获取并删除所有过期的任务
private void fetchExpiredTimeouts(List<HashedWheelTimeout> expiredTimeouts, long deadline) {

    // Find the expired timeouts and decrease the round counter
    // if necessary.  Note that we don't send the notification
    // immediately to make sure the listeners are called without
    // an exclusive lock.
    lock.writeLock().lock();
    try {
        // 由于要从迭代器中删除任务，所以使用了写锁
        fetchExpiredTimeouts(expiredTimeouts, wheel[(int) (tick & mask)].iterator(), deadline);
    } finally {
        // Note that the tick is updated only while the writer lock is held,
        // so that newTimeout() and consequently new HashedWheelTimeout() never see an old value
        // while the reader lock is held.
        tick ++;
        lock.writeLock().unlock();
    }
}
```

​	读写锁的使用场景总结：

- 主要用于读多写少的场景，用来替代传统的同步锁，以提升并发访问性能
- 读写锁是可重入、可降级的，一个线程获取读写锁后，可以继续递归获取；从写锁可以降级为读锁，以便快速释放锁资源；
- ReentrantReadWriteLock 支持获取锁的公平策略，在某些特殊的应用场景下，可以提升并发访问的性能，**同时兼顾线程等待公平性**；
- 读写锁支持非阻塞的尝试获取锁，如果获取失败，直接返回 false，而不是同步阻塞。这个功能在一些场景下非常有用。例如多个线程同步读写某个资源，当发生异常或者需要释放资源的时候，有哪个线程释放是个难题。因为某些资源不能重复释放，或者重复执行，这样，可以通过 tryLock 方法尝试获取锁，如果拿不到，说明已经被其他线程占用，直接退出即可。
- 获取锁之后一定要释放，否则会发生锁溢出异常。通常的做法是通过 finally 块释放锁。如果是 tryLock，获取锁成功才需要放锁。

#### 21.2.7 线程安全性文档说明

​	当一个类的方法或者成员变量被并发使用的时候，这个类的行为如何，是该类与其客户端程序建立约定的重要组成部分。否则，使用这个类的程序员做出该类是否是线程安全的假设，如果假设错误，可能会造成未同步或者同步过度的结果。

​	在 Netty 中，对于一些关键的类库，给出了线程安全性的 API DOC，尽管不完善，但是相比于一些更糟糕的产品，还是迈出了重要的一步。

![1547024807970](C:\Users\wr1ttenyu\AppData\Roaming\Typora\typora-user-images\1547024807970.png)

​	由于 ChannelPipelin 的应用非常广泛，因此，在 API 中对它的线程安全性进行了详细的说明，这样，开发者就可以放心的调用相关 API。

####21.2.8 不要依赖线程优先级

​	当有多个线程同时运行的时候，由线程调度器来决定线程的运行、等待以及线程的切换时间点，由于各个操作系统的线程调度器实现大相径庭，因此，依赖 JDK 自带的线程优先级来设置优先级策略的方法是错误和非平台可移植的。

​	Netty 中默认的线程工厂实现类，开放了包含设置线程优先级字段的构造函数。这是个错误的决定，对于使用者来说，既然 JDK 类库提供了优先级字段，就会本能地认为它被正确的执行，但实际上 **JDK 的线程优先级是无法跨平台正确运行的**。



## 第22章 高性能之道

### 22.1 RPC 调用性能模型分析

#### 22..1.1 传统的 RPC 调用性能差的三宗罪

- 网络传输方式问题：

  传统的 RPC 框架或者基于 RMI 等方式的远程服务调用采用了 **同步阻塞 I/O**

- 序列化性能差：

  Java 序列化机制是 Java 内部的一种对象编解码技术，无法跨语言、码流太大、性能差。

- 线程模型问题

  由于采用同步阻塞 I/O，这会**导致每个 TCP 连接都占用 1 个线程**

####22.1.2 I/O 通信性能三原则

​	尽管影响 I/O 通信性能的因素非常多，但是从架构层面看，主要有三个要素：

- 传输：用什么样的通道将数据发送给对方，可以选择 BIO、NIO 或者 AIO；
- 协议：采用什么样的通信协议，HTTP 等共有协议或者内部私有协议；
- 线程：数据报如何读取？读取之后的编解码在哪个线程进行，编解码后的消息如何派发，Reactor 线程模型的不同，对性能的影响也非常大。

### 22.2 Netty 高性能之道

####22.2.1 异步非阻塞通信



















































































