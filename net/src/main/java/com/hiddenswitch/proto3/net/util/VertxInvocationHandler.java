package com.hiddenswitch.proto3.net.util;

import co.paralleluniverse.fibers.Suspendable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.ext.sync.Sync;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import static io.vertx.ext.sync.Sync.awaitFiber;

/**
 * Created by bberman on 2/1/17.
 */
public class VertxInvocationHandler<T> implements InvocationHandler, Serializable {
	RpcClient<T> ApiClient;
	protected String name;
	EventBus eb;

	@Override
	@Suspendable
	@SuppressWarnings("unchecked")
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		// Call default methods normally
		if (method.isDefault()) {
			// Invoked with the proxy instance, which shouldn't matter for anything that is a default method on an interface
			return method.invoke(proxy, args);
		}

		final boolean sync = ApiClient.sync;
		final Handler<AsyncResult<Object>> next = ApiClient.next;

		final String methodName = method.getName();

		ApiClient.next = null;
		ApiClient.sync = false;

		if (next == null
				&& !sync) {
			throw new RuntimeException();
		}

		if (eb == null) {
			throw new RuntimeException();
		}
		Object result = null;
		if (sync) {
			result = awaitFiber(done -> {
				call(methodName, args, done);
			});
		} else {
			call(methodName, args, next);
		}

		return result;
	}

	@Suspendable
	private void call(String methodName, Object[] args, Handler<AsyncResult<Object>> next) {
		Buffer result = Buffer.buffer(512);

		try {
			Serialization.serialize(args[0], new VertxBufferOutputStream(result));
		} catch (IOException e) {
			next.handle(Future.failedFuture(e));
			return;
		}

		eb.send(name + "::" + methodName, result, Sync.fiberHandler(new Handler<AsyncResult<Message<Object>>>() {
			@Override
			@Suspendable
			public void handle(AsyncResult<Message<Object>> reply) {
				if (reply.succeeded()) {
					try {
						Object body = Serialization.deserialize(new VertxBufferInputStream((Buffer) reply.result().body()));
						next.handle(Future.succeededFuture(body));
					} catch (IOException | ClassNotFoundException e) {
						next.handle(Future.failedFuture(e));
					}
				} else {
					next.handle(Future.failedFuture(reply.cause()));
				}
			}
		}));
	}
}
