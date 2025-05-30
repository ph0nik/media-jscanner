package app.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import service.PropertiesService;
import util.MediaType;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class UserPathValidator implements HandlerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(UserPathValidator.class);
    private final PropertiesService propertiesService;

    @Autowired
    public UserPathValidator(PropertiesService propertiesService) {
        this.propertiesService = propertiesService;
    }

    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {
        if (
                propertiesService.isMoviePathsProvided()
                        && propertiesService.doUserPathsExist(MediaType.MOVIE)
        ) {
            LOG.info("[ interceptor ] user paths exist, allowing request: {}", request.getRequestURI());
            return true;
        }
        else {
            LOG.info("[ interceptor ] blocking request: {}", request.getRequestURI());
            response.sendRedirect("/config");
            return false;
        }
    }
}
