package com.campus.secondhand.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.secondhand.entity.Address;

import java.util.List;

public interface AddressService extends IService<Address> {

    List<Address> listMine();

    void add(Address address);

    void update(Long id, Address address);

    void delete(Long id);

    void setDefault(Long id);
}
