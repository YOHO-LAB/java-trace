## Introduction
Java trace framework based on `spring cloud sleuth`.  Features:
 -  support `spring mvc 3.1+` instead of `spring boot`
 -  support log span tag  

## How to use
### load all `META-INF/spring/*.xml` to `spring context`

### add the interceptor to `springmvc`   
```xml

    <mvc:interceptors>
		<ref bean="trace.traceInterceptor"/>
		 ...
	</mvc:interceptors>

```

### modify `logback.xml` , add the following `MDC`

```xml
   <appender name="trace_log" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <encoder>
            <pattern>[%X{X-B3-TraceId:-},%X{X-B3-SpanId:-},%X{X-Span-Export:-}] - %-5level [%thread] %logger{35} - %m%n</pattern>
        </encoder>
        <file>d:/yoho.gateway.trace.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- daily rollover -->
            <fileNamePattern>yoho.trace.%d{yyyy-MM-dd}.%i.log.gz
            </fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <!-- or whenever the file size reaches 100MB -->
                <maxFileSize>10KB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
            <!-- keep 30 days' worth of history -->
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>


```
