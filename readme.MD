## 使用方法

### 添加 `springmvc` 的 interceptor
```xml

    <mvc:interceptors>
		<ref bean="trace.traceInterceptor"/>
		 ...
	</mvc:interceptors>

```

### 配置`logback.xml` 的  `encoder` 添加上 `MDC`

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