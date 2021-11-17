package edu.ecnu.hwu.pojos;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class Task {

    public int tid;

    public int pid;

    public int did;

    public long releaseTime;

    public long expectedDeliveryTime;

    public int weight;

    public double distance;
}
