/**
 * Created by xiyuan_fengyu on 2017/3/15.
 */
(function () {

    var clusterInfoApp;

    var webSocket;

    var onMessageTaskDebugUrls = {};

    $(document).ready(function () {
        clusterInfoApp = ExVue({
            el: "#clusterInfo",
            data: {
                debugModal: {
                    url: "",
                    js: "",
                    host: location.host,
                    urlClass: "form-group"
                },
                editModal: {
                    title: "",
                    taskId: "",
                    newValue: "",
                    host: "",
                    placeholder: "",
                    type: "",
                    msg: "",
                    msgType: ""
                }
            },
            methods: {
                startDebug: function (event) {
                    if (this.debugModal.url == "") {
                        this.debugModal.urlClass = "form-group has-error";
                        setTimeout(() => this.debugModal.urlClass = "form-group", 1000);
                    }
                    else {
                        window.open("http://localhost/jspider/debug?host=" + this.debugModal.host + "&js=" + this.debugModal.js + "&url=" + this.debugModal.url);
                    }
                },
                showDebugModal: function (event) {
                    var target = $(event.target);
                    this.debugModal.url = target.attr("data-url");
                    this.debugModal.js = target.text();

                    let js = this.debugModal.js;
                    let queue;
                    if (this.debugModal.url == "" && (queue = target.attr("data-queue"))) {
                        if (onMessageTaskDebugUrls[js]) {
                            this.debugModal.url = onMessageTaskDebugUrls[js];
                            $("#debugModal").modal("show");
                        }
                        else {
                            sendMessage({
                                key: "getUrlFromQueue",
                                value: {
                                    "queue": queue
                                }
                            }, (res) => {
                                if (res.value.url != "") {
                                    onMessageTaskDebugUrls[js] = res.value.url;
                                    this.debugModal.js = js;
                                    this.debugModal.url = res.value.url;
                                }
                                $("#debugModal").modal("show");
                            });
                        }
                    }
                    else {
                        $("#debugModal").modal("show");
                    }
                },
                rerunOnStartTask: function (event) {
                    var target = $(event.target);
                    sendMessage({
                        key: "saveTaskChange",
                        value: {
                            "taskId": target.attr("data-id"),
                            "taskType": target.attr("data-type")
                        }
                    });
                },
                showEditModal: function (event) {
                    var target = $(event.target);
                    this.editModal.newValue = target.text();
                    this.editModal.placeholder = "New Value";
                    this.editModal.taskId = target.attr("data-id");
                    this.editModal.host = "";
                    this.editModal.type = target.attr("data-type");
                    this.editModal.title = target.attr("data-title");
                    this.editModal.msg = "-";
                    this.editModal.msgType = "normal";
                    showEditModal();
                },
                saveChange: function () {
                    saveEditModalChange(this.editModal.taskId, this.editModal.type, this.editModal.newValue, this.editModal.host, function (res) {
                        if (res.value) {
                            clusterInfoApp.editModal.msgType = res.value.success ? "success" : "fail";
                            clusterInfoApp.editModal.msg = res.value.message;
                            if (res.value.success) {
                                setTimeout(function () {
                                    $("#editModal").modal("hide");
                                }, 1000);
                            }
                        }
                    });
                },
                restartPhantom: function (event) {
                    var target = $(event.target);
                    var worker = target.attr("data-worker");
                    var port = target.attr("data-port");
                    restartPhantomMsg(worker, port);
                },
                stopPhantom: function (event) {
                    var target = $(event.target);
                    var worker = target.attr("data-worker");
                    var port = target.attr("data-port");
                    stopPhantomMsg(worker, port);
                },
                newPhantom: function (event) {
                    var target = $(event.target);
                    var worker = target.attr("data-worker");
                    this.editModal.newValue = "";
                    this.editModal.placeholder = "New Phantom Server Port";
                    this.editModal.taskId = "0";
                    this.editModal.host = target.attr("data-worker");
                    this.editModal.type = "newPhantom";
                    this.editModal.title = "Start A New Phantom Server";
                    this.editModal.msg = "-";
                    this.editModal.msgType = "normal";
                    showEditModal();
                },
                showShutdownModal: function (event) {
                    showShutdownModal();
                },
                shutdown: function (event) {
                    sendMessage({
                        key: "shutdownMaster"
                    });
                }
            }
        });
        startWebSocket("ws://" + location.host);
    });

    function showEditModal() {
        $("#editModal").modal("show");
    }

    function showShutdownModal() {
        $("#shutdownModal").modal("show");
    }

    function ExVue(appConfig) {
        if (typeof appConfig == "object" && appConfig.el) {
            if (appConfig.data == null || typeof appConfig.data != "object") {
                appConfig.data = {};
            }
            appConfig.data.v_ = 0;
            document.querySelector(appConfig.el).setAttribute("v-if", "v_");
            appConfig.data.has = function (key) {
                return this[key] != null;
            };
            var app = new Vue(appConfig);
            app.setData = function (data) {
                if (typeof data == "object") {
                    for (var key in data) {
                        this[key] = data[key];
                    }
                    this.v_ += 1;
                }
            };
            return app;
        }
        else throw "Vue config param is not right!";
    }

    var isWebUIShutdown = false;

    function startWebSocket(address) {
        webSocket = new WebSocket(address);

        webSocket.onmessage = function (event) {
            if (event.data) {
                var data = JSON.parse(event.data);
                if (data.id) {
                    var callback = webSocket.callback[data.id];
                    if (callback && typeof callback == "function") {
                        callback(data);
                    }
                }
                else {
                    if (data.key == "clusterInfo") {
                        clusterInfoApp.setData(data.value);
                    }
                    else if (data.key == "shutdownProgress") {
                        if (clusterInfoApp.shutdownProgress == null) {
                            clusterInfoApp.setData({
                                shutdownProgress: []
                            });
                        }
                        var len = clusterInfoApp.shutdownProgress.length;
                        if (len == 0 || clusterInfoApp.shutdownProgress[len - 1].progress != data.progress) {
                            clusterInfoApp.shutdownProgress.push(data);
                        }
                        else {
                            clusterInfoApp.shutdownProgress[len - 1] = data;
                        }

                        if (data.progress >= 100) {
                            isWebUIShutdown = true;
                            setTimeout(function () {
                                sendMessage({
                                    key: "shutdownWebUI"
                                });
                            }, 2000);
                        }
                    }
                }
            }
        };
        webSocket.onopen = function (event) {
            webSocket.connected = true;
        };
        webSocket.onclose = function (event) {
            webSocket.connected = false;
            // delayToRestart(address);
        };
        webSocket.onerror = function (event) {
            webSocket.close(1000);
        };
    }

    function delayToRestart(address) {
        if (!isWebUIShutdown) {
            setTimeout(function () {
                startWebSocket(address);
            }, 5000);
        }
    }

    function saveEditModalChange(taskId, type, newValue, host, callback) {
        sendMessage({
            key: "saveTaskChange",
            value: {
                "taskId": taskId,
                "taskType": type,
                "newValue": newValue,
                "host": host
            }
        }, callback);
    }

    function restartPhantomMsg(worker, port, callback) {
        sendMessage({
            key: "restartPhantom",
            value: {
                "worker": worker,
                "port": port
            }
        }, callback);
    }

    function stopPhantomMsg(worker, port, callback) {
        sendMessage({
            key: "stopPhantom",
            value: {
                "worker": worker,
                "port": port
            }
        }, callback);
    }

    function sendMessage(msg, callback) {
        if (msg != null && msg.key && webSocket != null && webSocket.connected) {
            msg.id = new Date().getTime() + "_" + parseInt(Math.random() * 9000 + 1000);
            webSocket.send(JSON.stringify(msg));
            if (typeof callback == "function") {
                webSocket.callback = webSocket.callback || {};
                webSocket.callback[msg.id] = callback;
            }
        }
    }

})();