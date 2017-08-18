/**
 * Created by xiyuan_fengyu on 2017/3/24.
 */

//用户数据
window.userDatas = userDatas;

//资源请求成功的记录，用于查看bodySize
window.resources = {};

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
window.screenshot = function (picName, selectorOrJqueryObj, quality) {
    var selector;
    if (typeof selectorOrJqueryObj == "string") {
        selector = selectorOrJqueryObj;
    }
    else {
        var oldId = selectorOrJqueryObj.attr("id");
        if (oldId != null && oldId.length > 0) {
            selector =  "#" + oldId;
        }
        else {
            var randomId = "screenshot_" + new Date().getTime() + "_" + parseInt(Math.random() * 10000);
            selectorOrJqueryObj.attr("id", randomId);
            selector = "#" + randomId;
        }
    }

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

    var scripts = [];
    var jsSrcAndCheckers = arguments[1];
    for (var src in jsSrcAndCheckers) {
        var checker = jsSrcAndCheckers[src];
        if (checker == true || (typeof checker == "function" && checker())) {
            scripts.push(src);
        }
    }

    if (scripts.length > 0) {
        //在 JspiderDebuger 的 content.js 中定义的用来加载js的方法
        chromeLoadScript(callback, scripts, sender);
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