package com.yoho.core.trace.instrument.web.client;

import com.yoho.core.trace.Span;
import com.yoho.core.trace.SpanInjector;
import org.springframework.http.HttpRequest;
import org.springframework.util.StringUtils;

/**
 * Span injector that injects tracing info to {@link HttpRequest}
 *
 * @author Marcin Grzejszczak
 *
 * @since 1.0.0
 */
class HttpRequestInjector implements SpanInjector<HttpRequest> {

	@Override
	public void inject(Span span, HttpRequest carrier) {
		setIdHeader(carrier, Span.TRACE_ID_NAME, span.getTraceId());
		setIdHeader(carrier, Span.SPAN_ID_NAME, span.getSpanId());
		setHeader(carrier, Span.SAMPLED_NAME, span.isExportable() ? Span.SPAN_SAMPLED : Span.SPAN_NOT_SAMPLED);
		setHeader(carrier, Span.SPAN_NAME_NAME, span.getName());
		setIdHeader(carrier, Span.PARENT_ID_NAME, getParentId(span));
		setHeader(carrier, Span.PROCESS_ID_NAME, span.getProcessId());
	}

	private Long getParentId(Span span) {
		return !span.getParents().isEmpty() ? span.getParents().get(0) : null;
	}

	private void setHeader(HttpRequest request, String name, String value) {
		if (StringUtils.hasText(value) && !request.getHeaders().containsKey(name)) {
			request.getHeaders().add(name, value);
		}
	}

	private void setIdHeader(HttpRequest request, String name, Long value) {
		if (value != null) {
			setHeader(request, name, Span.idToHex(value));
		}
	}
}