package com.gmall.list.service;

import com.gmall.model.list.SearchParam;
import com.gmall.model.list.SearchResponseVo;

public interface ListYcService  {
    void createIndex();

    void cancelSale(Long skuId);

    void onSale(Long skuId);

    void incrHotScore(Long skuId);

    SearchResponseVo search(SearchParam searchParam);
}
