package pt.up.fe.comp2023;

import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.misc.Triple;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;

import java.util.*;
import java.util.stream.Collectors;

public class MyJmmOptimization implements JmmOptimization {
    private  Map<String, Triple<Boolean,Type,String>> fieldsState = new HashMap<>();
    private int ifLabelCount= 0, whileLabelCount =0;
    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        return JmmOptimization.super.optimize(semanticsResult);
    }

    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {
        StringBuilder codeBuilder = new StringBuilder();
        SymbolTable st = jmmSemanticsResult.getSymbolTable();
        for(String s:st.getImports()) {
            codeBuilder.append("import ").append(s).append(";\n");
        }
        codeBuilder.append(st.getClassName()).append(st.getSuper().equals("") ? "" : " extends " + st.getSuper()).append(" {\n");
        for(Symbol v:st.getFields()){
            codeBuilder.append(".field private ").append(v.getName()).append(typeToOllir(v.getType())).append(";\n");
            fieldsState.put(v.getName(), new Triple<>(false,v.getType(),v.getName()));
        }
        codeBuilder.append(".construct ").append(st.getClassName()).append("().V {\n").append("invokespecial(this, \"<init>\").V;\n").append("}\n");

        ArrayList<JmmNode> methodNodeList = new ArrayList<>(
                    jmmSemanticsResult.getRootNode().getChildren().stream().filter(
                        (n) -> n.getKind().equals("ClassDeclare")
                    ).collect(
                        ArrayList<JmmNode>::new,
                        (arr, node) -> arr.addAll(
                            node.getChildren().stream().filter(
                                    (m) -> m.getKind().startsWith("MethodDeclare")
                                ).collect(Collectors.toList())),
                        ArrayList::addAll
                    )
        );
        for(JmmNode methodNode : methodNodeList) {
            String methodName = methodNode.get("name");
            codeBuilder.append(".method ");
            if(methodNode.get("isPublic").equals("true")) {
                codeBuilder.append("public ");
            }
            else {
                codeBuilder.append("private ");
            }

            if(methodName.equals("main"))
                codeBuilder.append("static ");

            codeBuilder.append(methodName).append("(");
            boolean first=true;
            for(Symbol p:st.getParameters(methodName)){
                if(first)
                    first=false;
                else
                    codeBuilder.append(", ");
                codeBuilder.append(p.getName()).append(typeToOllir(p.getType()));
            }
            codeBuilder.append(")");
            codeBuilder.append(typeToOllir(st.getReturnType(methodName)));
            codeBuilder.append(" {\n");

            codeBuilder.append(methodVisitor(methodNode,jmmSemanticsResult));

            codeBuilder.append("}\n");
        }
        codeBuilder.append("}\n");

        String ollirCode = codeBuilder.toString();

        //return new OllirResult(ollirCode,jmmSemanticsResult.getConfig());
        return new OllirResult(jmmSemanticsResult,ollirCode,jmmSemanticsResult.getReports());
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        return JmmOptimization.super.optimize(ollirResult);
    }

    String typeToOllir(Type type){
        StringBuilder sb=new StringBuilder();
        if(type.isArray())
            sb.append(".array");

        switch (type.getName()) {
            case "int" -> sb.append(".i32");
            case "boolean" -> sb.append(".bool");
            case "void" -> sb.append(".V");
            default -> sb.append(".").append(type.getName());
        }
        return sb.toString();
    }
    Type ollirToType(String s){
        int lastPeriod = s.lastIndexOf('.'),
                secondLastPeriod = s.lastIndexOf('.',lastPeriod-1);
        boolean isArray = false;
        if(lastPeriod == -1) {
            throw new RuntimeException("String is not an Ollir Type.");
        }
        if(secondLastPeriod != -1){
            if(s.substring(secondLastPeriod,lastPeriod).equals(".array"))
                isArray = true;
        }
        String end = s.substring(lastPeriod);
        if(end.equals(".i32"))
            return new Type("int",isArray);
        if(end.equals(".bool"))
            return new Type("boolean",isArray);
        return new Type(end.substring(1),isArray);
    }
    String getTypeDefaultValue(Type type){
        return switch (type.getName()) {
            case "int" -> "0.i32";
            case "boolean" -> "1.bool";
            default -> "";
        };
    }
    JmmNode getMethodNode(JmmSemanticsResult semanticsResult, String method ){
        JmmNode root= semanticsResult.getRootNode();
        for(JmmNode rootChild : root.getChildren()){
            if(rootChild.getKind().equals("ClassDeclare")) {
                for(JmmNode classNode : rootChild.getChildren()) {
                    if(classNode.getKind().equals("MethodDeclare")){
                        if(classNode.get("name").equals(method))
                            return classNode;
                    }
                    if(method.equals("main") && classNode.getKind().equals("MethodDeclareMain")){
                        return classNode;
                    }
                }
            }
        }
        return null;
    }
    Symbol getMethodVariable(JmmSemanticsResult semanticsResult, String method, String localVariable){
        Optional<Symbol> first = semanticsResult.getSymbolTable().getLocalVariables(method).stream().filter((n) -> n.getName().equals(localVariable)).findFirst();
        return first.orElse(null);
    }
    Pair<Boolean,Triple<Boolean,Type,String>> getVariableState(String variableName, Map<String,Triple<Boolean,Type,String>> methodVarsState){
        var local = methodVarsState.get(variableName);
        if(local!=null)
            return new Pair<>(false, // not a field
                    local);
        var field = fieldsState.get(variableName);
        if(field!=null)
            return new Pair<>(true, // is a field
                field);
        else
            return null; //check if it's an import.
    }
    Triple<Boolean,Type,String> addTemporaryVariable(Map<String, Triple<Boolean,Type,String>> localVarsState, Type type){
        for(int i=0;;++i){
            if(!localVarsState.containsKey( "tmp"+ i)) {
                String name = "tmp"+i;
                var triple = new Triple<>(true,type,name); //assumed to be immediately initialized
                localVarsState.put(name,triple);
                return triple;
            }
        }
    }
    Pair<String,ArrayList<String>> expressionVisitor(JmmNode node, JmmSemanticsResult semanticsResult, Map<String, Triple<Boolean,Type,String>> localVarsState){
        ArrayList<String> previousStatements = new ArrayList<>();
        String result="undefined";
        switch (node.getKind()){
            case "BinaryOp": {
                Pair<String,ArrayList<String>> exp1 = expressionVisitor(node.getJmmChild(0),semanticsResult,localVarsState),
                        exp2 = expressionVisitor(node.getJmmChild(1),semanticsResult,localVarsState);
                previousStatements = exp1.b;
                previousStatements.addAll(exp2.b);
                String op = node.get("op");
                Type opType;
                if(op.equals("<") || op.equals("&&"))
                    opType = new Type("boolean",false);
                else
                    opType = new Type("int",false);
                var tmp = addTemporaryVariable(localVarsState,opType);
                previousStatements.add(
                        tmp.c + typeToOllir(tmp.b) + " :=" + typeToOllir(tmp.b) + " "
                        + exp1.a + " " + op + typeToOllir(opType) + " " + exp2.a + ";\n"
                );
                result = tmp.c + typeToOllir(tmp.b);
                break;
            }
            case "IndexOp":{
                Pair<String,ArrayList<String>> exp1 = expressionVisitor(node.getJmmChild(0),semanticsResult,localVarsState),
                        exp2 = expressionVisitor(node.getJmmChild(1),semanticsResult,localVarsState);
                if( ! ollirToType(exp2.a).getName().equals("int"))
                    {result = "Non integer used as array index.";break;}
                //throw new RuntimeException("Non integer used as array index.");
                if(! ollirToType(exp1.a).isArray())
                    {result = "Using index operator[] on a non array.";break;}
                    //throw new RuntimeException("Using index operator[] on a non array.");
                previousStatements = exp1.b;
                previousStatements.addAll(exp2.b);
                var typeExp1 = ollirToType(exp1.a);
                String target;
                if(node.getJmmChild(1).getKind().equals("Integer")){
                    var tmp=addTemporaryVariable(localVarsState,new Type("int",false));
                    target = tmp.c + typeToOllir(tmp.b);
                    previousStatements.add(target + " :=.i32 " + exp2.a + ";\n");
                }else{
                    target = exp2.a;
                }
                result = exp1.a.substring(0,exp1.a.length()-typeToOllir(typeExp1).length()) + "["+target+"].i32";
                break;
            }
            case "LengthOp": {
                var expression = expressionVisitor(node.getJmmChild(0), semanticsResult, localVarsState);
                previousStatements = expression.b;
                var tmp = addTemporaryVariable(localVarsState,ollirToType(".i32"));
                previousStatements.add(tmp.c+ typeToOllir(tmp.b)+" :=.i32 arraylength(" + expression.a + ").i32;\n");
                result = tmp.c + typeToOllir(tmp.b);
                break;
            }
            case "FuncOp":{
                var calledExpression = expressionVisitor(node.getJmmChild(0),semanticsResult,localVarsState);
                String invokeType, target = calledExpression.a;
                Type returnType;
                List<String> arguments = new ArrayList<>();
                if(calledExpression.a.equals("this")){
                    var calledMethod = getMethodNode(semanticsResult,node.get("name"));
                    if(calledMethod == null){
                        result = "Method does not exist";
                        break;
                        //throw new RuntimeException("Method does not exist");
                    }
                    if(calledMethod.get("isPublic").equals("true")){
                        invokeType = "virtual";
                    }else{
                        invokeType = "special";
                    }
                    returnType = semanticsResult.getSymbolTable().getReturnType(node.get("name"));
                }else {
                    Type idType = ollirToType(calledExpression.a);
                    if(idType.isArray()) {
                        //throw new RuntimeException("Calling function of array");
                        result = "Calling function of array";break;
                    }
                    switch(idType.getName()){
                        case "int":
                        case "boolean":
                            //throw new RuntimeException("Calling function of a primitive type);
                        case "import":
                            invokeType="static";
                            returnType = new Type("void",false);
                            target = target.substring(0,target.indexOf('.'));
                            break;
                        default:
                            invokeType="virtual";
                            returnType = new Type("void",false);
                            break;
                    }
                }
                previousStatements = calledExpression.b;
                for(int i =1;i<node.getChildren().size();++i){
                    var expressionResult = expressionVisitor(node.getJmmChild(i),semanticsResult,localVarsState);
                    previousStatements.addAll(expressionResult.b);
                    if(node.getJmmChild(i).getKind().equals("IndexOp")){
                        var tmp = addTemporaryVariable(localVarsState,ollirToType(expressionResult.a));
                        previousStatements.add(tmp.c+typeToOllir(tmp.b)+" :="+typeToOllir(tmp.b) + " " + expressionResult.a + ";\n");
                        arguments.add(tmp.c+typeToOllir(tmp.b));
                    }else {
                        arguments.add(expressionResult.a);
                    }
                }

                if (invokeType.equals("static") || node.getJmmParent().getKind().equals("SemiColon")) {
                    result = "invoke"+ invokeType + "(" + target + ", \"" + node.get("name") + (arguments.size()>0?"\", ":"\"") + arguments.stream().reduce((String s1,String s2)->s1+", "+s2).orElse("") +")" + typeToOllir(returnType);
                } else {
                    var tmp = addTemporaryVariable(localVarsState, returnType);
                    previousStatements.add(tmp.c + typeToOllir(tmp.b) + " :=" + typeToOllir(tmp.b) +
                            " invoke" + invokeType + "(" + target + ", \"" + node.get("name") + (arguments.size() > 0 ? "\", " : "\"") + arguments.stream().reduce((String s1, String s2) -> s1 + ", " + s2).orElse("") + ")" + typeToOllir(returnType) + ";\n");
                    result = tmp.c + typeToOllir(tmp.b);
                }
                break;
            }
            case "NewArr":{
                var expression = expressionVisitor(node.getJmmChild(0),semanticsResult,localVarsState);
                previousStatements = expression.b;
                result = "new(array, " + expression.a + ").array.i32";
                break;
            }
            case "NewFunc": {
                var tmp = addTemporaryVariable(localVarsState, new Type(node.get("name"), false));
                previousStatements.add(tmp.c + typeToOllir(tmp.b) + " :=" + typeToOllir(tmp.b) + " new(" + tmp.b.getName() + ")" + typeToOllir(tmp.b) + ";\n");
                previousStatements.add("invokespecial(" + tmp.c + typeToOllir(tmp.b) + ", \"<init>\").V;\n");
                result = tmp.c + typeToOllir(tmp.b);
                break;
            }
            case "NegationOp": {
                var expression = expressionVisitor(node.getJmmChild(0), semanticsResult, localVarsState);
                if( ! ollirToType(expression.a).getName().equals(".bool")){
                    result = "Negation operator on a non boolean.";break;
                    //throw new RuntimeException("Negation operator on a non boolean.");
                }
                var tmp = addTemporaryVariable(localVarsState, ollirToType(expression.a));
                previousStatements = expression.b;
                previousStatements.add(tmp.c + ".bool" + " !=.bool " + expression.a + ";\n");
                result = tmp.c + ".bool";
                break;
            }
            case "ParOp":
                return expressionVisitor(node.getJmmChild(0),semanticsResult,localVarsState);
            case "Integer":
                result = node.get("value")+".i32";
                break;
            case "Identifier": {
                var varState = getVariableState(node.get("value"), localVarsState);
                if(varState== null){ //check imports
                    var imported = semanticsResult.getSymbolTable().getImports().stream().filter((String n)-> n.equals(node.get("value"))).findFirst();
                    if(imported.isEmpty()) {
                        result = "Undeclared identifier.";break;
                        //throw new RuntimeException("Undeclared identifier.");
                    }
                    else
                        result = imported.get() + ".import";
                    break;
                }
                if (varState.a) { //is Field
                    result = "getfield(this," + varState.b.c + typeToOllir(varState.b.b) + ")" + typeToOllir(varState.b.b);
                } else {
                    if (!varState.b.a) { //not initialized
                        previousStatements.add(
                                varState.b.c + typeToOllir(varState.b.b)
                                        + " :=" + typeToOllir(varState.b.b)
                                        + " " + getTypeDefaultValue(varState.b.b) + ";\n"
                        );
                    }
                    result = varState.b.c + typeToOllir(varState.b.b);
                }
                break;
            }
            case "This":
                result = "this";
                break;
            case "Bool":
                result = node.get("value")+".bool";
                break;
        }
        if(result.equals("undefined"))
            result = "node:[" + node.getKind() + "]"; //throw new RuntimeException();
        return new Pair<>(result,previousStatements);
    }
    String statementsVisitor(List<JmmNode> nodeList, JmmSemanticsResult semanticsResult, Map<String, Triple<Boolean,Type,String>> localVarsState, String methodName){
        StringBuilder stringBuilder = new StringBuilder();
        for(JmmNode child : nodeList){
            switch (child.getKind()) {
                case "VarDeclareStatement": {
                    var state = localVarsState.get(child.get("name"));
                    var state2 = new Triple<>(true, state.b, state.c);
                    localVarsState.put(child.get("name"), state2);

                    var expression = expressionVisitor(child.getJmmChild(0), semanticsResult, localVarsState);
                    for (String s : expression.b)
                        stringBuilder.append(s);
                    stringBuilder.append(state.c).append(typeToOllir(state.b)).append(" :=").append(typeToOllir(state.b)).append(" ").append(expression.a).append(";\n");

                    //if(! (getOllirType(expression.a).getName().equals(state.b.getName()) && getOllirType(expression.a).isArray() == state.b.isArray()))
                    //throw new RuntimeException("Assigning mismatching type.");
                    break;
                }
                case "SemiColon": {
                    var statements = expressionVisitor(child.getJmmChild(0), semanticsResult, localVarsState);
                    for (String s : statements.b)
                        stringBuilder.append(s);
                    if (!statements.a.equals(""))
                        stringBuilder.append(statements.a).append(";\n");
                    break;
                }
                case "Brackets": {
                    stringBuilder.append(statementsVisitor(child.getChildren(), semanticsResult, localVarsState, methodName));
                    //Untested
                    break;
                }
                case "ArrayAccess": {
                    var arrayVar = getVariableState(child.get("name"), localVarsState);
                    Pair<String, ArrayList<String>> expIndex = expressionVisitor(child.getJmmChild(0), semanticsResult, localVarsState),
                            expValue = expressionVisitor(child.getJmmChild(1), semanticsResult, localVarsState);
                    for (String s : expIndex.b)
                        stringBuilder.append(s);
                    for (String s : expValue.b)
                        stringBuilder.append(s);
                    String target;
                    if(child.getJmmChild(0).getKind().equals("Integer")){
                        var tmp = addTemporaryVariable(localVarsState,new Type("int",false));
                        target = tmp.c + typeToOllir(tmp.b);
                        stringBuilder.append(target).append(" :=").append(typeToOllir(tmp.b)).append(" ").append(expIndex.a).append(";\n");
                    }else{
                        target = expIndex.a;
                    }

                    stringBuilder.append(arrayVar.b.c).append("[").append(target).append("]").append(typeToOllir(new Type(arrayVar.b.b.getName(), false)))
                            .append(" :=").append(typeToOllir(new Type(arrayVar.b.b.getName(),false))).append(" ")
                            .append(expValue.a).append(";\n");
                    break;
                }
                case "IfElseStatement": {
                    var ifExpression = expressionVisitor(child.getJmmChild(0),semanticsResult,localVarsState);
                    for(String s: ifExpression.b)
                        stringBuilder.append(s);
                    int ifLabel = ifLabelCount;
                    ifLabelCount++;
                    stringBuilder.append("if(").append(ifExpression.a).append(") goto ifbody_").append(ifLabel).append(";\n");
                    stringBuilder.append(statementsVisitor(new ArrayList<>(Collections.singleton(child.getJmmChild(1))),semanticsResult,localVarsState,methodName));
                    stringBuilder.append("goto endif_").append(ifLabel).append(";\n");
                    stringBuilder.append("ifbody_").append(ifLabel).append(":\n");
                    stringBuilder.append(statementsVisitor(new ArrayList<>(Collections.singleton(child.getJmmChild(2))),semanticsResult,localVarsState,methodName));
                    stringBuilder.append("endif_").append(ifLabel).append(":\n"); //TODO append NoOp if last instruction in method.
                    break;
                }
                case "WhileStatement": {
                    var whileExpression = expressionVisitor(child.getJmmChild(0),semanticsResult,localVarsState);
                    for(String s: whileExpression.b)
                        stringBuilder.append(s);
                    int whileLabel = whileLabelCount;
                    whileLabelCount++;

                    stringBuilder.append("if(").append(whileExpression.a).append(") goto whilebody_").append(whileLabel).append(";\n");
                    stringBuilder.append("goto endwhile_").append(whileLabel).append(";\n");
                    stringBuilder.append("whilebody_").append(whileLabel).append(":\n");
                    stringBuilder.append(statementsVisitor(new ArrayList<>(Collections.singleton(child.getJmmChild(1))),semanticsResult,localVarsState,methodName));
                    stringBuilder.append("endwhile_").append(whileLabel).append(":\n");
                    break; //TODO append NoOp if last instruction in method.
                }

                    //catch all 'expression' types, as this means the node is a return statement.
                case "BinaryOp": case "IndexOp": case "LengthOp": case "FuncOp": case "NewArr":
                case "NewFunc": case "NegationOp": case "ParOp": case "Integer": case "Bool":
                case "Identifier": case "This": {
                    var expression = expressionVisitor(child, semanticsResult, localVarsState);
                    for (String s : expression.b)
                        stringBuilder.append(s);
                    var retType = typeToOllir(semanticsResult.getSymbolTable().getReturnType(methodName));
                    stringBuilder.append("ret").append(retType).append(" ").append(expression.a).append(";\n");

                    break;
                }
            }
        }
        return stringBuilder.toString();
    }
    String methodVisitor(JmmNode methodNode, JmmSemanticsResult semanticsResult){
        String methodName = methodNode.get("name");
        Map<String, Triple<Boolean,Type,String>> localVarsState = new HashMap<>();
        for(var aVar:semanticsResult.getSymbolTable().getLocalVariables(methodName))
            localVarsState.put(aVar.getName(), new Triple<>(false, aVar.getType(),aVar.getName()));
        for(int i=0; i<semanticsResult.getSymbolTable().getParameters(methodName).size() ;++i) {
            var aVar= semanticsResult.getSymbolTable().getParameters(methodName).get(i);
            localVarsState.put(aVar.getName(), new Triple<>(true, aVar.getType(), "$"+(i+1)+"."+aVar.getName()));
        }
        return statementsVisitor(methodNode.getChildren(),semanticsResult,localVarsState,methodName);
    }
}
