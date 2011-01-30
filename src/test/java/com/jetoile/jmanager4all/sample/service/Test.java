package com.jetoile.jmanager4all.sample.service;

import com.jetoile.jmanager4all.sample.service.TestMBean;


/**
 * User: khanh Date: 02/01/11 Time: 19:49
 */
public class Test implements TestMBean {
	private String msg = "hello";
	private boolean happy = true;

	@Override
	public String sayHello() {
		return msg;
	}

	@Override
	public boolean isHappy() {
		return this.happy;
	}

	@Override
	public void setHappy(boolean isHappy) {
		this.happy = isHappy;
	}

	@Override
	public void setMsg(String msg) {
		this.msg = msg;
	}
}
