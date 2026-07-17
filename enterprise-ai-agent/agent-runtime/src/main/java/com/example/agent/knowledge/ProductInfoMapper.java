package com.example.agent.knowledge;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 商品结构化信息 Mapper — MySQL 查询
 *
 * 负责查询价格、库存、销量等频繁变化的业务数据。
 * 这些数据不适合放向量数据库（会过时），需要实时查询。
 */
@Mapper
public interface ProductInfoMapper extends BaseMapper<ProductInfo> {

    /**
     * 根据商品 ID 列表批量查询
     * 用于：向量检索返回相关商品 ID 后，查询实时业务数据
     */
    List<ProductInfo> selectByIds(List<String> ids);

    /**
     * 按类别查询在售商品
     */
    List<ProductInfo> selectByCategory(String category);

    /**
     * 查询所有在售商品
     */
    List<ProductInfo> selectOnSale();
}
