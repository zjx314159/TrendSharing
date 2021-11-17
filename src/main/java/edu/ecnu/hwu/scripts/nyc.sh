#!/bin/bash

nT=(200000 250000 300000 350000 424635)
nW=(1000 2000 3000 4000 5000)
batch_period=(5 10 15 20 25)
edt_ratio=(1.5 1.8 2.0 2.3 2.5)
speed=(4.0 4.5 10.5 5.5 6.0)

algo=$1

echo 'varying nT'
for t in ${nT[@]}
  do
    echo $t
    java -jar nyc.jar $t ${nW[2]} ./dataset/nyc/nyc_node.csv ./dataset/nyc/nyc_order.txt ./dataset/nyc/worker.txt ${speed[2]} $algo ${batch_period[2]} ${edt_ratio[2]} &
  done

echo 'varying nW'
for w in ${nW[@]}
  do
    if [ $w != ${nW[2]} ];then
      echo $w
      java -jar nyc.jar ${nT[2]} $w ./dataset/nyc/nyc_node.csv ./dataset/nyc/nyc_order.txt ./dataset/nyc/worker.txt ${speed[2]} $algo ${batch_period[2]} ${edt_ratio[2]} &
    fi
  done

echo 'varying batch_period'
for period in ${batch_period[@]}
  do
    if [ $period != ${batch_period[2]} ];then
      echo $period
      java -jar nyc.jar ${nT[2]} ${nW[2]} ./dataset/nyc/nyc_node.csv ./dataset/nyc/nyc_order.txt ./dataset/nyc/worker.txt ${speed[2]} $algo $period ${edt_ratio[2]} &
    fi
  done

echo 'varying edt_ratio'
for ratio in ${edt_ratio[@]}
  do
    if [ $ratio != ${edt_ratio[2]} ];then
      echo $ratio
      java -jar nyc.jar ${nT[2]} ${nW[2]} ./dataset/nyc/nyc_node.csv ./dataset/nyc/nyc_order.txt ./dataset/nyc/worker.txt ${speed[2]} $algo ${batch_period[2]} $ratio &
    fi
  done

#echo 'varying speed'
#for spd in ${speed[@]}
#  do
#    if [ $spd != ${speed[2]} ];then
#      echo $spd
#      java -jar nyc.jar ${nT[2]} ${nW[2]} ./dataset/nyc/nyc_node.csv ./dataset/nyc/nyc_order.txt ./dataset/nyc/worker.txt $spd $algo ${batch_period[2]} ${edt_ratio[2]} &
#    fi
#  done