package pt.up.fe.comp2023.backend;

import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.Field;
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
        StringBuilder classDir = new StringBuilder(".class ");

        if(ollirClass.isFinalClass()) {
            classDir.append("final ");
        }
        if(ollirClass.isStaticClass()) {
            classDir.append("static ");
        }

        if (ollirClass.getClassAccessModifier().toString().equals("DEFAULT")) {
            classDir.append("public ");
        } else {
            classDir.append(ollirClass.getClassAccessModifier().toString()).append(" ");
        }

        classDir.append(ollirClass.getClassName());
        return classDir.toString();
    }

    private String getSuperDir(ClassUnit ollirClass) {
        StringBuilder superDir = new StringBuilder(".super ");
        if(ollirClass.getSuperClass() == null) {
            ollirClass.setSuperClass("/java/lang/Object");
        }

        superDir.append(ollirClass.getSuperClass());
        return superDir.toString();
    }

    private String getFieldsDir(ClassUnit ollirClass) {
        StringBuilder fieldsDefinitions = new StringBuilder();


        return fieldsDefinitions.toString();
    }

    private String getMethodsDir(ClassUnit ollirClass) {
        StringBuilder methodsDefinitions = new StringBuilder();


        return methodsDefinitions.toString();
    }
}
