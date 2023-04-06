package pt.up.fe.comp2023.backend;

import org.specs.comp.ollir.*;

import java.util.ArrayList;

public class InstructionTranslator {
    private int stackCounter = 1;
    private int indentation = 1;

    public String translateInstruction(Instruction instruction, Method method) {
        StringBuilder instructionTranslated = new StringBuilder();
        InstructionType instType = instruction.getInstType();
        stackCounter = 1;

        switch (instType) {
            case CALL -> instructionTranslated.append(translateInstruction((CallInstruction) instruction, method));
            case GOTO -> instructionTranslated.append(translateInstruction((GotoInstruction) instruction, method));
            case NOPER -> instructionTranslated.append(translateInstruction((SingleOpInstruction) instruction, method));
            case ASSIGN -> instructionTranslated.append(translateInstruction((AssignInstruction) instruction, method));
            case BRANCH -> instructionTranslated.append(translateInstruction((CondBranchInstruction) instruction, method));
            case RETURN -> instructionTranslated.append(translateInstruction((ReturnInstruction) instruction, method));
            case GETFIELD -> instructionTranslated.append(translateInstruction((GetFieldInstruction) instruction, method));
            case PUTFIELD -> instructionTranslated.append(translateInstruction((PutFieldInstruction) instruction, method));
            case UNARYOPER -> instructionTranslated.append(translateInstruction((UnaryOpInstruction) instruction, method));
            case BINARYOPER -> instructionTranslated.append(translateInstruction((BinaryOpInstruction) instruction, method));
            default -> {}
        }

        return instructionTranslated.toString();
    }

    // CALL
    public String translateInstruction(CallInstruction instruction, Method method) {
        return "";
    }

    // GOTO
    public String translateInstruction(GotoInstruction instruction, Method method) {
        return getIndentation() + "goto " + instruction.getLabel();
    }

    // NOPER
    public String translateInstruction(SingleOpInstruction instruction, Method method) {
        return "";
    }

    // ASSIGN
    public String translateInstruction(AssignInstruction instruction, Method method) {
        Element dest = instruction.getDest();

        if (dest.isLiteral()) {
            return "";
        }

        Instruction rhs = instruction.getRhs();

        if (dest instanceof ArrayOperand) {
            return getCorrespondingStore(dest, method) + "\n" + translateInstruction(rhs, method) + "\n" + getIndentation() + "iastore";
        }

        if (rhs.getInstType() == InstructionType.CALL) {
            CallInstruction callInstruction = (CallInstruction) rhs;
            if (callInstruction.getInvocationType() == CallType.NEW) {
                ElementType elementType = callInstruction.getFirstArg().getType().getTypeOfElement();
                if (elementType != ElementType.ARRAYREF) {
                    return translateInstruction(rhs, method);
                }
            }
        }

        return translateInstruction(rhs, method) + "\n" + getCorrespondingStore(dest, method);
    }

    // BRANCH
    public String translateInstruction(CondBranchInstruction instruction, Method method) {
        return translateInstruction(instruction.getCondition(), method) + "\n" +
                getIndentation() + "ifne " + instruction.getLabel();
    }

    // RETURN
    public String translateInstruction(ReturnInstruction instruction, Method method) {
        StringBuilder jasminInstruction = new StringBuilder();
        ElementType returnType = instruction.getReturnType().getTypeOfElement();

        switch (returnType) {
            case BOOLEAN:
            case INT32:
            case OBJECTREF:
            case CLASS:
            case STRING:
            case ARRAYREF:
                // why this line???
                jasminInstruction.append(getCorrespondingLoad(instruction.getOperand(), method)).append("\n");

                jasminInstruction.append(getIndentation());

                if(returnType == ElementType.BOOLEAN || returnType == ElementType.INT32) {
                    jasminInstruction.append("ireturn");
                } else {
                    jasminInstruction.append("areturn");
                }
                break;
            case VOID:
                jasminInstruction.append(getIndentation()).append("return");
            default:
                break;
        }
        return jasminInstruction.toString();
    }

    // GETFIELD
    public String translateInstruction(GetFieldInstruction instruction, Method method) {
        return "";
    }

    // PUTFIELD
    public String translateInstruction(PutFieldInstruction instruction, Method method) {
        return "";
    }

    // UNARYOPER
    public String translateInstruction(UnaryOpInstruction instruction, Method method) {
        StringBuilder jasminInstruction = new StringBuilder();
        Operation operation = instruction.getOperation();
        OperationType operationType = operation.getOpType();
        Element element = instruction.getOperand();

        jasminInstruction.append(getCorrespondingLoad(element, method)).append("\n");

        switch (operationType) {
            case NOT -> jasminInstruction.append("not\n");
            case NOTB -> jasminInstruction.append("notb\n");
            default -> {
            }
        }

        return jasminInstruction.toString();
    }

    // BINARYOPER
    public String translateInstruction(BinaryOpInstruction instruction, Method method) {
        StringBuilder jasminInstruction = new StringBuilder();
        Operation operation = instruction.getOperation();
        OperationType operationType = operation.getOpType();

        Element leftElement = instruction.getLeftOperand();
        Element rightElement = instruction.getRightOperand();

        jasminInstruction.append(getCorrespondingLoad(leftElement, method)).append("\n");
        jasminInstruction.append(getCorrespondingLoad(rightElement, method)).append("\n");
        jasminInstruction.append(getIndentation());

        switch (operationType) {
            case ADD:
                jasminInstruction.append("iadd");
                break;
            case SUB:
                jasminInstruction.append("isub");
                break;
            case MUL:
                jasminInstruction.append("imul");
                break;
            case DIV:
                jasminInstruction.append("idiv");
                break;
            case LTH:
                // todo
                break;
            case AND, ANDB:
                jasminInstruction.append("iand");
                break;
            case OR, ORB:
                jasminInstruction.append("ior");
                break;
            case EQ:
                // todo
                break;
            default:
                // more to implement
                break;
        }
        return jasminInstruction.toString();
    }

