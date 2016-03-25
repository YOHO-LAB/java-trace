package com.yoho.core.trace.instrument.web.client;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *  对RestTemplate添加过滤器
 * Created by chunhua.zhang@yoho.cn on 2016/3/11.
 */
public class RestTemplateDecorator implements   ApplicationListener<ContextRefreshedEvent> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private TraceRestTemplateInterceptor traceRestTemplateInterceptor;

    public RestTemplateDecorator(TraceRestTemplateInterceptor traceRestTemplateInterceptor){
        this.traceRestTemplateInterceptor = traceRestTemplateInterceptor;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

        Map<String, RestTemplate> restTemplates =  event.getApplicationContext().getBeansOfType(RestTemplate.class);
        if(restTemplates == null){
            return;
        }
        logger.info("found total {} RestTemplate", restTemplates.size());

        for(Map.Entry<String, RestTemplate> entry : restTemplates.entrySet()){
            RestTemplate restTemplate = entry.getValue();
            List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>(restTemplate.getInterceptors());
            interceptors.add(traceRestTemplateInterceptor);
            restTemplate.setInterceptors(interceptors);

            logger.info("setter interceptors for restTemplate at name:{} bean:{} success ", entry.getKey(), entry.getValue());

        }
    }

}
