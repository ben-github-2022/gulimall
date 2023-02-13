package com.atguigu.gulimall.product.vo;

import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * @Author 小坏
 * @Date 2020/12/22 11:45
 * @Version 1.0
 * @program: 父工程 gulimall 万物起源之地
 */

@Data
@ToString
public class SkuItemSaleAttrVo {
    private Long attrId;
    private String attrName;
    /*颜色的集合*/
    private List<AttrValueWithSkuIdVo> attrValues;
}
