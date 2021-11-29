package com.gmall.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class UserController {

    @RequestMapping("/login.html")
    public String login(String originUrl, Model model) {
        model.addAttribute("originUrl", originUrl);
        return "login";
    }
}
