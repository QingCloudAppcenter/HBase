#!/bin/sh
set -x
set -e

HOME_DIR=/opt

# appcenter@yunify.com
URL=https://bigdata-package.pek3a.qingstor.com/hbase/2.4.4-v3.0.0
JDK_PACKAGE=jdk-8u141-linux-x64.tar.gz
HADOOP_PACKAGE=hadoop-3.2.1.tar.gz
HBASE_PACKAGE=hbase-2.4.4-bin.tar.gz
PHOENIX_PACKAGE=phoenix-hbase-2.4-5.1.2-bin.tar.gz
PHOENIX_SERVER=phoenix-server-hbase-2.4-5.1.2.jar
HBASE_HBCK_JAR=hbase-hbck2-1.2.0.jar

JDK_DIR=jdk1.8.0_141
HADOOP_DIR=hadoop-3.2.1
HBASE_DIR=hbase-2.4.4
PHOENIX_DIR=phoenix-hbase-2.4-5.1.2-bin

cd ${HOME_DIR}
rm -rf ${HADOOP_PACKAGE} ${HBASE_PACKAGE} ${PHOENIX_PACKAGE}
wget ${URL}/${HBASE_PACKAGE}
wget ${URL}/${HADOOP_PACKAGE}
wget ${URL}/${PHOENIX_PACKAGE}

rm -rf ${HADOOP_DIR} hadoop; rm -rf ${HBASE_DIR} hbase; rm -rf ${PHOENIX_DIR} phoenix
tar -xzvf ${HADOOP_PACKAGE}; tar -xzvf ${HBASE_PACKAGE}; tar -xzvf ${PHOENIX_PACKAGE}
rm -rf ${HADOOP_PACKAGE} ${HBASE_PACKAGE} ${PHOENIX_PACKAGE}
ln -s ${HADOOP_DIR} hadoop; ln -s ${HBASE_DIR} hbase; ln -s ${PHOENIX_DIR} phoenix
cd phoenix/bin/; rm -rf hbase-site.xml; ln -s ${HOME_DIR}/hbase/conf/hbase-site.xml hbase-site.xml; cd ${HOME_DIR}
cp phoenix/${PHOENIX_SERVER} hbase/lib/
#rm -rf hbase/lib/guava-*; cp phoenix/lib/guava-13.0.1.jar hbase/lib/
cd hbase/lib; wget ${URL}/${HBASE_HBCK_JAR}; cd ${HOME_DIR}
# append DRFAS to log4j.properties in hbase
wget https://raw.githubusercontent.com/QingCloudAppcenter/HBase/master/src/add_log4j.properties
cat add_log4j.properties >> hbase/conf/log4j.properties; rm -rf add_log4j.properties

rm -rf /etc/hosts; touch /etc/hosts

# lzo
wget ${URL}/libgplcompression.tar.gz;
tar -xzvf libgplcompression.tar.gz; mv libgplcompression/* hadoop/lib/native/; rm -rf libgplcompression/ libgplcompression.tar.gz
wget ${URL}/lzolib-2.10.tar.gz;
tar -xzvf lzolib-2.10.tar.gz; cp lzolib/* /usr/lib/; mkdir -p /usr/lib64; cp lzolib/* /usr/lib64/; rm -rf lzolib lzolib-2.10.tar.gz
wget ${URL}/lzop; mv lzop /usr/local/bin/lzop; rm -rf /usr/bin/lzop; ln -s /usr/local/bin/lzop /usr/bin/lzop
wget ${URL}/hadoop-lzo-0.4.20.jar; mv hadoop-lzo-0.4.20.jar hadoop/share/hadoop/common/

# jdk
cd /usr/; rm -rf ${JDK_PACKAGE} jdk; wget ${URL}/${JDK_PACKAGE}; tar -xzvf ${JDK_PACKAGE};
ln -s ${JDK_DIR} jdk; rm -rf ${JDK_PACKAGE}; cd ${HOME_DIR}

# app agent
wget ${URL}/app-agent-linux-amd64.tar.gz
tar -xzvf app-agent-linux-amd64.tar.gz; cd app-agent-linux-amd64; sh install.sh; cd ${HOME_DIR}; rm -rf app-agent-linux-amd64*

echo "* soft nproc 65535" >> /etc/security/limits.conf
echo "* hard nproc 65535" >> /etc/security/limits.conf
echo "* soft nofile 65535" >> /etc/security/limits.conf
echo "* hard nofile 65535" >> /etc/security/limits.conf
echo "root soft nproc 65535" >> /etc/security/limits.conf
echo "root hard nproc 65535" >> /etc/security/limits.conf
echo "root soft nofile 65535" >> /etc/security/limits.conf
echo "root hard nofile 65535" >> /etc/security/limits.conf

echo "fs.file-max=65536" >> /etc/sysctl.conf
echo "kernel.sysrq=1" >> /etc/sysctl.conf
echo "vm.swappiness=10" >> /etc/sysctl.conf
echo "net.ipv6.conf.all.disable_ipv6=1" >> /etc/sysctl.conf
echo "net.ipv6.conf.default.disable_ipv6=1" >> /etc/sysctl.conf
echo "net.ipv6.conf.lo.disable_ipv6=1" >> /etc/sysctl.conf

cp rc.local /etc/rc.local && chmod +x /etc/rc.local
sed -i '/^exit 0$/i\rm -rf /tmp/hostname' /etc/rc.local
sed -i '/^exit 0$/i\rm -rf /data/hadoop/pids' /etc/rc.local
sed -i '/^exit 0$/i\rm -rf /data/hbase/pids' /etc/rc.local

