/*
 * Copyright 2013-2015 the original author or authors.
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

package com.yoho.core.trace.log;

import java.util.regex.Pattern;

import com.yoho.core.trace.Span;
import org.slf4j.Logger;
import org.slf4j.MDC;

/**
 *
 * trace logger
 *
 * Span listener that logs to the console when a span got
 * started / stopped / continued.
 *
 * @author Spencer Gibb
 * @author  chunhua.zhang
 * @since 1.0.0
 */
public class Slf4jSpanLogger implements SpanLogger {

	private final static String TRACE_LOGGER_NAME = "TRACE";
	private final Logger log;
	private final Pattern nameSkipPattern;

	public Slf4jSpanLogger(String nameSkipPattern) {
		this.nameSkipPattern = Pattern.compile(nameSkipPattern);
		this.log = org.slf4j.LoggerFactory.getLogger(TRACE_LOGGER_NAME);
	}

	Slf4jSpanLogger(String nameSkipPattern, Logger log) {
		this.nameSkipPattern = Pattern.compile(nameSkipPattern);
		this.log = log;
	}

	@Override
	public void logStartedSpan(Span parent, Span span) {
		MDC.put(Span.SPAN_ID_NAME, Span.idToHex(span.getSpanId()));
		MDC.put(Span.SPAN_EXPORT_NAME, String.valueOf(span.isExportable()));
		MDC.put(Span.TRACE_ID_NAME, Span.idToHex(span.getTraceId()));
		log("Starting span: {} With parent: {} ", span, parent);
	}

	@Override
	public void logContinuedSpan(Span span) {
		MDC.put(Span.SPAN_ID_NAME, Span.idToHex(span.getSpanId()));
		MDC.put(Span.TRACE_ID_NAME, Span.idToHex(span.getTraceId()));
		MDC.put(Span.SPAN_EXPORT_NAME, String.valueOf(span.isExportable()));
		log("Continued span: {} With parent: {}", span, null);
	}

	@Override
	public void logStoppedSpan(Span parent, Span span) {
		log("Stopped span: {} With parent: {}", span, parent);
		if (parent != null) {
			MDC.put(Span.SPAN_ID_NAME, Span.idToHex(parent.getSpanId()));
			MDC.put(Span.SPAN_EXPORT_NAME, String.valueOf(parent.isExportable()));
		}
		else {
			MDC.remove(Span.SPAN_ID_NAME);
			MDC.remove(Span.SPAN_EXPORT_NAME);
			MDC.remove(Span.TRACE_ID_NAME);
		}
	}

	private void log(String text, Span span, Span parent) {
		if (this.nameSkipPattern.matcher(span.getName()).matches()) {
			return;
		}
		this.log.trace(text, span, parent);
	}

}
