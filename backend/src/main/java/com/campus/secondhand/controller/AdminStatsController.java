package com.campus.secondhand.controller;

import com.alibaba.excel.EasyExcel;
import com.campus.secondhand.common.Result;
import com.campus.secondhand.service.SalesStatsService;
import com.campus.secondhand.vo.SalesRowVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 销售统计接口（管理员）
 */
@RestController
@RequestMapping("/admin/stats")
public class AdminStatsController {

    @Autowired
    private SalesStatsService salesStatsService;

    /** 销售统计（汇总+明细） */
    @GetMapping("/sales")
    public Result<Map<String, Object>> sales(@RequestParam(defaultValue = "day") String period,
                                             @RequestParam(required = false) String startDate,
                                             @RequestParam(required = false) String endDate) {
        return Result.success(salesStatsService.salesStats(period, startDate, endDate));
    }

    /** 导出销售明细 Excel */
    @GetMapping("/sales/export")
    public void export(HttpServletResponse response,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate) throws Exception {
        List<SalesRowVO> rows = salesStatsService.salesRows(startDate, endDate);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String filename = URLEncoder.encode("销售统计_" + System.currentTimeMillis(), StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment;filename=" + filename + ".xlsx");
        EasyExcel.write(response.getOutputStream(), SalesRowVO.class).sheet("销售明细").doWrite(rows);
    }
}
