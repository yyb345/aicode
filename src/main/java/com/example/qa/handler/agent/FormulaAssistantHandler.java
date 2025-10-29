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
 * 配方助手处理器
 * 处理与化学配方相关的问题，如分析配方、优化化学配方、推荐材料比例等
 * 使用 AgentRouter 进行智能路由判断
 */
@Component
public class FormulaAssistantHandler implements BusinessChainHandler {

    private static final Logger logger = LoggerFactory.getLogger(FormulaAssistantHandler.class);

    @Autowired
    private MaterialsContextService materialsContextService;

    @Autowired
    private OpenAIService openAIService;

    @Autowired
    private AgentRouter agentRouter;

    @Override
    public boolean canHandle(String question) {
        try {
            // 使用 AgentRouter 进行路由，如果返回 formula_assistant 则处理
            String agentCode = agentRouter.route(question);
            boolean canHandle = "formula_assistant".equals(agentCode);
            
            if (canHandle) {
                logger.debug("FormulaAssistantHandler 匹配到问题: {}", question);
            }
            
            return canHandle;
        } catch (Exception e) {
            logger.error("AgentRouter 路由判断失败，回退到关键词匹配: {}", e.getMessage(), e);
            // 如果 AgentRouter 失败，回退到原有的关键词匹配逻辑
            String lowerQuestion = question.toLowerCase();
            return lowerQuestion.contains("配方") || lowerQuestion.contains("formula") ||
                   lowerQuestion.contains("比例") || lowerQuestion.contains("优化") ||
                   lowerQuestion.contains("化学");
        }
    }

    @Override
    public Flux<String> handleStream(String question) {
        try {
            // 获取所有文档作为上下文
            String context = materialsContextService.getAllDocumentsAsContext();
            
            // 构建包含上下文的问题
            String contextualQuestion = "基于以下材料文档内容，请作为配方助手回答用户的问题：\n\n" +
                                      "材料文档内容：\n" + context + "\n\n" +
                                      "用户问题：" + question + "\n\n" +
                                      "请根据上述材料文档内容，帮助用户分析配方、优化化学配方或推荐材料比例。\n" +
                                      "重要提示：如果材料文档中完全没有与用户问题相关的配方或材料信息，请只返回 \"__SKIP__\"（不要返回任何其他内容）。如果有相关信息，请正常回答。";

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
                        String errorMsg = "处理配方问题时出现错误：" + throwable.getMessage();
                        return Flux.just("data: " + errorMsg + "\n\n");
                    });

        } catch (Exception e) {
            return Flux.just("data: 处理配方问题时出现错误：" + e.getMessage() + "\n\n");
        }
    }

    @Override
    public String handleSync(String question) {
        try {
            // 获取所有文档作为上下文
            String context = materialsContextService.getAllDocumentsAsContext();
            
            // 构建包含上下文的问题
            String contextualQuestion = "基于以下材料文档内容，请作为配方助手回答用户的问题：\n\n" +
                                      "材料文档内容：\n" + context + "\n\n" +
                                      "用户问题：" + question + "\n\n" +
                                      "请根据上述材料文档内容，帮助用户分析配方、优化化学配方或推荐材料比例。\n" +
                                      "重要提示：如果材料文档中完全没有与用户问题相关的配方或材料信息，请只返回 \"__SKIP__\"（不要返回任何其他内容）。如果有相关信息，请正常回答。";

            // 使用OpenAI服务进行问答
            return openAIService.chat(contextualQuestion);

        } catch (Exception e) {
            return "处理配方问题时出现错误：" + e.getMessage();
        }
    }

    @Override
    public String getHandlerName() {
        return "formula_assistant";
    }

    @Override
    public int getPriority() {
        return 15; // 配方助手优先级中等偏上
    }
}

