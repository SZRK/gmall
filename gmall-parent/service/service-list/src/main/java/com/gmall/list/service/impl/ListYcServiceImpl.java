package com.gmall.list.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.gmall.common.constant.RedisConst;
import com.gmall.list.dao.GoodsDao;
import com.gmall.list.service.ListYcService;
import com.gmall.model.list.*;
import com.gmall.model.product.BaseCategoryView;
import com.gmall.model.product.BaseTrademark;
import com.gmall.model.product.SkuAttrValue;
import com.gmall.model.product.SkuInfo;
import com.gmall.product.client.ProductFeignClient;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class ListYcServiceImpl implements ListYcService {

    // ??????ES?????????(Spring ??????)
    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate; // Sping Data??????

    // ES???????????????
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private GoodsDao goodsDao;

    @Autowired
    private RedisTemplate redisTemplate;

    // ??????ES?????????
    @Override
    public void createIndex() {
        elasticsearchRestTemplate.createIndex(Goods.class);
        elasticsearchRestTemplate.putMapping(Goods.class);
    }

    //????????????
    @Override
    public void cancelSale(Long skuId) {
        goodsDao.deleteById(skuId);
    }

    // ????????????,?????????????????????????????????????????????????????????????????????Goods?????????
    @Override
    public void onSale(Long skuId) {
        // ?????????????????????????????????????????????????????????
        Goods goods = new Goods();

        // supplyAsync ???????????????????????????????????????
        CompletableFuture<SkuInfo> skuInfoCompletableFuture =
                CompletableFuture.supplyAsync(() -> {
                    SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
                    goods.setCreateTime(new Date());
                    goods.setId(skuId);
                    goods.setPrice(skuInfo.getPrice().doubleValue());
                    goods.setTitle(skuInfo.getSkuName());
                    goods.setDefaultImg(skuInfo.getSkuDefaultImg());
                    return skuInfo;
                }, threadPoolExecutor);

        // thenAcceptAsync????????????????????????????????????????????????????????????????????????????????? ???????????????????????????
        // thenRunAsyac: ?????????????????????????????????, ?????????????????????????????????????????? ??????????????????????????????
        // thenApplyAsync
        CompletableFuture<Void> trademarkCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            BaseTrademark trademark = productFeignClient.getTrademark(skuInfo.getTmId());
            if (null == trademark) {
                return ;
            }
            goods.setTmId(trademark.getId());
            goods.setTmName(trademark.getTmName());
            goods.setTmLogoUrl(trademark.getLogoUrl());
        }, threadPoolExecutor);

        // thenAccept b??????a??????????????????a???????????????
        CompletableFuture<Void> categoryViewCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            goods.setCategory1Id(categoryView.getCategory1Id());
            goods.setCategory1Name(categoryView.getCategory1Name());
            goods.setCategory2Id(categoryView.getCategory2Id());
            goods.setCategory2Name(categoryView.getCategory2Name());
            goods.setCategory3Id(categoryView.getCategory3Id());
            goods.setCategory3Name(categoryView.getCategory3Name());
        }, threadPoolExecutor);

        // runAsync ??????????????????????????????????????????
        CompletableFuture<Void> searchAttrCompletableFuture = CompletableFuture.runAsync(() -> {
            List<SkuAttrValue> attrAndAttrValueByskuIdList =
                    productFeignClient.getAttrAndAttrValueByskuId(skuId);
            List<SearchAttr> attrsList = attrAndAttrValueByskuIdList.stream().map(skuAttrValue -> {
                SearchAttr searchAttr = new SearchAttr();
                searchAttr.setAttrId(skuAttrValue.getAttrId());
                searchAttr.setAttrName(skuAttrValue.getBaseAttrInfo().getAttrName());
                searchAttr.setAttrValue(skuAttrValue.getBaseAttrValue().getValueName());
                return searchAttr;
            }).collect(Collectors.toList());
            goods.setAttrs(attrsList);
        }, threadPoolExecutor);

        CompletableFuture.allOf(trademarkCompletableFuture, categoryViewCompletableFuture, searchAttrCompletableFuture).join();
        // ??????????????????
        goodsDao.save(goods);
    }


    // ??????????????????
    @Override
    public void incrHotScore(Long skuId) {
        String hotCacheKey = "HotScore";
        Double hotScore  = redisTemplate.opsForZSet()
                .incrementScore(hotCacheKey, RedisConst.SKUKEY_PREFIX + skuId, 1);
        if (hotScore % 10 == 0) {
            // ??????????????????ES
            Optional<Goods> optionalGoods = goodsDao.findById(skuId);
            Goods goods = optionalGoods.get();
            goods.setHotScore(Math.round(hotScore)); // ?
            goodsDao.save(goods);
        }
    }
    // ????????????
    @Override
    public SearchResponseVo search(SearchParam searchParam) {
        // ??????????????????
        SearchRequest searchRequest = buildSearchRequest(searchParam);
        // ????????????
        try {
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            // ?????????????????????
            SearchResponseVo searchResponseVo =   parseSearchResponse(searchResponse);

            // ?????????????????????????????????????????????
            Integer pageNo = searchParam.getPageNo();
            Integer pageSize = searchParam.getPageSize();
            searchResponseVo.setPageNo(pageNo);
            searchResponseVo.setPageSize(pageSize);
            // ????????? = ??????????????? + ??????????????? - 1 ??? / ???????????????
            searchResponseVo.setTotalPages((searchResponseVo.getTotal() + pageSize - 1) / pageSize);
            return searchResponseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ????????????????????????
    private SearchResponseVo parseSearchResponse(SearchResponse searchResponse) {
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        // ??????????????????
        searchResponseVo.setTotal(searchResponse.getHits().totalHits);
        SearchHit[] hits = searchResponse.getHits().getHits();
        List<Goods> list = Arrays.stream(hits).map(hit -> {
            Goods goods = JSONObject.parseObject(hit.getSourceAsString(), Goods.class);
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            // ??????????????????
            if (!CollectionUtils.isEmpty(highlightFields)) {
                HighlightField HighlightTitle = highlightFields.get("title");
                goods.setTitle(HighlightTitle.getFragments()[0].toString());
            }
            return goods;
        }).collect(Collectors.toList());
        searchResponseVo.setGoodsList(list);
        // ????????????
        ParsedLongTerms tmIdAgg = searchResponse.getAggregations().get("tmIdAgg");
        List<SearchResponseTmVo> listSearchResponseTmVo =  tmIdAgg.getBuckets().stream().map(bucket -> {
          SearchResponseTmVo srtVo = new SearchResponseTmVo();
          srtVo.setTmId(bucket.getKeyAsNumber().longValue());
          // ??????tmName?????????
          ParsedStringTerms tmNameAgg = bucket.getAggregations().get("tmNameAgg");
          srtVo.setTmName(tmNameAgg.getBuckets().get(0).getKeyAsString());
          // ??????tmLogoUrl?????????
            ParsedStringTerms tmLogoUrlAgg = bucket.getAggregations().get("tmLogoUrlAgg");
          srtVo.setTmLogoUrl(tmLogoUrlAgg.getBuckets().get(0).getKeyAsString());
          return srtVo;
        }).collect(Collectors.toList());
        searchResponseVo.setTrademarkList(listSearchResponseTmVo);

        // ??????????????????    List<SearchResponseAttrVo> attrsList = new ArrayList<>();
        ParsedNested attrsAgg = searchResponse.getAggregations().get("attrsAgg");
        ParsedLongTerms attrIdAgg = attrsAgg.getAggregations().get("attrIdAgg");
        List<SearchResponseAttrVo> attrsList = attrIdAgg.getBuckets().stream().map(bucket -> {
            SearchResponseAttrVo sraVo = new SearchResponseAttrVo();
            sraVo.setAttrId(bucket.getKeyAsNumber().longValue());
            // ??????tmName?????????
            ParsedStringTerms attrNameAgg = bucket.getAggregations().get("attrNameAgg");
            sraVo.setAttrName(attrNameAgg.getBuckets().get(0).getKeyAsString());
            // ??????tmLogoUrl?????????
            ParsedStringTerms attrVauleAgg = bucket.getAggregations().get("attrValueAgg");
            List<String> attrValueList = attrVauleAgg.getBuckets().stream().map(
                    Terms.Bucket::getKeyAsString
            ).collect(Collectors.toList());
            sraVo.setAttrValueList(attrValueList);
            return sraVo;
        }).collect(Collectors.toList());
        searchResponseVo.setAttrsList(attrsList);
        return searchResponseVo;

    }

    // ??????????????????
    private SearchRequest buildSearchRequest(SearchParam searchParam) {
        SearchRequest searchRequest = new SearchRequest();
        // ?????????DSL??????{}
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // ???????????????
        // 1.??????title
        String keyword = searchParam.getKeyword();
        if (!StringUtils.isEmpty(keyword)) {
            // match : ???????????????????????????????????????
            boolQueryBuilder.must(QueryBuilders.matchQuery("title", keyword).operator(Operator.AND));
        } else {
            // ????????????
            boolQueryBuilder.must(QueryBuilders.matchAllQuery());
        }
        // ????????????Id????????????????????????term??????
        Long category1Id = searchParam.getCategory1Id();
        Long category2Id = searchParam.getCategory2Id();
        Long category3Id = searchParam.getCategory3Id();
        if (null != category1Id) {
            // must ??? filter????????????????????????and
            boolQueryBuilder.filter(QueryBuilders.termQuery("category1Id", category1Id));
        }
        if (null != category2Id) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("category2Id", category2Id));
        }
        if (null != category3Id) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("category3Id", category3Id));
        }
        // 3.????????????  2:??????
        String trademark = searchParam.getTrademark();

        if (!StringUtils.isEmpty(trademark)) {
            String[] trademarkSplit = trademark.split(":");
            boolQueryBuilder.filter(QueryBuilders.termQuery("tmId", trademarkSplit[0]));
        }
        //  4.????????????
        String[] props = searchParam.getProps();
        // props=1:4500-11999:??????
        if (null != props && props.length > 0) {
            for (String prop : props) {
                BoolQueryBuilder subBoolQueryBuilder = QueryBuilders.boolQuery();
                //props=23:4G:????????????
                String[] propSplit = prop.split(":");
                if (propSplit.length == 3) {
                    subBoolQueryBuilder.filter(QueryBuilders.matchQuery("attrs.attrId", propSplit[0]));
                    subBoolQueryBuilder.filter(QueryBuilders.matchQuery("attrs.attrValue", propSplit[1]));
                }
                boolQueryBuilder.filter(QueryBuilders.nestedQuery("attrs", subBoolQueryBuilder, ScoreMode.None));
            }
        }
        // 5.??????
        Integer pageNo = searchParam.getPageNo();
        Integer pageSize = searchParam.getPageSize();
        // ?????????
        searchSourceBuilder.from((pageNo - 1) * pageSize);
        // ???????????????
        searchSourceBuilder.size(pageSize);
        // 6.??????
        String order = searchParam.getOrder();
        if (!StringUtils.isEmpty(order)) {
            //  order=1:asc  2 ?????? ??????
            String[] orderSplit = order.split(":");
            String orderPara = null;
            switch (orderSplit[0]) {
                case "1" : orderPara = "hotScore"; break;
                case "2" : orderPara = "price"; break;
                case "3" : orderPara = "createTime"; break;
            }
            searchSourceBuilder.sort(orderPara, orderSplit[1].equalsIgnoreCase("desc") ? SortOrder.DESC : SortOrder.ASC);
        } else {
            // ????????????
            searchSourceBuilder.sort("hotScore", SortOrder.DESC);
        }
        // 7.????????????
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title")
                .preTags("<font style='color:red'>")
                .postTags("</font>");
        //  ??????????????????
        searchSourceBuilder.aggregation(
                AggregationBuilders.terms("tmIdAgg").field("tmId").size(100)
                        .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName"))
                        .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl")));
        // ??????????????????
        searchSourceBuilder.aggregation(
                AggregationBuilders.nested("attrsAgg", "attrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId").size(100)
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue")))
        );
        // ??????????????????????????????searchSourceBuilder
        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.highlighter(highlightBuilder);
        // ????????????????????????
        searchRequest.source(searchSourceBuilder);
        searchRequest.indices("goods");
        return searchRequest;
    }
}
