package pt.up.fe.comp2023.backend;

import org.specs.comp.ollir.Element;
import org.specs.comp.ollir.Method;

public class MethodDefinitionGenerator {
    private Method method;

    public void setMethod(Method m) {
        method = m;
    }

    public Method getMethodDefinition() {
        StringBuilder methodDefinition = new StringBuilder();

        if(method.isConstructMethod()) {
            method.setMethodName("<init>");
        }

        methodDefinition.append(getMethodHeader());

        StringBuilder instructions = new StringBuilder();

        // .limit locals
        //
        // .limit stack

        //instructions

        return method;
    }

    private String getMethodHeader() {
        StringBuilder header = new StringBuilder(".method ");

        if (method.getMethodAccessModifier().toString().equals("DEFAULT")) {
            header.append("public ");
        } else {
            header.append(method.getMethodAccessModifier().toString().toLowerCase()).append(" ");
        }

        if(method.isFinalMethod()) {
            header.append("final ");
        }

        if(method.isStaticMethod()) {
            header.append("static ");
        }

        header.append(method.getMethodName()).append(getMethodDescriptor());

        return header.toString();
    }

    private String getMethodDescriptor() {
        StringBuilder descriptor = new StringBuilder("(");

        for(Element param: method.getParams()) {
            descriptor.append(JasminUtils.translateType(method.getOllirClass(), param.getType()));
        }

        descriptor.append(")").append(JasminUtils.translateType(method.getOllirClass(), method.getReturnType()));

        return descriptor.toString();
    }
}
