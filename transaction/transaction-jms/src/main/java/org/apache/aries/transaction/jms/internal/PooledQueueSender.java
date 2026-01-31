/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.transaction.jms.internal;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.QueueSender;

/**
 * 
 */
public class PooledQueueSender extends PooledProducer implements QueueSender {

    public PooledQueueSender(QueueSender messageProducer, Destination destination) throws JMSException {
        super(messageProducer, destination);
    }

    public void send(Queue queue, Message message, int i, int i1, long l) throws JMSException {
        getQueueSender().send(queue, message, i, i1, l);
    }

    public void send(Queue queue, Message message) throws JMSException {
        getQueueSender().send(queue, message);
    }

    public Queue getQueue() throws JMSException {
        return getQueueSender().getQueue();
    }

    @Override
    public void send(Destination destination, Message message, int deliveryMode, int priority, long timeToLive, jakarta.jms.CompletionListener completionListener) throws JMSException {
        getQueueSender().send(destination, message, deliveryMode, priority, timeToLive, completionListener);
    }

    @Override
    public void send(Message message, jakarta.jms.CompletionListener completionListener) throws JMSException {
        getQueueSender().send(message, completionListener);
    }

    @Override
    public void send(Destination destination, Message message, jakarta.jms.CompletionListener completionListener) throws JMSException {
        getQueueSender().send(destination, message, completionListener);
    }

    @Override
    public void send(Message message, int deliveryMode, int priority, long timeToLive, jakarta.jms.CompletionListener completionListener) throws JMSException {
        getQueueSender().send(message, deliveryMode, priority, timeToLive, completionListener);
    }

    @Override
    public long getDeliveryDelay() throws JMSException {
        return getQueueSender().getDeliveryDelay();
    }

    @Override
    public void setDeliveryDelay(long deliveryDelay) throws JMSException {
        getQueueSender().setDeliveryDelay(deliveryDelay);
    }

    protected QueueSender getQueueSender() {
        return (QueueSender) getMessageProducer();
    }

}
