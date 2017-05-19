/**
 * Created by xiyuan_fengyu on 2017/2/16.
 */
var webpage = require('webpage');
var system = require('system');
var fs = require('fs');

var tag_req_crawl = "/crawl?";
var tag_return = "{\"return\":";
var tag_screenshot = "{\"screenshot\":";
var tag_download = "{\"download\":";
var tag_error = "{\"error\":";

var flag_server_start = "server.start";

var jspiderHome = system.args[2];
var srcPath = system.args[3];
fs.changeWorkingDirectory(jspiderHome);
phantom.libraryPath = srcPath;
//添加配置
try {
    var configStr = fs.read("config/phantom.json");
    var config = JSON.parse(configStr);
    config.page = config.page || {};
    if (!config.page.loadImages) {
        config.page.abortImgRequest = true;
        config.page.loadImages = true;
    }
    phantom.pageSettings = config.page;
    var cookies = config.cookies;
    for (var i = 0, len = cookies.length; i < len; i++) {
        addCookie(cookies[i]);
    }
}
catch (e) {}


var server = require('webserver').create();
var port = parseInt(system.args[1]);
server.listen(port, function(req, res){
    var reqUrl = req.url;
    var isBadRequest = true;
    var matcher;
    if (reqUrl.startsWith(tag_req_crawl)) {
        matcher = reqUrl.match(/^\/crawl\?js=(.*?)&timeout=(.*?)&url=(.*?)$/);
        if (matcher) {
            isBadRequest = false;
            crawl(decodeURI(matcher[1]), decodeURI(matcher[2]), decodeURI(matcher[3]), res);
        }
    }

    if (isBadRequest) {
        response(res, {
            error: "BadRequest",
            url: reqUrl
        });
    }
});

sendFlag(flag_server_start);



function prepareJs(curPage, params) {
    return curPage.evaluate(function (userDatas) {
        //植入常用代码

        //用户数据
        window.userDatas = userDatas;


        //获取所有满足 条件 的链接，条件可以是一个函数或则正则表达式
        window.links = function (condition) {
            var arr = [];
            var map = {};
            jQuery("a").each(function () {
                var href = this.href;
                if (map[href] == null) {
                    if (condition == null
                        || (typeof condition == "string" && href.match(condition))
                        || (typeof condition == "function" && condition(this))) {
                        map[href] = true;
                    }
                }
            });
            for (var key in map) {
                arr.push(key);
            }
            return arr;
        };

        //截图
        window.screenshot = function (picName, selector, quality) {
            console.log(JSON.stringify({
                screenshot: picName || "screenshot.jpeg",
                quality: quality || 100,
                selector: selector
            }));
        };

        //下载
        window.download = function (url, savePath) {
            url += (url.match("\\?") ? "&" : "?") + "fileDownload=true";
            console.log(JSON.stringify({
                download: url,
                savePath: savePath
            }));
        };

        //返回结果
        window.sendResult = function(obj) {
            console.log(JSON.stringify({
                return: obj
            }));
        };

        //返回错误
        window.sendError = function(error, msg) {
            console.log(JSON.stringify({
                error: error,
                msg: msg,
                url: location.href
            }));
        };

        //等待某个条件达成后执行回调函数
        window.waitFor = function(condition, callback, delay, timeout, startTime) {
            //默认延迟10毫秒
            delay = delay || 10;
            //默认超时时间30秒, -1为永不超时
            timeout = timeout || 30000;

            startTime = startTime || new Date().getTime();

            setTimeout(function () {
                if (condition == true || (typeof condition == "function" && condition() == true)) {
                    if (typeof callback == "function") {
                        callback(true);
                    }
                }
                else if (timeout == -1 || new Date().getTime() - startTime < timeout) {
                    waitFor(condition, callback, delay, timeout, startTime);
                }
                else {
                    callback(false);
                }
            }, delay);
        };


        //加载js
        window.loadScript = function() {
            //检查参数是否正确
            var len = arguments.length;
            var callback = arguments[0];
            if (len != 2 || typeof callback != "function") {
                throw "at least 2 arguments for loadScript required, and the first one must be a callback function"
            }

            var progress = {};
            var onScriptLoad = function (src) {
                progress[src] = true;
                for (var key in progress) {
                    if (progress[key] == false) {
                        return;
                    }
                }
                callback();
            };

            var scripts = [];
            var jsSrcAndCheckers = arguments[1];
            for (var src in jsSrcAndCheckers) {
                var checker = jsSrcAndCheckers[src];
                if (checker == true || (typeof checker == "function" && checker())) {
                    var script = document.createElement("script");
                    script.type = "text/javascript";
                    if (script.readyState) {
                        script.onreadystatechange = function () {
                            if (this.readyState == "loaded" || this.readyState == "complete") {
                                this.onreadystatechange = null;
                                onScriptLoad(this.src);
                            }
                        };
                    } else
                    {
                        script.onload = function () {
                            onScriptLoad(this.src);
                        };
                    }
                    script.src = src;
                    progress[src] = false;
                    scripts.push(script);
                }
            }

            if (scripts.length > 0) {
                //必须要等待网页就绪后，才能插入元素
                if (document.body) {
                    for (var i = 0, sLen = scripts.length; i < sLen; i++) {
                        document.body.appendChild(scripts[i]);
                    }
                }
                else {
                    window.onload = function () {
                        for (var i = 0, sLen = scripts.length; i < sLen; i++) {
                            document.body.appendChild(scripts[i]);
                        }
                    };
                }
            }
            else {
                callback();
            }
        };

        //加载jQuery
        window.loadJQuery = function (callback, src) {
            src = src || "http://cdn.bootcss.com/jquery/3.1.1/jquery.min.js";
            var srcAndChecker = {};
            srcAndChecker[src] = function() {
                return typeof jQuery == "undefined";
            };
            loadScript(callback, srcAndChecker);
        };

    }, params.userDatas);
}

