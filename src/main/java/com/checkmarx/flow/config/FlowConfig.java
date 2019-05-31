package com.checkmarx.flow.config;

import com.checkmarx.flow.utils.ScanUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import java.beans.ConstructorProperties;
import java.nio.charset.Charset;
import java.util.Properties;

@Configuration
public class FlowConfig {

    private final FlowProperties properties;
    public static final int QUEUE_CAPACITY = 10000;


    @ConstructorProperties({"properties"})
    public FlowConfig(FlowProperties properties) {
        this.properties = properties;
    }

    @Bean
    public RestTemplate getRestTemplate(){
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters()
                .add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
        return restTemplate;
    }

    @Bean("webHook")
    public TaskExecutor webHookTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(properties.getWebHookQueue());
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("flow-web");
        executor.initialize();
        return executor;
    }

    @Bean("scanRequest")
    public TaskExecutor scanRequestTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(properties.getScanResultQueue());
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("scan-results");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    @Bean
    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        FlowProperties.Mail mail = properties.getMail();

        if(mail == null || !mail.isEnabled()){
            return mailSender;
        }
        Properties props = mailSender.getJavaMailProperties();

        if(!ScanUtils.empty(mail.getUsername()) &&
                mail.getPort() != null && !ScanUtils.empty(mail.getHost())){
            mailSender.setHost(mail.getHost());
            mailSender.setPort(mail.getPort());
            mailSender.setUsername(mail.getUsername());
            mailSender.setPassword(mail.getPassword());
            props.put("mail.smtp.auth", "true");

        }
        props.put("mail.transport.protocol", JavaMailSenderImpl.DEFAULT_PROTOCOL);
        props.put("mail.smtp.starttls.enable", "true");

        return mailSender;
    }

}
