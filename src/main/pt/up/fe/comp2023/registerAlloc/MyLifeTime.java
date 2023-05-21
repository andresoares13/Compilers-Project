package pt.up.fe.comp2023.registerAlloc;


import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.*;


public class MyLifeTime {

    private OllirResult ollirResult;
    private ArrayList<MyLifeTimeCalculator> methodFlowList;

    public MyLifeTime(OllirResult ollirResult){
        this.ollirResult = ollirResult;
    }


}
