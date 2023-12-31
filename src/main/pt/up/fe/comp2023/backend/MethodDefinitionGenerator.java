package pt.up.fe.comp2023.backend;

import org.specs.comp.ollir.*;

public class MethodDefinitionGenerator {
    private Method method;

    public void setMethod(Method m) {
        method = m;
    }

    public String getMethodDefinition() {
        StringBuilder methodDefinition = new StringBuilder();

        if(method.isConstructMethod()) {
            method.setMethodName("<init>");
        }

        methodDefinition.append(getMethodHeader());

        StringBuilder instructions = new StringBuilder();
        InstructionTranslator instructionTranslator = new InstructionTranslator();
        boolean hasReturn = false;

        for(Instruction instruction: method.getInstructions()) {
            if(instruction.getInstType() == InstructionType.RETURN) {
                hasReturn = true;
            }

            instructions.append(instructionTranslator.translateInstruction(instruction, method)).append("\n");
        }


        if(!method.isConstructMethod()) {
            methodDefinition.append("\t.limit stack ").append(instructionTranslator.getStackCounter()).append("\n");
            methodDefinition.append("\t.limit locals ").append(getLocalsLimit()).append("\n");
        }

        methodDefinition.append(instructions);

        if(!hasReturn) {
            methodDefinition.append("\treturn\n");
        }

        methodDefinition.append(".end method\n");

        return methodDefinition.toString();
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

        header.append(method.getMethodName()).append(getMethodDescriptor()).append("\n");

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

    private int getLocalsLimit(){
        if(method == null)
            return 0;

        return this.method.getVarTable().values().stream().mapToInt(Descriptor::getVirtualReg).max().orElse(0) + 1;
    }
}
