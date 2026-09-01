package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.entity.Favorite;
import com.campus.secondhand.vo.ProductVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    IPage<ProductVO> selectFavoritePage(Page<ProductVO> page, @Param("userId") Long userId, @Param("keyword") String keyword);

    /**
     * 物理删除收藏记录。表上有唯一索引 (user_id, product_id)，
     * 逻辑删除会残留行导致再次收藏时违反唯一约束，因此取消收藏必须物理删除。
     */
    @Delete("DELETE FROM t_favorite WHERE user_id = #{userId} AND product_id = #{productId}")
    int physicalDelete(@Param("userId") Long userId, @Param("productId") Long productId);
}
