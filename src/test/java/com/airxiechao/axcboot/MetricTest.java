package com.airxiechao.axcboot;

import com.airxiechao.axcboot.util.os.HostUtil;
import com.alibaba.fastjson.JSON;

public class MetricTest {
    public static void main(String[] args) throws Exception {
        while (true) {
            Thread.sleep(1000);
            System.out.println(JSON.toJSONString(HostUtil.getHostMetric().getCpu().getLoad()));
        }
    }
}
