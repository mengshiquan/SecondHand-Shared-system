package com.campus.secondhand.service.impl;

import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.mapper.OrderMapper;
import com.campus.secondhand.service.SalesStatsService;
import com.campus.secondhand.util.UserContext;
import com.campus.secondhand.vo.SalesRowVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 销售统计服务：基于已完成订单（status=COMPLETED，以 update_time 为成交时间）
 */
@Service
public class SalesStatsServiceImpl implements SalesStatsService {

    @Autowired
    private OrderMapper orderMapper;

    @Override
    public Map<String, Object> salesStats(String period, String startDate, String endDate) {
        checkAdmin();
        List<SalesRowVO> rows = orderMapper.selectSalesRows(parseStart(startDate, period), parseEnd(endDate));

        BigDecimal totalAmount = BigDecimal.ZERO;
        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
        Map<String, Integer> bySeller = new LinkedHashMap<>();
        for (SalesRowVO row : rows) {
            totalAmount = totalAmount.add(row.getPrice());
            String cat = row.getCategoryName() != null ? row.getCategoryName() : "未分类";
            byCategory.merge(cat, row.getPrice(), BigDecimal::add);
            bySeller.merge(row.getSellerNickname(), 1, Integer::sum);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCount", rows.size());
        result.put("totalAmount", totalAmount);
        result.put("byCategory", byCategory);
        result.put("bySeller", bySeller);
        result.put("rows", rows);
        return result;
    }

    @Override
    public List<SalesRowVO> salesRows(String startDate, String endDate) {
        checkAdmin();
        return orderMapper.selectSalesRows(parseStart(startDate, null), parseEnd(endDate));
    }

    private void checkAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException("无管理员权限");
        }
    }

    private LocalDateTime parseStart(String date, String period) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (date == null || date.isEmpty()) {
            LocalDate today = LocalDate.now();
            if ("week".equals(period)) return today.minusDays(7).atStartOfDay();
            if ("month".equals(period)) return today.minusDays(30).atStartOfDay();
            return today.atStartOfDay();
        }
        return LocalDate.parse(date, fmt).atStartOfDay();
    }

    private LocalDateTime parseEnd(String date) {
        if (date == null || date.isEmpty()) return LocalDateTime.now();
        return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atTime(LocalTime.MAX);
    }
}
