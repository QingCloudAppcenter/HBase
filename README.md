# HBase

## 映像构建

### Node 角色映像构建
1. 基于青云 focal5x64 (Ubuntu Server 20.04.5 LTS 64bit) 映像创建主机。
2. 主机内执行以下命令：
    ```bash
    wget https://raw.githubusercontent.com/QingCloudAppcenter/HBase/master/src/scripts/install.sh; sh install.sh node
    ```
3. 修改 ubuntu 用户密码。
4. 关闭主机，制作成新映像。

### Client 角色映像构建
1. 基于青云 focal5x64 (Ubuntu Server 20.04.5 LTS 64bit) 映像创建主机。
2. 主机内执行以下命令：
    ```bash
    wget https://raw.githubusercontent.com/QingCloudAppcenter/HBase/master/src/scripts/install.sh; sh install.sh client
    ```
3. 修改 ubuntu 用户密码。
4. 关闭主机，制作成新映像。

## 编译

### 安装依赖（hadoop-3.2.1/BUILDING.txt）
```bash
apt-get -y install build-essential autoconf automake libtool cmake zlib1g-dev pkg-config libssl-dev libsasl2-dev
```

下载 protobuf-2.5.0.tar.gz 和 snappy-1.1.3.tar.gz 并解压安装

```bash
./configure
make
make install
```

### Hadoop 3.2.1
mvn package: 编译生成安装包 hadoop-3.2.1.tar.gz

mvn install: 编译 jar 包并安装到 .m2 目录下 
```bash
mvn clean package -Pnative,dist -Drequire.snappy -Dsnappy.lib=/usr/local/lib -Dbundle.snappy -Drequire.openssl -Dopenssl.lib=/usr/lib/x86_64-linux-gnu -Dbundle.openssl -Dtar -DskipTests
mvn clean install -Pnative,dist -Drequire.snappy -Dsnappy.lib=/usr/local/lib -Dbundle.snappy -Drequire.openssl -Dopenssl.lib=/usr/lib/x86_64-linux-gnu -Dbundle.openssl -Dtar -DskipTests
```

### HBase 2.4.4
```bash
mvn -DskipTests package -Dhadoop.profile=3.0 assembly:single install
```

### Phoenix 5.1.2
1. 修改 pom.xml，将 hadoop 和 hbase 改为对应版本：
    ```xml
    <!-- Hadoop Versions -->
    <hbase.version>2.4.4</hbase.version>
    <hadoop.version>3.2.1</hadoop.version>
    ```
2. 不编译测试文件：
    ```bash
    mvn install -DskipTests -Dhbase.profile=2.4 -Dhbase.version=2.4.4
    ```
    因 Phoenix 5.0.0 是根据 HBase 2.0 早期版本的实现，针对 2.0.6 版本需要进行部分代码修改：

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