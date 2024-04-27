package com.airxiechao.axcboot.storage.db.influxdb;

import com.airxiechao.axcboot.storage.db.influxdb.annotation.InfluxDbField;
import com.airxiechao.axcboot.storage.db.influxdb.annotation.InfluxDbMeasurement;
import com.airxiechao.axcboot.storage.db.influxdb.annotation.InfluxDbTag;
import com.airxiechao.axcboot.storage.db.influxdb.annotation.InfluxDbTime;
import com.airxiechao.axcboot.util.AnnotationUtil;
import com.airxiechao.axcboot.util.ClsUtil;
import com.influxdb.client.*;
import com.influxdb.client.domain.Bucket;
import com.influxdb.client.domain.BucketRetentionRules;
import com.influxdb.client.domain.Organization;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class InfluxDbManager {
    private InfluxDBClient client;
    private String bucket;
    private String organization;

    public InfluxDbManager(String address, String token, String bucket, String organization) {
        this.client = InfluxDBClientFactory.create(address, token.toCharArray());
        this.bucket = bucket;
        this.organization = organization;
    }

    public InfluxDBClient getClient() {
        return client;
    }

    public void init(Integer retentionSeconds){
        // create organization
        Organization org;
        OrganizationsApi organizationsApi = client.getOrganizationsApi();
        Optional<Organization> optOrg = organizationsApi.findOrganizations().stream().filter(o -> o.getName().equals(organization)).findFirst();
        if(optOrg.isPresent()){
            org = optOrg.get();
        }else{
            org = organizationsApi.createOrganization(organization);
        }

        // create bucket
        BucketsApi bucketsApi = this.client.getBucketsApi();
        Bucket influxDbBucket = bucketsApi.findBucketByName(bucket);
        if(null == influxDbBucket){
            BucketRetentionRules retentionRules = new BucketRetentionRules();
            retentionRules.setEverySeconds(retentionSeconds);

            influxDbBucket = new Bucket();
            influxDbBucket.setName(bucket);
            influxDbBucket.setRetentionRules(Arrays.asList(retentionRules));
            influxDbBucket.setOrgID(org.getId());

            influxDbBucket = client.getBucketsApi().createBucket(influxDbBucket);
        }
    }

    public void write(Object obj) throws Exception {
        // add measurement
        InfluxDbMeasurement annMeasurement = AnnotationUtil.getClassAnnotation(obj.getClass(), InfluxDbMeasurement.class);
        if(null == annMeasurement){
            throw new Exception("no measurement");
        }
        String measurement = annMeasurement.value();

        Point point = Point.measurement(measurement);

        // add time
        Set<Field> times = ClsUtil.getFields(obj.getClass(), InfluxDbTime.class);
        if(times.size() == 0){
            throw new Exception("no time");
        }
        for (Field time : times) {
            time.setAccessible(true);
            Object timeValue = times.stream().collect(Collectors.toList()).get(0).get(obj);
            Long timestamp;
            if(timeValue instanceof Long) {
                timestamp = (Long)timeValue;
            } else if(timeValue instanceof Date) {
                timestamp = ((Date) timeValue).getTime();
            } else {
                timestamp = Long.valueOf(timeValue.toString());
            }

            point = point.time(timestamp, WritePrecision.MS);
            break;
        }

        // add tags
        Set<Field> tags = ClsUtil.getFields(obj.getClass(), InfluxDbTag.class);
        Map<String, String> tagMap = new HashMap<>();
        for (Field tag : tags) {
            tag.setAccessible(true);
            tagMap.put(tag.getName(), tag.get(obj).toString());
        }
        point = point.addTags(tagMap);

        // add fields
        Set<Field> fields = ClsUtil.getFields(obj.getClass(), InfluxDbField.class);
        if(fields.size() == 0){
            throw new RuntimeException("no fields");
        }
        Map<String, Object> fieldMap = new HashMap<>();
        for (Field field : fields) {
            field.setAccessible(true);
            fieldMap.put(field.getName(), field.get(obj));
        }
        point = point.addFields(fieldMap);

        WriteApiBlocking writeApi = client.getWriteApiBlocking();
        writeApi.writePoint(bucket, organization, point);
    }

    public Map<String, List<InfluxDbRecord>> query(List<String> fluxPipes){
        String query = String.format("from(bucket: \"%s\") %s", bucket,
                fluxPipes.stream().map(p -> String.format("|> %s ", p)).collect(Collectors.joining()));
        List<FluxTable> tables = client.getQueryApi().query(query, organization);

        Map<String, List<InfluxDbRecord>> series = new HashMap<>();
        for (FluxTable table : tables) {
            if(table.getRecords().size() > 0) {
                List<InfluxDbRecord> list = new ArrayList<>();
                for (FluxRecord record : table.getRecords()) {
                    list.add(new InfluxDbRecord(record.getValue(), record.getTime().toEpochMilli()));
                }

                String seriesName = table.getRecords().get(0).getField();
                series.put(seriesName, list);
            }
        }

        return series;
    }

    public void close(){
        client.close();
    }

}
