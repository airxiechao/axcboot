package com.airxiechao.axcboot.communication.rest.util;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;
import com.airxiechao.axcboot.util.*;

import java.util.List;
import java.util.Random;

public class RestClientUtil {

    public static HealthService getServiceFromConsul(String serviceName, String serviceTag) throws Exception{
        if(StringUtil.isBlank(serviceName)){
            throw new Exception("no service name");
        }

        ConsulClient client = new ConsulClient("localhost");
        HealthServicesRequest.Builder requestBuilder = HealthServicesRequest.newBuilder()
                .setPassing(true)
                .setQueryParams(QueryParams.DEFAULT);

        if(!StringUtil.isBlank(serviceTag)){
            requestBuilder.setTag(serviceTag);
        }

        HealthServicesRequest request = requestBuilder.build();

        List<HealthService> healthyServices = client.getHealthServices(serviceName, request).getValue();
        if(healthyServices.size() == 0){
            throw new Exception(String.format("no healthy service [%s] with tag [%s]", serviceName, serviceTag));
        }

        HealthService service =  healthyServices.get(new Random().nextInt(healthyServices.size()));
        return service;
    }
}
