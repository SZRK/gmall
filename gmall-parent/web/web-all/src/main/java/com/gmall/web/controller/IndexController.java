package com.gmall.web.controller;

import com.alibaba.fastjson.JSONObject;
import com.gmall.common.result.Result;
import com.gmall.model.product.BaseCategoryView;
import com.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class IndexController {

    @Autowired
    private ProductFeignClient productFeignClient;

    // 静态化页面技术
    @GetMapping("/")
    public String toIndex(Model model) {
        List<Map> listMap = getCategoryViewMaps();
        model.addAttribute("list", listMap);

        return "index/index";
    }
    @Autowired
    private SpringTemplateEngine templateEngine;

    /**
     * 生成静态页面
     * @return
     * @throws IOException
     */
    @GetMapping("/createHtml")
    @ResponseBody
    public Result createHtml() throws IOException {
        List<Map> baseCategoryViewList = getCategoryViewMaps();
        Context context = new Context();
        context.setVariable("list", baseCategoryViewList);
        FileWriter write = new FileWriter("D:\\nginx-1.8.0\\gmall");
        templateEngine.process("index/index.html", context, write);
        return Result.ok();
    }


    private List<Map> getCategoryViewMaps() {
        List<BaseCategoryView> baseCategoryViewList = productFeignClient.getBaseCategoryViewList();
        List<Map> listMap = new ArrayList<>();
        // 将listMapBycategory1Id 安装一级分类进行分组,key为 Category1Id， 值为每一条数据

        Map<Long, List<BaseCategoryView>> baseCategoryViewlistMapByCategory1Id =
                baseCategoryViewList.stream()
                .collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));

        // 集合中的数据为每个一级分类Id对应的所有数据
        Set<Map.Entry<Long, List<BaseCategoryView>>> entriesBaseCategoryViewByCategory1Id =
                baseCategoryViewlistMapByCategory1Id.entrySet();
        System.out.println(JSONObject.toJSON(baseCategoryViewlistMapByCategory1Id));
        int index = 1;
        for (Map.Entry<Long, List<BaseCategoryView>> longListEntry : entriesBaseCategoryViewByCategory1Id) {
            // 拿到了每一个根据一级没类分组后的总数据
            HashMap<String, Object> listMapBycategory1Id = new HashMap<>();

            listMapBycategory1Id.put("index", index++);
            listMapBycategory1Id.put("categoryName", longListEntry.getValue().get(0).getCategory1Name());
            listMapBycategory1Id.put("categoryId", longListEntry.getKey());

            // 准备二级分类信息
            Map<Long, List<BaseCategoryView>>  baseCategoryViewlistMapByCategory2Id
                    = longListEntry.getValue().stream()
                    .collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));

            Set<Map.Entry<Long, List<BaseCategoryView>>> entriesBaseCategoryViewByCategory2Id
                    = baseCategoryViewlistMapByCategory2Id.entrySet();

            List<Map> listByCatetgoryBy2Id = new ArrayList<>();

            for (Map.Entry<Long, List<BaseCategoryView>> listEntry : entriesBaseCategoryViewByCategory2Id) {
                HashMap<String, Object> listMapBycategory2Id = new HashMap<>();
                listMapBycategory2Id.put("categoryName", listEntry.getValue().get(0).getCategory2Name());
                listMapBycategory2Id.put("categoryId", listEntry.getKey());
                listByCatetgoryBy2Id.add(listMapBycategory2Id);

                // 三级分类Id都不相同，因此不用分类
                List<HashMap<Object, Object>> collect = listEntry.getValue().stream().map(baseCategoryView -> {
                    HashMap<Object, Object> listMapBycategory3Id = new HashMap<>();
                    listMapBycategory3Id.put("categoryName", baseCategoryView.getCategory3Name());
                    listMapBycategory3Id.put("categoryId", baseCategoryView.getCategory3Id());
                    return listMapBycategory3Id;
                }).collect(Collectors.toList());
                listMapBycategory2Id.put("categoryChild", collect);


            }
            listMapBycategory1Id.put("categoryChild", listByCatetgoryBy2Id);
            listMap.add(listMapBycategory1Id);
        }
        return listMap;
    }
}