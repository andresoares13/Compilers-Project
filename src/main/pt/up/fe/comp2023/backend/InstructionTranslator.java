package pt.up.fe.comp2023.backend;

import org.specs.comp.ollir.*;

public class InstructionTranslator {
    private int stackCounter = 1;

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
        return "";
    }

    // NOPER
    public String translateInstruction(SingleOpInstruction instruction, Method method) {
        return "";
    }

    // ASSIGN
    public String translateInstruction(AssignInstruction instruction, Method method) {
        return "";
    }

    // BRANCH
    public String translateInstruction(CondBranchInstruction instruction, Method method) {
        return "";
    }

    // RETURN
    public String translateInstruction(ReturnInstruction instruction, Method method) {
        return "";
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
        return "";
    }

    // BINARYOPER
    public String translateInstruction(BinaryOpInstruction instruction, Method method) {
        return "";
    }

    public int getStackCounter() {
        return stackCounter;
    }

}
