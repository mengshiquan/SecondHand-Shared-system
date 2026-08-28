package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.entity.Address;
import com.campus.secondhand.mapper.AddressMapper;
import com.campus.secondhand.service.AddressService;
import com.campus.secondhand.util.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 收货地址服务实现
 */
@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements AddressService {

    private static final int MAX_PER_USER = 10;

    @Override
    public List<Address> listMine() {
        return list(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, UserContext.getUserId())
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getUpdateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Address address) {
        Long userId = UserContext.getUserId();
        long count = count(new LambdaQueryWrapper<Address>().eq(Address::getUserId, userId));
        if (count >= MAX_PER_USER) {
            throw new BusinessException("最多保存 " + MAX_PER_USER + " 个收货地址");
        }
        address.setId(null);
        address.setUserId(userId);
        // 第一个地址自动设为默认
        if (count == 0) {
            address.setIsDefault(1);
        } else if (Integer.valueOf(1).equals(address.getIsDefault())) {
            clearDefault(userId);
        } else {
            address.setIsDefault(0);
        }
        save(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, Address address) {
        Address exist = getOwned(id);
        exist.setReceiverName(address.getReceiverName());
        exist.setPhone(address.getPhone());
        exist.setAddress(address.getAddress());
        if (Integer.valueOf(1).equals(address.getIsDefault())
                && !Integer.valueOf(1).equals(exist.getIsDefault())) {
            clearDefault(exist.getUserId());
            exist.setIsDefault(1);
        }
        updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Address exist = getOwned(id);
        removeById(id);
        // 删除默认地址后，自动把最近一个设为默认
        if (Integer.valueOf(1).equals(exist.getIsDefault())) {
            Address latest = getOne(new LambdaQueryWrapper<Address>()
                    .eq(Address::getUserId, exist.getUserId())
                    .orderByDesc(Address::getUpdateTime)
                    .last("LIMIT 1"));
            if (latest != null) {
                latest.setIsDefault(1);
                updateById(latest);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        Address exist = getOwned(id);
        clearDefault(exist.getUserId());
        exist.setIsDefault(1);
        updateById(exist);
    }

    private Address getOwned(Long id) {
        Address exist = getById(id);
        if (exist == null || !exist.getUserId().equals(UserContext.getUserId())) {
            throw new BusinessException("地址不存在");
        }
        return exist;
    }

    private void clearDefault(Long userId) {
        update(new LambdaUpdateWrapper<Address>()
                .eq(Address::getUserId, userId)
                .set(Address::getIsDefault, 0));
    }
}
