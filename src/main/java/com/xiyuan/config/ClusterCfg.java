package com.xiyuan.config;

import com.xiyuan.common.util.ClassUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClusterCfg {

	private static final Properties properties;

	public static final String cluster_master_host;

	public static final int cluster_master_netty_port;

	public static final int cluster_master_webui_port;

	public static final HashMap<String, Set<WorkerCfg>> cluster_workers;

	static {
		properties = new Properties();
		File file = new File(AppInfo.getConfigPath() + "/cluster.properties");
		if (file.exists()) {
			try (FileInputStream in = new FileInputStream(file)) {
				properties.load(in);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			try (InputStream in = ClusterCfg.class.getClassLoader().getResourceAsStream("config/cluster.properties")) {
				properties.load(in);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		cluster_master_host = properties.getProperty("cluster.master.host");
		cluster_master_netty_port = Integer.parseInt(properties.getProperty("cluster.master.netty.port"));
		cluster_master_webui_port = Integer.parseInt(properties.getProperty("cluster.master.webui.port"));

		cluster_workers = new HashMap<>();
		Pattern worderPattern = Pattern.compile("cluster\\.worker([0-9]+)\\.host");
		for (Map.Entry<Object, Object> keyVal : properties.entrySet()) {
			String key = (String) keyVal.getKey();
			Matcher matcher = worderPattern.matcher(key);
			if (matcher.find()) {
				try {
					int index = Integer.parseInt(matcher.group(1));
					String host = properties.getProperty(key);
					String phantomPortsStr = properties.getProperty("cluster.worker" + index + ".phantom.ports");
					String[] split = phantomPortsStr.split(",");
					int[] phantomPorts = new int[split.length];
					for (int i = 0, len = split.length; i < len; i++) {
						phantomPorts[i] = Integer.parseInt(split[i]);
					}

					Set<WorkerCfg> workers;
					if (!cluster_workers.containsKey(host)) {
						workers = new HashSet<>();
						cluster_workers.put(host, workers);
					}
					else workers = cluster_workers.get(host);
					workers.add(new WorkerCfg(index, host, phantomPorts));
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static class WorkerCfg {

		public final int index;

		public final String host;

		public final int[] phantom_ports;

		public WorkerCfg(int index, String host, int[] phantom_ports) {
			this.index = index;
			this.host = host;
			this.phantom_ports = phantom_ports;
		}

		@Override
		public String toString() {
			return "worker" + index + "@" + host + ":" + Arrays.toString(phantom_ports);
		}
	}

	public static void print() {
		System.out.println("master@" + cluster_master_host + ":" + cluster_master_netty_port);
		for (Map.Entry<String, Set<WorkerCfg>> keyVal : cluster_workers.entrySet()) {
			for (WorkerCfg workerCfg : keyVal.getValue()) {
				System.out.println(workerCfg);
			}
		}
	}

	public static void main(String[] args) {
		print();
	}

}