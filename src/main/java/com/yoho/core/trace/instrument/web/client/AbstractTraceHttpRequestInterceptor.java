/*
 * Copyright 2013-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yoho.core.trace.instrument.web.client;

import java.net.URI;

import com.yoho.core.trace.Span;
import com.yoho.core.trace.SpanInjector;
import com.yoho.core.trace.Tracer;
import org.springframework.http.HttpRequest;

/**
 * Abstraction over classes that interact with Http requests. Allows you
 * to enrich the request headers with trace related information.
 *
 * @author Marcin Grzejszczak
 *
 * @since 1.0.0
 */
public abstract class AbstractTraceHttpRequestInterceptor {

	protected final Tracer tracer;
	protected final SpanInjector<HttpRequest> spanInjector;

	protected AbstractTraceHttpRequestInterceptor(Tracer tracer,
												  SpanInjector<HttpRequest> spanInjector) {
		this.tracer = tracer;
		this.spanInjector = spanInjector;
	}

	/**
	 * Enriches the request with proper headers and publishes
	 * the client sent event
	 */
	protected void publishStartEvent(HttpRequest request) {
		URI uri = request.getURI();
		String spanName = uriScheme(uri) + ":" + uri.getPath();
		Span newSpan = this.tracer.createSpan(spanName);
		this.spanInjector.inject(newSpan, request);
		newSpan.logEvent(Span.CLIENT_SEND);
	}

	private String uriScheme(URI uri) {
		return uri.getScheme() == null ? "http" : uri.getScheme();
	}

	/**
	 * Close the current span and log the client received event
	 */
	public void finish() {
		if (!isTracing()) {
			return;
		}
		currentSpan().logEvent(Span.CLIENT_RECV);
		this.tracer.close(this.currentSpan());
	}

	protected Span currentSpan() {
		return this.tracer.getCurrentSpan();
	}

	protected boolean isTracing() {
		return this.tracer.isTracing();
	}

}
