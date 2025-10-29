package com.example.qa.router;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpHeaders;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

import com.example.vector.LocalVectorStore;
import com.example.vector.LocalVectorStore.Item;
import com.example.vector.LocalVectorStore.Result;
import java.sql.SQLException;

/**
 * AgentRouter: 基于 Embedding 相似度的意图识别与路由工具
 * 
 * 主要功能：
 * - 基于语义相似度匹配用户意图到最合适的 Agent
 * - 支持多 Agent 示例集合的相似度计算
 * - 使用向量数据库存储和查询
 * - 缓存机制减少 API 调用
 * - 异常处理和日志记录
 *
 * 使用示例：
 *   String agent = agentRouter.route("帮我分析一下这个文档内容");
 *   System.out.println(agent);
 */

@Component
public class AgentRouter {

    private static final Logger logger = LoggerFactory.getLogger(AgentRouter.class);

    @Value("${openai.api-key:}")
    private String apiKey;
    @Value("${openai.base-url}")
    private String baseUrl;

    private static final String EMBEDDING_MODEL = "text-embedding-3-small";
    private static final double THRESHOLD = 0.50; // 置信度阈值
    private static final String VECTOR_DB_PATH = "agent_embeddings.db";

    // WebClient for HTTP requests (延迟初始化)
    private WebClient webClient;
    
    // 缓存机制
    private final Map<String, double[]> embeddingCache;
    
    // Jackson ObjectMapper
    private final ObjectMapper objectMapper;
    
    // 向量数据库
    private LocalVectorStore vectorStore;
    
    // Agent 到示例的映射（用于初始化）
    private Map<String, List<String>> AGENT_EXAMPLES;

    /**
     * 从配置文件读取 Agent 示例
     * 文件格式：agent_code 后跟多行示例，agent 之间用空行分隔
     */
    private Map<String, List<String>> createAgentExamples() {
        Map<String, List<String>> examples = new HashMap<>();
        
        try {
            ClassPathResource resource = new ClassPathResource("agent_examples.txt");
            
            if (!resource.exists()) {
                logger.warn("agent_examples.txt 文件不存在，使用默认配置");
                return getDefaultExamples();
            }
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                
                String currentAgent = null;
                List<String> currentExamples = new ArrayList<>();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    
                    // 空行表示一个 agent 的结束，保存当前 agent
                    if (line.isEmpty()) {
                        if (currentAgent != null && !currentExamples.isEmpty()) {
                            examples.put(currentAgent, new ArrayList<>(currentExamples));
                            currentExamples.clear();
                            currentAgent = null;
                        }
                        continue;
                    }
                    
                    // 如果当前没有 agent，说明这是新的 agent code
                    if (currentAgent == null) {
                        currentAgent = line;
                    } else {
                        // 否则这是示例内容
                        currentExamples.add(line);
                    }
                }
                
                // 处理文件末尾的 agent（没有空行结尾的情况）
                if (currentAgent != null && !currentExamples.isEmpty()) {
                    examples.put(currentAgent, new ArrayList<>(currentExamples));
                }
            }
            
