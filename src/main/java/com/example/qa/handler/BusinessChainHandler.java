package com.example.qa.handler;

import reactor.core.publisher.Flux;

/**
 * 业务链处理器接口
 * 用于处理不同类型的问答请求
 */
public interface BusinessChainHandler {

    /**
     * 判断是否处理该问题
     * @param question 用户问题
     * @return true 表示可以处理，false 表示不能处理
     */
    boolean canHandle(String question);

    /**
     * 流式处理问题
     * @param question 用户问题
     * @return 流式响应
     */
    Flux<String> handleStream(String question);

    /**
     * 同步处理问题
     * @param question 用户问题
     * @return 同步响应
     */
    String handleSync(String question);

    /**
     * 获取处理器名称，用于日志和调试
     * @return 处理器名称
     */
    String getHandlerName();

    /**
     * 获取处理器优先级，数值越小优先级越高
     * @return 优先级
     */
    default int getPriority() {
        return 100;
    }
}

