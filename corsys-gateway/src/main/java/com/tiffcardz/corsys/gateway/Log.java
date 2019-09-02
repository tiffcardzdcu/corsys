package com.tiffcardz.corsys.gateway;

import java.util.Date;

public class Log {

	public static void log(Class<?> cls, String str) {
		System.out.println(new Date() + " " + cls.getSimpleName() + ": " + str);
	}
}
