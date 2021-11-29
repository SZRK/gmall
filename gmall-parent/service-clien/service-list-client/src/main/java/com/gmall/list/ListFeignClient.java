package com.gmall.list;

import com.gmall.model.list.SearchParam;
import com.gmall.model.list.SearchResponseVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("service-listyc")
public interface ListFeignClient {

    // 搜索商品
    @PostMapping("/api/list/search")
    public SearchResponseVo search(@RequestBody SearchParam searchParam);
}
