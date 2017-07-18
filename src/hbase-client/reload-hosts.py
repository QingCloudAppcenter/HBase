import shutil

if __name__ == '__main__':
    hosts_name = "/etc/hosts"
    gen_hosts_name = "/opt/hbase/conf/hosts"
    tmp_hosts_name = "%s.tmp" % gen_hosts_name
    new_hosts_name = "%s.new" % gen_hosts_name
    hosts = open(hosts_name, 'r')
    tmp_hosts = open(tmp_hosts_name, 'r')
    new_hosts = open(new_hosts_name, 'w')

    ip_host_dict = {}
    for line in tmp_hosts.readlines():
        sp = line.split(" ")
        if len(sp) >= 2:
            ip_host_dict[sp[0]] = sp[1]
    tmp_hosts.close()

    for line in hosts.readlines():
        if line[:-1].strip() and not line.startswith("#"):
            sp = line.split(" ")
            if len(sp) >= 2:
                ip = sp[0]
                host = line[line.index(" "):]
                if ip in ip_host_dict:
                    if ip_host_dict[ip] != host:
                        new_hosts.write(ip + ' ' + ip_host_dict[ip] + '\n')
                    else:
                        new_hosts.write(line)
                    ip_host_dict.pop(ip)
                else:
                    new_hosts.write(line)
            else:
                new_hosts.write(line)
        else:
            new_hosts.write(line)

    for ip, host in ip_host_dict.items():
        new_hosts.write(ip + ' ' + host + '\n')

    shutil.move(new_hosts_name, hosts_name)

    hosts.close()
    new_hosts.close()
