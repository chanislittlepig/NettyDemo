package com.jianhua.springmvc;

import com.jianhua.springmvc.config.SpringConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.ServletException;

/**
 * @author lijianhua
 */
public class Dispatcher {

    private static DispatcherServlet dispatcher;

    public static DispatcherServlet getDispatcher() throws ServletException {
        if(dispatcher == null){
            synchronized (Dispatcher.class){
                if(dispatcher == null){
                    ApplicationContext rootContext = createRootApplicationContext();
                    XmlWebApplicationContext webApplicationContext = new XmlWebApplicationContext();
                    webApplicationContext.setConfigLocation("classpath:spring.xml");
                    webApplicationContext.setParent(rootContext);

                    MockServletConfig mockServletConfig = new MockServletConfig(webApplicationContext.getServletContext(), "dispatcherServlet");
                    dispatcher = new DispatcherServlet(webApplicationContext);
                    dispatcher.init(mockServletConfig);
                }
            }
        }
        return dispatcher;
    }

    private static ApplicationContext createRootApplicationContext() {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(SpringConfig.class);
        return applicationContext;
    }


}