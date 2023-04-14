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

import java.util.*;
import java.util.stream.Collectors;

public class MyJmmOptimization implements JmmOptimization {
    private  Map<String, Triple<Boolean,Type,String>> fieldsState = new HashMap<>();
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

        return new OllirResult(ollirCode,jmmSemanticsResult.getConfig());
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
            if(localVarsState.get( "tmp"+ i)==null) {
                String name = "tmp"+1;
                var triple = new Triple<>(true,type,name); //assumed to be immediately initialized
                localVarsState.put(name,triple);
                return triple;
            }
        }
    }
    Pair<String,ArrayList<String>> expressionVisitor(JmmNode node, JmmSemanticsResult semanticsResult, Map<String, Triple<Boolean,Type,String>> localVarsState, String methodName){
        ArrayList<String> previousStatements = new ArrayList<>();
        String result="undefined";
        switch (node.getKind()){//TODO
            case "BinaryOp": {
                Pair<String,ArrayList<String>> exp1 = expressionVisitor(node.getJmmChild(0),semanticsResult,localVarsState,methodName),
                        exp2 = expressionVisitor(node.getJmmChild(1),semanticsResult,localVarsState,methodName);
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
                Pair<String,ArrayList<String>> exp1 = expressionVisitor(node.getJmmChild(0),semanticsResult,localVarsState,methodName),
                        exp2 = expressionVisitor(node.getJmmChild(0),semanticsResult,localVarsState,methodName);
                if( ! ollirToType(exp2.a).getName().equals(".i32"))
                    {result = "Non integer used as array index.";break;}
                //throw new RuntimeException("Non integer used as array index.");
                if(! ollirToType(exp2.a).isArray())
                    {result = "Using index operator[] on a non array.";break;}
                    //throw new RuntimeException("Using index operator[] on a non array.");
                previousStatements = exp1.b;
                previousStatements.addAll(exp2.b);
                result = exp1.a + "["+exp2.a+"].i32";
                break;
            }
            case "LengthOp": {
                var expression = expressionVisitor(node.getJmmChild(0), semanticsResult, localVarsState, methodName);
                previousStatements = expression.b;
                result = "arraylength(" + expression.a + ").i32";
                break;
            }
            case "FuncOp":{ //TODO
                var calledExpression = expressionVisitor(node.getJmmChild(0),semanticsResult,localVarsState,methodName);
                String invokeType, returnType, target = calledExpression.a;
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
                    returnType = typeToOllir(semanticsResult.getSymbolTable().getReturnType(node.get("name")));
                }else {
                    Type idType = ollirToType(calledExpression.a);
                    if(idType.isArray()) {
                        //throw new RuntimeException("Calling function of array");
                        result = "Calling function of array";break;
                    }
                    switch(idType.getName()){
                        case "int":
                        case "boolean":
                            //thow new RuntimeException("Calling function of a primitive type);
                        case "import":
                            invokeType="static";
                            returnType = ".V";
                            target = target.substring(0,target.indexOf('.'));
                            break;
                        default:
                            invokeType="virtual";
                            returnType = ".V";
                            break;

                    }
                    /*
                        if(is var)
                            virtual(var.type,name)
                        else(is import)
                            static(import, name
                     */
                }
                previousStatements = calledExpression.b;
                for(int i =1;i<node.getChildren().size();++i){
                    var expressionResult = expressionVisitor(node.getJmmChild(i),semanticsResult,localVarsState,methodName);
                    previousStatements.addAll(expressionResult.b);
                    arguments.add(expressionResult.a);
                }
                result = "invoke"+ invokeType + "(" + target + ", \"" + node.get("name") + (arguments.size()>0?"\", ":"\"") + arguments.stream().reduce((String s1,String s2)->s1+", "+s2).orElse("") +")" + returnType;
                /*switch(node.getJmmChild(0).getKind()){
                    case "This": { //TODO check arguments match?

                        for(int i =1;i<node.getChildren().size();++i){
                            var expressionResult = expressionVisitor(node.getJmmChild(i),semanticsResult,localVarsState,methodName);
                            previousStatements.addAll(expressionResult.b);
                            arguments.append(", ").append(expressionResult.a);
                        }
                        result = "invokevirtual(this, \"" + node.get("name") + "\"" + arguments + ")" + toOllirType(semanticsResult.getSymbolTable().getReturnType(node.get("name")));
                        break;
                    }
                    case "Identifier":
                    case "NewFunc":{ //TODO check arguments match?
                        StringBuilder arguments = new StringBuilder();
                        var expression0 = expressionVisitor(node.getJmmChild(0),semanticsResult,localVarsState,methodName);
                        previousStatements.addAll(expression0.b);
                        for(int i =1;i<node.getChildren().size();++i){
                            var expressionResult = expressionVisitor(node.getJmmChild(i),semanticsResult,localVarsState,methodName);
                            previousStatements.addAll(expressionResult.b);
                            arguments.append(", ").append(expressionResult.a);
                        }
                        result = "invokespecial("+ expression0.a + ", \"" + node.get("name") + "\"" + arguments + ")" + toOllirType(semanticsResult.getSymbolTable().getReturnType(node.get("name")));
                        break;
                    }
                        //valid
                    case "IndexOp":
                    case "ParOp":
                    case "FuncOp":
                        break;
                        //invalids
                    case "Integer":
                    case "Bool":
                    case "NegationOp":
                    case "NewArr":
                    case "LengthOp":
                    case "BinaryOp":
                        throw new RuntimeException("Invalid target for new keyword.");
                }*/
                break;
            }
            case "NewArr":{
                var expression = expressionVisitor(node.getJmmChild(0),semanticsResult,localVarsState,methodName);
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
                var expression = expressionVisitor(node.getJmmChild(0), semanticsResult, localVarsState, methodName);
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
                return expressionVisitor(node.getJmmChild(0),semanticsResult,localVarsState,methodName);
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
            result = "node:[" + node.getKind() + "]";
        return new Pair<>(result,previousStatements);
    }
    String methodVisitor(JmmNode methodNode, JmmSemanticsResult semanticsResult){
        String methodName = methodNode.get("name");
        Map<String, Triple<Boolean,Type,String>> localVarsState = new HashMap<>();
        for(var aVar:semanticsResult.getSymbolTable().getLocalVariables(methodName))
            localVarsState.put(aVar.getName(), new Triple<>(false, aVar.getType(),aVar.getName()));
        for(int i=0; i<semanticsResult.getSymbolTable().getParameters(methodName).size() ;++i) {
            var aVar= semanticsResult.getSymbolTable().getParameters(methodName).get(i);
            localVarsState.put(aVar.getName(), new Triple<>(true, aVar.getType(), "$"+i+"."+aVar.getName()));
        }
        StringBuilder stringBuilder = new StringBuilder();
        List<JmmNode> children = methodNode.getChildren();
        for(int i=0; i< children.size();++i){
            JmmNode child = children.get(i);
            switch (child.getKind()){
                case "VarDeclareStatement": {
                    var state = localVarsState.get(child.get("name"));
                    var state2 = new Triple<>(true, state.b,state.c);
                    localVarsState.put(child.get("name"), state2);

                    var expression = expressionVisitor(child.getJmmChild(0), semanticsResult, localVarsState,methodNode.get("name"));
                    for (String s : expression.b)
                        stringBuilder.append(s);
                    stringBuilder.append(state.c).append(typeToOllir(state.b)).append(" :=").append(typeToOllir(state.b)).append(" ").append(expression.a).append(";\n");

                    //if(! (getOllirType(expression.a).getName().equals(state.b.getName()) && getOllirType(expression.a).isArray() == state.b.isArray()))
                        //throw new RuntimeException("Assigning mismatching type.");
                    break;
                }
                case "SemiColon": {
                    var statements = expressionVisitor(child.getJmmChild(0),semanticsResult,localVarsState,methodName);
                    for(String s : statements.b)
                        stringBuilder.append(s);
                    if(!statements.a.equals(""))
                        stringBuilder.append(statements.a).append(";\n");
                    break;
                }
                case "Brackets": {
                    List<JmmNode> unvisited = children.subList(i+1,children.size());
                    unvisited.addAll(i+1,child.getChildren());
                    //Untested
                }
                case "IfElseStatement"://TODO
                case "WhileStatement"://TODO
                case "ArrayAccess"://TODO
                    break;


                //catch all 'expression' as this means the node is from the return statement.
                case "BinaryOp":
                case "IndexOp":
                case "LengthOp":
                case "FuncOp":
                case "NewArr":
                case "NewFunc":
                case "NegationOp":
                case "ParOp":
                case "Integer":
                case "Bool":
                case "Identifier":
                case "This": {
                    var expression = expressionVisitor(child, semanticsResult, localVarsState,methodName);
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
}

/*TODO
    extends
    array out of bounds?
    invokes

 */