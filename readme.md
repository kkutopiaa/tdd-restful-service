spike： 并不是把所有细节都理解完，目的是花最小的成本，搞懂各个接口之间的交互方式，得到一个清晰的组件架构愿景即可。

初步 spike 的内容：
1. 可处理HTTP请求的某个 Resource 资源。
2. 可通过 MessageBodyWriter 扩展点，将内容写入 HTTP 响应中。

清楚了 Servlet 如何使用 Application 和 Providers；
清楚了需要在什么地方使用 DI 注入容器。  

初步 spike 之后，得到了一个较为清晰的架构愿景：  
![](imgs/01.初步Spike之后.jpg)



但是，JAX-RS 里有 2 中依赖注入： 
1. Application Scope 的 Inject 注入
2. Request Scope 的 Context 注入
这部分的架构愿景现在仍是不清晰的，需要进一步 Spike。

然后得到了架构愿景：
![](imgs/02.Spike-DI 之后.jpg)

之后，需要 Spike 下 Resource Dispatcher 的部分。
然后进一步得到了架构愿景和调用栈顺序：
![](imgs/03.Spike-dispatch之后.png)
![](imgs/04.Spike-dispatch之后的调用栈顺序.jpg)
得到了以下的明确信息：
- ResourceServlet：以 Servlet 的形式作为入口，处理 Http 请求。
- Application：指明 RESTful 应用所需的所有组件，比如 Root Resource、Providers 等，也是对于框架提供的服务的访问入口。
- ResourceRouter：Http 请求派发算法的实现载体。
- Providers：三个扩展点，也就是 MessageBodyWriter，MessageBodyReader 以及 ExceptionMapper。