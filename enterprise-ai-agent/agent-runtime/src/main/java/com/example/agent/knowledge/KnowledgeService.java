package com.example.agent.knowledge;

import org.springframework.stereotype.Service;

/**
 * 知识库服务
 *
 * v3.0 知识库的索引和检索逻辑已拆分：
 * - 索引管理：KnowledgeController（手动触发，带状态检查）
 * - 向量检索：RetrievalService（语义搜索）
 *
 * 保留此类用于后续扩展（如：增量索引、文档格式解析等）
 */
@Service
public class KnowledgeService {

    // v3.0 的索引逻辑在 KnowledgeController 中
    // v3.0 的检索逻辑在 RetrievalService 中
    // 此类保留，后续课程扩展用

}
