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

package com.yoho.core.trace;

/**
 * Represents an event in time associated with a span. Every span has zero or more Logs,
 * each of which being a timestamped event name.
 *
 * @author Spencer Gibb
 * @since 1.0.0
 */
public class Log {
	/**
	 * The epoch timestamp of the log record; often set via {@link System#currentTimeMillis()}.
	 */
	private final long timestamp;

	/**
	 * Event (if not null) should be the stable name of some notable moment in the lifetime of a span.
	 * For instance, a span representing a browser page load might add an Event for each of the
	 * Performance.timing moments here: https://developer.mozilla.org/en-US/docs/Web/API/PerformanceTiming
	 *
	 * <p>While it is not a formal requirement, Event strings will be most useful if they are *not*
	 * unique; rather, tracing systems should be able to use them to understand how two similar spans
	 * relate from an internal timing perspective.
	 */
	private final String event;

	public Log(long timestamp, String event) {
		this.timestamp = timestamp;
		this.event = event;
	}

	public long getTimestamp() {
		return this.timestamp;
	}

	public String getEvent() {
		return this.event;
	}
}