function onPageLoadFinished(page, params) {
    if (params.status != "success") {
        crawlResponse({
            error: "OpenFailed",
            msg: "fail to load this page",
            url: page.url,
            original: params.original != page.url ? params.original : null
        }, params);
    }
    else if (params.js) {
        prepareJs(page, params);
        page.injectJs(phantom.libraryPath + "/" + params.js);

        page.onUrlChanged = function(targetUrl) {
            prepareJs(page, params);
            page.injectJs(phantom.libraryPath + "/" + params.js);
        };
        page.onPageCreated = function(newPage) {
            if (phantom.pageSettings) {
                newPage.settings = phantom.pageSettings;
            }
            newPage.onLoadFinished = function(status) {
                params.pages.push(newPage);
                params.status = status;
                onPageLoadFinished(newPage, params);
            };
            setPageListener(newPage, params);
        };
    }
}

function setPageListener(page, params) {
    if (phantom.pageSettings && phantom.pageSettings.abortImgRequest) {
        //阻止图片请求（根据url和img元素的src来匹配，所以并不能保证所有的图片请求被拦截）
        page.onResourceRequested = function(requestData, networkRequest) {
            var lowerCaseUrl = requestData.url.toLowerCase();
            var match = lowerCaseUrl.match("\\.(jpg|jpeg|png|bmp|gif|ico)");
            if (lowerCaseUrl.indexOf("filedownload=true") > -1) {
                return;
            }
            else if (match != null) {
                networkRequest.abort();
            }
            else if (lowerCaseUrl.match("\\.(js|css|json)")) {
                //
            }
            else {
                var accept = "";
                for (var i = 0, len = requestData.headers.length; i < len; i++) {
                    var item = requestData.headers[i];
                    if (item.name == "Accept") {
                        accept = item.value;
                        break;
                    }
                }

                if (accept.indexOf("image/") > -1) {
                    networkRequest.abort();
                }
            }
        };
    }

    page.onConsoleMessage = function(msg) {
        println(msg.substring(0, Math.min(500, msg.length)));
        if (msg.substring(0, tag_return.length) == tag_return || msg.substring(0, tag_error.length) == tag_error) {
            crawlResponse(msg, params);
        }
        else if (msg.substring(0, tag_screenshot.length) == tag_screenshot) {
            var json = JSON.parse(msg);
            if (json.selector) {
                page.clipRect = page.evaluate(function (domSelector) {
                    var rect = document.querySelector(domSelector).getBoundingClientRect();
                    return {
                        top: rect.top,
                        left: rect.left,
                        width: rect.width,
                        height: rect.height
                    };
                }, json.selector);
                page.render("screenshot/" + json.screenshot, {quality: json.quality});
            }
            else {
                page.clipRect = {
                    top: 0,
                    left: 0,
                    width: 0,
                    height: 0
                };
                page.render("screenshot/" + json.screenshot, {quality: json.quality});
            }
        }
        else if (msg.substring(0, tag_download.length) == tag_download) {
            var json = JSON.parse(msg);
            var response = page.evaluate(function(url) {
                var xhr = new XMLHttpRequest();
                xhr.overrideMimeType('text/plain; charset=x-user-defined');
                xhr.open("GET", url, false);
                xhr.send();

                var byteStr = "";
                for (var i = 0, len = xhr.response.length; i < len; ++i) {
                    var c = xhr.response.charCodeAt(i);
                    byteStr += String.fromCharCode(c & 0xff);
                }
                return byteStr;
            }, json.download);
            fs.write("download/" + json.savePath, response, "b");
        }

    };
    page.onError = function(msg, trace) {
        if (trace && trace.length > 0 ) {
            var msgStack = [msg];
            trace.forEach(function(t) {
                msgStack.push(t.file + ': ' + t.line + (t.function ? ' (in function "' + t.function +'")' : ''));
            });
            console.error(msgStack.join('\n'));

            //只有用户本地js引发的异常，文件地址如：phantomjs://code/BilibiliComment.js
            if (trace[0].file.startsWith("phantomjs://code/")) {
                crawlResponse({
                    error: "JsException",
                    msg: msg,
                    url: page.url
                }, params);
            }
        }
    };
    page.onAlert = function(msg) {
        console.log("alert: " + msg);
    };
}

