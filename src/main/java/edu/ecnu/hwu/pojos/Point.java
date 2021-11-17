package edu.ecnu.hwu.pojos;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class Point {//Immutable Object

    private double lng;

    private double lat;
}
