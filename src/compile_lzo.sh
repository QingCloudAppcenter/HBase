mkdir -p /opt/hadoop/lzo/
mkdir -p /opt/hadoop-env/libgplcompression
cd /opt/
wget http://www.oberhumer.com/opensource/lzo/download/lzo-2.10.tar.gz
wget https://github.com/twitter/hadoop-lzo/archive/release-0.4.20.zip
wget http://www.lzop.org/download/lzop-1.03.tar.gz

tar -xzvf lzo-2.10.tar.gz
tar -xzvf lzop-1.03.tar.gz
unzip release-0.4.20.zip

# compile lzo
cd lzo-2.10
export CFLAGS=-m64
./configure -enable-shared -prefix=/opt/hadoop/lzo/
make && make install

cd /opt/hadoop/lzo/lib
tar -czvf liblzo.tar.gz liblzo2*
mv liblzo.tar.gz /opt/hadoop-env/

# compile lzop
cd /opt/lzop-1.03
./configure --enable-shared
make
make install
cp /usr/local/bin/lzop /opt/hadoop-env

# compile hadoop-lzo
cd /opt/hadoop-lzo-release-0.4.20
sed -i 's/2.6.4/2.7.3/g' pom.xml
export C_INCLUDE_PATH=/opt/hadoop/lzo/include
export LIBRARY_PATH=/opt/hadoop/lzo/lib
mvn clean package -Dmaven.test.skip=true
cd target/native/Linux-amd64-64
tar -cBf - -C lib . | tar -xBvf - -C /opt/hadoop-env/libgplcompression/
cd /opt/hadoop-env/
tar -czvf libgplcompression.tar.gz libgplcompression/*
rm -rf libgplcompression
cp /opt/hadoop-lzo-release-0.4.20/target/hadoop-lzo-0.4.20.jar /opt/hadoop-env/