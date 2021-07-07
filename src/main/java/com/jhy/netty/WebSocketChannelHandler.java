package com.jhy.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * 初始化连接时候的各个组件
 * @author Hongyu Jiang
 * @since Jul. 6 2021
 */
public class WebSocketChannelHandler extends ChannelInitializer<SocketChannel> {
	protected void initChannel(SocketChannel socketChannel) throws Exception {
		ChannelPipeline pipeline = socketChannel.pipeline();
		// http的解码器,websocket协议本身就是基于http协议的,所以这边也要使用http解码器
		pipeline.addLast("http-codec", new HttpServerCodec());
		// 负责将http的一些信息例如版本和http的内容继承一个FullHttpRequest
		pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
		// 大文件写入的类
		pipeline.addLast("http-chunked", new ChunkedWriteHandler());
		// websocket处理类
		pipeline.addLast("handler", new WebSocketServerHandler());
	}
}
