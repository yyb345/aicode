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
 * 文档分析器处理器
 * 处理文档分析相关的问题，如解析文档内容、分析PDF文件、提取文档摘要等
 * 使用 AgentRouter 进行智能路由判断
 */
@Component
public class DocAnalyzerHandler implements BusinessChainHandler {

    private static final Logger logger = LoggerFactory.getLogger(DocAnalyzerHandler.class);

    @Autowired
    private MaterialsContextService materialsContextService;

    @Autowired
    private OpenAIService openAIService;

    @Autowired
    private AgentRouter agentRouter;

    @Override
    public boolean canHandle(String question) {
        try {
            // 使用 AgentRouter 进行路由，如果返回 doc_analyzer 则处理
            String agentCode = agentRouter.route(question);
            boolean canHandle = "doc_analyzer".equals(agentCode);
            
            if (canHandle) {
                logger.debug("DocAnalyzerHandler 匹配到问题: {}", question);
            }
            
            return canHandle;
        } catch (Exception e) {
            logger.error("AgentRouter 路由判断失败，回退到关键词匹配: {}", e.getMessage(), e);
            // 如果 AgentRouter 失败，回退到原有的关键词匹配逻辑
            String lowerQuestion = question.toLowerCase();
            return lowerQuestion.contains("文档") || lowerQuestion.contains("document") ||
                   lowerQuestion.contains("pdf") || lowerQuestion.contains("分析") ||
                   lowerQuestion.contains("摘要") || lowerQuestion.contains("解析");
        }
    }

    @Override
    public Flux<String> handleStream(String question) {
        try {
            // 获取所有文档作为上下文
            String context = materialsContextService.getAllDocumentsAsContext();
            
            // 构建包含上下文的问题
            String contextualQuestion = "基于以下文档内容，请进行文档分析并回答用户的问题：\n\n" +
                                      "文档内容：\n" + context + "\n\n" +
                                      "用户问题：" + question + "\n\n" +
                                      "请根据上述文档内容进行深入分析，包括但不限于：文档结构解析、内容摘要提取、关键信息识别等。\n" +
                                      "重要提示：如果文档中完全没有与用户问题相关的信息，请只返回 \"__SKIP__\"（不要返回任何其他内容）。如果有相关信息，请正常回答。";

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
                        String errorMsg = "处理文档分析问题时出现错误：" + throwable.getMessage();
                        return Flux.just("data: " + errorMsg + "\n\n");
                    });

        } catch (Exception e) {
            return Flux.just("data: 处理文档分析问题时出现错误：" + e.getMessage() + "\n\n");
        }
    }

    @Override
    public String handleSync(String question) {
        try {
            // 获取所有文档作为上下文
            String context = materialsContextService.getAllDocumentsAsContext();
            
            // 构建包含上下文的问题
            String contextualQuestion = "基于以下文档内容，请进行文档分析并回答用户的问题：\n\n" +
                                      "文档内容：\n" + context + "\n\n" +
                                      "用户问题：" + question + "\n\n" +
                                      "请根据上述文档内容进行深入分析，包括但不限于：文档结构解析、内容摘要提取、关键信息识别等。\n" +
                                      "重要提示：如果文档中完全没有与用户问题相关的信息，请只返回 \"__SKIP__\"（不要返回任何其他内容）。如果有相关信息，请正常回答。";

            // 使用OpenAI服务进行问答
            return openAIService.chat(contextualQuestion);

        } catch (Exception e) {
            return "处理文档分析问题时出现错误：" + e.getMessage();
        }
    }

    @Override
    public String getHandlerName() {
        return "doc_analyzer";
    }

    @Override
    public int getPriority() {
        return 10; // 文档分析优先级较高
    }
}

