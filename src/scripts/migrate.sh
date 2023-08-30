#!/bin/sh
set -x
set -e

role=$1

HOME_DIR=/opt

VERSION=$(curl -L -s https://api.github.com/repos/QingCloudAppcenter/HBase/releases/latest | grep tag_name | sed "s/ *\"tag_name\": *\"\(.*\)\",*/\1/")
TAR_URL=https://github.com/QingCloudAppcenter/HBase/archive/${VERSION}.tar.gz

cd ${HOME_DIR}
wget ${TAR_URL}
tar -xzvf ${VERSION}.tar.gz
cd HBase-${VERSION}/src

if [ "x$role" = "xclient" ]
then
    echo "cp client conf"
    cd client
    cp conf.d/* /etc/confd/conf.d/
    cp templates/* /etc/confd/templates/
    cp core-site.xml /opt/hadoop/etc/hadoop/
    cp hdfs-site.xml /opt/hadoop/etc/hadoop/
    cp mapred-site.xml /opt/hadoop/etc/hadoop/
    cp hadoop-env.sh /opt/hadoop/etc/hadoop/
    cp hbase-env.sh /opt/hbase/conf/
    cp hbase-site.xml /opt/hbase/conf/
    cp reload-xml.py /opt/hbase/bin/
    cp reload-hosts.py /opt/hbase/bin/
    cat client_profile >> /etc/profile
    cat client_profile >> /root/.bashrc
else
    echo "cp node conf"
    cd node
    cp conf.d/* /etc/confd/conf.d/
    cp templates/* /etc/confd/templates/
    cp exclude-node.sh /opt/hadoop/sbin/exclude-node.sh
    cp reload-hdfs-site.sh /opt/hbase/bin/reload-hdfs-site.sh
    cp refresh-nodes.sh /opt/hadoop/sbin/refresh-nodes.sh
    cp restart-hbase.sh /opt/hbase/bin/restart-hbase.sh
    cp start-hadoop-slave.sh /opt/hadoop/sbin/start-hadoop-slave.sh
    cp start-regionserver.sh /opt/hbase/bin/start-regionserver.sh
    cp stop.sh /opt/hbase/bin/stop.sh

    cd ../scripts
    cat forbidden_ports >> /etc/sysctl.conf
fi

cd ${HOME_DIR}
rm -rf HBase-${VERSION}; rm -rf ${VERSION}.tar.gz

