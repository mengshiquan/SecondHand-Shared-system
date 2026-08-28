package com.campus.secondhand.controller;

import com.campus.secondhand.common.Result;
import com.campus.secondhand.entity.Address;
import com.campus.secondhand.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收货地址接口
 */
@RestController
@RequestMapping("/address")
public class AddressController {

    @Autowired
    private AddressService addressService;

    /** 地址列表 */
    @GetMapping("/list")
    public Result<List<Address>> list() {
        return Result.success(addressService.listMine());
    }

    /** 新增地址 */
    @PostMapping
    public Result<Void> add(@RequestBody Address address) {
        addressService.add(address);
        return Result.success();
    }

    /** 修改地址 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Address address) {
        addressService.update(id, address);
        return Result.success();
    }

    /** 删除地址 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.delete(id);
        return Result.success();
    }

    /** 设为默认 */
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(id);
        return Result.success();
    }
}
