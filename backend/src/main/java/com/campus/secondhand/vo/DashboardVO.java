package com.campus.secondhand.vo;

import lombok.Data;

/**
 * 后台管理仪表盘 VO
 */
@Data
public class DashboardVO {

    private Long userCount;
    private Long productCount;
    private Long orderCount;
    private Long todayOrderCount;
}
