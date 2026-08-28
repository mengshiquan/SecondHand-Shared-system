package com.campus.secondhand.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售统计明细行（页面表格与 Excel 导出共用）
 */
@Data
public class SalesRowVO {

    @ExcelProperty("成交时间")
    private LocalDateTime dealTime;

    @ExcelProperty("订单编号")
    private String orderNo;

    @ExcelProperty("商品标题")
    private String productTitle;

    @ExcelProperty("分类")
    private String categoryName;

    @ExcelProperty("卖家")
    private String sellerNickname;

    @ExcelProperty("买家")
    private String buyerNickname;

    @ExcelProperty("成交价")
    private BigDecimal price;
}
