package pt.up.fe.comp2023.visitors;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class VariableSemanticVisitor extends PreorderJmmVisitor<Integer, Type> {
    List<Report> reports = new ArrayList<>();
    SymbolTable symbolTable;

    public VariableSemanticVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;

    }

    @Override
    protected void buildVisitor() {
        addVisit("Integer", this::visitIntLiteral);
        addVisit("Bool", this::visitBooleanType);
        addVisit("NewArr", this::visitNewIntArrVarAttribution);
        addVisit("NewFunc", this::visitNewClassAttribution);
        addVisit("Identifier", this::visitId);
        addVisit("This", this::visitThisExpr);
        addVisit("NegationOp", this::visitNotExpr);
        setDefaultVisit(this::defaultVisitor);
    }

    private Type visitNotExpr(JmmNode visitNotExpr, Integer dummy) {
        Type r = new Type("", false);

        int line = 1;//Integer.valueOf(visitNotExpr.get("line"));
        int col = 1;//Integer.valueOf(visitNotExpr.get("col"));

        switch(visitNotExpr.getJmmChild(0).getKind()) {
            case "BinaryOp": {
                BinaryExpressionVisitor binOpSemanticVisitor = new BinaryExpressionVisitor(symbolTable);
                r = binOpSemanticVisitor.visit(visitNotExpr.getJmmChild(0),0);
                break;
            }
            case "IndexOp":
            case "LengthOp":{
                IndexLengthVisitor indexingSemanticVisitor = new IndexLengthVisitor(symbolTable);
                r = indexingSemanticVisitor.visit(visitNotExpr.getJmmChild(0), 0);
                break;
            }
            case "MethodDeclare":
            case "FuncOp":{
                MethodListVisitor methodSemanticVisitor = new MethodListVisitor(symbolTable);
                r = methodSemanticVisitor.visit(visitNotExpr.getJmmChild(0), 0);
                break;
            }

            default:{
                r = visit(visitNotExpr.getJmmChild(0));
                break;
            }
        }

        if (!r.getName().equals("boolean")) {
            Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Not operation can only be applied to boolean types");
            reports.add(report);
        }

        return new Type("boolean", false);
    }

    private Type visitThisExpr(JmmNode visitThisExpr, Integer dummy) {
        JmmNode parent = visitThisExpr.getJmmParent();


        Integer line = 1;//Integer.valueOf(visitThisExpr.get("line"));
        Integer col = 1;//Integer.valueOf(visitThisExpr.get("col"));
        while(!parent.getKind().equals("MethodDeclare") && !parent.getKind().equals("ImportDeclare") && !parent.getKind().equals("MethodDeclareMain")) {

            if (parent.getJmmParent() == null){
                break;
            }
            parent = parent.getJmmParent();
        }

        if (parent.getKind().equals("MethodDeclareMain")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col,
                    "This keyword cannot be used in main method"));
        }
        return new Type(this.symbolTable.getClassName(), false);
    }

    private Type visitNewClassAttribution(JmmNode newClassAttribution, Integer dummy) {
        return new Type(newClassAttribution.get("name"), false);
    }

    private Type visitNewIntArrVarAttribution(JmmNode newIntArrVarAttribution, Integer dummy) {
        Type varType = null;
        switch(newIntArrVarAttribution.getJmmChild(0).getKind()) {
            case "BinaryOp": {
                BinaryExpressionVisitor binOpSemanticVisitor = new BinaryExpressionVisitor(symbolTable);
                varType = binOpSemanticVisitor.visit(newIntArrVarAttribution.getJmmChild(0),0);
                break;
            }
            case "IndexOp":
            case "LengthOp":{
                IndexLengthVisitor indexingSemanticVisitor = new IndexLengthVisitor(symbolTable);
                varType = indexingSemanticVisitor.visit(newIntArrVarAttribution.getJmmChild(0), 0);
                break;
            }
            case "MethodDeclare":
            case "FuncOp":{
                MethodListVisitor methodSemanticVisitor = new MethodListVisitor(symbolTable);
                varType = methodSemanticVisitor.visit(newIntArrVarAttribution.getJmmChild(0), 0);
                break;
            }


            default:{
                varType = visit(newIntArrVarAttribution.getJmmChild(0));
                break;
            }
        }
        Integer line = 1;//Integer.valueOf(newIntArrVarAttribution.getJmmChild(0).get("line"));

        Integer col = 1;//Integer.valueOf(newIntArrVarAttribution.getJmmChild(0).get("col"));
        if (varType.getName() != "int") {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col,
                    "New array inititalization needs int as size"));
        }
        return new Type("int", true);
    }

    private Type visitId(JmmNode id, Integer dummy) {
        String name = id.get("value");
        int line = 1;//Integer.valueOf(id.get("line"));
        int col = 1;//Integer.valueOf(id.get("col"));

        List<String> methods = symbolTable.getMethods();
        for (int i=0;i<methods.size();i++){
            List<Symbol> tempSymbolListPar = symbolTable.getParameters(methods.get(i));
            List<Symbol> tempSymbolListVar = symbolTable.getLocalVariables(methods.get(i));
            for (int k=0;k<tempSymbolListPar.size();k++){

                if (tempSymbolListPar.get(k).getName().equals(name)){
                    return  tempSymbolListPar.get(k).getType();
                }
            }
            for (int k=0;k<tempSymbolListVar.size();k++){

                if (tempSymbolListVar.get(k).getName().equals(name)){
                    return  tempSymbolListVar.get(k).getType();
                }
            }
        }


        return new Type("none", false);
    }

    private Type visitIntLiteral(JmmNode intLiteral, Integer dummy) {
        return new Type("int", false);
    }

    private Type visitBooleanType(JmmNode booleanType, Integer dummy) {
        return new Type("boolean", false);
    }


    private Type defaultVisitor(JmmNode node, Integer temp){
        for (JmmNode child : node.getChildren()) {
            visit(child, 0);
        }
        return new Type("",false);
    }

    public List<Report> getReports() {
        return this.reports;
    }


}