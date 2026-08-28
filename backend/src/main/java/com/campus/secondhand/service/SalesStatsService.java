package com.campus.secondhand.service;

import com.campus.secondhand.vo.SalesRowVO;

import java.util.List;
import java.util.Map;

public interface SalesStatsService {

    /** 销售统计：汇总 + 明细。period: day/week/month（影响默认时间范围） */
    Map<String, Object> salesStats(String period, String startDate, String endDate);

    List<SalesRowVO> salesRows(String startDate, String endDate);
}