    private String getCorrespondingLoad(Element element, Method ancestorMethod) {
        //stackCounter++;
        if (element.isLiteral()) {
            LiteralElement literalElement = (LiteralElement) element;

            switch (literalElement.getType().getTypeOfElement()) {
                case INT32, BOOLEAN -> {
                    StringBuilder jasminInstruction = new StringBuilder(getIndentation());
                    String literal = JasminUtils.trimLiteral(literalElement.getLiteral());

                    try {
                        int literalInt = Integer.parseInt(literal);

                        if (literalInt <= 5) {
                            jasminInstruction.append("iconst_").append(literal);
                        } else if (literalInt < Math.pow(2, 7)) {
                            jasminInstruction.append("bipush ").append(literal);
                        } else if (literalInt < Math.pow(2, 15)) {
                            jasminInstruction.append("sipush ").append(literal);
                        } else {
                            throw new Exception("");
                        }
                    } catch (Exception e) {
                        jasminInstruction.append("ldc ").append(literal);
                    }
                    return jasminInstruction.toString();
                }
                default -> { return "";}
            }
        } else {
            Operand operand = (Operand) element;

            Descriptor operandDescriptor = ancestorMethod.getVarTable().get(operand.getName());
            if (operandDescriptor.getVirtualReg() < 0) {
                return "";
            }

            String spacer = operandDescriptor.getVirtualReg() < 4 ? "_" : " ";

            switch (operandDescriptor.getVarType().getTypeOfElement()) {
                case INT32, BOOLEAN -> {
                    return getIndentation() + "iload" + spacer + operandDescriptor.getVirtualReg();
                }
                case ARRAYREF -> {
                    //stackCounter++;
                    StringBuilder jasminInstruction = new StringBuilder();
                    jasminInstruction.append(getIndentation()).append("aload").append(spacer).append(operandDescriptor.getVirtualReg());
                    if (element instanceof ArrayOperand) {
                        ArrayOperand arrayOperand = (ArrayOperand) operand;

                        jasminInstruction.append("\n");

                        ArrayList<Element> indexes = arrayOperand.getIndexOperands();
                        Element index = indexes.get(0);

                        jasminInstruction.append(getCorrespondingLoad(index, ancestorMethod)).append("\n");
                        jasminInstruction.append(getIndentation()).append("iaload");
                    }
                    return jasminInstruction.toString();
                }
                case CLASS, OBJECTREF, THIS, STRING -> {
                    //stackCounter++;
                    return getIndentation() + "aload" + spacer + operandDescriptor.getVirtualReg();
                }
                default -> {
                    return "";
                }
            }
        }
    }

    private String getCorrespondingStore(Element element, Method ancestorMethod) {
        if (element.isLiteral()) {
            return "";
        } else {
            Operand operand = (Operand) element;

            Descriptor operandDescriptor = ancestorMethod.getVarTable().get(operand.getName());

            String spacer = operandDescriptor.getVirtualReg() < 4 ? "_" : " ";

            switch (operand.getType().getTypeOfElement()) {
                case INT32, BOOLEAN -> {
                    if (element instanceof ArrayOperand) {
                        ArrayOperand arrayOperand = (ArrayOperand) operand;
                        StringBuilder jasminInstruction = new StringBuilder();
                        jasminInstruction.append(getIndentation()).append("aload").append(spacer).append(operandDescriptor.getVirtualReg()).append("\n");

                        ArrayList<Element> indexes = arrayOperand.getIndexOperands();
                        Element index = indexes.get(0);

                        jasminInstruction.append(getCorrespondingLoad(index, ancestorMethod));
                        return jasminInstruction.toString();
                    }
                    return getIndentation() + "istore" + spacer + operandDescriptor.getVirtualReg();
                }
                case CLASS, OBJECTREF, THIS, STRING -> {
                    return getIndentation() + "astore" + spacer + operandDescriptor.getVirtualReg();
                }
                case ARRAYREF -> {
                    StringBuilder jasminInstruction = new StringBuilder();
                    if (element instanceof ArrayOperand) {
                        ArrayOperand arrayOperand = (ArrayOperand) operand;
                        jasminInstruction.append(getIndentation()).append("aload").append(spacer).append(operandDescriptor.getVirtualReg()).append("\n");

                        ArrayList<Element> indexes = arrayOperand.getIndexOperands();
                        Element index = indexes.get(0);

                        jasminInstruction.append(getCorrespondingLoad(index, ancestorMethod)).append("\n");
                    } else {
                        jasminInstruction.append(getIndentation()).append("astore").append(spacer).append(operandDescriptor.getVirtualReg());
                    }
                    return jasminInstruction.toString();
                }
                default -> {
                    return "";
                }
            }
        }
    }

    private String getIndentation() {
        return "\t".repeat(indentation);
    }

    public int getStackCounter() {
        return stackCounter;
    }

}
