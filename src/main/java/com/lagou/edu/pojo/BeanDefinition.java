package com.lagou.edu.pojo;

import java.util.ArrayList;
import java.util.List;

public class BeanDefinition {
    private String id;
    private String clazz;
    private Boolean isTransaction;
    private List<PropertyRef> refs = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public List<PropertyRef> getRefs() {
        return refs;
    }

    public void setRefs(List<PropertyRef> refs) {
        this.refs = refs;
    }

    public Boolean getTransaction() {
        return isTransaction;
    }

    public void setTransaction(Boolean transaction) {
        isTransaction = transaction;
    }
}
