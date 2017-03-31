package com.xiyuan.common.log;

import com.xiyuan.common.util.ClassUtil;
import com.xiyuan.config.AppInfo;
import com.xiyuan.config.ClusterCfg;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * Created by xiyuan_fengyu on 2017/3/8.
 */
public class LogManager {

    static {
        String configPath = AppInfo.getConfigPath();
        Properties properties = new Properties();
        File log4jCfgFile = new File(configPath + "/log4j.properties");
        if (log4jCfgFile.exists()) {
            try {
                properties.load(new FileInputStream(log4jCfgFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                properties.load(LogManager.class.getClassLoader().getResourceAsStream("config/log4j.properties"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (Map.Entry<Object, Object> keyVal : properties.entrySet()) {
            String key = (String) keyVal.getKey();
            if (key.toLowerCase().endsWith(".file")) {
                String value = (String) keyVal.getValue();
                if (!value.matches("^(([a-zA-z]:/)|/).*")) {
                    properties.put(key, AppInfo.getLogPath() + "/" + (AppInfo.isMaster() ? "master" : "worker") + "." + value);
                }
            }
        }

        PropertyConfigurator.configure(properties);
    }

    public static Logger logger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

}
