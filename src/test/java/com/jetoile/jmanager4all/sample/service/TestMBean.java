package com.jetoile.jmanager4all.sample.service;

/**
 * User: khanh Date: 02/01/11 Time: 19:50
 */
public interface TestMBean {
	String sayHello();

	boolean isHappy();

	void setHappy(boolean isHappy);

	void setMsg(String msg);
}
