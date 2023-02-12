package com.atguigu.gulimall.product;


import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.BrandService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallProductApplicationTests {

    @Autowired
    BrandService brandService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

 /*   @Autowired
    private OSSClient ossClient;
*/

    @Autowired
    CategoryService categoryService;

    @Autowired
    RedissonClient redissonClient;

    @Test
    public void redissonClientTest(){
        System.out.println(redissonClient);
    }



    @Test
    public void redisTest(){
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        opsForValue.set("hello","world"+ UUID.randomUUID().toString());
        String helloString = opsForValue.get("hello");
        System.out.println(helloString);
    }


 /*   @Test
    public void upload() throws IOException {

        InputStream inputStream=new FileInputStream("C:\\Users\\Ben\\Desktop\\dog.JPG");
        ossClient.putObject("gulimall-test-2022","dog.jpg",inputStream);

        ossClient.shutdown();
        inputStream.close();
        System.out.println("upload done....");

    }*/

    @Test
    public void testFindPath(){
        Long[] catelogPath = categoryService.findCatelogPath(225L);
        log.info("完整路径：{}",Arrays.asList(catelogPath));
    }


    @Test
    public void contextLoads() {

        BrandEntity brandEntity = new BrandEntity();
        brandEntity.setBrandId(1L);
        brandEntity.setDescript("华为");


        brandEntity.setName("华为");
        brandService.save(brandEntity);
        System.out.println("保存成功....");

        brandService.updateById(brandEntity);


        List<BrandEntity> list = brandService.list(new QueryWrapper<BrandEntity>().eq("brand_id", 1L));
        list.forEach((item) -> {
            System.out.println(item);
        });

    }

}
