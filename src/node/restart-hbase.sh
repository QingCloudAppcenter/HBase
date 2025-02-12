#!/bin/sh
	
# touch /opt/hbase/bin/restart-hbase.sh;chmod +x /opt/hbase/bin/restart-hbase.sh
pid=`ps ax | grep java | grep HMaster | grep -v grep| awk '{print $1}'`
if [ "x$pid" = "x" ]
then
  exit 0
else
  USER=root /opt/hbase/bin/stop.sh
fi

loop=60
force=1
while [ "$loop" -gt 0 ]
do
  pid=`ps ax | grep java | grep HMaster | grep -v grep| awk '{print $1}'`
  if [ "x$pid" = "x" ]
  then
    force=0
    break
  else
    sleep 10s
    loop=`expr $loop - 1`
  fi
done

if [ "$force" -eq 1 ]
then
  kill -9 $pid
fi

# clean local jars of hbase-regionservers..
for regionserver in `cat "${HBASE_HOME}/conf/regionservers"`; do
  if [ "x${regionserver}" != "x" ]; then
    ssh "${regionserver}" "rm /data/hbase/tmp/local/jars/*.jar"
  fi
done

USER=root /opt/hbase/bin/start.sh
