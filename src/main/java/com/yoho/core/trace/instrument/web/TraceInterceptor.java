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
package com.yoho.core.trace.instrument.web;

import com.yoho.core.trace.*;
import com.yoho.core.trace.sampler.NeverSampler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.regex.Pattern;

import static org.springframework.util.StringUtils.hasText;

/**
 * Filter that takes the value of the {@link Span#SPAN_ID_NAME} and
 * {@link Span#TRACE_ID_NAME} header from either request or response and uses them to
 * create a new span.
 * <p/>
 * <p/>
 * In order to keep the size of spans manageable, this only add tags defined in
 * {@link TraceKeys}. If you need to add additional tags, such as headers subtype this and
 * override {@link #addRequestTags} or {@link #addResponseTags}.
 *
 * @author Jakub Nabrdalik, 4financeIT
 * @author Tomasz Nurkiewicz, 4financeIT
 * @author Marcin Grzejszczak
 * @author Spencer Gibb
 * @author Dave Syer
 * @author  chunhua.zhang  modify to interceptor
 * @see Tracer
 * @see TraceKeys
 * @since 1.0.0
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class TraceInterceptor implements HandlerInterceptor {

    public static final String DEFAULT_SKIP_PATTERN =
            "/api-docs.*|/autoconfig|/configprops|/dump|/health|/info|/metrics.*|/mappings|/trace|/swagger.*|.*\\.png|.*\\.css|.*\\.js|.*\\.html|/favicon.ico|/hystrix.stream";
    protected static final String TRACE_REQUEST_ATTR = TraceInterceptor.class.getName()
            + ".TRACE";
    private final static Logger log = LoggerFactory.getLogger(TraceInterceptor.class);
    private static final String HTTP_COMPONENT = "http";
    private final Tracer tracer;
    private final TraceKeys traceKeys;
    private final Pattern skipPattern;
    private final SpanReporter spanReporter;
    private final SpanExtractor<HttpServletRequest> spanExtractor;
    private final SpanInjector<HttpServletResponse> spanInjector;

    private UrlPathHelper urlPathHelper = new UrlPathHelper();

    public TraceInterceptor(Tracer tracer, TraceKeys traceKeys, SpanReporter spanReporter,
                            SpanExtractor<HttpServletRequest> spanExtractor, SpanInjector<HttpServletResponse> spanInjector) {
        this(tracer, traceKeys, Pattern.compile(DEFAULT_SKIP_PATTERN), spanReporter,
                spanExtractor, spanInjector);
    }

    public TraceInterceptor(Tracer tracer, TraceKeys traceKeys, Pattern skipPattern,
                            SpanReporter spanReporter, SpanExtractor<HttpServletRequest> spanExtractor,
                            SpanInjector<HttpServletResponse> spanInjector) {
        this.tracer = tracer;
        this.traceKeys = traceKeys;
        this.skipPattern = skipPattern;
        this.spanReporter = spanReporter;
        this.spanExtractor = spanExtractor;
        this.spanInjector = spanInjector;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        try {
            String uri = this.urlPathHelper.getPathWithinApplication(request);
            boolean skip = this.skipPattern.matcher(uri).matches()
                    || Span.SPAN_NOT_SAMPLED.equals(ServletUtils.getHeader(request, response, Span.SAMPLED_NAME));
            Span spanFromRequest = (Span) request.getAttribute(TRACE_REQUEST_ATTR);
            if (spanFromRequest != null) {
                this.tracer.continueSpan(spanFromRequest);
            }
            addToResponseIfNotPresent(response, Span.SAMPLED_NAME, skip ? Span.SPAN_NOT_SAMPLED : Span.SPAN_SAMPLED);
            String name = HTTP_COMPONENT + ":" + uri;
            spanFromRequest = createSpan(request, skip, spanFromRequest, name);

            addRequestTags(request);
            // Add headers before filter chain in case one of the filters flushes the
            // response...
            this.spanInjector.inject(spanFromRequest, response);

            //------ add span to request ----
            request.setAttribute("SPAN-FROM-REQUEST", spanFromRequest);
            request.setAttribute("SPAN-SKIP", skip);

        } catch (Exception e) {
            log.error("exception happened.", e);
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        //do nothing
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        //------get something from request-----
        Span spanFromRequest = (Span) request.getAttribute("SPAN-FROM-REQUEST");
        boolean skip = (Boolean) request.getAttribute("SPAN-SKIP");

        //不处理异常的请求
        if (request.isAsyncStarted()) {
            this.tracer.detach(spanFromRequest);
            // TODO: how to deal with response annotations and async?
            return;
        }

        addToResponseIfNotPresent(response, Span.SAMPLED_NAME, skip ? Span.SPAN_NOT_SAMPLED : Span.SPAN_SAMPLED);
        if (spanFromRequest != null) {
            addResponseTags(response, ex);
            if (spanFromRequest.hasSavedSpan()) {
                Span parent = spanFromRequest.getSavedSpan();
                if (parent != null && parent.isRemote()) {
                    parent.logEvent(Span.SERVER_SEND);
                    this.spanReporter.report(parent);
                }
            }
            // Double close to clean up the parent (remote span as well)
            this.tracer.close(spanFromRequest);
        }
    }


    /**
     * Creates a span and appends it as the current request's attribute
     */
    private Span createSpan(HttpServletRequest request,
                            boolean skip, Span spanFromRequest, String name) {
        if (spanFromRequest != null) {
            return spanFromRequest;
        }
        Span parent = this.spanExtractor
                .joinTrace(request);
        if (parent != null) {
            spanFromRequest = this.tracer.createSpan(name, parent);
            if (parent.isRemote()) {
                parent.logEvent(Span.SERVER_RECV);
            }
            request.setAttribute(TRACE_REQUEST_ATTR, spanFromRequest);
        } else {
            if (skip) {
                spanFromRequest = this.tracer.createSpan(name, NeverSampler.INSTANCE);
            } else {
                spanFromRequest = this.tracer.createSpan(name);
            }
            request.setAttribute(TRACE_REQUEST_ATTR, spanFromRequest);
        }
        return spanFromRequest;
    }

    /**
     * Override to add annotations not defined in {@link TraceKeys}.
     */
    protected void addRequestTags(HttpServletRequest request) {

        //add uid
        this.tracer.addTag(this.traceKeys.getYoho().getUid(), request.getParameter("uid"));


        //add http
        String uri = this.urlPathHelper.getPathWithinApplication(request);
        this.tracer.addTag(this.traceKeys.getHttp().getUrl(), getFullUrl(request));
        this.tracer.addTag(this.traceKeys.getHttp().getHost(), request.getServerName());
        this.tracer.addTag(this.traceKeys.getHttp().getPath(), uri);
        this.tracer.addTag(this.traceKeys.getHttp().getMethod(), request.getMethod());
        for (String name : this.traceKeys.getHttp().getHeaders()) {
            Enumeration<String> values = request.getHeaders(name);
            if (values.hasMoreElements()) {
                String key = this.traceKeys.getHttp().getPrefix() + name.toLowerCase();
                ArrayList<String> list = Collections.list(values);
                String value = list.size() == 1 ? list.get(0)
                        : StringUtils.collectionToDelimitedString(list, ",", "'", "'");
                this.tracer.addTag(key, value);
            }
        }
    }

    /**
     * Override to add annotations not defined in {@link TraceKeys}.
     */
    protected void addResponseTags(HttpServletResponse response, Throwable e) {
        int httpStatus = response.getStatus();
        if (httpStatus == HttpServletResponse.SC_OK && e != null) {
            // Filter chain threw exception but the response status may not have been set
            // yet, so we have to guess.
            this.tracer.addTag(this.traceKeys.getHttp().getStatusCode(),
                    String.valueOf(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
        } else if ((httpStatus < 200) || (httpStatus > 299)) {
            this.tracer.addTag(this.traceKeys.getHttp().getStatusCode(),
                    String.valueOf(response.getStatus()));
        }
    }

    private void addToResponseIfNotPresent(HttpServletResponse response, String name,
                                           String value) {
        if (!hasText(response.getHeader(name))) {
            response.addHeader(name, value);
        }
    }


    private String getFullUrl(HttpServletRequest request) {
        StringBuffer requestURI = request.getRequestURL();
        String queryString = request.getQueryString();
        if (queryString == null) {
            return requestURI.toString();
        } else {
            return requestURI.append('?').append(queryString).toString();
        }
    }


}
