package com.example.vector;

import java.sql.*;
import java.util.*;
import com.google.gson.Gson;

/**
 * 轻量级本地向量数据库（基于 SQLite）
 * 特点：
 *  - 向量以 JSON 字符串存储
 *  - 支持插入文本 + 向量
 *  - 支持按余弦相似度查询最相近内容
 *  - 无需任何扩展（仅 sqlite-jdbc + gson）
 */
public class LocalVectorStore {

    private final Connection conn;
    private final Gson gson = new Gson();

    public LocalVectorStore(String dbPath) throws SQLException {
        this.conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        initTable();
    }

    // 创建表
    private void initTable() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS embeddings (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "content TEXT, " +
                    "agent_code TEXT, " +
                    "embedding TEXT)");
        }
    }

    // 插入一条向量记录
    public void insert(String content, String agentCode, float[] embedding) throws SQLException {
        String json = gson.toJson(embedding);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO embeddings (content, agent_code, embedding) VALUES (?, ?, ?)")) {
            ps.setString(1, content);
            ps.setString(2, agentCode);
            ps.setString(3, json);
            ps.executeUpdate();
        }
    }

    // 批量插入
    public void insertBatch(List<Item> items) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO embeddings (content, agent_code, embedding) VALUES (?, ?, ?)")) {
            for (Item item : items) {
                ps.setString(1, item.content);
                ps.setString(2, item.agentCode);
                ps.setString(3, gson.toJson(item.embedding));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // 查询前K个最相似项
    public List<Result> queryTopK(float[] queryVec, int k) throws SQLException {
        List<Result> results = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, content, agent_code, embedding FROM embeddings")) {

            while (rs.next()) {
                float[] emb = gson.fromJson(rs.getString("embedding"), float[].class);
                double sim = cosineSim(queryVec, emb);
                results.add(new Result(rs.getInt("id"), rs.getString("content"), 
                    rs.getString("agent_code"), sim));
            }
        }
        results.sort((a, b) -> Double.compare(b.sim, a.sim)); // 按相似度降序
        return results.size() > k ? results.subList(0, k) : results;
    }

    // 检查数据库中是否有数据
    public boolean hasData() throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM embeddings")) {
            if (rs.next()) {
                int count = rs.getInt("count");
                return count > 0;
            }
            return false;
        }
    }

    public void close() throws SQLException {
        conn.close();
    }

    // ========= 辅助数据结构 =========
    public static class Item {
        public String content;
        public String agentCode;
        public float[] embedding;
        public Item(String c, String a, float[] e) { this.content = c; this.agentCode = a; this.embedding = e; }
    }

    public static class Result {
        public int id;
        public String content;
        public String agentCode;
        public double sim;
        public Result(int id, String c, String a, double s) { 
            this.id = id; 
            this.content = c; 
            this.agentCode = a;
            this.sim = s; 
        }
    }

    // ========= 工具函数 =========
    private static double cosineSim(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}

