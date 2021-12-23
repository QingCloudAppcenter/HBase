package com.qingcloud.appcenter.hbase.util;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.io.compress.Compression;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;

/**
 * HBase 工具类
 * @author yudong
 */
public class HBaseUtil {

    private static final Logger logger = LoggerFactory.getLogger(HBaseUtil.class);

    private static Configuration conf;
    private static Connection conn;
    private static Admin admin;

    public static void init(String zkHost, String zkPort, String zNodeParent, String rootDir){
        try {
            if (conf == null) {
                conf = HBaseConfiguration.create();
                conf.set("hbase.zookeeper.quorum", zkHost);
                conf.set("hbase.zookeeper.property.clientPort", zkPort);
                conf.set("zookeeper.znode.parent", zNodeParent);
                conf.set("hbase.rootdir", rootDir);
            }
        } catch (Exception e) {
            logger.error("HBase Configuration Initialization failure !");
            throw new RuntimeException(e) ;
        }
    }

    public static void init(String confPath){
        try {
            if (conf == null) {
                conf = HBaseConfiguration.create();
                conf.addResource(new Path(confPath));
            }
        } catch (Exception e) {
            logger.error("HBase Configuration Initialization failure !");
            throw new RuntimeException(e) ;
        }
    }

    /**
     * 获得链接
     * @return Connection
     */
    public static synchronized Connection getConnection() {
        try {
            if(conn == null || conn.isClosed()){
                conn = ConnectionFactory.createConnection(conf);
            }
            System.out.println("HbaseConnection hashCode: " + conn.hashCode());
        } catch (IOException e) {
            logger.error("Failed to connect hbase!");
            throw new RuntimeException(e);
        }
        return conn;
    }

    /**
     * 获得HBase Admin
     * @return Admin
     */
    public static synchronized Admin getAdmin() {
        try {
            if(admin == null){
                admin = getConnection().getAdmin();
            }
        } catch (IOException e) {
            logger.error("Failed to get hbase admin!");
            throw new RuntimeException(e);
        }
        return admin;
    }

    /**
     * 判断表是否存在
     * @param tableName:
     * @throws IOException: IO exception
     */
    public static boolean tableExist(String tableName) throws Exception {
        boolean exist = false;
        try {
            if (getAdmin().tableExists(TableName.valueOf(tableName))) {
                exist = true;
            }

        } finally {
            admin.close();
            closeConnect(conn);
        }
        return exist;
    }

    /**
     * 建表
     * @param tableName: name of table
     * @param cfs: ColumnFamily list
     * @throws IOException: IO exception
     */
    public static void createTable(String tableName, String[] cfs, byte[][] splitKeys) throws Exception {
        TableName tableNameObj;
        try {
            tableNameObj = TableName.valueOf(tableName);
            if (getAdmin().tableExists(tableNameObj)) {
                logger.warn("Table: {} is exists!", tableName);
                return;
            }

            TableDescriptorBuilder tableDescBuilder = TableDescriptorBuilder.newBuilder(tableNameObj);
            for (String cf : cfs) {
                ColumnFamilyDescriptorBuilder cfdb = ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(cf));
                cfdb.setCompactionCompressionType(Compression.Algorithm.SNAPPY);
                ColumnFamilyDescriptor cfd = cfdb.build();
                tableDescBuilder.setColumnFamily(cfd);
            }
            TableDescriptor tableDesc = tableDescBuilder.build();
            logger.info("Creating Table: {} ..", tableName);
            getAdmin().createTable(tableDesc, splitKeys);
            logger.info("Table: {} create success!", tableName);
        } finally {
            admin.close();
            closeConnect(conn);
        }
    }

    /**
     * 获取  Table
     * @param tableName 表名
     * @return
     * @throws IOException
     */
    public static Table getTable(String tableName) throws IOException {
        return getConnection().getTable(TableName.valueOf(tableName));
    }

    /**
     * 往指定表添加数据
     * @param tableName  	表名
     * @param puts	 			需要添加的数据
     * @return long				返回执行时间
     * @throws IOException
     */
    public static long insert(String tableName, ArrayList<Put> puts) throws Exception {
        return insert(tableName, puts, true);
    }

    /**
     * 往指定表添加数据
     * @param tableName  	 表名
     * @param puts	 		 需要添加的数据
     * @param autoCloseConn	 自动关闭连接
     * @return long			 返回执行时间
     * @throws IOException
     */
    public static long insert(String tableName, ArrayList<Put> puts, boolean autoCloseConn) throws Exception {
        long currentTime = System.currentTimeMillis();
        try (HTable table = (HTable) getTable(tableName)) {
            table.put(puts);
        } finally {
            if (autoCloseConn) {
                conn.close();
            }
        }
        return System.currentTimeMillis() - currentTime;
    }

    /**
     * 关闭连接
     * @throws IOException
     */
    public static void closeConnect() throws IOException {
        closeConnect(conn);
    }

    /**
     * 关闭连接
     * @throws IOException
     */
    public static void closeConnect(Connection conn) throws IOException {
        if(null != conn){
            try {
                conn.close();
                logger.info("conn closed.");
            } catch (Exception e) {
                logger.error("closeConnect failure !", e);
                throw e;
            }
        }
    }
}
