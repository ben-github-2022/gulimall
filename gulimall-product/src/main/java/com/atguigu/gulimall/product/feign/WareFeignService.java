package com.atguigu.gulimall.product.feign;


import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("gulimall-ware")
public interface WareFeignService {

}
