# HBase

## 映像构建

### Node 角色映像构建
1. 基于青云 trustysrvx64i (Ubuntu Server 14.04.5 LTS 64bit) 映像创建主机。
2. 主机内执行以下命令：
    ```bash
    wget https://raw.githubusercontent.com/QingCloudAppcenter/HBase/master/src/scripts/install.sh; sh install.sh node
    ```
3. 修改 ubuntu 用户密码。
4. 关闭主机，制作成新映像。

### Client 角色映像构建
1. 基于青云 trustysrvx64i (Ubuntu Server 14.04.5 LTS 64bit) 映像创建主机。
2. 主机内执行以下命令：
    ```bash
    wget https://raw.githubusercontent.com/QingCloudAppcenter/HBase/master/src/scripts/install.sh; sh install.sh client
    ```
3. 修改 ubuntu 用户密码。
4. 关闭主机，制作成新映像。

## 编译

### Hadoop 2.7.7
```bash
mvn clean package -Pnative,dist -Drequire.snappy -Dsnappy.lib=/usr/local/lib -Dbundle.snappy -Drequire.openssl -Dopenssl.lib=/usr/lib/x86_64-linux-gnu -Dbundle.openssl -Dtar -DskipTests
```

### HBase 2.0.6
```bash
mvn -DskipTests package assembly:single install
```

### Phoenix 5.0.0
1. 修改 pom.xml，将 hadoop 和 hbase 改为对应版本：
    ```xml
    <!-- Hadoop Versions -->
    <hbase.version>2.0.6</hbase.version>
    <hadoop.version>2.7.7</hadoop.version>
    ```
2. 不编译测试文件：
    ```bash
    mvn install -Dmaven.test.skip=true
    ```
    因 Phoenix 5.0.0 是根据 HBase 2.0 早期版本的实现，针对 2.0.6 版本需要进行部分代码修改：
* Base64 问题
    ```java
    import org.apache.hadoop.hbase.util.Base64;
    替换为：
    import java.util.Base64;
    
    Base64.decode(${param})
    替换为：
    Base64.getDecoder().decode(${param})
  
    Base64.encodeBytes(${param})
    替换为：
    Bytes.toString(Base64.getEncoder().encode(${param}))
    因引入了 Bytes，需要增加：
    import org.apache.hadoop.hbase.util.Bytes;
    ```
* SubsetConfiguration 问题
    ```java
    import org.apache.commons.configuration2.SubsetConfiguration;
    替换为：
    import org.apache.commons.configuration.SubsetConfiguration;
    ```
* 重载 getMetaPriorityQueueLength 函数问题
    ```java
    phoenix-core/src/main/java/org/apache/hadoop/hbase/ipc/PhoenixRpcScheduler.java 中增加：
    @Override
    public int getMetaPriorityQueueLength() {
        return delegate.getMetaPriorityQueueLength();
    }
    ```
* getRegionsFromMergeQualifier 无法找到的问题
    HBase 2.0.6 版本中删除了[该方法](https://github.com/apache/hbase/commit/688a38833c66ece7159676db0571dbca8d7fed1d#diff-081750e39413c3b1930fc9952ed0d920)
    
    IndexHalfStoreFileReaderGenerator.java 中做如下修改：
    ```java
                if (result == null || result.isEmpty()) {
                    List<RegionInfo> parents = MetaTableAccessor.getMergeRegions(ctx.getEnvironment().getConnection(),
                            region.getRegionInfo().getRegionName());
                    if (parents == null || parents.isEmpty()) return reader;
                    byte[] splitRow =
                            CellUtil.cloneRow(KeyValueUtil.createKeyValueFromKey(r.getSplitKey()));
                    // We need not change any thing in first region data because first region start key
                    // is equal to merged region start key. So returning same reader.
                    if (Bytes.compareTo(parents.get(0).getStartKey(), splitRow) == 0) {
                        if (parents.get(0).getStartKey().length == 0
                                && region.getRegionInfo().getEndKey().length != parents.get(0).getEndKey().length) {
                            childRegion = parents.get(0);
                            regionStartKeyInHFile =
                                    parents.get(0).getStartKey().length == 0 ? new byte[parents.get(0)
                                            .getEndKey().length] : parents.get(0).getStartKey();
                        } else {
                            return reader;
                        }
                    } else {
                        childRegion = parents.get(1);
                        regionStartKeyInHFile = parents.get(1).getStartKey();
                    }
                    splitKey = KeyValueUtil.createFirstOnRow(region.getRegionInfo().getStartKey().length == 0 ?
                        new byte[region.getRegionInfo().getEndKey().length] :
                            region.getRegionInfo().getStartKey()).getKey();
                } else {
                    RegionInfo parentRegion = MetaTableAccessor.getRegionInfo(result);
                    regionStartKeyInHFile =
                            parentRegion.getStartKey().length == 0 ? new byte[parentRegion
                                    .getEndKey().length] : parentRegion.getStartKey();
                }
    ```
* CellComparatorImpl 构造方法参数问题
    两参数，增加为三参数：
    ```java
    public IndexMemStore() {
      this(new CellComparatorImpl(){
          @Override
          public int compare(Cell a, Cell b, boolean ignoreSequenceid) {
              return super.compare(a, b, true);
          }
      });
    }
    ```