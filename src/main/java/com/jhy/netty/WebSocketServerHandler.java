package com.jhy.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

import java.util.Date;

import static io.netty.handler.codec.http.HttpUtil.setContentLength;
import static java.lang.Thread.sleep;

/**
 * 接收/处理/响应客户端websocket请求的核心业务处理类
 * 	处理业务逻辑和通道管理
 * @author Hongyu Jiang
 * @since Jun. 29 2021
 */
@ChannelHandler.Sharable
public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {

	private WebSocketServerHandshaker handshaker;
	private static final String WEB_SOCKET_URL = "ws://192.168.55.111:9999/websocket";


	// 服务端处理客户端websocket的核心方法
	protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
		System.out.print("");
		// 处理客户端向服务端发起http握手请求的业务(传统的HTTP接入,握手流程走这里)
		if (o instanceof FullHttpRequest) {
			handHttpRequest(channelHandlerContext, (FullHttpRequest) o);
		} else if (o instanceof WebSocketFrame) { // 处理WebSocket连接业务(WebSocket接入)
			handWebsocketFrame(channelHandlerContext, (WebSocketFrame) o);
		}
	}


	/*  负责响应 WebSocket握手请求业务  */
	/**
	 * 处理客户端向服务端发起http握手请求的业务
	 * @param ctx
	 * @param req
	 */
	private void handHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
		// 判断不是http握手请求的情况,如果HTTP解码失败,返回HTTP异常
		if (!req.getDecoderResult().isSuccess()
			|| !("websocket".equals(req.headers().get("Upgrade")))) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
			return;
		}
		// 构造握手响应返回,目前是本机的地址
		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
			WEB_SOCKET_URL, null, false);
		handshaker = wsFactory.newHandshaker(req);
		if (handshaker == null) {
			WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
		} else {
			handshaker.handshake(ctx.channel(), req);
		}
	}

	/**
	 * 服务端向客户端响应消息
	 * @param ctx
	 * @param req
	 * @param res
	 */
	private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, DefaultFullHttpResponse res) {
		// 服务端向客户端发送数据
		if (res.getStatus().code() != 200) {
			ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
			res.content().writeBytes(buf);
			buf.release();
			setContentLength(res, res.content().readableBytes());
		}
		// 如果是非Keep-Alive,关闭连接
		ChannelFuture f = ctx.channel().writeAndFlush(res);
		if (res.getStatus().code() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}


	/*  WebSocket连接业务(负责处理WebSocket的消息)  */
	/**
	 * 处理客户端与服务端之间的WebSocket业务
	 * @param ctx
	 * @param frame
	 */
	private void handWebsocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
		// 判断是否是关闭(链路)WebSocket的指令
		if (frame instanceof CloseWebSocketFrame) {
			handshaker.close(ctx.channel(), ((CloseWebSocketFrame) frame).retain());
		}
		// 判断是否是ping消息(心跳消息)
		if (frame instanceof PingWebSocketFrame) {
			ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
			return;
		}

		// 本例程仅支持文本消息,不支持二进制消息.判断是否是二进制消息,如果是二进制消息,抛出异常
		if (!(frame instanceof TextWebSocketFrame)) {
			System.out.println("目前我们不支持二进制消息");;
			throw new RuntimeException("["+this.getClass().getName()+"]不支持消息");
		}
		// 返回应答消息
		// 获取客户端向服务端发送的消息
		String request = ((TextWebSocketFrame) frame).text();
		System.out.println("服务端收到客户端的消息======>>>" + request);
		TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame(new Date().toString()
														+ ctx.channel().id()
														+ "===>>>"
														+ request);
		// 群发,服务端向每个连接上来的客户端群发消息
		NettyConfig.group.writeAndFlush(textWebSocketFrame);

		for (int i=0; i<10; i++) {
			TextWebSocketFrame textWebSocketFrame1 = new TextWebSocketFrame("序号：["+i+"] "+new Date().toString()
				+ ctx.channel().id()
				+ "===>>>"
				+ request);
			// 群发,服务端向每个连接上来的客户端群发消息
			NettyConfig.group.writeAndFlush(textWebSocketFrame1);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}


	// 客户端与服务端创建连接时调用
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		//super.channelActive(ctx);

		NettyConfig.group.add(ctx.channel());
		System.out.println("客户端与服务端连接开启...");
	}

	// 客户端与服务端断开连接时调用
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		//super.channelInactive(ctx);

		NettyConfig.group.remove(ctx.channel());
		System.out.println("客户端与服务端连接关闭...");
	}

	// 服务端接收客户端发送过来的数据结束之后调用
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		//super.channelReadComplete(ctx);

		ctx.flush();
	}

	// 工程出现异常的时候调用
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		//super.exceptionCaught(ctx, cause);
		/*cause.printStackTrace();
		ctx.close();*/

		super.exceptionCaught(ctx, cause);
		Channel channel = ctx.channel();
		if (channel.isActive()) ctx.close();
	}


}
