package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    StringRedisTemplate redisTemplate;


    // index/catalog.json
    @ResponseBody
    @GetMapping("/index/catalog.json")
    public Map<String,List<Catelog2Vo>> getCatalogJson(){

        Map<String,List<Catelog2Vo>> catalogJson= categoryService.getCatalogJson();

        return catalogJson;
    }



    @ResponseBody
    @GetMapping("/write")
    public String writeValue(){
        RReadWriteLock myReadWriteLock = redissonClient.getReadWriteLock("myReadWriteLock");
        RLock wLock = myReadWriteLock.writeLock();
        String value ="";
        try{
            wLock.lock();
            value = UUID.randomUUID().toString();
            Thread.sleep(3000);
            redisTemplate.opsForValue().set("value",value);
        }catch (Exception e){
           e.printStackTrace();
        }finally {
            wLock.unlock();
        }
        return value;
    }

    @ResponseBody
    @GetMapping("/read")
    public String readValue(){
        RReadWriteLock myReadWriteLock = redissonClient.getReadWriteLock("myReadWriteLock");
        RLock rLock = myReadWriteLock.readLock();
        String value ="";
        rLock.lock();
        try{
            value = redisTemplate.opsForValue().get("value");
        }catch (Exception e){

        }finally {
            rLock.unlock();
        }

        return value;
    }



    @ResponseBody
    @GetMapping("/hello")
    public String hello() {
        RLock myLock = redissonClient.getLock("myLock");
        myLock.lock(); //阻塞式等到
        try {
            //占锁成功，执行业务代码
            System.out.println("加锁成功");
            Thread.sleep(3000);
        } catch (Exception e){
            e.printStackTrace();
        } finally{
            myLock.unlock();
            System.out.println("解锁成功");
        }
        return "hello";
    }

    @GetMapping({"/", "/index.html"})
    public String indexPage(Model model) {

        // TODO 查出所有一级分类
        List<CategoryEntity> categoryEntities = categoryService.getLevelOneCategories();


        // thymeleaf 默认前缀"classpath:/templates/";
        // thymeleaf 默认后缀  private String suffix = ".html";
        // 视图解析器会进行拼接
        model.addAttribute("categories", categoryEntities);
        return "index";
    }
}
