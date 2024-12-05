#!/bin/bash

# 1. 총 슬롯 수 계산
slots=$(condor_status -compact | awk '{if (NR > 2) print $4}' | grep -E '^[0-9]+$' | awk '{s+=$1} END {print s}')

# 2. 총 작업 수 계산
jobs=$(condor_q -totals | grep "Total for all users" | awk '{if ($3 ~ /^[0-9]+$/) print $3; else print 0}')

# 3. Claimed 노드 목록 계산
claimed_nodes=$(condor_status -run | awk '{if(NR>2) print $1}' | tr '\n' ',' | sed 's/,$//')

# 출력
echo "${slots:-0}"
echo "${jobs:-0}"
echo "${claimed_nodes:-none}"
