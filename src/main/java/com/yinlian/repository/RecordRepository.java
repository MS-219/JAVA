package com.yinlian.repository;

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Repository
public class RecordRepository {
    private final List<JSONObject> records = new ArrayList<>();

    public synchronized void pushRecord(JSONObject record) {
        records.add(record);
    }

    public synchronized JSONObject findRecord(Predicate<JSONObject> predicate) {
        for (JSONObject r : records) {
            if (predicate.test(r)) {
                return r;
            }
        }
        return null;
    }

    public synchronized List<JSONObject> getAllRecords() {
        return new ArrayList<>(records);
    }
}