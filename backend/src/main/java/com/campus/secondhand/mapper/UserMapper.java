package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.secondhand.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 统计含已逻辑删除行的记录数（绕过逻辑删除过滤）。
     * 用户名/学号唯一性需覆盖已注销账号，否则残留行会导致同名注册报数据库异常。
     */
    @Select("SELECT COUNT(*) FROM t_user WHERE username = #{username}")
    long countByUsernameIncludeDeleted(@Param("username") String username);

    @Select("SELECT COUNT(*) FROM t_user WHERE student_id = #{studentId}")
    long countByStudentIdIncludeDeleted(@Param("studentId") String studentId);
}
