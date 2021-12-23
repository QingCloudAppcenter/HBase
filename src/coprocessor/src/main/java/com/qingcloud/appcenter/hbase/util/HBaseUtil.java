package com.qingcloud.appcenter.hbase.util;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.io.compress.Compression;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * HBase 工具类
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
     * 判断表是否存在
     * @param tableName:
     * @throws IOException: IO exception
     */
    public static boolean tableExist(String tableName) throws Exception {
        Connection conn = getConnection();
        HBaseAdmin admin = (HBaseAdmin) conn.getAdmin();
        boolean exist = false;
        try {
            TableName _tableName = TableName.valueOf(tableName);
            if (admin.tableExists(_tableName)) {
                exist = true;
            }

        } finally {
            admin.close();
            closeConnect(conn);
        }
        return exist;
    }

    /**
     * 创建命名空间
     * @param namespace:
     * @throws IOException: IO exception
     */
    public static void createNamespace(String namespace) throws Exception {
        Connection conn = getConnection();
        HBaseAdmin admin = (HBaseAdmin) conn.getAdmin();
        try {
            NamespaceDescriptor namespaceDescriptor = NamespaceDescriptor.create(namespace).build();
            admin.createNamespace(namespaceDescriptor);
        } finally {
            admin.close();
            closeConnect(conn);
        }
    }

    /**
     * 建表
     * @param tableName: name of table
     * @param cfs: ColumnFamily list
     * @throws IOException: IO exception
     */
    public static void createTable(String tableName, String[] cfs, byte[][] splitKeys) throws Exception {
        Connection conn = getConnection();
        HBaseAdmin admin = (HBaseAdmin) conn.getAdmin();
        try {
            TableName _tableName = TableName.valueOf(tableName);
            if (admin.tableExists(_tableName)) {
                logger.warn("Table: {} is exists!", tableName);
                return;
            }

            TableDescriptorBuilder tableDescBuilder = TableDescriptorBuilder.newBuilder(_tableName);
            for (String cf : cfs) {
                ColumnFamilyDescriptorBuilder cfdb = ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(cf));
                cfdb.setCompactionCompressionType(Compression.Algorithm.SNAPPY);
                ColumnFamilyDescriptor cfd = cfdb.build();
                tableDescBuilder.setColumnFamily(cfd);
            }
            TableDescriptor tableDesc = tableDescBuilder.build();
            logger.info("Creating Table: {} ..", tableName);
            admin.createTable(tableDesc, splitKeys);
            logger.info("Table: {} create success!", tableName);
        } finally {
            admin.close();
            closeConnect(conn);
        }
    }

    /**
     * 删除表
     * @param tableName
     * @throws IOException
     */
    public static void deleteTable(String tableName) throws IOException {
        Connection conn = getConnection();
        HBaseAdmin admin = (HBaseAdmin) conn.getAdmin();
        try {
            TableName _tableName = TableName.valueOf(tableName);
            if (!admin.tableExists(_tableName)) {
                logger.warn("Table: {} is not exists!", tableName);
                return;
            }
            admin.disableTable(_tableName);
            admin.deleteTable(_tableName);
            logger.info("Table: {} delete success!", tableName);
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
    public static Table getTable(String tableName){
        try {
            return getConnection().getTable(TableName.valueOf(tableName));
        } catch (Exception e) {
            logger.error("Obtain Table failure !", e);
        }
        return null;
    }


    /**
     * 检索指定表的第一行记录。<br>
     * （如果在创建表时为此表指定了非默认的命名空间，则需拼写上命名空间名称，格式为【namespace:tablename】）。
     * @param tableName 表名称(*)。
     * @param filterList 过滤器集合，可以为null。
     * @return
     */
    public static Result selectFirstResultRow(String tableName,FilterList filterList) {
        if(StringUtils.isBlank(tableName)) return null;

        Table table = null;
        try {
            table = getTable(tableName);
            Scan scan = new Scan();
            if(filterList != null) {
                scan.setFilter(filterList);
            }
            ResultScanner scanner = table.getScanner(scan);
            Iterator<Result> iterator = scanner.iterator();
            int index = 0;
            while(iterator.hasNext()) {
                Result rs = iterator.next();
                if(index == 0) {
                    scanner.close();
                    return rs;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                table.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
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
        HTable table = (HTable) getConnection().getTable(TableName.valueOf(tableName));
        try {
            table.put(puts);
        } finally {
            table.close();
            if(autoCloseConn) {
                conn.close();
            }
        }
        return System.currentTimeMillis() - currentTime;
    }

    /**
     * 删除单条数据
     * @param tablename
     * @param row
     * @throws IOException
     */
    public static void delete(String tablename, String row) throws IOException {
        Table table = getTable(tablename);
        if(table!=null){
            try {
                Delete d = new Delete(row.getBytes());
                table.delete(d);
            } finally {
                table.close();
            }
        }
    }

    /**
     * 删除多行数据
     * @param tablename
     * @param rows
     * @throws IOException
     */
    public static void delete(String tablename, String[] rows) throws IOException {
        Table table = getTable(tablename);
        if (table != null) {
            try {
                List<Delete> list = new ArrayList<Delete>();
                for (String row : rows) {
                    Delete d = new Delete(row.getBytes());
                    list.add(d);
                }
                if (list.size() > 0) {
                    table.delete(list);
                }
            } finally {
                table.close();
            }
        }
    }

    /**
     * 关闭连接
     * @throws IOException
     */
    public static void closeConnect(){
        closeConnect(conn);
    }

    /**
     * 关闭连接
     * @throws IOException
     */
    public static void closeConnect(Connection conn){
        if(null != conn){
            try {
                conn.close();
                logger.info("conn closed.");
            } catch (Exception e) {
                logger.error("closeConnect failure !", e);
            }
        }
    }

    /**
     * 获取单条数据
     * @param tableName
     * @param rowKey
     * @return
     * @throws IOException
     */
    public static Result getRow(String tableName, String rowKey) {
        Table table = getTable(tableName);
        Result rs = null;
        if(table!=null){
            try{
                Get g = new Get(Bytes.toBytes(rowKey));
                rs = table.get(g);
            } catch (IOException e) {
                logger.error("getRow failure !", e);
            } finally{
                try {
                    table.close();
                } catch (IOException e) {
                    logger.error("getRow failure !", e);
                }
            }
        }
        return rs;
    }

    /**
     * 获取多行数据
     * @param tableName
     * @param rows
     * @return
     * @throws Exception
     */
    public static <T> Result[] getRows(String tableName, List<T> rows) {
        Table table = getTable(tableName);
        List<Get> gets = null;
        Result[] results = null;
        try {
            if (table != null) {
                gets = new ArrayList<Get>();
                for (T row : rows) {
                    if(row!=null){
                        gets.add(new Get(Bytes.toBytes(String.valueOf(row))));
                    }else{
                        throw new RuntimeException("hbase have no data");
                    }
                }
            }
            if (gets.size() > 0) {
                results = table.get(gets);
            }
        } catch (IOException e) {
            logger.error("getRows failure !", e);
        } finally {
            try {
                table.close();
            } catch (IOException e) {
                logger.error("table.close() failure !", e);
            }
        }
        return results;
    }

    /**
     * 扫描整张表，注意使用完要释放。
     * @param tableName
     * @return
     * @throws IOException
     */
    public static ResultScanner get(String tableName) {
        Table table = getTable(tableName);
        ResultScanner results = null;
        if (table != null) {
            try {
                Scan scan = new Scan();
                scan.setCaching(1000);
                results = table.getScanner(scan);
            } catch (IOException e) {
                logger.error("getResultScanner failure !", e);
            } finally {
                try {
                    table.close();
                } catch (IOException e) {
                    logger.error("table.close() failure !", e);
                }
            }
        }
        return results;
    }

    /**
     * 格式化输出结果
     */
    public static void formatRow(KeyValue rs){
        formatRows(new KeyValue[]{rs});
    }

    /**
     * 格式化输出结果
     */
    public static void formatRows(KeyValue[] rs){
        for(KeyValue kv : rs){
            System.out.println(" column family  :  " + Bytes.toString(kv.getFamilyArray()));
            System.out.println(" column   :  " + Bytes.toString(kv.getQualifierArray()));
            System.out.println(" value   :  " + Bytes.toString(kv.getValueArray()));
            System.out.println(" timestamp   :  " + String.valueOf(kv.getTimestamp()));
            System.out.println("--------------------");
        }
    }

    /**
     * byte[] 类型的长整形数字转换成 long 类型
     * @param byteNum
     * @return
     */
    public static long bytes2Long(byte[] byteNum) {
        long num = 0;
        for (int ix = 0; ix < 8; ++ix) {
            num <<= 8;
            num |= (byteNum[ix] & 0xff);
        }
        return num;
    }
}
