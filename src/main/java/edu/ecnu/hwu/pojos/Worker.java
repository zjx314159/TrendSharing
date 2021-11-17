package edu.ecnu.hwu.pojos;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class Worker {

    public int wid;

    public int curPos;

    public int cap;

    public double speed;

    public List<Integer> schedule;

    public List<Long> arrivalTime;

    public List<Integer> picked;
}
