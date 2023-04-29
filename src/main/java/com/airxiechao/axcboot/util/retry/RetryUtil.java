package com.airxiechao.axcboot.util.retry;

public class RetryUtil {

    /**
     * 重试多次执行
     * @param times
     * @param runnable
     * @return
     */
    public static boolean retry(int times, RetryRunnable runnable){
        for(int i = 0; i < times; ++i){
            try{
                runnable.run();
                return true;
            }catch (Exception e){

            }
        }

        return false;
    }

}
