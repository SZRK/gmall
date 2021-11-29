package com.gmall.web.controller;

import com.gmall.list.ListFeignClient;
import com.gmall.model.list.SearchParam;
import com.gmall.model.list.SearchResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class ListController {

    @Autowired
    private ListFeignClient listFeignClient;

    @GetMapping("/list.html")
    public String search(SearchParam searchParam,  Model model) {
        // 将页面所需要的属性及属性值回显
        SearchResponseVo searchResponseVo = listFeignClient.search(searchParam);
        // 商品集合
        model.addAttribute("goodsList", searchResponseVo.getGoodsList());
        // 分页相关
        model.addAttribute("pageNo", searchResponseVo.getPageNo());
        model.addAttribute("totalPages", searchResponseVo.getTotalPages());
        // 回显查询相关数据
        model.addAttribute("searchParam", searchParam);
        // 品牌集合
        model.addAttribute("trademarkList", searchResponseVo.getTrademarkList());
        // 平台属性集合
        model.addAttribute("attrsList", searchResponseVo.getAttrsList());
        // 排序字段
        model.addAttribute("orderMap", builderOrderMap(searchParam));
        // 搜索URL地址
        model.addAttribute("urlParam", builderUrlParam(searchParam));
        // 回显平台属性
        model.addAttribute("propsParamList", buliderPropsParamList(searchParam));
        // 回显查询品牌
        model.addAttribute("trademarkParam", buliderTrademarkParam(searchParam));
        return "list/index";
    }
    // 构建回显平台属性参数
    private List<Map> buliderPropsParamList(SearchParam searchParam) {
        String[] props = searchParam.getProps();
        if (props != null && props.length > 0) {
           return  Arrays.stream(props).map(prop -> {
                HashMap<Object, Object> map = new HashMap<>();
                String[] s = prop.split(":");
                map.put("attrName", s[2]);
                map.put("attrValue", s[1]);
                map.put("attrId", s[0]);
                return map;
            }).collect(Collectors.toList());
        }
        return null;
    }
    // 构建回显品牌参数
    private String buliderTrademarkParam(SearchParam searchParam) {
        String trademark = searchParam.getTrademark();
        if (!StringUtils.isEmpty(trademark)) {
            //trademark=2:华为
            String[] t = trademark.split(":");
            return "品牌:" + t[1];
        }
        return null;
    }
    // 构建UrlParam
    private String builderUrlParam(SearchParam searchParam) {
        StringBuilder urlParam = new StringBuilder();
        urlParam.append("http://list.gmall.com/list.html?");
            // 关键字
        String keyword = searchParam.getKeyword();
        if (!StringUtils.isEmpty(keyword)) {
            urlParam.append("keyword=").append(keyword);
        }
        //平台属性
        String[] props = searchParam.getProps();
        if (props != null && props.length > 0) {
            for (String prop : props) {
                urlParam.append("&props=").append(prop);
            }
        }
        // 品牌
        String trademark = searchParam.getTrademark();
        if (!StringUtils.isEmpty(trademark)) {
            urlParam.append("&trademark=").append(trademark);
        }

        return urlParam.toString();
    }

    // 构架回显排序参数
    private Map builderOrderMap(SearchParam searchParam) {
        Map<Object, Object> orderMap = new HashMap<>();
        String order = searchParam.getOrder();
        if (!StringUtils.isEmpty(order)) {
            // 有排序字段
            String[] orderParam = order.split(":");
            // order=1:asc  排序规则   0:asc
            orderMap.put("type", orderParam[0]);
            orderMap.put("sort", orderParam[1]);
        } else {
            // 没有排序字段，则使用默认排序
            orderMap.put("type",1);
            orderMap.put("sort", "desc");
        }
        return orderMap;
    }
}
