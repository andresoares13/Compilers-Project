package pt.up.fe.comp2023.backend;

import org.specs.comp.ollir.ClassUnit;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.Collections;

public class JmmBackend implements JasminBackend {

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        StringBuilder jasminCode = new StringBuilder();

        ClassUnit ollirClass = ollirResult.getOllirClass();
        // getClass
        jasminCode.append(getClassDir(ollirClass)).append("\n");
        // getSuper
        jasminCode.append(getSuperDir(ollirClass)).append("\n");
        // getFields
        jasminCode.append(getFieldsDir(ollirClass)).append("\n");
        // getMethods
        jasminCode.append(getMethodsDir(ollirClass)).append("\n");


        return new JasminResult(ollirResult, jasminCode.toString(), Collections.emptyList());
    }

    private String getClassDir(ClassUnit ollirClass) {
        System.out.println(ollirClass);
        return "";
    }

    private String getSuperDir(ClassUnit ollirClass) {
        System.out.println(ollirClass);
        return "";
    }

    private String getFieldsDir(ClassUnit ollirClass) {
        System.out.println(ollirClass);
        return "";
    }

    private String getMethodsDir(ClassUnit ollirClass) {
        System.out.println(ollirClass);
        return "";
    }
}
