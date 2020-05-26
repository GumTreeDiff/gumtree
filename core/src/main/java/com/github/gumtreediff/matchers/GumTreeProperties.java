package com.github.gumtreediff.matchers;

import java.io.InputStream;
import java.util.Properties;

public class GumTreeProperties {

	public static Properties properties;

	static {
		reset();
	}

	public static void reset() {
		InputStream propFile;
		try {
			properties = new Properties();
			propFile = GumTreeProperties.class.getClassLoader().getResourceAsStream("config-gumtree.properties");

			properties.load(propFile);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void setProperty(String key, String value) {
		properties.setProperty(key, value);
	}

	public static String getProperty(String key) {
		return properties.getProperty(key);
	}

	public static Integer getPropertyInteger(String key) {
		return Integer.valueOf(properties.getProperty(key));
	}

	public static Boolean getPropertyBoolean(String key) {
		return Boolean.valueOf(properties.getProperty(key));
	}

	public static Double getPropertyDouble(String key) {
		return Double.valueOf(properties.getProperty(key));
	}

}
