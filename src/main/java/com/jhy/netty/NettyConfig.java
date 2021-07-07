package com.jhy.netty;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * 存储整个工程的全局配置
 * @author Hongyu Jiang
 * @since Jun. 29 2021
 */
public class NettyConfig {

	/**
	 * 通道连接池
	 * 存储每一个客户端接入进来时的channel对象
	 */
	public static ChannelGroup group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);


}
