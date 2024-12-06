#/bin/bash
slots=$(condor_status -compact | awk '{if (NR > 2) print $4}' | grep -E '^[0-9]+$' | awk '{s+=$1} END {print s}')

# 총 작업 수 계산
jobs=$(condor_q -totals | grep "Total for all users" | awk '{for (i=1; i<=NF; i++) if ($i ~ /^[0-9]+$/) {print $i; exit}}')

# Claimed 노드 목록 계산
claimed_nodes=$(condor_status -run | awk '{if(NR>2) print $1}' | tr '\n' ',' | sed 's/,$//')

# Java가 읽을 수 있도록 숫자만 출력
echo "${slots:-0}"
echo "${jobs:-0}"
echo "${claimed_nodes:-none}"