package com.polarbookshop.orderservice.config;

import com.polarbookshop.orderservice.order.event.OrderDispatchedMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

@Configuration
public class ClientConfig {

    @Bean
    public WebClient webClient(ClientProperties clientProperties) {
        return WebClient.builder()
                .baseUrl(clientProperties.catalogServiceUri().toString())
                .build();
    }

    @Bean
    @Profile("!integration")
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
                byte[] payload = (byte[]) message.getPayload();
                try (ByteArrayInputStream bis = new ByteArrayInputStream(payload);
                     ObjectInputStream ois = new ObjectInputStream(bis) {
                         @Override
                         protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
                             ObjectStreamClass desc = super.readClassDescriptor();
                             /* Remap the SENDER's package to YOUR package, because the class name in the serialized
                                data will be from the sender's package
                              */
                             if (desc.getName().endsWith("OrderDispatchedMessage")) {
                                 return ObjectStreamClass.lookup(targetClass);
                             }
                             return desc;
                         }
                     }) {
                    return (OrderDispatchedMessage) ois.readObject();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to convert message payload to OrderDispatchedMessage", e);
                }
            }
        };
    }
}