function crawl(js, timeout, url, res) {
    //解析用户的额外数据
    var userDatasStr = url.split("userDatas=")[1];
    userDatasStr = decodeURIComponent(userDatasStr);
    var userDatas;
    try {
        userDatas = JSON.parse(userDatasStr);
    }
    catch (e) {
        userDatas = {};
    }
    if (userDatas.proxyIp && userDatas.proxyPort) {
        phantom.setProxy(userDatas.proxyIp, userDatas.proxyPort);
    }

    var page = webpage.create();
    var params = {
        taskId: new Date().getTime() + "_" + parseInt(Math.random() * 10000),
        changeIndex: -1,
        original: url,
        js: js,
        res: res,
        globalData: {},
        userDatas: userDatas,
        pages: [page]
    };

    if (phantom.pageSettings) {
        page.settings = phantom.pageSettings;
    }
    page.open(url, function (status) {
        params.status = status;
        onPageLoadFinished(page, params);
    });
    setPageListener(page, params);

    //任务超时
    try {
        timeout = parseInt(timeout) * 1000
    }
    catch (e) {
        timeout = 30000
    }
    setTimeout(function () {
        if (params.isResClosed != true) {
            crawlResponse({
                error: "TaskTimeout",
                msg: "task timeout!",
                url: url
            }, params);
        }
    }, timeout);
}

function crawlResponse(result, params) {
    if (params.isResClosed != true) {
        response(params.res, result);

        for (var i = 0, len = params.pages.length; i < len; i++) {
            params.pages[i].close();
        }
        params.isResClosed = true;
    }
}

function addCookie(cookie) {
    var domain = cookie.domain;
    var path = cookie.path;
    var split = cookie.cookie.split("; ");
    for (var i = 0, len = split.length; i < len; i++) {
        var keyVal = split[i];
        var equalIndex = keyVal.indexOf("=");
        if (equalIndex > -1) {
            var key = keyVal.substring(0, equalIndex);
            var value = keyVal.substring(equalIndex + 1);
            phantom.addCookie({
                name: key,
                value: value,
                domain: domain,
                path: path,
            });
        }
    }
}

function response(res, result) {
    if (typeof result != "string") {
        result = JSON.stringify(result);
    }
    res.write(result);
    res.close();
}

function sendFlag(flag) {
    console.log("flag://" + flag);
}

String.prototype.startsWith = function (prefix) {
    var len = this.length;
    var prefixLen = prefix.length;
    return prefixLen <= len && prefix == this.substring(0, prefixLen);
};

function println(obj) {
    console.log(obj == null ? "" : JSON.stringify(obj));
}