            logger.info("成功从配置文件加载 {} 个 Agent 的示例", examples.size());
            
        } catch (Exception e) {
            logger.error("读取 agent_examples.txt 配置文件失败: {}", e.getMessage(), e);
            logger.warn("使用默认 Agent 示例配置");
            return getDefaultExamples();
        }
        
        // 如果读取结果为空，返回默认配置
        if (examples.isEmpty()) {
            logger.warn("配置文件为空或格式错误，使用默认配置");
            return getDefaultExamples();
        }
        
        return Collections.unmodifiableMap(examples);
    }
    
    /**
     * 获取默认的 Agent 示例（作为后备方案）
     */
    private Map<String, List<String>> getDefaultExamples() {
        Map<String, List<String>> examples = new HashMap<>();
        examples.put("doc_analyzer", Arrays.asList("解析文档内容", "分析PDF文件", "提取文档摘要"));
        examples.put("formula_assistant", Arrays.asList("帮我分析配方", "优化化学配方", "推荐材料比例"));
        examples.put("patent_search", Arrays.asList("查询专利用途", "找一下相关专利", "搜索专利信息"));
        examples.put("material_scout", Arrays.asList("查询材料属性", "查找化合物信息", "材料数据库检索"));
        examples.put("tech_qa", Arrays.asList("智能问答", "帮我翻译一下", "解释一个词的含义"));
        return Collections.unmodifiableMap(examples);
    }

    public AgentRouter() {
        this.embeddingCache = new ConcurrentHashMap<>();
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        // 首先加载 Agent 示例配置
        this.AGENT_EXAMPLES = createAgentExamples();

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("OpenAI API Key 未配置，AgentRouter 可能无法正常工作");
            return;
        }
        
        // 初始化向量数据库
        try {
            this.vectorStore = new LocalVectorStore(VECTOR_DB_PATH);
            
            if (!hasDataInVectorStore()) {
                logger.info("初始化向量数据库，插入 Agent 示例...");
                initializeVectorStore();
            }
            
        } catch (Exception e) {
            logger.error("初始化向量数据库失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 检查向量数据库是否已有数据
     */
    private boolean hasDataInVectorStore() {
        if (vectorStore == null) {
            return false;
        }
        try {
            return vectorStore.hasData();
        } catch (SQLException e) {
            logger.error("检查向量数据库数据失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 初始化向量数据库，插入所有 Agent 示例
     */
    private void initializeVectorStore() throws Exception {
        List<Item> items = new ArrayList<>();
        
        // 遍历所有 Agent 示例并生成 embeddings
        for (Map.Entry<String, List<String>> entry : AGENT_EXAMPLES.entrySet()) {
            String agentCode = entry.getKey();
            List<String> examples = entry.getValue();
            
            for (String example : examples) {
                // 获取 embedding
                double[] embeddingDouble = getEmbedding(example);
                // 转换为 float[]
                float[] embeddingFloat = new float[embeddingDouble.length];
                for (int i = 0; i < embeddingDouble.length; i++) {
                    embeddingFloat[i] = (float) embeddingDouble[i];
                }
                
                // 存储示例内容，agent_code 单独存储
                items.add(new Item(example, agentCode, embeddingFloat));
            }
        }
        
        // 批量插入
        if (!items.isEmpty()) {
            vectorStore.insertBatch(items);
            logger.info("成功插入 {} 条 Agent 示例", items.size());
        }
    }

    /**
     * 主入口：根据用户问题返回最匹配的Agent Code
     * 
     * @param userQuery 用户查询
     * @return 最匹配的 Agent Code，如果置信度低于阈值则返回 fallback_agent
     */
    public String route(String userQuery) {
        if (userQuery == null || userQuery.trim().isEmpty()) {
            return "fallback_agent";
        }

        try {
            // 检查向量数据库是否可用
            if (vectorStore == null) {
                logger.error("向量数据库未初始化");
                return "fallback_agent";
            }
            
            // 获取用户查询的 embedding
            double[] queryVecDouble = getEmbedding(userQuery);
            // 转换为 float[]
            float[] queryVec = new float[queryVecDouble.length];
            for (int i = 0; i < queryVecDouble.length; i++) {
                queryVec[i] = (float) queryVecDouble[i];
            }
            
            // 从向量数据库查询最相似的示例 (查询 top 5)
            List<Result> results = vectorStore.queryTopK(queryVec, 5);
            
            if (results.isEmpty()) {
                return "fallback_agent";
            }
            
            // 获取相似度最高的结果
            Result topResult = results.get(0);
            double bestScore = topResult.sim;
            
            // 检查置信度阈值
            if (bestScore < THRESHOLD) {
                logger.debug("置信度 {} 低于阈值 {}，返回 fallback_agent", bestScore, THRESHOLD);
                return "fallback_agent";
            }
            
            // 直接从结果中获取 agent code
            String agentCode = topResult.agentCode;
            
            if (agentCode == null || agentCode.trim().isEmpty()) {
                return "fallback_agent";
            }
            
            logger.info("匹配到 Agent: {} (置信度: {})", agentCode, String.format("%.2f", bestScore));
            return agentCode;
            
        } catch (Exception e) {
            logger.error("路由失败: {}", e.getMessage(), e);
            return "fallback_agent";
        }
    }
    

    /**
     * 调用 OpenAI Embedding API，返回向量
     * 使用缓存机制避免重复调用
     */
    private double[] getEmbedding(String text) throws Exception {
        // 检查缓存
        if (embeddingCache.containsKey(text)) {
            return embeddingCache.get(text);
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("OpenAI API Key 未配置");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("input", text);
        body.put("model", EMBEDDING_MODEL);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri("/embeddings")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(status -> status.isError(), resp -> {
                        logger.error("OpenAI API 返回错误状态: {}", resp.statusCode());
                        return resp.bodyToMono(String.class)
                                .flatMap(bodyContent -> {
                                    logger.error("错误响应: {}", bodyContent);
                                    return Mono.error(
                                        new RuntimeException("OpenAI API 调用失败: " + resp.statusCode() + " - " + bodyContent));
                                });
                    })
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                throw new RuntimeException("OpenAI API 返回空响应");
            }

            JsonNode json = objectMapper.valueToTree(response);
            
            if (!json.has("data") || !json.get("data").isArray() || json.get("data").size() == 0) {
                throw new RuntimeException("OpenAI API 返回数据异常: " + json.toString());
            }

            JsonNode embeddingArray = json.get("data").get(0).get("embedding");
            double[] embedding = new double[embeddingArray.size()];
            for (int i = 0; i < embeddingArray.size(); i++) {
                embedding[i] = embeddingArray.get(i).asDouble();
            }

            // 存入缓存
            embeddingCache.put(text, embedding);
            
            return embedding;
        } catch (Exception e) {
            logger.error("调用 OpenAI API 失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        embeddingCache.clear();
    }

    /**
     * 获取缓存大小
     */
    public int getCacheSize() {
        return embeddingCache.size();
    }

    /**
     * 设置置信度阈值
     */
    public void setThreshold(double threshold) {
        // 阈值设置方法
    }

    /**
     * 获取所有可用的 Agent
     */
    public Set<String> getAvailableAgents() {
        if (AGENT_EXAMPLES == null) {
            return Collections.emptySet();
        }
        return AGENT_EXAMPLES.keySet();
    }

    /**
     * 示例测试
     */
    public static void main(String[] args) {
        // 注意：此方法需要配置了 OpenAI API Key 才能正常运行
    }
}

