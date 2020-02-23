package com.lagou.edu.utils;

import com.alibaba.druid.pool.DruidDataSource;

/**
 * @author 应癫
 */
public class DruidUtils {

    private DruidUtils(){
    }

    private static DruidDataSource druidDataSource = new DruidDataSource();


    static {
        druidDataSource.setDriverClassName("com.mysql.jdbc.Driver");
        druidDataSource.setUrl("jdbc:mysql://122.51.194.17:3306/manicureGame");
        druidDataSource.setUsername("manicureGame");
        druidDataSource.setPassword("zaq5986587");

    }

    public static DruidDataSource getInstance() {
        return druidDataSource;
    }

}
