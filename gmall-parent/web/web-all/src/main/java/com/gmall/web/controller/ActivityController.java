package com.gmall.web.controller;

import com.gmall.activity.ActivityFeignClient;
import com.gmall.common.result.Result;
import com.gmall.common.util.AuthContextHolder;
import com.gmall.common.util.MD5;
import com.gmall.model.activity.SeckillGoods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class ActivityController {

    @Autowired
    private ActivityFeignClient activityFeignClient;
    @GetMapping("/seckill.html")
    public String toSecKillPage(Model model) {

        List<SeckillGoods> secKillGoodsList = activityFeignClient.getSecKillGoodsList();
        model.addAttribute("list", secKillGoodsList);
        return "seckill/index";
    }

    @GetMapping("/seckill/{skuId}.html")
    public String toSecKillGoodInfoPage(@PathVariable String skuId, Model model) {
        SeckillGoods secKillGoodsInfo = activityFeignClient.getSecKillGoodsInfo(skuId);
        model.addAttribute("item", secKillGoodsInfo);
        return "seckill/item";
    }

    @GetMapping("/seckill/queue.html")
    public String toQueuePage(String skuId, String skuIdStr, Model model) {
        model.addAttribute("skuId", skuId);
        model.addAttribute(skuIdStr, skuIdStr);
        return "seckill/queue";
    }


}
