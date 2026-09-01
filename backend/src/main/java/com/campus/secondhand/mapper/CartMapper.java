package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.secondhand.entity.Cart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CartMapper extends BaseMapper<Cart> {

    /**
     * 物理删除购物车项。表上有唯一索引 (user_id, product_id)，
     * 逻辑删除会残留行导致再次加购时违反唯一约束。
     */
    @Delete("DELETE FROM t_cart WHERE user_id = #{userId} AND product_id = #{productId}")
    int physicalDelete(@Param("userId") Long userId, @Param("productId") Long productId);
}
