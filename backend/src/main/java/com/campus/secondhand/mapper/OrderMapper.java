package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.entity.Order;
import com.campus.secondhand.vo.OrderVO;
import com.campus.secondhand.vo.SalesRowVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    IPage<OrderVO> selectOrderPage(Page<OrderVO> page,
                                   @Param("userId") Long userId,
                                   @Param("role") String role,
                                   @Param("status") String status,
                                   @Param("refundStatus") String refundStatus);

    OrderVO selectOrderDetail(@Param("id") Long id);

    List<SalesRowVO> selectSalesRows(@Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);
}
