package com.qingcloud.appcenter.hbase;

import com.qingcloud.appcenter.hbase.util.Constant;
import com.qingcloud.appcenter.hbase.util.HBaseUtil;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;


public class MyHbaseTable {
    private static final Logger logger = LoggerFactory.getLogger(MyHbaseTable.class);

    private final String tableName; // "NameSpace:TableName" or "TableName"
    private final String[] families;
    private long startRowKey;
    private int startRowKeyPrefix = 0;
    private long maxBatchNum;
    private long leftNum;
    private MyPut myPut;

    public static final String RandomRange ="0123456789";

    public MyHbaseTable(String tableName, String[] columnFamilies) {
        this(tableName, columnFamilies, null);
    }

    public MyHbaseTable(String tableName, String[] families, String startRowKey) {
        this.tableName = tableName;
        this.families = families;
        if (startRowKey != null) {
            this.startRowKey = Long.parseLong(startRowKey);
        }
    }

    ///// create table start ///////////////////////////////////////////
    public void create() throws Exception {
        byte[][] splitKeys = new byte[9][];
        for (int i=0; i<9; i++) {
            String key = String.format("%d|", i);
            logger.debug("SplitKeys: {}", key);
            splitKeys[i] = Bytes.toBytes(key);
        }

        HBaseUtil.createTable(this.tableName, this.families, splitKeys);
    }
    ///// create table end ///////////////////////////////////////////


    ///// insert data start ///////////////////////////////////////////
    public void insert(long totalRows, long maxBatchNum) throws IOException {
        this.leftNum = totalRows;
        this.maxBatchNum = maxBatchNum;
        this.myPut = new MyPut();
        try {
            Generater g = new Generater();
            g.start();

            Writer w = new Writer();
            w.start();
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            HBaseUtil.closeConnect();
        }
    }

    public class Generater extends Thread{
        ArrayList<byte[]> familyBytes = new ArrayList<>();

        byte[] col1 = Bytes.toBytes("g");  //goods id
        byte[] col2 = Bytes.toBytes("p");  //pi hao

        byte[] col1Value;
        byte[] col2Value;

        public Generater() {
            for (String f : families) {
                familyBytes.add(Bytes.toBytes(f));
            }
        }

