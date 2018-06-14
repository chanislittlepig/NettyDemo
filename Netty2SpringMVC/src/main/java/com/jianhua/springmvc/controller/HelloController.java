package com.jianhua.springmvc.controller;

import com.jianhua.springmvc.service.AppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author lijianhua
 */
@Controller
@RequestMapping("/app")
public class HelloController {

    @Autowired
    private AppService appService;

    @RequestMapping("/index")
    public String index(){
        return "index";
    }

    @ResponseBody
    @RequestMapping("/add")
    public String add(){
        return "this is a add request";
    }

}