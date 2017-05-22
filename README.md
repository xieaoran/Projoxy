# HTTP 请求

我们先来看一下不同版本的 HTTP / HTTPS 请求及其处理方案。

## HTTP/1.0

> Specifications: [Hypertext Transfer Protocol -- HTTP/1.0](https://www.w3.org/Protocols/HTTP/1.0/spec.html)

> Special thanks to [hjc4869](https://hjc.im/)

下面是与目标服务器直接连接时，HTTP/1.0 中 GET 请求的例子：

    GET / HTTP/1.0

在使用代理的情况下，请求中的相对 URI 会被替换为完整 URI：

    GET http://www.xieaoran.com/ HTTP/1.0

## HTTP/1.1

> Specifications: [Hypertext Transfer Protocol -- HTTP/1.1](https://www.w3.org/Protocols/rfc2616/rfc2616.html)

> 所有例子均使用 Chrome 60.0.3088.3 dev 64bit 得到。

这是目前普遍使用的 HTTP 协议版本。

下面是与目标服务器直接连接时，HTTP/1.1 中 GET 请求的例子：

    GET / HTTP/1.1
    Host: www.xieaoran.com
    Connection: keep-alive

在使用代理的情况下，请求中的相对 URI 会被替换为完整 URI，并且指示是否保持持久连接的 Connection 头被替换为 Proxy-Connection：

    GET http://www.xieaoran.com/ HTTP/1.1
    Host: www.xieaoran.com
    Proxy-Connection: keep-alive

> **关于 Proxy-Connection**
> 
> 互联网上，存在着大量简陋并过时的代理服务器在继续工作，它们很可能无法理解 Connection - 无论是请求报文还是响应报文中的 Connection。而代理服务器在遇到不认识的 Header 时，往往都会选择继续转发。大部分情况下这样做是对的，很多使用 HTTP 协议的应用软件扩展了 HTTP 头部，如果代理不传输扩展字段，这些软件将无法工作。
>
> 如果浏览器对这样的代理发送了 Connection: keep-alive，那么结果会变得很复杂。这个 Header 会被不理解它的代理原封不动的转给服务端，如果服务器也不能理解就还好，能理解就彻底杯具了。服务器并不知道 keep-alive 是由代理错误地转发而来，它会认为代理希望建立持久连接，服务端同意之后也返回一个 keep-alive。同样，响应中的 keep-alive 也会被代理原样返给浏览器，同时代理还会傻等服务器关闭连接——实际上，服务端已经按照 keep-alive 指示保持了连接，即使数据回传完成，也不会关闭连接。另一方面，浏览器收到 keep-alive 之后，会复用之前的连接发送剩下的请求，但代理不认为这个连接上还会有其他请求，请求被忽略。这样，浏览器会一直处于挂起状态，直到连接超时。
>
> 这个问题最根本的原因是代理服务器转发了禁止转发的 Header。但是要升级所有老旧的代理也不是件简单的事，所以浏览器厂商和代理实现者协商了一个变通的方案：首先，显式给浏览器设置代理后，浏览器会把请求头中的 Connection 替换为 Proxy-Connetion。这样，对于老旧的代理，它不认识这个 Header，会继续发给服务器，服务器也不认识，代理和服务器之间不会建立持久连接（不能正确处理 Connection 的都是 HTTP/1.0 代理），服务器不返回 keep-alive，代理和浏览器之间也不会建立持久连接。而对于新代理，它可以理解 Proxy-Connetion，会用 Connection 取代无意义的 Proxy-Connection，并将其发送给服务器，以收到预期的效果。

*注：请求中还有其余的一些请求头，这里不作关注，按原格式发送给目标服务器即可。*

*有关 Cache-Control, ETag, Expires, If-\*, Last-Modified, Pragma 等缓存相关的请求头，将在稍后的缓存部分做详细解释和处理。*

## HTTP/2.0

> 由于协议中规定 HTTP/2.0 仅用于 HTTPS，故正确处理 CONNECT 请求即可，相关处理方案将在下文 HTTPS 部分中介绍。

## HTTPS

客户端需要指示代理服务器与目标 HTTPS 服务器建立连接时，会向代理服务器发送如下格式的请求：

    CONNECT www.xieaoran.com:443 HTTP/1.1

代理服务器收到 CONNECT 请求后，与对应的目标服务器建立连接，之后想客户端发送如下格式的响应：

    HTTP/1.1 200 Connection Established

之后，客户端与目标服务器进行正常的握手、密钥交换和数据传输。所有内容均以密文的形式传送，对代理服务器不可读，此处代理服务器所进行的只是客户端与目标服务器间的数据转发。

![HTTPS 连接](https://www.xieaoran.com/content/images/2017/05/HTTPS.png)

> 这里也简要介绍一下 HTTPS 连接的过程。
> 
> 1. 客户端发起 HTTPS 请求。
>
> 2. 服务器按照配置文件，读取证书文件（一般由证书颁发机构下发）中的公私钥对。
>
> 3. 服务器向客户端发送 RSA 证书的公钥，包括证书颁发机构、过期时间等附加信息。
>
> 4. 客户端解析服务器发送的证书公钥，验证其颁发机构是否可信、本地时间是否已超过过期时间等。若发现证书无效，则显示安全警告。
> 
> 5. 若证书公钥有效，客户端使用该证书公钥加密一个随机生成的对称密钥（一般为 AES），并将加密后的密文发送给服务器。
> 
> 6. 服务器收到密文后，使用证书私钥解密，得到对称密钥。 
> 
> 7. 服务器使用对称密钥加密响应内容，将加密后的密文发送给客户端。
> 
> 8. 客户端使用对称密钥解密响应内容。

## 缓存相关

> 待更新

# 第一次尝试 - Java NIO

![Java NIO](https://www.xieaoran.com/content/images/2017/05/nio.png)

## 简介

> 待更新