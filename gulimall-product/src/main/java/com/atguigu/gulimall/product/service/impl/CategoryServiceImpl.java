package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.nacos.client.logging.logback.LogbackNacosLogging;
import com.atguigu.gulimall.product.entity.Catelog2Vo;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

//    @Autowired
//    CategoryDao categoryDao;

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    StringRedisTemplate redisTemplate;


    //查询所有categories
    //缓存中存放的是json格式的字符串，从redis获取json字符串后也需要转换成对应的对象
    //TODO redis堆外内存溢出：lettuce的bug，在使用netty通信时产生溢出
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        String catelogJson = redisTemplate.opsForValue().get("catelogJson");
        if (StringUtils.isEmpty(catelogJson)) {
            //缓存没有，需求去数据库查询
            //在高并发条件下，需要考虑高并发的请求问题，需要将getCatalogJsonFromDB方法加锁
            Map<String, List<Catelog2Vo>> catelogJsonFromDB = getCatalogJsonFromDB();

            //同时返回数据库的查询结果
            return catelogJsonFromDB;
        }

        //将redis返回的json字符串转换成为Map<String,List<Catelog2Vo>对象

        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catelogJson,
                new TypeReference<Map<String, List<Catelog2Vo>>>() {
                });

        //

        return result;
    }


    private Map<String, List<Catelog2Vo>> getCatalogJsonFromDBWithDistributedLock() {
        //给锁设置UUID
        String lockToken = UUID.randomUUID().toString();
        //第一步，占分布式锁，占redis锁（redis命令操作SETNX key value）,注意保证锁的过期时间和获取锁是一个原子操作
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", lockToken, 30, TimeUnit.SECONDS);
        if (lock == true) {

            try {
                return getCatalogJsonFromDB();
            } finally {
                //  删除锁:必须保证获取锁的值和删除是一个原子操作，需要使用lua脚本
 /*           String lockValue = redisTemplate.opsForValue().get("lock");o
            if(lockValue.equals(lockToken)){
                redisTemplate.delete("lock");//删除锁
            }*/
                String script = "if redis.call('get', KEYS[1]) == ARG[1] then return" +
                        " redis.call('del', KEYS[1]) else return 0 end";
                redisTemplate.execute(new DefaultRedisScript<Long>(script,Long.class), Arrays.asList("lock"),
                        lockToken);
            }
            //说明对象占锁成功，可以执行业务操作，需要解锁
            //给锁设置过期时间
            //   redisTemplate.expire("lock",30, TimeUnit.SECONDS);// 该位置设置过期时间不合适，需要保证获取锁和设置过期时间是一个原子操作
        } else {
            //加锁失败，需要重试机制
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return getCatalogJsonFromDBWithDistributedLock(); //自旋重试
        }


        //TODO 编写获取二级和三级菜单的数据，以json格式返回

    }

    private Map<String, List<Catelog2Vo>> getCatalogJsonFromDBWithLocalLock() {

        //本地锁不能解决分布式问题
        synchronized (this) {
            String catelogJson = redisTemplate.opsForValue().get("catelogJson");
            //即使某个对象争取到锁，应该先再去缓存查询一下是否已经被之前获取多锁的对象把查询到的数据放进了缓存
            if (redisTemplate.opsForValue().get("catelogJson") == null) {
                //执行数据库查询，需要保证从数据库查询结果，将结果放入缓存时一个原子操作
                //TODO 需要完成查询逻辑
                Map<String, List<Catelog2Vo>> catelogJsonFromDB = new HashMap<>();
                //将查询的结果转换为json对象再放入redis
                String toJSONString = JSON.toJSONString(catelogJsonFromDB);
                redisTemplate.opsForValue().set("catelogJson", toJSONString);
                return catelogJsonFromDB;
            } else {
                Map<String, List<Catelog2Vo>> result = JSON.parseObject(catelogJson,
                        new TypeReference<Map<String, List<Catelog2Vo>>>() {
                        });

            }
        }


        //TODO 编写获取二级和三级菜单的数据，以json格式返回
        return null;
    }

    private Map<String, List<Catelog2Vo>> getCatalogJsonFromDB() {

        //本地锁不能解决分布式问题
        synchronized (this) {
            String catelogJson = redisTemplate.opsForValue().get("catelogJson");
            //即使某个对象争取到锁，应该先再去缓存查询一下是否已经被之前获取多锁的对象把查询到的数据放进了缓存
            if (redisTemplate.opsForValue().get("catelogJson") == null) {
                //执行数据库查询，需要保证从数据库查询结果，将结果放入缓存时一个原子操作
                //TODO 需要完成查询逻辑
                Map<String, List<Catelog2Vo>> catelogJsonFromDB = new HashMap<>();
                //将查询的结果转换为json对象再放入redis
                String toJSONString = JSON.toJSONString(catelogJsonFromDB);
                redisTemplate.opsForValue().set("catelogJson", toJSONString);
                return catelogJsonFromDB;
            } else {
                Map<String, List<Catelog2Vo>> result = JSON.parseObject(catelogJson,
                        new TypeReference<Map<String, List<Catelog2Vo>>>() {
                        });

            }
        }


        //TODO 编写获取二级和三级菜单的数据，以json格式返回
        return null;
    }


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //1、查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);

        //2、组装成父子的树形结构

        //2.1）、找到所有的一级分类
        List<CategoryEntity> level1Menus = entities.stream().filter(categoryEntity ->
                categoryEntity.getParentCid() == 0
        ).map((menu) -> {
            menu.setChildren(getChildrens(menu, entities));
            return menu;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());


        return level1Menus;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO  1、检查当前删除的菜单，是否被别的地方引用

        //逻辑删除
        baseMapper.deleteBatchIds(asList);
    }

    //[2,25,225]
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);

        Collections.reverse(parentPath);


        return parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * 级联更新所有关联的数据
     *
     * @param category
     */
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }


    @CacheEvict(value = "category",key = "'categoryEntities'")
    @Cacheable(value={"category","product"},key="'categoryEntities'") //表示该方法的返回结果会存入缓存，如果缓存有数据，该方法不会调用。
                                      // 同时可以用数组形式知道多个缓存分区名字
    @Override
    public List<CategoryEntity> getLevelOneCategories() {
        System.out.println("获取1级菜单的方法被调用了。。。。");
        List<CategoryEntity> categoryEntities = baseMapper.
                selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return categoryEntities;
    }


    //225,25,2
    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        //1、收集当前节点id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid() != 0) {
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;

    }


    //递归查找所有菜单的子菜单
    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all) {

        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            return categoryEntity.getParentCid() == root.getCatId();
        }).map(categoryEntity -> {
            //1、找到子菜单
            categoryEntity.setChildren(getChildrens(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            //2、菜单的排序
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());

        return children;
    }


}