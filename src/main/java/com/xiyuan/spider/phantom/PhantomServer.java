package com.xiyuan.spider.phantom;

import com.xiyuan.common.log.LogManager;
import com.xiyuan.common.util.FileUtil;
import com.xiyuan.config.AppInfo;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by xiyuan_fengyu on 2017/2/16.
 */
public class PhantomServer {

    public final int port;

    private boolean isDestory = false;

    private PhantomServerListener listener;

    private Process process;

    private static Logger logger = LogManager.logger(PhantomServer.class);

    public PhantomServer(final int port, final PhantomServerListener listener) {
        this.port = port;
        this.listener = listener;
        restart();
    }

    public void restart() {
        destoryProgress();

        if (!isDestory) {
            Thread daemon = new Thread() {
                @Override
                public void run() {
                    Runtime rt = Runtime.getRuntime();
                    try {
                        process = rt.exec("phantomjs --config=" + AppInfo.getConfigPath() + "/phantom.json " + phantomJsServerJsPath + " " + port + " " + AppInfo.getJspiderHome() + " " +  AppInfo.getSrcPath());
                        InputStream in = process.getInputStream();
                        BufferedReader br = new BufferedReader(new InputStreamReader(in));
                        String temp;
                        while((temp = br.readLine()) != null){
                            if (temp.startsWith("flag://")) {
                                String flag = temp.substring("flag://".length());
                                switch (flag) {
                                    case "server.start": {
                                        listener.onStart(PhantomServer.this);
                                        break;
                                    }
                                }
                            }
                            else {
                                logger.info(temp);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    finally {
                        destoryProgress();
                        listener.onStop(PhantomServer.this);
                    }

                    if (!isDestory) {
                        try {
                            sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (process == null) {
                            restart();
                        }
                    }
                }
            };
            daemon.setDaemon(true);
            daemon.start();
        }
    }

    private void destoryProgress() {
        try {
            if (process != null) {
                process.destroy();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            process = null;
        }
    }

    public void destory() {
        isDestory = true;
        destoryProgress();
        logger = null;
    }

    public interface PhantomServerListener {

        void onStart(PhantomServer server);

        void onStop(PhantomServer server);

    }

    private static final String phantomJsServerJsPath = FileUtil.getAbsPathAndCopyIfInJar("phantom/PhantomServer.js");

//    public static void main(String[] args) {
//        new PhantomServer(20180, new PhantomServerListener() {
//            @Override
//            public void onStart(PhantomServer server) {
//                System.out.println("onStart");
//            }
//
//            @Override
//            public void onStop(PhantomServer server) {
//                System.out.println("onStop");
//            }
//        });
//        try {
//            Thread.sleep(10000000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }

}