        @Override
        public void run() {
            try {
                while (!myPut.doneGenerating()) {
                    if (myPut.getGenerateablePuts() != null) {
                        generate();
                    }
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void generate() {
            myPut.startGenerating();

            logger.info("Generating {} records to {}, times: {}..",
                    Constant.NF.format(myPut.GBatchNum), myPut.getGeneratingObj(),
                    Constant.NF.format(myPut.getGenerateTimes()));
//            byte[] wideValue = Bytes.toBytes("");
            long rowKeyEnd = startRowKey + myPut.GBatchNum;
            Put put;
            for (; startRowKey<rowKeyEnd; startRowKey++) {
                col1Value = Bytes.toBytes(getRandomString(16));
                col2Value = Bytes.toBytes(getRandomString(16));

                String s = reverse(String.format("%d%09d", startRowKeyPrefix, startRowKey));
                put = new Put(Bytes.toBytes(s));
                for (byte[] fb : familyBytes) {
                    put.addColumn(fb, col1, col1Value);
                    put.addColumn(fb, col2, col2Value);
                }
//                // generate wide row
//            if (Constant.WideTableRowKey != 0L &&
//                    startRowKey == Constant.WideTableRowKey) {
//                for (long j=0; j<Constant.WideTableColumnNum; j++) {
//                    put.addColumn(familyBytes.get(0), Bytes.toBytes(String.format("%01d", j)), wideValue);
//                }
//            }
                myPut.putting.add(put);
                startRowKeyPrefix = startRowKeyPrefix==9?0:startRowKeyPrefix+1;
            }
            myPut.generated();
        }

        public String getRandomString(int size){
            Random random=new Random();
            StringBuilder sb=new StringBuilder();
            for(int i=0; i<size; i++){
                int number=random.nextInt(10);
                sb.append(RandomRange.charAt(number));
            }
            return sb.toString();
        }

        public String reverse(String s) {
            int length = s.length();
            String tmpStr = "";
            for (int i = 0; i < length; i++){
                tmpStr = s.charAt(i) + tmpStr;
            }
            return tmpStr;
        }
    }


    public class Writer extends Thread {
        long usedSec = 0;

        @Override
        public void run() {
            try {
                while (leftNum > 0) {
                    if (myPut.getWriteablePuts()!=null) {
                        write();
                    }
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void write() throws Exception {
            myPut.startWriting();

            logger.info("Inserting {} records with {} with {} times..",
                    Constant.NF.format(myPut.WBatchNum), myPut.getWritingObj(), myPut.getWriteTimes());

            usedSec = HBaseUtil.insert(tableName, myPut.getWritingPuts(), false);
            logger.info("Done inserted {} records {} with {} times, spend time: {} millis.",
                    Constant.NF.format(myPut.WBatchNum), myPut.getWritingObj(), myPut.getWriteTimes(), Constant.NF.format(usedSec));
            myPut.writed();

            logger.info("left number: {}.", leftNum);
        }
    }

    private class MyPut {
        static final int IDLE_STATUS = 0;
        static final int GENERATING_STATUS = 1;
        static final int GENERATED_STATUS = 2;
        static final int WRITING_STATUS = 3;

        long times1 = 0;
        int status1 = IDLE_STATUS;
        ArrayList<Put> puts1;

        long times2 = 0;
        int status2 = IDLE_STATUS;
        ArrayList<Put> puts2;

        ArrayList<Put> putting ;
        long GBatchNum = 0;
        long WBatchNum = 0;

        private MyPut(){
            puts1 = new ArrayList<>();
            puts2 = new ArrayList<>();
        }

        private ArrayList<Put> getGenerateablePuts() {
            if (status1 == IDLE_STATUS) {
                return puts1;
            }else if (status2 == IDLE_STATUS) {
                return puts2;
            }
            return null;
        }

        private String getGeneratingObj() {
            if (status1 == GENERATING_STATUS) {
                return "puts1";
            }else if (status2 == GENERATING_STATUS) {
                return "puts2";
            }
            return "";
        }

        private long getGenerateTimes() {
            if (status1 == GENERATING_STATUS) {
                return times1;
            }else if (status2 == GENERATING_STATUS) {
                return times2;
            }
            return 0;
        }

        private void startGenerating() {
            if (status1 == IDLE_STATUS) {
                status1 = GENERATING_STATUS;
                times1++;
                putting = puts1;
            }else if (status2 == IDLE_STATUS) {
                status2 = GENERATING_STATUS;
                times2++;
                putting = puts2;
            }else {
                return;
            }
            GBatchNum = Math.min(maxBatchNum, leftNum);
        }

        private void generated() {
            if (status1 == GENERATING_STATUS) {
                status1 = GENERATED_STATUS;
            }else if (status2 == GENERATING_STATUS) {
                status2 = GENERATED_STATUS;
            }
        }

        private boolean doneGenerating() {
            return WBatchNum >= leftNum || leftNum < 1;
        }

        private ArrayList<Put> getWriteablePuts() {
            if (status1 == GENERATED_STATUS) {
                return puts1;
            }else if (status2 == GENERATED_STATUS) {
                return puts2;
            }
            return null;
        }

        private String getWritingObj() {
            if (status1 == WRITING_STATUS) {
                return "puts1";
            }else if (status2 == WRITING_STATUS) {
                return "puts2";
            }
            return "";
        }

        private void startWriting() {
            if (status1 == GENERATED_STATUS) {
                status1 = WRITING_STATUS;
                WBatchNum = puts1.size();
            }else if (status2 == GENERATED_STATUS) {
                status2 = WRITING_STATUS;
                WBatchNum = puts2.size();
            }
        }

        private long getWriteTimes() {
            if (status1 == WRITING_STATUS) {
                return times1;
            }else if (status2 == WRITING_STATUS) {
                return times2;
            }
            return 0;
        }

        private ArrayList<Put> getWritingPuts() {
            if (status1 == WRITING_STATUS) {
                return puts1;
            }else if (status2 == WRITING_STATUS) {
                return puts2;
            }
            return null;
        }

        private void writed() {
            if (status1 == WRITING_STATUS) {
                leftNum -= puts1.size();
                puts1.clear();
                status1 = IDLE_STATUS;
            }else if (status2 == WRITING_STATUS) {
                leftNum -= puts2.size();
                puts2.clear();
                status2 = IDLE_STATUS;
            }else{
                return;
            }
            WBatchNum = 0;
        }
    }
    ///// insert data end ///////////////////////////////////////////
}
