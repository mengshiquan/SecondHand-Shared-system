package com.campus.secondhand.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.secondhand.entity.Address;

import java.util.List;

public interface AddressService extends IService<Address> {

    /**
     * 查询当前用户收货地址，默认地址优先，其次按更新时间倒序。
     */
    List<Address> listMine();

    /**
     * 新增当前用户地址；单个用户上限 10 条，首个地址自动设为默认。
     */
    void add(Address address);

    /**
     * 更新本人地址；仅在目标地址不存在或不属于当前用户时抛出业务异常。
     */
    void update(Long id, Address address);

    /**
     * 逻辑删除本人地址；若删除默认地址，会自动指定最近地址为默认。
     */
    void delete(Long id);

    /**
     * 将本人指定地址设为唯一默认地址。
     */
    void setDefault(Long id);
}
