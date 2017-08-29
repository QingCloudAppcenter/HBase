#!/bin/sh

image_ip=$1
package_dir=$2

scp $package_dir/hadoop-2.7.3.tar.gz $package_dir/hbase-1.2.6-bin.tar.gz $package_dir/phoenix-4.11.0-HBase-1.2.tar.gz root@$image_ip:/opt/
ssh root@$image_ip "rm -rf /opt/hadoop-2.7.3;rm -rf /opt/hadoop;rm -rf /opt/hbase-1.2.6;rm -rf /opt/hbase;rm -rf /opt/phoenix-4.11.0-HBase-1.2;rm -rf /opt/phoenix"
scp add_log4j.properties root@$image_ip:/opt/
ssh root@$image_ip "cd /opt/;tar -xzvf hadoop-2.7.3.tar.gz;tar -xzvf hbase-1.2.6-bin.tar.gz;tar -xzvf phoenix-4.11.0-HBase-1.2.tar.gz"
ssh root@$image_ip "cd /opt/;ln -s hadoop-2.7.3 hadoop;ln -s hbase-1.2.6 hbase;ln -s phoenix-4.11.0-HBase-1.2 phoenix"
ssh root@$image_ip "cd /opt/phoenix/bin/;rm -rf hbase-site.xml;ln -s /opt/hbase/conf/hbase-site.xml hbase-site.xml"
ssh root@$image_ip "rm -rf /opt/hbase/lib/guava-*;cp /opt/phoenix/lib/guava-13.0.1.jar /opt/hbase/lib/"
ssh root@$image_ip "sed -i s/RFA/DRFA/g /opt/hbase/bin/hbase-daemon.sh"
ssh root@$image_ip "sed -i s/RFA/DRFA/g /opt/hadoop/sbin/hadoop-daemon.sh"
ssh root@$image_ip "cat /opt/add_log4j.properties >> /opt/hbase/conf/log4j.properties"
ssh root@$image_ip "rm -rf /opt/add_log4j.properties"
ssh root@$image_ip "rm -rf /etc/hosts;touch /etc/hosts"
scp $package_dir/libgplcompression.tar.gz root@$image_ip:/opt/hadoop/lib/native/;
ssh root@$image_ip "cd /opt/hadoop/lib/native/; tar -xzvf libgplcompression.tar.gz;mv libgplcompression/* .;rm -rf libgplcompression/"
ssh root@$image_ip "rm -rf /opt/hadoop/lib/native/libgplcompression.tar.gz"

scp $package_dir/liblzo.tar.gz root@$image_ip:/usr/lib/;
ssh root@$image_ip "cd /usr/lib/; tar -xzvf liblzo.tar.gz"
ssh root@$image_ip "rm -rf /usr/lib/liblzo.tar.gz"
ssh root@$image_ip "mkdir -p /usr/lib64/;"
scp $package_dir/liblzo.tar.gz root@$image_ip:/usr/lib64/;
ssh root@$image_ip "cd /usr/lib64/; tar -xzvf liblzo.tar.gz"
ssh root@$image_ip "rm -rf /usr/lib64/liblzo.tar.gz"

scp $package_dir/lzop root@$image_ip:/usr/local/bin/lzop;
ssh root@$image_ip "rm -rf /usr/bin/lzop; ln -s /usr/local/bin/lzop /usr/bin/lzop"

scp $package_dir/hadoop-lzo-0.4.20.jar root@$image_ip:/opt/hadoop/share/hadoop/common/

ssh root@$image_ip "echo \"* soft nproc 65535\" >> /etc/security/limits.conf"
ssh root@$image_ip "echo \"* hard nproc 65535\" >> /etc/security/limits.conf"
ssh root@$image_ip "echo \"* soft nofile 65535\" >> /etc/security/limits.conf"
ssh root@$image_ip "echo \"* hard nofile 65535\" >> /etc/security/limits.conf"
ssh root@$image_ip "echo \"root soft nproc 65535\" >> /etc/security/limits.conf"
ssh root@$image_ip "echo \"root hard nproc 65535\" >> /etc/security/limits.conf"
ssh root@$image_ip "echo \"root soft nofile 65535\" >> /etc/security/limits.conf"
ssh root@$image_ip "echo \"root hard nofile 65535\" >> /etc/security/limits.conf"

ssh root@$image_ip "echo \"fs.file-max=65536\" >> /etc/sysctl.conf"
ssh root@$image_ip "echo \"kernel.sysrq=1\" >> /etc/sysctl.conf"
ssh root@$image_ip "echo \"vm.swappiness=10\" >> /etc/sysctl.conf"
ssh root@$image_ip "echo \"net.ipv6.conf.all.disable_ipv6=1\" >> /etc/sysctl.conf"
ssh root@$image_ip "echo \"net.ipv6.conf.default.disable_ipv6=1\" >> /etc/sysctl.conf"
ssh root@$image_ip "echo \"net.ipv6.conf.lo.disable_ipv6=1\" >> /etc/sysctl.conf"


scp $package_dir/jdk-8u65-linux-x64.tar.gz root@$image_ip:/usr;
ssh root@$image_ip "cd /usr/;tar -xzvf jdk-8u65-linux-x64.tar.gz;ln -s jdk1.8.0_65 jdk";
ssh root@$image_ip "echo \"export JAVA_HOME=/usr/jdk\" >> /etc/profile";
ssh root@$image_ip "echo 'export PATH=\$JAVA_HOME/bin:\$PATH' >> /etc/profile";
ssh root@$image_ip "source /etc/profile"

scp $package_dir/app-agent-linux-amd64.tar.gz root@$image_ip:/tmp/
ssh root@$image_ip "cd /tmp/;tar -xzvf app-agent-linux-amd64.tar.gz;cd app-agent-linux-amd64;sh install.sh;rm -rf /tmp/app-agent-linux-amd64*"

ssh root@image_ip "sed -i '/^exit 0$/i\rm -rf /tmp/hostname' /etc/rc.local"
ssh root@image_ip "sed -i '/^exit 0$/i\rm -rf /data/hadoop/pids' /etc/rc.local"
ssh root@image_ip "sed -i '/^exit 0$/i\rm -rf /data/hbase/pids' /etc/rc.local"

