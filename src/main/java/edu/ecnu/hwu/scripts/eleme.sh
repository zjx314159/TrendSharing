#!/bin/bash

nW=(400 450 500 550 600)
batch_period=(60 180 300 480 600)

algo=$1

echo 'varying nW'
for w in ${nW[@]}
  do
    echo $w
    java -jar eleme.jar ./dataset/eleme/daily/task ./dataset/eleme/daily/worker $algo ${batch_period[2]} $w &
  done

echo 'varying batch_period'
for period in ${batch_period[@]}
  do
    if [ $period != ${batch_period[2]} ];then
      echo $period
      java -jar eleme.jar ./dataset/eleme/daily/task ./dataset/eleme/daily/worker $algo $period ${nW[2]} &
    fi
  done