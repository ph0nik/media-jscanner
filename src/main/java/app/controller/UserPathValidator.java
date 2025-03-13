package app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import service.PropertiesService;
import util.MediaType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class UserPathValidator implements HandlerInterceptor {

    private PropertiesService propertiesService;

    @Autowired
    public UserPathValidator(PropertiesService propertiesService) {
        this.propertiesService = propertiesService;
    }

    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {
        if (
                propertiesService.userMoviePathsExist()
                        && propertiesService.doUserPathsExist(MediaType.MOVIE)
        ) return true;
        else {
            response.sendRedirect("/config");
            return false;
        }
    }
}
