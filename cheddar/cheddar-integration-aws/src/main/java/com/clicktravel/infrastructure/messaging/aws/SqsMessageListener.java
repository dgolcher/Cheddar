/*
 * Copyright 2014 Click Travel Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package com.clicktravel.infrastructure.messaging.aws;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.clicktravel.cheddar.infrastructure.messaging.MessageHandler;
import com.clicktravel.cheddar.infrastructure.messaging.MessageListener;
import com.clicktravel.common.concurrent.RateLimiter;

public class SqsMessageListener extends SqsMessageQueueAccessor implements MessageListener {

    private static final int NUM_THREADS = 10;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<String, MessageHandler> messageHandlers = new ConcurrentHashMap<>();
    private final RateLimiter rateLimiter;
    private SqsMessageProcessor queueProcessor;

    public SqsMessageListener(final String queueName, final RateLimiter rateLimiter) {
        super(queueName);
        this.rateLimiter = rateLimiter;
    }

    public SqsMessageListener(final String queueName) {
        this(queueName, null);
    }

    @Override
    public void start() {
        if (queueProcessor != null) {
            logger.debug("Already listening for messages on queue [" + queueName() + "]");
        } else {
            logger.info("Starting to listen for messages on queue [" + queueName() + "] for these message types :["
                    + messageTypeSummary() + "]");
            final SqsMessageProcessorExecutor executor = new SqsMessageProcessorExecutor(queueName(), NUM_THREADS);
            queueProcessor = new SqsMessageProcessor(amazonSqsClient(), queueName(), messageHandlers, rateLimiter,
                    executor);
            new Thread(queueProcessor).start();
        }
    }

    @Override
    public void registerMessageHandler(final String messageType, final MessageHandler messageHandler) {
        messageHandlers.put(messageType, messageHandler);
    }

    @Override
    public void prepareForShutdown() {
        logger.debug("Shutdown of processing for queue [" + queueName() + "] is imminent. Reducing queue poll time.");
        queueProcessor.shutdownImminent();
    }

    @Override
    public void shutdown() {
        logger.debug("Starting shutdown of processing for queue [" + queueName() + "]");
        queueProcessor.shutdown();
    }

    @PreDestroy
    public void destroy() {
        shutdown();
    }

    @Override
    public void shutdownAfterQueueDrained() {
        logger.debug("Draining then shutting down processing for queue [" + queueName() + "]");
        queueProcessor.shutdownWhenQueueDrained();
    }

    @Override
    public void awaitTermination() {
        logger.debug("Awaiting termination of processing for queue [" + queueName() + "]");
        queueProcessor.awaitTermination();
        logger.debug("Terminated processing for queue [" + queueName() + "]");
    }

    private String messageTypeSummary() {
        final StringBuilder sb = new StringBuilder();
        for (final String messageType : messageHandlers.keySet()) {
            if (sb.length() != 0) {
                sb.append(", ");
            }
            sb.append(messageType);
        }
        return sb.toString();
    }

}
