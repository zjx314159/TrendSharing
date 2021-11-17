#!/bin/bash

nW=(400 450 500 550 600)
batch_period=(60 180 300 480 600)
vars_str=('nW' 'batch_period')
metrics=('totalTardiness' 'totalTravelTime' 'totalLatency' 'makeSpan' 'totalRunningTime')

declare -A label

label['gdp']="marker='*', ls=':', c='r', linewidth=2, label='GDP', markersize=10"
label['fesi']="marker='d', ls='-.', c='orange', linewidth=2, label='FESI', markersize=10"
label['tbtarp']="marker='.', ls=':', c='g', linewidth=2, label='TBTARP', markersize=10"

declare -A totalTardiness
declare -A totalTravelTime
declare -A totalLatency
declare -A makespan
declare -A totalRunningTime

clearCache(){
  for algo in ${!label[@]}
    do
      unset totalTardiness[${algo}]
      unset totalTravelTime[${algo}]
      unset totalLatency[${algo}]
      unset makespan[${algo}]
      unset totalRunningTime[${algo}]
    done
}

outputRes(){
  echo "====================== Output totalTardiness ======================" >> ${1}.txt
  for algo in ${!label[@]}
    do
      echo "plt.plot(k,[${totalTardiness[${algo}]%?}],${label[${algo}]})" >> ${1}.txt
    done
  echo "====================== Output totalTravelTime ======================" >> ${1}.txt
  for algo in ${!label[@]}
    do
      echo "plt.plot(k,[${totalTravelTime[${algo}]%?}],${label[${algo}]})" >> ${1}.txt
    done
  echo "====================== Output totalLatency ======================" >> ${1}.txt
  for algo in ${!label[@]}
    do
      echo "plt.plot(k,[${totalLatency[${algo}]%?}],${label[${algo}]})" >> ${1}.txt
    done
  echo "====================== Output makespan ======================" >> ${1}.txt
  for algo in ${!label[@]}
    do
      echo "plt.plot(k,[${makespan[${algo}]%?}],${label[${algo}]})" >> ${1}.txt
    done
  echo "====================== Output totalRunningTime ======================" >> ${1}.txt
  for algo in ${!label[@]}
    do
      echo "plt.plot(k,[${totalRunningTime[${algo}]%?}],${label[${algo}]})" >> ${1}.txt
    done
}

# algo_batchPeriod_edtRatio_nW_nT_speed
extractData() {
  clearCache
    echo "====================== Varying $1 on eleme ======================" >> ${1}.txt
    for algo in ${!label[@]}
    do
      arr=$2
      for args in ${arr[@]}
        do
          totalTardiness[${algo}]=${totalTardiness[${algo}]}`grep -oP 'totalTardiness = .*' ${algo}_${args}.txt | sed 's/totalTardiness = //g'`","
          totalTravelTime[${algo}]=${totalTravelTime[${algo}]}`grep -oP 'totalTravelTime = .*' ${algo}_${args}.txt | sed 's/totalTravelTime = //g'`","
          totalLatency[${algo}]=${totalLatency[${algo}]}`grep -oP 'totalLatency = .*' ${algo}_${args}.txt | sed 's/totalLatency = //g'`","
          makespan[${algo}]=${makespan[${algo}]}`grep -oP 'makeSpan = .*' ${algo}_${args}.txt | sed 's/makeSpan = //g'`","
          totalRunningTime[${algo}]=${totalRunningTime[${algo}]}`grep -oP 'totalRunningTime = .*' ${algo}_${args}.txt | sed 's/totalRunningTime = //g'`","
        done
    done
    outputRes $1
}

for var in ${vars_str[@]}
  do
    rm ${var}.txt
  done

i=0
files=()
for w in ${nW[@]}
  do
    files[${i}]=${batch_period[2]}_nW${w}
    ((i++))
  done
extractData 'nW' "${files[*]}"

i=0
files=()
for period in ${batch_period[@]}
  do
    files[${i}]=${period}_nW${nW[2]}
    ((i++))
  done
extractData 'batch_period' "${files[*]}"