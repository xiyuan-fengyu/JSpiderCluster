## JSpiderCluster  

#### Warning
<html>
  <span style="color: #ff490d;font-weight: bold;">
    由于各种原因，这个项目不再维护，推荐使用
    <a href="https://github.com/xiyuan-fengyu/ppspider" target="_blank">这个新框架</a>
  </span>
</html>  

Java + phantomjs 实现的一个分布式爬虫。  
java部分主要进行集群的管理，任务队列的管理和任务分发，支持jar和class热发布。  
phantomjs执行实际的抓取任务，抓取逻辑采用js编写，可以方便的导入jquery，
使用jquery可以很方便地从网页中提取需要的信息，在抓取逻辑的js中可以使用一些扩展功能，例如下载，截图，导入js。

#### 快速起步
1. 安装phantomjs  

windows  
在环境变量Path中添加 “phantomjs安装目录/bin” 这个路径  
  
linux  
在 /etc/profile 的最后添加  
export PHANTOM_HOME=phantomjs安装目录  
export PATH=$PATH:$PHANTOM_HOME/bin  
然后 source /etc/profile 使其生效  
  
mac通过添加软链接的方式  
sudo ln -s phantomjs安装目录/bin/phantomjs /YOUR_HOME/phantomjs  
    
2. 从[这里](https://github.com/xiyuan-fengyu/JSpider_HelloWorld)下载Hello World示例  
3. 用IDE打开示例项目，将libs目录下的JSpiderCluster.jar添加为依赖  
4. 运行com.xiyuan.helloworld.Luncher，可以在控制台最后看到 https://www.baidu.com 网页的title"百度一下，你就知道"
5. 在浏览器中打开 http://localhost:9000/ 可以看到当前爬虫的工作情况，整个界面的功能将在后续做详细介绍  

#### 管理界面的功能介绍
![管理界面说明](githubRes/webUI.png)
1. 集群关闭    
鼠标移动到Master的地址后面会出现shutdown按钮，
点击弹出确认面板，确认后会停止集群，
并显示关闭过程的信息，系统会自动将集群的任务信息保存到cache/queueMap.cache文件中，
下一次启动的时候，系统会自动加载这个文件并恢复之前的任务信息。
可以手动删除这个文件来避免加载之前的任务信息，通过脚本或者命令直接杀死进程则不会保存任务信息。

2. js调试功能  
从[这里](https://github.com/xiyuan-fengyu/JSpiderDebugger)下载调试插件，仅适用于Chrome浏览器。
在Chrome的扩展程序页面 chrome://extensions/ 点击 “加载已解压的扩展程序”安装插件。
在管理界面点击js弹出debug信息确认面板，如果是OnMessageTask类型的任务且队列里没有任务，则url需要自行填写；
点击debug后，会自动打开一个新的tab页并跳转到目标url，然后执行js；按F12打开 开发者面板，定位到要调试的js，
添加断点，然后刷新就可以调试了。
![找到debug目标js](githubRes/debug.png)  
注意：js在Chrome中的运行效果和phantomjs并不完全一致。

#### 使用说明
1. 如何修改配置  
一共三个配置文件，用户可以在项目的resources目录下创建config文件夹，然后在config下创建配置文件，
用户自定义的配置会覆盖内置的配置。    
集群配置，默认为如下配置  
[src/main/resources/config/cluster.properties](src/main/resources/config/cluster.properties)  
master节点只有一个，worker可以有多个，worker节点的配置方式为：
```
cluster.worker非负数.host=worker节点ip
cluster.worker非负数.phantom.ports=phantomjs的端口列表
```  
所有节点的配置要保持一致。在本地开发调试的时候，可以不编写这个配置文件，系统会自动使用默认配置；
正式部署的时候，如果要部署到多台机器，请把ip改为局域网ip。  

日志配置，默认配置为：  
[src/main/resources/config/log4j.properties](src/main/resources/config/log4j.properties)    

Phantomjs配置，默认配置为：  
[src/main/resources/config/phantom.json](src/main/resources/config/phantom.json)   
cookies中可以添加多个一级域名的cookie；page中loadImages设置是否加载图片；  
webSecurityEnabled设置是否检查跨域请求；  
根节点中还支持很多其他phantomjs启动属性，可以参考[这里](http://phantomjs.org/api/command-line.html)

2. 三种不同类型的任务  
OnStartTask  
集群启动后（master和worker都启动之后），会立即执行一次的任务。可以在管理界面重新执行。  
OnTimeTask  
在特定时间执行的任务。通过Quartz Cron表达式来配置时间，时间最高精确到秒。Quartz Cron表达式可以参考[这里](http://cron.qqe2.com/)  
OnMessageTask  
任务队列驱动的任务。可以通过@AddToQueue注解 和 QueueManager.addToQueue方法 往特定的队列中添加任务。

3. 用户编写抓取逻辑的js可以使用的内置方法  
参考 [src/main/resources/phantom/windowEx.js](src/main/resources/phantom/windowEx.js) 
（这个js仅供调试插件调用，实际实现在[src/main/resources/phantom/PhantomServer.js](src/main/resources/phantom/PhantomServer.js)中）  

#### 例子
1. [模拟百度搜索的过程](https://github.com/xiyuan-fengyu/JSpider_BaiduSearch)  
演示了如何在一个js文件中编写多个页面的抓取逻辑;但是强烈建议将这种需要在多个页面中完成的抓取任务分解为多个OnMessageTask任务来完成；  
  
2. [截图和下载文件](https://github.com/xiyuan-fengyu/JSpider_Screenshot)  
截图统一保存到master节点的工作目录下的screenshot文件夹中  
下载文件统一保存到master节点的工作目录下的download文件夹中  

3. [使用OnStartTask,OnTimeTask,OnMessageTask协同完成一个任务](https://github.com/xiyuan-fengyu/JSpider_TasksCoopertion)  
演示了三种任务的协同工作，通过不同的Filter往任务队列里添加消息，以及通过parallelConfig动态设置OnMessageTask的并行数。

4. 通过mybatis将结果存入数据库 TODO

5. [创建动态代理任务](https://github.com/xiyuan-fengyu/JSpider_ProxyTask)  
演示了如何创建动态代理任务，适用场景：网站对ip的访问频率有限制的情况；这个示例中并不存在ip访问频率的问题。

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

2. 下载功能必须将config/phantom.json中page的webSecurityEnabled属性设置为false；
  将page.loadImages设置为false不会影响图片的下载，但是会影响截图。

3. $("一个A标签").click()不跳转页面？  
可以通过dom元素的click()来模拟点击,下面两种写法都可以(第一种需要引入jQuery)  
``$("一个A标签")[0].click();``  
``document.querySelector("一个A标签").click();``  
