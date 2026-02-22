package com.polarbookshop.orderservice.config;

import com.polarbookshop.orderservice.order.event.OrderDispatchedMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import tools.jackson.databind.ObjectMapper;

@Profile("integration")
@Configuration
public class LocalClientConfig {
    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    public MessageConverter dispatchedOrderConverter() {
        return new AbstractMessageConverter() {
            @Override
            protected boolean supports(Class<?> clazz) {
                return false;
            }

            @Override
            protected boolean canConvertFrom(org.springframework.messaging.Message<?> message, Class<?> targetClass) {
                if (OrderDispatchedMessage.class.equals(targetClass)) {
                    return true;
                }
                return super.canConvertFrom(message, targetClass);
            }

            @Override
            protected @Nullable OrderDispatchedMessage convertFromInternal(
                    @NonNull Message<?> message, Class<?> targetClass, @Nullable Object conversionHint) {
                return objectMapper.readValue((byte[]) message.getPayload(), OrderDispatchedMessage.class);
            }
        };
    }
}
