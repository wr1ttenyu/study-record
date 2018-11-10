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

### 15.1 ByteBuffer 功能说明

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
        // 
        setArray(newArray);
    }
    return this;
}
```

