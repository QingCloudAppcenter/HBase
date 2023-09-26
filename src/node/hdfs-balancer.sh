#!/bin/sh

Threshold=$(echo "$@" | jq .threshold)

nohup /opt/hadoop/sbin/start-balancer.sh -threshold ${Threshold} &
