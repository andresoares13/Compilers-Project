package pt.up.fe.comp2023.visitors;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class BinaryExpressionVisitor extends PreorderJmmVisitor <Integer, Type> {

    List<Report> reports = new ArrayList<>();
    SymbolTable symbolTable;


    public BinaryExpressionVisitor(SymbolTable table){
        this.symbolTable = table;
    }

    @Override
    protected void buildVisitor() {

        addVisit("BinaryOp", this::visitBinaryOp);
        addVisit("ParOp",this::visitParOp);

        setDefaultVisit(this::defaultVisitor);
    }

    private  Type visitParOp(JmmNode node, Integer temp){
        Type type = new Type("",false);

        switch(node.getJmmChild(0).getKind()) {
            case "Integer":
            case "Bool":
            case "Identifier":
            case "NegationOp":
            case "NewArr":
            case "NewFunc":
            case "NotExpr": {
                VariableSemanticVisitor variableSemanticVisitor = new VariableSemanticVisitor(symbolTable);
                type = variableSemanticVisitor.visit(node.getJmmChild(0), 0);

                break;
            }
            case "LengthOp":
            case "IndexOp":{
                IndexLengthVisitor indexingSemanticVisitor = new IndexLengthVisitor(symbolTable);
                type = indexingSemanticVisitor.visit(node.getJmmChild(0), 0);
                break;
            }
            case "MethodDeclare":
            case "FuncOp":{

                MethodListVisitor methodSemanticVisitor = new MethodListVisitor(symbolTable);
                type = methodSemanticVisitor.visit(node.getJmmChild(0), 0);
                break;
            }
            case "This":{
                type = symbolTable.getReturnType(node.getJmmParent().get("name"));
                break;
            }

            case "BinaryOp":{
                BinaryExpressionVisitor binaryVisitor = new BinaryExpressionVisitor(symbolTable);
                type = binaryVisitor.visit(node.getJmmChild(0),0);
                break;
            }

            default: {
                type = visit(node.getJmmChild(0));
                break;
            }
        }

        return type;
    }

    private Type visitBinaryOp(JmmNode node, Integer temp){
        String operand = node.get("op");
        Type leftOperand = new Type("", false);
        Type rightOperand = new Type("", false);



        switch(node.getJmmChild(0).getKind()) {
            case "Integer":
            case "Bool":
            case "Identifier":
            case "NegationOp":
            case "NewArr":
            case "NewFunc":
            case "NotExpr": {
                VariableSemanticVisitor variableSemanticVisitor = new VariableSemanticVisitor(symbolTable);
                leftOperand = variableSemanticVisitor.visit(node.getJmmChild(0), 0);

                break;
            }
            case "LengthOp":
            case "IndexOp":{
                IndexLengthVisitor indexingSemanticVisitor = new IndexLengthVisitor(symbolTable);
                leftOperand = indexingSemanticVisitor.visit(node.getJmmChild(0), 0);
                break;
            }
            case "MethodDeclare":
            case "FuncOp":{

                MethodListVisitor methodSemanticVisitor = new MethodListVisitor(symbolTable);
                leftOperand = methodSemanticVisitor.visit(node.getJmmChild(0), 0);
                break;
            }
            case "This":{
                leftOperand = symbolTable.getReturnType(node.getJmmParent().get("name"));
                break;
            }

            case "ParOp":{
                BinaryExpressionVisitor binaryVisitor = new BinaryExpressionVisitor(symbolTable);
                leftOperand = binaryVisitor.visit(node.getJmmChild(0).getJmmChild(0),0);
                break;
            }

            default: {
                leftOperand = visit(node.getJmmChild(0));
                break;
            }
        }



        switch(node.getJmmChild(1).getKind()) {
            case "Integer":
            case "Bool":
            case "Identifier":
            case "NegationOp":
            case "NewArr":
            case "NewFunc":{
                VariableSemanticVisitor variableSemanticVisitor = new VariableSemanticVisitor(symbolTable);
                rightOperand = variableSemanticVisitor.visit(node.getJmmChild(1), 0);

                break;
            }
            case "LengthOp":
            case "IndexOp":{
                IndexLengthVisitor indexingSemanticVisitor = new IndexLengthVisitor(symbolTable);
                rightOperand = indexingSemanticVisitor.visit(node.getJmmChild(1), 0);
                break;
            }
            case "MethodDeclare":
            case "FuncOp":{
                MethodListVisitor methodSemanticVisitor = new MethodListVisitor(symbolTable);
                rightOperand = methodSemanticVisitor.visit(node.getJmmChild(1), 0);
                break;
            }
            case "This":{
                rightOperand = symbolTable.getReturnType(node.getJmmParent().get("name"));
                break;
            }
            case "ParOp":{
                BinaryExpressionVisitor binaryVisitor = new BinaryExpressionVisitor(symbolTable);
                rightOperand = binaryVisitor.visit(node.getJmmChild(1).getJmmChild(0),0);
                break;
            }

            default:{
                rightOperand = visit(node.getJmmChild(1));
                break;
            }
        }


        int lineLeft = Integer.parseInt(node.getJmmChild(0).get("lineStart"));
        int colLeft = Integer.parseInt(node.getJmmChild(0).get("colStart"));
        int lineRight = Integer.parseInt(node.getJmmChild(1).get("lineEnd"));
        int colRight = Integer.parseInt(node.getJmmChild(1).get("colEnd"));





        if (node.getJmmParent().getKind().equals("FuncOp") && operand.equals("<")){
            MethodListVisitor tempMethodVisitor = new MethodListVisitor(symbolTable);
            rightOperand= tempMethodVisitor.visit(node.getJmmParent(),0);

        }


        if(!leftOperand.getName().equals(rightOperand.getName())){

            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, lineRight, colRight, "Error in operation " + operand + " : operands have different types"));
        }
        else if( ( leftOperand.isArray() || rightOperand.isArray() ) && ( operand.equals("+") || operand.equals("-") || operand.equals("*") || operand.equals("/") || operand.equals("<") ) ) {
            if (!(rightOperand.isArray() && node.getJmmParent().getKind().equals("LengthOp"))){
                if (!(rightOperand.isArray() && node.getJmmParent().getKind().equals("IndexOp") && (node.getJmmParent().getJmmChild(1).getKind().equals("Integer") || node.getJmmParent().getJmmChild(1).getKind().equals("Identifier")))){
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, lineLeft, colLeft, "Error in operation " + operand + " : array cannot be used in this operation"));
                }

            }

        }
        else if(!leftOperand.getName().equals("int") && ( operand.equals("+") || operand.equals("-") || operand.equals("*") || operand.equals("/") || operand.equals("<") ) ) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, lineLeft, colLeft, "Error in operation " + operand + " : operands have invalid types for this operation. " + operand + " expects operands of type integer"));
        }
        else if(!leftOperand.getName().equals("boolean") && ( operand.equals("&&") || operand.equals("!") )) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, lineLeft, lineLeft, "Error in operation "+ operand + " : operands have invalid types for this operation. " + operand + " expects operands of type boolean"));
        }
        else {
            switch(operand) {
                case "<":
                case "&&":
                    return new Type("boolean", false);
                case "+":
                case "-":
                case "/":
                case "*":
                    return new Type("int", false);
                default:
                    return new Type(leftOperand.getName(), leftOperand.isArray());
            }
        }
        return new Type(leftOperand.getName(),leftOperand.isArray());
    }

    private Type defaultVisitor(JmmNode node, Integer temp){
        Type type = new Type("",false);
        for (JmmNode child : node.getChildren()) {
            type=visit(child, 0);
        }
        return type;
    }

    public List<Report> getReports() {
        return this.reports;
    }
}
