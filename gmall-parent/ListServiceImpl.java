package com.atguigu.gmall.list.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.list.dao.GoodsDao;
import com.atguigu.gmall.list.service.ListService;
import com.atguigu.gmall.model.list.*;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ListServiceImpl implements ListService {

    //1、ES官方客户端（低级别）
    //1、ES官方客户端（高级别）
    @Autowired
    private RestHighLevelClient restHighLevelClient;
    //2、Spring 官方客户端 底层封装了ES官方客户端（低级别）
    //3、Spring 官方客户端（高级别）
    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;//一般用于查询
    @Autowired
    private GoodsDao goodsDao;//一般用于增删改
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;

    //创建索引库
    //mappings 映射
    @Override
    public void createIndex() {
        //创建索引库
        elasticsearchRestTemplate.createIndex(Goods.class);
        //创建mapping（映射）
        elasticsearchRestTemplate.putMapping(Goods.class);

        System.out.println("创建索引库和映射" + elasticsearchRestTemplate.toString());
    }

    //上架 添加商品数据到ES索引库
    @Override
    public void onSale(Long skuId) {
        Goods goods = new Goods();
        //将goods中所需要的数据查询处理 并设置保存到goods中
        //1、库存信息
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);

        //2、库存Id
        goods.setId(skuId);
        //3、图片地址
        goods.setDefaultImg(skuInfo.getSkuDefaultImg());
        //4、时间
        goods.setCreateTime(new Date());

        //5、价格
        goods.setPrice(skuInfo.getPrice().doubleValue());

        //6、title
        goods.setTitle(skuInfo.getSkuName());

        //7、一二三级分类
        BaseCategoryView baseCategoryView = productFeignClient.getBaseCategoryView(skuInfo.getCategory3Id());
        goods.setCategory1Id(baseCategoryView.getCategory1Id());
        goods.setCategory1Name(baseCategoryView.getCategory1Name());
        goods.setCategory2Id(baseCategoryView.getCategory2Id());
        goods.setCategory2Name(baseCategoryView.getCategory2Name());
        goods.setCategory3Id(baseCategoryView.getCategory3Id());
        goods.setCategory3Name(baseCategoryView.getCategory3Name());

        //8、品牌
        BaseTrademark baseTrademarkById = productFeignClient.getBaseTrademarkById(skuInfo.getTmId());
        goods.setTmId(baseTrademarkById.getId());
        goods.setTmLogoUrl(baseTrademarkById.getLogoUrl());
        goods.setTmName(baseTrademarkById.getTmName());

        //9、skuInfo与品牌属性值的关联数据
        List<SkuAttrValue> skuAttrValueList = productFeignClient.getSkuAttrValueList(skuId);
        List<SearchAttr> attrList = skuAttrValueList.stream().map(skuAttrValue -> {
            SearchAttr searchAttr = new SearchAttr();
            searchAttr.setAttrId(skuAttrValue.getBaseAttrInfo().getId());
            searchAttr.setAttrName(skuAttrValue.getBaseAttrInfo().getAttrName());
            searchAttr.setAttrValue(skuAttrValue.getBaseAttrValue().getValueName());
            return searchAttr;
        }).collect(Collectors.toList());
        goods.setAttrs(attrList);

        //保存索引
        goodsDao.save(goods);
    }

    //下架 删除ES索引库中色商品数据
    @Override
    public void canselSale(Long skuId) {
        goodsDao.deleteById(skuId);
    }

    //商品热度评分 更新操作
    @Override
    public void hotScore(Long skuId, Integer score) {
        String hotScore = "hotScore";

        Double score1 = redisTemplate.opsForZSet().incrementScore(hotScore, skuId, score);
        if (score1 % 10 == 0) {//10,20...
            //达到阈值10分 跟新一次操作
            //1、查询索引库中数据
            Optional<Goods> byId = goodsDao.findById(skuId);
            Goods goods = byId.get();
            //2、追加分数
            goods.setHotScore(Math.round(score1));//取整
            //3、跟新索引库中的数据
            goodsDao.save(goods);
            System.out.println("热度评分更新了");
        }
        System.out.println("当前商品 ： " + skuId + "热度评分：" + score1);
    }

    //开始搜索
    @Override
    public SearchResponseVo search(SearchParam searchParam) {

        //1、搜索请求对象
        SearchRequest searchRequest = buildSearchRequest(searchParam);

        //2、开始搜索
        try {
            SearchResponse searchResponse = restHighLevelClient.
                    search(searchRequest, RequestOptions.DEFAULT);

            //3、解析搜索结果
            SearchResponseVo vo = parseSearchResponse(searchResponse);

            //3、1计算总页数
            //总页数 = （总条数 + 每页数 - 1） / 每页数
            vo.setPageNo(searchParam.getPageNo());
            vo.setPageSize(searchParam.getPageSize());
            long totalPages = (vo.getTotal() + vo.getPageSize() - 1) / vo.getPageSize();
            vo.setTotalPages(totalPages);
            return vo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //解析搜索结果
    private SearchResponseVo parseSearchResponse(SearchResponse searchResponse) {
        SearchResponseVo vo = new SearchResponseVo();
        //1、总条数
        SearchHits hits = searchResponse.getHits();
        long totalHits = hits.getTotalHits();
        System.out.println("总条数 = " + totalHits);
        vo.setTotal(totalHits);

        //2、商品集合
        SearchHit[] hits1 = hits.getHits();
        List<Goods> goodsList = Arrays.stream(hits1).map(hit -> {
            //商品信息
            String sourceAsString = hit.getSourceAsString();
            Goods goods = JSONObject.parseObject(sourceAsString, Goods.class);

            //获取高亮
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (highlightFields != null && highlightFields.size() > 0) {
                HighlightField highlightField = highlightFields.get("title");
                String highlighTitle = highlightField.getFragments()[0].toString();
                goods.setTitle(highlighTitle);
            }
            return goods;
        }).collect(Collectors.toList());
        vo.setGoodsList(goodsList);

        //3、品牌集合
        ParsedLongTerms tmIdAgg = searchResponse.getAggregations().get("tmIdAgg");

        List<SearchResponseTmVo> trademarkList = tmIdAgg.getBuckets().stream().map(bucket -> {
            SearchResponseTmVo tmVo = new SearchResponseTmVo();

            //品牌ID
            tmVo.setTmId(bucket.getKeyAsNumber().longValue());

            //品牌名称
            ParsedStringTerms tmNameAgg = ((Terms.Bucket) bucket).getAggregations().get("tmNameAgg");
            tmVo.setTmName(tmNameAgg.getBuckets().get(0).getKeyAsString());

            //品牌的Logo
            ParsedStringTerms tmLogoUrlAgg = ((Terms.Bucket) bucket).getAggregations().get("tmLogoUrlAgg");
            tmVo.setTmLogoUrl(tmLogoUrlAgg.getBuckets().get(0).getKeyAsString());
            return tmVo;
        }).collect(Collectors.toList());

        vo.setTrademarkList(trademarkList);


        //4、平台属性集合
        ParsedNested attrsAgg = searchResponse.getAggregations().get("attrsAgg");

        ParsedLongTerms attrIdAgg = attrsAgg.getAggregations().get("attrIdAgg");

        List<SearchResponseAttrVo> attrVoList = attrIdAgg.getBuckets().stream().map(bucket -> {
            SearchResponseAttrVo attrVo = new SearchResponseAttrVo();

            //平台属性ID
            attrVo.setAttrId(bucket.getKeyAsNumber().longValue());

            //平台属性名称
            ParsedStringTerms attrNameAgg = ((Terms.Bucket) bucket).getAggregations().get("attrNameAgg");
            attrVo.setAttrName(attrNameAgg.getBuckets().get(0).getKeyAsString());

            //平台属性值集合
            ParsedStringTerms attrValueAgg = ((Terms.Bucket) bucket).getAggregations().get("attrValueAgg");
            List<String> attrValueList = attrValueAgg.getBuckets().stream().map(Terms.Bucket::getKeyAsString)
                    .collect(Collectors.toList());

            attrVo.setAttrValueList(attrValueList);

            return attrVo;
        }).collect(Collectors.toList());

        vo.setAttrsList(attrVoList);

        return vo;
    }

    //构建搜索请求对象

private SearchRequest buildSearchRequest(SearchParam searchParam) {
        //1、创建搜索请求对象  DSL：GET goods/_search GET请求
        SearchRequest searchRequest = new SearchRequest();

        //2、构建搜索资源对象 ： {}
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //组合查询条件的对象
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();


        //2.1 查询关键词
        String keyword = searchParam.getKeyword();
        if (!StringUtils.isEmpty(keyword)) {
            //matchQuery ：先分词在模糊查询
            //termQuety ： 精确查询
            boolQueryBuilder.must(QueryBuilders.matchQuery("title", keyword)
                    .operator(Operator.AND));
        } else {
            //没有关键词
            //查询全部索引库
            boolQueryBuilder.must(QueryBuilders.matchAllQuery());
        }

        //2.2 查询一二三级分类 termQuety ： 精确查询
        Long category1Id = searchParam.getCategory1Id();
        if (category1Id != null) {
            // k :域名  v :域值    filter == must
            boolQueryBuilder.filter(QueryBuilders.termQuery("category1Id", category1Id));
        }

        Long category2Id = searchParam.getCategory2Id();
        if (category2Id != null) {
            // k :域名  v :域值    filter == must
            boolQueryBuilder.filter(QueryBuilders.termQuery("category2Id", category2Id));
        }

        Long category3Id = searchParam.getCategory3Id();
        if (category3Id != null) {
            // k :域名  v :域值    filter == must
            boolQueryBuilder.filter(QueryBuilders.termQuery("category3Id", category3Id));
        }


        //2.3 品牌搜索 精准查询  品牌ID：品牌名称
        String trademark = searchParam.getTrademark();
        if (!StringUtils.isEmpty(trademark)) {
            String[] t = trademark.split(":"); //0 : id  1: 名称
            boolQueryBuilder.filter(QueryBuilders.termQuery("tmId", t[0]));
        }

        // 2.4 平台属性
        String[] props = searchParam.getProps();
        if (props != null && props.length > 0) {
            //props = 23:4G:运行内存  prop:平台属性ID：平台属性值：平台属性名称
            for (String prop : props) {
                //子组合对象
                BoolQueryBuilder subBoolQueryBuilder = QueryBuilders.boolQuery();
                String[] p = prop.split(":");
                if (p.length == 3) {
                    //过滤查询
                    //平台属性ID
                    subBoolQueryBuilder.must(QueryBuilders.termQuery("attrs.attrId", p[0]));
                    //平台属性值
                    subBoolQueryBuilder.must(QueryBuilders.termQuery("attrs.attrValue", p[1]));
                    //平台属性名称
                    //subBoolQueryBuilder.filter(QueryBuilders.termQuery("attrs.attrName",p[2]));
                }
                //父组合对象 添加 子组合对象 and
                boolQueryBuilder.filter(QueryBuilders.nestedQuery(
                        "attrs", subBoolQueryBuilder, ScoreMode.None));
            }
        }


        //将组合查询条件对象放入searchSourceBuilder中
        searchSourceBuilder.query(boolQueryBuilder);

        //3、分页
        Integer pageNo = searchParam.getPageNo();
        Integer pageSize = searchParam.getPageSize();
        searchSourceBuilder.from((pageNo - 1) * pageSize);//开始行
        searchSourceBuilder.size(pageSize);//每页数

        //4、排序   1：asc  1: desc   2:asc  2:desc ....
        String order = searchParam.getOrder();
        if (!StringUtils.isEmpty(order)) {
            String[] o = order.split(":");//2 0: 1
            String field = "";
            switch (o[0]) {
                case "1":
                    field = "hotScore";
                    break;
                case "2":
                    field = "price";
                    break;
                case "3":
                    field = "createTime";
                    break;
            }
            //ES中排序
            searchSourceBuilder.sort(field, "asc".equalsIgnoreCase(o[1]) ? SortOrder.ASC : SortOrder.DESC);
        } else {
            //默认是按照 综合 由高到低
            searchSourceBuilder.sort("hotScore", SortOrder.DESC);
        }

        //5、高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title")
                .preTags("<font style='color:red>")
                .postTags("</font>");
        searchSourceBuilder.highlighter(highlightBuilder);

        //6、品牌的集合 使用桶聚合查询
        searchSourceBuilder.aggregation(AggregationBuilders
                .terms("tmIdAgg").field("tmId").size(100)
                .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName"))
                .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl")));


        //7、平台属性集合 使用桶聚会查询
        searchSourceBuilder.aggregation(AggregationBuilders
                .nested("attrsAgg", "attrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue")))
        );


        System.out.println("DSL语句 ： " + searchSourceBuilder.toString());


        //6、将 {} 与  DSL：GET goods/_search GET请求 关联
        searchRequest.source(searchSourceBuilder);

        //指定搜索的索引库
        searchRequest.indices("goods");

        //返回搜索请求对象
        return searchRequest;
    }

}
