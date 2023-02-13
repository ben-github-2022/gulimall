package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.dao.SkuInfoDao;
import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import com.atguigu.gulimall.product.service.*;
import com.atguigu.gulimall.product.vo.SeckillInfoVo;
import com.atguigu.gulimall.product.vo.SkuItemSaleAttrVo;
import com.atguigu.gulimall.product.vo.SkuItemVo;
import com.atguigu.gulimall.product.vo.SpuItemAttrGroupVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {

    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private SpuInfoDescService spuInfoDescService;


    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    private ThreadPoolExecutor executor;


/*    @Autowired
    private SeckillFeignService seckillFeignService;*/


    /**
     * 查出某一个我们spu的所有属性分组以及分组里面的所有属性
     */
    @Autowired
    private AttrGroupService attrGroupService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page (
                new Query<SkuInfoEntity> ().getPage (params),
                new QueryWrapper<SkuInfoEntity> ()
        );

        return new PageUtils (page);
    }

    @Override
    public void saveSkuInfo(SkuInfoEntity skuInfoEntity) {
        this.baseMapper.insert (skuInfoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {

        QueryWrapper<SkuInfoEntity> wrapper = new QueryWrapper<> ();


        String key = (String) params.get ("key");
        if (!StringUtils.isEmpty (key)) {
            wrapper.and ((w) -> {
                w.eq ("sku_id", key).or ().like ("sku_name", key);
            });
        }

        String catelogId = (String) params.get ("catelogId");
        if (!StringUtils.isEmpty (catelogId) && !"0".equalsIgnoreCase (catelogId)) {
            wrapper.eq ("catalog_id", catelogId);
        }


        String brandId = (String) params.get ("brandId");
        if (!StringUtils.isEmpty (brandId) && !"0".equalsIgnoreCase (brandId)) {
            wrapper.eq ("brand_id", brandId);
        }


        String min = (String) params.get ("min");
        if (!StringUtils.isEmpty (min)) {
            wrapper.ge ("price", min);
        }

        String max = (String) params.get ("max");
        if (!StringUtils.isEmpty (max)) {
            try {
                BigDecimal bigDecimal = new BigDecimal (max);
                if (bigDecimal.compareTo (new BigDecimal ("0")) == 1) {
                    wrapper.le ("price", max);
                }

            } catch (Exception e) {

            }
        }


        IPage<SkuInfoEntity> page = this.page (
                new Query<SkuInfoEntity> ().getPage (params),
                wrapper
        );
        return new PageUtils (page);
    }

    @Override
    public List<SkuInfoEntity> getSkuBySpuId(Long spuId) {
        return this.list (new QueryWrapper<SkuInfoEntity> ().eq ("spu_id", spuId));
    }


    /**
     * 1、infoFuture.thenAcceptAsync 都是并列的
     * 2、都的等   CompletableFuture<SkuInfoEntity> infoFuture = CompletableFuture.supplyAsync(() -> { 完成
     * <p>
     * <p>
     * 核心线程20个、只要任务执行完了、他们肯定都在那等待、新任务来就会直接执行、就不用创建线程了
     * 有了线程池和异步编排最终也能提升系统的性能以及吞吐量
     *
     * @param skuId
     * @return
     */

    @Override
    public SkuItemVo item(Long skuId) throws ExecutionException, InterruptedException {
        SkuItemVo skuItemVo = new SkuItemVo ();

        CompletableFuture<SkuInfoEntity> infoFuture = CompletableFuture.supplyAsync (() -> {
            /**①
             *  sku基本信息获取 pms_sku_info
             *          1、只要查出第一步、就知道三级分类id以及知道spuId
            */
            SkuInfoEntity info = getById (skuId);
            skuItemVo.setInfo (info);
            return info;
        }, executor);

        CompletableFuture<Void> saleAttrFuture = infoFuture.thenAcceptAsync ((res) -> {
            //③ 获取spu的销售属性组合
            List<SkuItemSaleAttrVo> skuItemSale = skuSaleAttrValueService.getSaleAttrsBySpuId (res.getSpuId ());
            skuItemVo.setSaleAttr (skuItemSale);
        }, executor);


        CompletableFuture<Void> descFuture = infoFuture.thenAcceptAsync ((res) -> {
            /**④
             * 获取spu的介绍 pms_spu_info_desc
             * 1、获取spu的id
             * 2、再根据spu的id查询spu的介绍
                    */

            SpuInfoDescEntity spuInfoDesc = spuInfoDescService.getById (res.getSpuId ());
            skuItemVo.setDesp (spuInfoDesc);
        }, executor);


        CompletableFuture<Void> baseAttrFuture = infoFuture.thenAcceptAsync ((res) -> {
           /**⑤
             * 获取spu的规格参数信息
             * 2、获取当前商品的所有属性分组以及属性值
            */

            List<SpuItemAttrGroupVo> attrGroupVos = attrGroupService.getAttrGroupWithAttrsBySpuId (res.getSpuId (), res.getCatalogId ());
            skuItemVo.setGroupAttrs (attrGroupVos);
        }, executor);

       CompletableFuture<Void> imagesFuture = CompletableFuture.runAsync (() -> {
            //② sku图片信息 pms_sku_images
            List<SkuImagesEntity> skuImages = skuImagesService.getImagesBySkuId (skuId);
            skuItemVo.setImages (skuImages);
        }, executor);


       /* //3、查询前sku是否参与秒杀优惠
        CompletableFuture<Void> seckillFuture = CompletableFuture.runAsync (() -> {
            R seckillInfo = seckillFeignService.getSkuSeckillInfo (skuId);
            if (seckillInfo.getCode () == 0) {
                SeckillInfoVo dta = seckillInfo.getDta (new TypeReference<SeckillInfoVo> () {
                });
                skuItemVo.setSeckillInfo (dta);
            }
        }, executor);*/

        System.out.println (skuItemVo.toString ());
        //等到所有任务都完成 可以不写这个 infoFuture
        CompletableFuture.allOf (saleAttrFuture, descFuture, baseAttrFuture, imagesFuture).get ();

//        while (true) {
//            System.out.println();
//
//            int queueSize = executor.getQueue().size();
//            System.out.println("当前排队线程数：" + queueSize);
//
//            int activeCount = executor.getActiveCount();
//            System.out.println("当前活动线程数：" + activeCount);
//
//            long completedTaskCount = executor.getCompletedTaskCount();
//            System.out.println("执行完成线程数：" + completedTaskCount);
//
//            long taskCount = executor.getTaskCount();
//            System.out.println("总线程数：" + taskCount);
//
//            Thread.sleep(3000);
//            break;
//        }

        return skuItemVo;


//    }    @Override
//    public SkuItemVo item(Long skuId) {
//        SkuItemVo skuItemVo = new SkuItemVo();
//
//
//
///**①
// *  sku基本信息获取 pms_sku_info
// *          1、只要查出第一步、就知道三级分类id以及知道spuId
// */
//        SkuInfoEntity info = getById(skuId);
//        skuItemVo.setInfo(info);
//
//        Long catalogId = info.getCatalogId();
//        Long spuId = info.getSpuId();
//
//        //② sku图片信息 pms_sku_images
//        List<SkuImagesEntity> skuImages = skuImagesService.getImagesBySkuId(skuId);
//        skuItemVo.setImages(skuImages);
//
//
//        //③ 获取spu的销售属性组合
//        List<SkuItemSaleAttrVo> skuItemSale = skuSaleAttrValueService.getSaleAttrsBySpuId(spuId);
//        skuItemVo.setSaleAttr(skuItemSale);
//        /**④
//         * 获取spu的介绍 pms_spu_info_desc
//         * 1、获取spu的id
//         * 2、再根据spu的id查询spu的介绍
//         */
////        Long spuId = info.getSpuId();
//        SpuInfoDescEntity spuInfoDesc = spuInfoDescService.getById(spuId);
//        skuItemVo.setDesp(spuInfoDesc);
//
//
//        /**⑤
//         * //获取spu的规格参数信息
//         * 2、获取当前商品的所有属性分组以及属性值
//         */
//        List<SpuItemAttrGroupVo> attrGroupVos = attrGroupService.getAttrGroupWithAttrsBySpuId(spuId, catalogId);
//        skuItemVo.setGroupAttrs(attrGroupVos);
//        return skuItemVo;
//    }


    }
}
