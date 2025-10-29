package com.example.qa.handler.agent;

import com.example.qa.handler.BusinessChainHandler;
import com.example.qa.router.AgentRouter;
import com.example.qa.service.MaterialsContextService;
import com.example.qa.service.OpenAIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 技术问答处理器
 * 处理通用技术问答问题，如智能问答、翻译、解释词义等
 * 使用 AgentRouter 进行智能路由判断
 */
@Component
public class TechQAHandler implements BusinessChainHandler {

    private static final Logger logger = LoggerFactory.getLogger(TechQAHandler.class);

    @Autowired
    private MaterialsContextService materialsContextService;

    @Autowired
    private OpenAIService openAIService;

    @Autowired
    private AgentRouter agentRouter;

    @Override
    public boolean canHandle(String question) {
        try {
            // 使用 AgentRouter 进行路由，如果返回 tech_qa 则处理
            String agentCode = agentRouter.route(question);
            boolean canHandle = "tech_qa".equals(agentCode);
            
            if (canHandle) {
                logger.debug("TechQAHandler 匹配到问题: {}", question);
            }
            
            return canHandle;
        } catch (Exception e) {
            logger.error("AgentRouter 路由判断失败，回退到关键词匹配: {}", e.getMessage(), e);
            // 如果 AgentRouter 失败，回退到原有的关键词匹配逻辑
            String lowerQuestion = question.toLowerCase();
            return lowerQuestion.contains("翻译") || lowerQuestion.contains("translate") ||
                   lowerQuestion.contains("解释") || lowerQuestion.contains("含义") ||
                   lowerQuestion.contains("什么意思");
        }
    }

    @Override
    public Flux<String> handleStream(String question) {
        try {
            // 获取所有文档作为上下文，但作为可选的参考信息
            String context = materialsContextService.getAllDocumentsAsContext();
            
            // 构建包含上下文的问题
            String contextualQuestion = "请作为技术问答助手回答用户的问题。如果需要，可以参考以下材料文档内容：\n\n" +
                                      "材料文档内容：\n" + context + "\n\n" +
                                      "用户问题：" + question + "\n\n" +
                                      "请根据用户的问题提供准确、详细的技术回答。如果涉及翻译，请提供准确的翻译结果。如果涉及术语解释，请提供清晰的定义和说明。\n" +
                                      "重要提示：如果用户的问题与材料文档完全无关，且是一个通用技术问题，请直接基于您的知识回答，不要返回 \"__SKIP__\"。";

            // 使用OpenAI服务进行问答
            return openAIService.streamChat(contextualQuestion)
                    .map(content -> {
                        if (content == null || content.trim().isEmpty()) {
                            return "";
                        }
                        return content + "\n\n";
                    })
                    .filter(content -> !content.isEmpty())
                    .onErrorResume(throwable -> {
                        String errorMsg = "处理技术问答时出现错误：" + throwable.getMessage();
                        return Flux.just("data: " + errorMsg + "\n\n");
                    });

        } catch (Exception e) {
            return Flux.just("data: 处理技术问答时出现错误：" + e.getMessage() + "\n\n");
        }
    }

    @Override
    public String handleSync(String question) {
        try {
            // 获取所有文档作为上下文，但作为可选的参考信息
            String context = materialsContextService.getAllDocumentsAsContext();
            
            // 构建包含上下文的问题
            String contextualQuestion = "请作为技术问答助手回答用户的问题。如果需要，可以参考以下材料文档内容：\n\n" +
                                      "材料文档内容：\n" + context + "\n\n" +
                                      "用户问题：" + question + "\n\n" +
                                      "请根据用户的问题提供准确、详细的技术回答。如果涉及翻译，请提供准确的翻译结果。如果涉及术语解释，请提供清晰的定义和说明。\n" +
                                      "重要提示：如果用户的问题与材料文档完全无关，且是一个通用技术问题，请直接基于您的知识回答，不要返回 \"__SKIP__\"。";

            // 使用OpenAI服务进行问答
            return openAIService.chat(contextualQuestion);

        } catch (Exception e) {
            return "处理技术问答时出现错误：" + e.getMessage();
        }
    }

    @Override
    public String getHandlerName() {
        return "tech_qa";
    }

    @Override
    public int getPriority() {
        return 50; // 技术问答优先级较低，作为兜底处理器
    }
}

