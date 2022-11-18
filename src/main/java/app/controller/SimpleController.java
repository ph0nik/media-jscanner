//package app.controller;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//
//@Controller
//public class SimpleController {
//
//    @Value("${spring.application.name}")
//    private String appName;
//
//    /*
//    * Testing starting page
//    * */
//    @GetMapping("/")
//    public String homePage(Model model) {
//        model.addAttribute("appName", appName);
//        return "home";
//    }
//
//    @GetMapping("/jscanner")
//    public String startingPoint() {
//        return "redirect:/query";
//    }
//
//
//
//}
