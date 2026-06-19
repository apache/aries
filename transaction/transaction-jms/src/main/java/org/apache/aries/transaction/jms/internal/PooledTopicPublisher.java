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

import jakarta.jms.*;

/**
 * 
 */
public class PooledTopicPublisher extends PooledProducer implements TopicPublisher {

    public PooledTopicPublisher(TopicPublisher messageProducer, Destination destination) throws JMSException {
        super(messageProducer, destination);
    }

    public Topic getTopic() throws JMSException {
        return getTopicPublisher().getTopic();
    }

    public void publish(Message message) throws JMSException {
        getTopicPublisher().publish((Topic) getDestination(), message);
    }

    public void publish(Message message, int i, int i1, long l) throws JMSException {
        getTopicPublisher().publish((Topic) getDestination(), message, i, i1, l);
    }

    public void publish(Topic topic, Message message) throws JMSException {
        getTopicPublisher().publish(topic, message);
    }

    public void publish(Topic topic, Message message, int i, int i1, long l) throws JMSException {
        getTopicPublisher().publish(topic, message, i, i1, l);
    }

    protected TopicPublisher getTopicPublisher() {
        return (TopicPublisher) getMessageProducer();
    }

    @Override
    public void setDeliveryDelay(long deliveryDelay) throws JMSException {
        getTopicPublisher().setDeliveryDelay(deliveryDelay);
    }

    @Override
    public long getDeliveryDelay() throws JMSException {
        return getDeliveryMode();
    }

    @Override
    public void send(Message message, CompletionListener completionListener) throws JMSException {
        getTopicPublisher().send(message, completionListener);
    }

    @Override
    public void send(Message message, int deliveryMode, int priority, long timeToLive, CompletionListener completionListener)
            throws JMSException {
        getTopicPublisher().send(message, deliveryMode, priority, timeToLive, completionListener);
    }

    @Override
    public void send(Destination destination, Message message, CompletionListener completionListener) throws JMSException {
        getTopicPublisher().send(destination, message, completionListener);
    }

    @Override
    public void send(Destination destination, Message message, int deliveryMode, int priority, long timeToLive,
            CompletionListener completionListener) throws JMSException {
        getTopicPublisher().send(destination, message, deliveryMode, priority, timeToLive, completionListener);
    }
}
