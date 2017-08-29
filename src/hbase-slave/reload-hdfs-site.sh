#!/bin/sh

# touch /opt/hbase/bin/reload-hdfs-site.sh;chmod +x /opt/hbase/bin/reload-hdfs-site.sh

volume=`df -h | awk '{print $NF}' | grep \/data`
if [ x"$volume" = x"" ];then
  sed -i '/^<\/configuration>$/i\  <property>\n    <name>dfs.datanode.du.reserved<\/name>\n    <value>21474836480<\/value>\n  <\/property>' /opt/hadoop/etc/hadoop/hdfs-site.xml
fi