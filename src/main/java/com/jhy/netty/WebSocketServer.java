package com.jhy.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * 程序的入口，负责启动应用
 * @author Hongyu Jiang
 * @since Jul. 6 2021
 */
public class WebSocketServer implements Runnable {

	public void runServer(int port) {
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap bootstrap = new ServerBootstrap();
			bootstrap.group(bossGroup, workGroup);
			bootstrap.channel(NioServerSocketChannel.class);
			bootstrap.childHandler(new WebSocketChannelHandler());

			System.out.println("服务端开启等待客户端连接...");

			// 监听端口,服务器异步创建绑定
			Channel ch = bootstrap.bind(port).sync().channel();
			ch.closeFuture().sync();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// 优雅地退出程序
			bossGroup.shutdownGracefully();
			workGroup.shutdownGracefully();
		}
	}


	public static void main(String[] args) {
		new WebSocketServer().runServer(9999);
	}

	/**
	 * 通过开启一个线程的方式,单独启动server
	 */
	public void run() {
		try {
			new WebSocketServer().runServer(9999);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
