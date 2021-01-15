HBase endpoint coprocessor for counting table rows.

## Test Process

- write code and compile with command: mvn package
- create namespace at hbase shell: create_namespace 'ns1'
- create table with: java -jar target/hbase-operator-jar-with-dependencies.jar -a create_table -t ns1:table1
- add rows to table: java -jar target/hbase-operator-jar-with-dependencies.jar -a add_rows -t ns1:table1 -n 10000 -b 500 
- count rows: reference to [Counting Process](#Counting Process) 

## Counting Process

[HBase endpoint coprocessor row count manual](https://wiki.yunify.com/pages/viewpage.action?pageId=44970238) 
