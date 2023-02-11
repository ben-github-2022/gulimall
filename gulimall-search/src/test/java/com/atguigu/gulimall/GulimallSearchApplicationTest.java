package com.atguigu.gulimall;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gulimall.search.GulimallSearchApplication;
import com.atguigu.gulimall.search.config.GulimallElasticSearchConfiguration;
import lombok.Data;
import lombok.ToString;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = GulimallSearchApplication.class)
public class GulimallSearchApplicationTest {

    @Autowired
    private RestHighLevelClient restClient;


    @Test
    public void esTest(){
        System.out.println(restClient);
    }

    @Test
    public void searchData() throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("bank");

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchQuery("address","mill"));
        TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age").size(10);
        searchSourceBuilder.aggregation(ageAgg);

        AvgAggregationBuilder balanceAvg = AggregationBuilders.avg("balanceAvg").field("balance");
        searchSourceBuilder.aggregation(balanceAvg);


        System.out.println(searchSourceBuilder.toString());
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = restClient.search(searchRequest, GulimallElasticSearchConfiguration.COMMON_OPTIONS);
        System.out.println(searchResponse.toString());
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit searchHit : searchHits) {
           // searchHit.getId();
            String sourceAsString = searchHit.getSourceAsString();
            Account account = JSON.parseObject(sourceAsString, Account.class);
            System.out.println(account.toString());
        }

    }




    @Test
    public void indexData() throws IOException {
        IndexRequest indexRequest=new IndexRequest("users");
        indexRequest.id("1");
        User user= new User();
        user.setAge(18);
        user.setGender("Male");
        user.setName("zhangsan");
        String userJson=JSON.toJSONString(user);
        indexRequest.source(userJson, XContentType.JSON);

        IndexResponse indexResponse = restClient.index(indexRequest, GulimallElasticSearchConfiguration.COMMON_OPTIONS);

        System.out.println(indexResponse);

    }

    @Data
    class User{
        private String name;
        private int age;
        private String gender;
    }

    /**
     * Copyright 2023 json.cn
     */
    /**
     * Auto-generated: 2023-01-04 20:59:22
     *
     * @author json.cn (i@json.cn)
     * @website http://www.json.cn/java2pojo/
     */
    @Data
    @ToString
    public static class Account {

        private int account_number;
        private int balance;
        private String firstname;
        private String lastname;
        private int age;
        private String gender;
        private String address;
        private String employer;
        private String email;
        private String city;
        private String state;

    }

}
