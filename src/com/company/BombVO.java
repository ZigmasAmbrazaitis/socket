package com.company;

import java.util.Date;

public class BombVO {

    public double X;
    public double Y;
    public String name;

    /**
     * create bomb and put it on the screen
     */
    public BombVO() {
        name="bomb"+new Date().getTime();
        X = 64;
        Y = 64;
    }
}