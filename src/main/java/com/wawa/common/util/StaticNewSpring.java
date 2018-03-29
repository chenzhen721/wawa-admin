package com.wawa.common.util;

import com.wawa.AppProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: haigen.xiong
 * Date: 14-3-4
 * Time: 下午5:47
 * To change this template use File | Settings | File Templates.
 */
public class StaticNewSpring implements ServletContextListener {


//    public static final ScheduledExecutorService EXE = Executors.newScheduledThreadPool(
//            Math.max(3,Runtime.getRuntime().availableProcessors()/3),
//            new CustomizableThreadFactory("StaticSpring.EXE")
//    );


   /* static final ExecutorService EXE =
            new ThreadPoolExecutor(200, 300,
                    60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>(),
                    new CustomizableThreadFactory("StaticNewSpring.EXE")) ;
*/
   private static final String MIN_SIZE = AppProperties.get("buss.thread.minSize");
   private static final String MAX_SIZE = AppProperties.get("buss.thread.maxSize");
     //Math.max(200,Integer.parseInt(MIN_SIZE))
   // Math.max(300,Integer.parseInt(MAX_SIZE))
   static final BaseThreadPoolExecutor EXE =
           new BaseThreadPoolExecutor(Integer.parseInt(MIN_SIZE),
                   Integer.parseInt(MAX_SIZE) ,
                   60L, TimeUnit.SECONDS,
                   new LinkedBlockingQueue<Runnable>(),
                   new CustomizableThreadFactory("StaticNewSpring.EXE")) ;

    private static ApplicationContext context;

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        context = WebApplicationContextUtils.getRequiredWebApplicationContext(
                sce.getServletContext()
        );


//        WebSupport.init();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        EXE.shutdownNow();
    }


    public static ApplicationContext getContext(){

        if( null == context){
            throw new RuntimeException("context NOT INIT..");
        }

        return context;
    }

    public static Object get(String beanName){
        return getContext().getBean(beanName);
    }

    public static <T>T get(Class<T> clssName){
        return getContext().getBean(clssName);
    }

    public static void execute(Runnable run){
        EXE.execute(run);
    }

    public static int poolSize()
    {
        return  EXE.getChatPoolSize();
    }

    public static int activeCount()
    {
        return  EXE.getActiveCount() ;
    }

    public static String threadPoolInfoDetail()
    {
        String[] msg = EXE.toString().split("\\[");

        String info = msg[1].replaceAll("\\]","") ;

        return info ;
    }
}