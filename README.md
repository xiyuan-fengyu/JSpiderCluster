### JSpiderCluster  

Java + phantomjs 实现的一个分布式爬虫。  

####快速起步
1. 安装phantomjs，并配置环境变量  
2. 从[这里](http://www.baidu.com)下载Hello World示例  
3. 用IDE打开示例项目，将libs目录下的JSpiderCluster.jar添加为依赖  
4. 运行com.xiyuan.helloworld.Luncher，可以在控制台最后看到 https://www.baidu.com 网页的title"百度一下，你就知道"
5. 在浏览器中打开 http://localhost:9000/ 可以看到当前爬虫的工作情况，整个界面的功能将在后续做详细介绍  

#### 框架介绍

#### 管理界面的功能介绍

#### 例子

#### 如何部署

#### FAQ  
1. Linux中用phantomjs对中文网页截图，可能会出现中文乱码或者不显示中文的问题  
http://www.oicqzone.com/pc/2014091419765.html  
http://www.cnmiss.cn/?p=436  
解决办法就是安装字体。  
在centos中执行：  
```yum install bitmap-fonts bitmap-fonts-cjk```  
在ubuntu中执行：  
```sudo apt-get install xfonts-wqy```  
这样再去截图中文的页面就不会出现一堆的方框了。  


2. 下载功能必须将config/phantom.json中page的webSecurityEnabled属性设置为false