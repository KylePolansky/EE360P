/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package termproject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *
 * @author super
 */
public class ActiveMonitor {

    Map<Object,ExecutorService> executors = new HashMap();
    
    public Future execute(Object object, String method, Object... args) throws NoSuchMethodException
    {
        ExecutorService ex = executors.get(object);
        if(ex==null)
        {
            ex = Executors.newFixedThreadPool(1);
            executors.put(object,ex);
        }
        
        return ex.submit(new MethodRunnable(object,method,args));
    }
    
    private static class MethodRunnable implements Callable{
        Method method;
        Object object;
        Object[] args;

        public MethodRunnable(Object object, String method, Object... args) throws NoSuchMethodException{
            this.object=object;
            this.args=args;

            Class[] classes = new Class[args.length];
            for (int i=0; i<args.length; i++)
            {
                classes[i]=args[i].getClass();
            }
            
            this.method = object.getClass().getMethod(method,classes);
            
        }

        @Override
        public Object call() throws Exception {
            Object res = null;
            try{
                res=method.invoke(object,args);
            }catch(Exception e)
            {
                System.out.println("Exception in invoke method.");
                e.printStackTrace();
            }
            return res;
        }
        
    }
}


