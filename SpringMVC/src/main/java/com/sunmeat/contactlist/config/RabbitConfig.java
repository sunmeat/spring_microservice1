package com.sunmeat.contactlist.config;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// клас є конфігураційним для Spring (створює бiни для використання з RabbitMQ)
@Configuration
public class RabbitConfig {

    // cтворюємо чергу з назвою "feedbackQueue"
    @Bean
    Queue feedbackQueue() {
        return new Queue("feedbackQueue", false); // черга не є довговічною (non-durable)
    } // при закрытии rabbitMQ очередь будет потеряна и выгружена из памяти
    // если указать true, сообщения смогут храниться на диске!

    // cтворюємо обмінник (exchange) типу Topic з назвою "feedbackExchange"
    @Bean
    TopicExchange feedbackExchange() {
        return new TopicExchange("feedbackExchange");
    } // типы обменников:
    // Topic - отправка сообщения по ключу, но с поддержкой шаблонов
    // Direct - отправка сообщения строго по ключу маршрутизации
    // Fanout - рассылка сообщений во все очереди, привязанные к обменнику
    // Headers - отправка по заголовку сообщений, а не по ключу

    // налаштовуємо прив’язку черги до обмінника з шаблоном маршрутизації "feedback.#"
    @Bean
    Binding binding(Queue feedbackQueue, TopicExchange feedbackExchange) {
        return BindingBuilder.bind(feedbackQueue).to(feedbackExchange).with("feedback.#");
    }

    // налаштовуємо фабрику з'єднань з RabbitMQ, вказуючи локальний сервер
    @Bean
    ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory("localhost");
        connectionFactory.setUsername("guest"); // iм'я користувача для підключення
        connectionFactory.setPassword("guest");
        return connectionFactory;
    }

    // налаштовуємо шаблон для роботи з RabbitMQ (використовується для відправки повідомлень)
    @Bean
    AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }

    // створюємо адміністрування RabbitMQ для управління чергами та обмінниками
    @Bean
    RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}
