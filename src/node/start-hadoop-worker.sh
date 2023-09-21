#!/bin/sh
	
# touch /opt/hadoop/sbin/start-hadoop-worker.sh;chmod +x /opt/hadoop/sbin/start-hadoop-worker.sh
pid=`ps ax | grep java | grep DataNode | grep -v grep| awk '{print $1}'`
if [ "x$pid" = "x" ]
then
  USER=root /opt/hadoop/bin/hdfs --daemon start datanode
else
  exit 0
fi