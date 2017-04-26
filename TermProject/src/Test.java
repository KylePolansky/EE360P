/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package termproject;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.concurrent.Future;

/**
 *
 * @author super
 */
public class Test {
    private static int N=100;
    private static Integer A=560;
    private static Integer B=4009;
    static SecureRandom random = new SecureRandom();
    public static void main(String[] args)
    {
        
        parallelExecution();
        serialExecution();
        
    }
    
    private static void parallelExecution(){
                ActiveMonitor mon = new ActiveMonitor();
        
        long startTime = System.nanoTime();
        for(int i=0; i<N; i++)
        {
            Foo f = new Foo(nextName());
            Foo2 f2 = new Foo2(nextName());
            try{
                Future resFuture = mon.execute(f,"bar",A,B);
                int res = (int)resFuture.get();
                Future resFuture2 = mon.execute(f2,"bar2",A,B);
                int res2 = (int)resFuture2.get();
            }catch(NoSuchMethodException e)
            {
                System.out.println("No such method test.");
                e.printStackTrace();
            }catch(Exception e)
            {
                System.out.println("Exception in test.");
                e.printStackTrace();
            }
        }
        long endTime = System.nanoTime();
        System.out.println("Active Monitor Execution time: "+(endTime-startTime)/1000000+" ms.");
    }
        
    public static String nextName() {
        return new BigInteger(130, random).toString(32);
    }

    private static void serialExecution(){
        
        long startTime = System.nanoTime();
        for(int i=0; i<N; i++)
        {
            Foo f = new Foo(nextName());
            Foo2 f2 = new Foo2(nextName());
            int res = f.bar(A,B);
            int res2 = f2.bar2(A,B);
            check();
        }
        long endTime = System.nanoTime();
        System.out.println("Serial Execution time: "+(endTime-startTime)/1000000+" ms.");
    }
    
    private static class Foo{
        String name;
        public Foo(String name) {
            this.name=name;
        }
        public int bar(Integer a, Integer b)
        {
            int res=a;
            for(int i=0; i<b; i++)
            {
                for(int j=0; j<a; j++)
                    res=res*a;
            }
            return res;
        }
        
         @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Foo2 other = (Foo2) obj;
            return true;
        }
    }
    
    private static class Foo2{
        String name;
        public Foo2(String name) {
            this.name=name;
        }

        public int bar2(Integer a, Integer b)
        {
            int res=a;
            for(int i=0; i<b; i++)
            {
                for(int j=0; j<a; j++)
                    res=res*a;
            }
            return res;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 89 * hash + Objects.hashCode(this.name);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Foo2 other = (Foo2) obj;
            return true;
        }
        
        
    }
    
    private static void check(){try{Thread.sleep(10+random.nextInt(3));}catch(Exception e){};}
    
}
