package com.example.sseflux;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Test {

    public static void main(String[] args) throws Exception {
        new Test().swaggerDocTest();
    }

    public void swaggerDocTest() throws IOException {

        String swaggerUrl = "http://qa-s-core-eureka.patsnap.info/eureka/v2/api-docs?group=resourceApi";

        // 获取 swagger json
        String json = fetch(swaggerUrl);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);

        // 创建新的结果对象
        ObjectNode filtered = mapper.createObjectNode();
        filtered.setAll((ObjectNode) root);

        // 1. 过滤 paths
        ObjectNode pathsNode = (ObjectNode) root.get("paths");
        ObjectNode newPaths = mapper.createObjectNode();
        Set<String> usedDefinitions = new HashSet<>();

        for (Iterator<String> it = pathsNode.fieldNames(); it.hasNext(); ) {
            String path = it.next();
            if (path.contains("/material/docAnalyzer/getExtractMaterial")) {
                // 取出原始 path 节点
                ObjectNode pathNode = (ObjectNode) pathsNode.get(path);

                // 去掉 header 参数
                removeHeaderParameters(pathNode, mapper);

                // 放到新的 paths 中
                newPaths.set(path, pathNode);

                // 收集 definitions
                collectDefinitions(pathNode, usedDefinitions, root.get("definitions"));
            }
        }
        filtered.set("paths", newPaths);

        // 2. 过滤 definitions
        ObjectNode definitionsNode = (ObjectNode) root.get("definitions");
        ObjectNode newDefinitions = mapper.createObjectNode();
        for (String def : usedDefinitions) {
            if (definitionsNode.has(def)) {
                newDefinitions.set(def, definitionsNode.get(def));
            }
        }
        filtered.set("definitions", newDefinitions);

        // 3. 过滤 tags，只保留 name=Material API
        if (root.has("tags")) {
            ArrayNode tagsNode = (ArrayNode) root.get("tags");
            ArrayNode newTags = mapper.createArrayNode();
            for (JsonNode tag : tagsNode) {
                if (tag.has("name") && "Material API".equals(tag.get("name").asText())) {
                    newTags.add(tag);
                }
            }
            filtered.set("tags", newTags);
        }

        // 输出结果
        String result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(filtered);
        System.out.println(result);
    }

    private static String fetch(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        try (java.io.InputStream is = conn.getInputStream();
             java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A")) {
            return s.hasNext() ? s.next() : "";
        }
    }

    private static void collectDefinitions(JsonNode node, Set<String> usedDefinitions, JsonNode allDefinitions) {
        if (node == null) return;

        if (node.has("$ref")) {
            String ref = node.get("$ref").asText();
            if (ref.startsWith("#/definitions/")) {
                String defName = ref.replace("#/definitions/", "");
                if (usedDefinitions.add(defName)) { // 新增的 definition
                    JsonNode defNode = allDefinitions.get(defName);
                    if (defNode != null) {
                        // 递归收集该 definition 内部依赖
                        collectDefinitions(defNode, usedDefinitions, allDefinitions);
                    }
                }
            }
        }

        // 遍历子节点
        if (node.isContainerNode()) {
            for (JsonNode child : node) {
                collectDefinitions(child, usedDefinitions, allDefinitions);
            }
        }
    }

    /**
     * 删除 in=header 的参数
     */
    private static void removeHeaderParameters(ObjectNode pathNode, ObjectMapper mapper) {
        for (Iterator<String> it = pathNode.fieldNames(); it.hasNext(); ) {
            String method = it.next();
            JsonNode methodNode = pathNode.get(method);

            if (methodNode.has("parameters")) {
                ArrayNode params = (ArrayNode) methodNode.get("parameters");
                ArrayNode newParams = mapper.createArrayNode();

                for (JsonNode param : params) {
                    if (!param.has("in") || !"header".equals(param.get("in").asText())) {
                        newParams.add(param);
                    }
                }

                ((ObjectNode) methodNode).set("parameters", newParams);
            }
        }
    }
}
