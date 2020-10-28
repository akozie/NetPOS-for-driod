package com.sunyard.sysfuncservice;

interface DeviceServiceEngine{
	
	//静默安装指定的App
	//appFilePath 应用文件路径  runActivityName 安装完毕之后需要运行的activity   appName 服务界面提示的应用名称（正在安装xxxx应用,请耐心等待）
	boolean installApp(String appFilePath);
}