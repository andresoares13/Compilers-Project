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

public class AttributionVisitor extends PreorderJmmVisitor <Integer, Type>{
    List<Report> reports = new ArrayList<>();
    SymbolTable symbolTable;

    public AttributionVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;

    }

    @Override
    protected void buildVisitor() {
        addVisit("VarDeclareStatement", this::visitAttribution);
        addVisit("ArrayAccess", this::visitArrIndexAttribution);
        setDefaultVisit(this::defaultVisitor);
    }

    private Type visitAttribution(JmmNode atribution, Integer dummy){
        Type l = new Type("", false);
        Type r = new Type("", false);
        String name = atribution.get("name");
        int line = Integer.valueOf(atribution.getJmmChild(0).get("lineStart"));
        int col = Integer.valueOf(atribution.getJmmChild(0).get("colStart"));



        JmmNode parent = atribution;
        if (atribution.getJmmParent() != null){
            while(!parent.getKind().equals("MethodDeclare") && !parent.getKind().equals("ImportDeclare") && !parent.getKind().equals("MethodDeclareMain")) {
                if (parent.getJmmParent() == null){
                    break;
                }
                parent = parent.getJmmParent();
            }
        }



        List<String> methods = symbolTable.getMethods();

        List<Symbol> tempSymbolListPar = symbolTable.getParameters(parent.get("name"));
        List<Symbol> tempSymbolListVar = symbolTable.getLocalVariables(parent.get("name"));
        for (int k=0;k<tempSymbolListPar.size();k++){

            if (tempSymbolListPar.get(k).getName().equals(name)){
                l=  tempSymbolListPar.get(k).getType();
            }
        }
        for (int k=0;k<tempSymbolListVar.size();k++){

            if (tempSymbolListVar.get(k).getName().equals(name)){


                l=  tempSymbolListVar.get(k).getType();


            }
        }

        for (int k=0;k<symbolTable.getFields().size();k++){
            if (symbolTable.getFields().get(k).getName().equals(name) && l.getName().equals("")){
                l = symbolTable.getFields().get(k).getType();
                if (parent.get("name").equals("main")){
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Bad Access to field in Static Method"));
                    return new Type(l.getName(), l.isArray());
                }
            }
        }


        switch(atribution.getJmmChild(0).getKind()) {
            case "BinaryOp":{
                BinaryExpressionVisitor binOpSemanticVisitor = new BinaryExpressionVisitor(symbolTable);
                r = binOpSemanticVisitor.visit(atribution.getJmmChild(0), 0);
                break;
            }
            case "Integer":
            case "Bool":
            case "NewArr":
            case "NewFunc":
            case "Identifier":
            case "This":
            case "NegationOp":{
                VariableSemanticVisitor variableSemanticVisitor = new VariableSemanticVisitor(symbolTable);
                r = variableSemanticVisitor.visit(atribution.getJmmChild(0), 0);

                break;
            }
            case "IndexOp":
            case "LengthOp":{
                IndexLengthVisitor indexingSemanticVisitor = new IndexLengthVisitor(symbolTable);
                r = indexingSemanticVisitor.visit(atribution.getJmmChild(0), 0);
                break;
            }
            case "MethodDeclare":
            case "FuncOp":{
                MethodListVisitor methodSemanticVisitor = new MethodListVisitor(symbolTable);
                r = methodSemanticVisitor.visit(atribution.getJmmChild(0), 0);
                break;
            }

            default:{
                r = visit(atribution.getJmmChild(0));
                break;
            }
        }



        if (l.isArray() && !r.isArray() && !atribution.getJmmChild(0).getKind().equals("NewArr"))
        {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Arrays are not allowed to be assigned"));
        }

        if (symbolTable.getImports().contains(r.getName()) && atribution.getJmmChild(0).getKind().equals("FuncOp")){
            r = l;
        }

        if(!l.getName().equals(r.getName()) &&
                ( !symbolTable.getImports().contains(l.getName()) || !symbolTable.getImports().contains(r.getName())) &&
                (!l.getName().equals(symbolTable.getSuper()) || !r.getName().equals(symbolTable.getClassName())) &&
                !(l.isArray() && r.getName().equals("int") && r.isArray())){

            if (atribution.getJmmChild(0).getKind().equals("FuncOp") &&
                    !symbolTable.getMethods().contains(atribution.getJmmChild(0).get("name")) &&
                    !symbolTable.getSuper().equals("") && symbolTable.getImports().contains(symbolTable.getSuper()) &&
                    r.getName().equals(symbolTable.getClassName())){

            }
            else{
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Error in attribuition: assignee is not compatible with the assigned"));
            }





        }
        return new Type(l.getName(), l.isArray());
    }

    private Type visitArrIndexAttribution(JmmNode atribution, Integer dummy) {
        Type l = new Type("", false);
        Type r = new Type("", false);
        Type v = new Type("", false);



        String name = atribution.get("name");
        List<String> methods = symbolTable.getMethods();
        for (int i=0;i<methods.size();i++){
            List<Symbol> tempSymbolListPar = symbolTable.getParameters(methods.get(i));
            List<Symbol> tempSymbolListVar = symbolTable.getLocalVariables(methods.get(i));
            for (int k=0;k<tempSymbolListPar.size();k++){

                if (tempSymbolListPar.get(k).getName().equals(name)){
                    l=  tempSymbolListPar.get(k).getType();
                }
            }
            for (int k=0;k<tempSymbolListVar.size();k++){

                if (tempSymbolListVar.get(k).getName().equals(name)){
                    l=  tempSymbolListVar.get(k).getType();
                }
            }
        }

        for (int i=0;i<symbolTable.getFields().size();i++){
            if (symbolTable.getFields().get(i).getName().equals(name)){
                l = symbolTable.getFields().get(i).getType();
            }
        }

        switch(atribution.getJmmChild(0).getKind()) {
            case "BinaryOp":{
                BinaryExpressionVisitor binOpSemanticVisitor = new BinaryExpressionVisitor(symbolTable);
                r = binOpSemanticVisitor.visit(atribution.getJmmChild(0), 0);
                break;
            }
            case "Integer":
            case "Bool":
            case "NewArr":
            case "NewFunc":
            case "Identifier":
            case "This":
            case "NegationOp":{
                VariableSemanticVisitor variableSemanticVisitor = new VariableSemanticVisitor(symbolTable);
                r = variableSemanticVisitor.visit(atribution.getJmmChild(0), 0);
                break;
            }
            case "IndexOp":
            case "LengthOp":{
                IndexLengthVisitor indexingSemanticVisitor = new IndexLengthVisitor(symbolTable);
                r = indexingSemanticVisitor.visit(atribution.getJmmChild(0), 0);
                break;
            }
            case "MethodDeclare":
            case "FuncOp":{
                MethodListVisitor methodSemanticVisitor = new MethodListVisitor(symbolTable);
                r = methodSemanticVisitor.visit(atribution.getJmmChild(0), 0);
                break;
            }

            default:{
                r = visit(atribution.getJmmChild(0));
                break;
            }
        }

        switch(atribution.getJmmChild(1).getKind()) {
            case "BinaryOp":{
                BinaryExpressionVisitor binOpSemanticVisitor = new BinaryExpressionVisitor(symbolTable);
                v = binOpSemanticVisitor.visit(atribution.getJmmChild(1), 0);

                break;
            }
            case "Integer":
            case "Bool":
            case "NewArr":
            case "NewFunc":
            case "Identifier":
            case "This":
            case "NegationOp":{
                VariableSemanticVisitor variableSemanticVisitor = new VariableSemanticVisitor(symbolTable);
                v = variableSemanticVisitor.visit(atribution.getJmmChild(1), 0);
                break;
            }
            case "IndexOp":
            case "LengthOp":{
                IndexLengthVisitor indexingSemanticVisitor = new IndexLengthVisitor(symbolTable);
                v = indexingSemanticVisitor.visit(atribution.getJmmChild(1), 0);
                break;
            }
            case "MethodDeclare":
            case "FuncOp":{
                MethodListVisitor methodSemanticVisitor = new MethodListVisitor(symbolTable);
                v = methodSemanticVisitor.visit(atribution.getJmmChild(1), 0);
                break;
            }

            default:{
                v = visit(atribution.getJmmChild(1));
                break;
            }
        }

        int line = 1;//Integer.valueOf(atribution.getJmmChild(0).get("line"));
        int col = 1;//Integer.valueOf(atribution.getJmmChild(0).get("col"));

        if (!l.isArray()){

            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Error in attribuition: assignee is not an array"));
        }

        if (!r.getName().equals("int")){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Error in attribuition: attribution index is not int"));
        }

        if (!v.getName().equals("int")){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Error in attribuition: assigned value is not int"));
        }

        return new Type("int", true);
    }

    public List<Report> getReports() {
        return this.reports;
    }

    private Type defaultVisitor(JmmNode node, Integer temp){
        for (JmmNode child : node.getChildren()) {
            visit(child, 0);
        }
        return new Type("",false);
    }


}
