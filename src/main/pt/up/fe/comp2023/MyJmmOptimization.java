package pt.up.fe.comp2023;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MyJmmOptimization implements JmmOptimization {
    private final VarContext fieldsState = new VarContext();
    private int ifLabelCount= 0, whileLabelCount =0;

    private boolean oFlagValue = false;
    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        return JmmOptimization.super.optimize(semanticsResult);
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        return MyRegisterAllocation.optimize(ollirResult);
    }

    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {
        StringBuilder codeBuilder = new StringBuilder();
        {
            String optimize = jmmSemanticsResult.getConfig().get("optimize");
            if (optimize != null) {
                oFlagValue = Boolean.parseBoolean(optimize);
            }
        }
        SymbolTable st = jmmSemanticsResult.getSymbolTable();
        for(String s:st.getImports()) {
            codeBuilder.append("import ").append(s).append(";\n");
        }
        codeBuilder.append(st.getClassName()).append(st.getSuper().equals("") ? "" : " extends " + st.getSuper()).append(" {\n");
        for(Symbol v:st.getFields()){
            codeBuilder.append(".field private ").append(v.getName()).append(typeToOllir(v.getType())).append(";\n");
            fieldsState.put(v.getName(), new VarState(false,v.getType(),v.getName(),false, false, false));
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
    Pair<Boolean, VarState> getVariableState(String variableName, VarContext methodVarsState){
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
    void setVariableInitialized(String variableName, VarContext methodVarsState){
        boolean isLocal = methodVarsState.containsKey(variableName);
        VarState state;
        if(isLocal){
            state = methodVarsState.get(variableName);
        }else{
            state = fieldsState.get(variableName);
        }
        state.isInitialized = true;
    }
    VarState addTemporaryVariable(VarContext localVarsState, Type type){
        for(int i=0;;++i){
            VarState state = localVarsState.get("tmp" + i);
            if(state == null) {
                String name = "tmp" + i;
                VarState addedTemp = new VarState(true,type,name,false, false, true);
                localVarsState.put(name,addedTemp); //assumed to be immediately initialized
                return addedTemp;
            }
            if(state.isOpen) {
                state.isOpen = false;
                state.type = type;
                return state;
            }
        }
    }
    void releaseTemporaryVariable(VarState var){
        var.isOpen = true;
    }
    StringableResult expressionVisitor(JmmNode node, JmmSemanticsResult semanticsResult, VarContext localVarsState){
        LinkedList<Stringable> previousStatements = new LinkedList<>();
        ArrayList<VarState> previousReleases = new ArrayList<>();
        Object result = "undefined";
        boolean isConstant = false, resultIsString = true;
        Type resultType = null;
        switch (node.getKind()){
            case "BinaryOp": {
                StringableResult exp1 = expressionVisitor(node.getJmmChild(0),semanticsResult,localVarsState),
                        exp2 = expressionVisitor(node.getJmmChild(1),semanticsResult,localVarsState);
                previousStatements = exp1.previousStatements;
                previousStatements.addAll(exp2.previousStatements);
                previousReleases = exp1.freeVars;
                previousReleases.addAll(exp2.freeVars);
                String op = node.get("op");
                if(op.equals("<") || op.equals("&&"))
                    resultType = new Type("boolean",false);
                else
                    resultType = new Type("int",false);

                isConstant = exp1.isConstant && exp2.isConstant && oFlagValue;
                if(isConstant){
                    switch(op){
                        case "<":
                            result = (Integer.parseInt(exp1.result.substring(0,exp1.result.lastIndexOf("."))) < Integer.parseInt(exp2.result.substring(0,exp2.result.lastIndexOf("."))) ? "1":"0")+ ".bool";
                            break;
                        case "&&":
                            result = (exp1.result.equals("1.bool") && exp2.result.equals("1.bool")?"1":"0") + ".bool";
                            break;
                        case "+":
                            result = (Integer.parseInt(exp1.result.substring(0,exp1.result.lastIndexOf("."))) + Integer.parseInt(exp2.result.substring(0,exp2.result.lastIndexOf(".")))) + ".i32";
                            break;
                        case "-":
                            result = (Integer.parseInt(exp1.result.substring(0,exp1.result.lastIndexOf("."))) - Integer.parseInt(exp2.result.substring(0,exp2.result.lastIndexOf(".")))) + ".i32";
                            break;
                        case "/":
                            if(exp2.result.equals("0.i32")){
                                //divide by 0 Error.
                                throw new RuntimeException("Divide by 0. in " + node.toString());
                            }
                            result = (Integer.parseInt(exp1.result.substring(0, exp1.result.lastIndexOf("."))) / Integer.parseInt(exp2.result.substring(0, exp2.result.lastIndexOf(".")))) + ".i32";
                            break;
                        case "*":
                            result = (Integer.parseInt(exp1.result.substring(0, exp1.result.lastIndexOf("."))) * Integer.parseInt(exp2.result.substring(0, exp2.result.lastIndexOf(".")))) + ".i32";
                            break;
                    }
                    break;
                }
                result = exp1.result + " " + op + typeToOllir(resultType) + " " + exp2.result;
                if(oFlagValue){
                    switch (op){
                        case "&&":
                            if((exp1.isConstant && exp1.result.equals("0.bool")) || (exp2.isConstant && exp2.result.equals("0.bool"))){
                                isConstant = true;
                                result = "0.bool";
                            }
                            break;
                        case "+":
                            if(exp1.isConstant && exp1.result.equals("0.i32")){
                                result = exp2.result;
                                break;
                            }if(exp2.isConstant && exp2.result.equals("0.i32")){
                            result = exp1.result;
                            break;
                        }
                            break;
                        case "-":
                            if(exp2.isConstant && exp2.result.equals("0.i32")){
                                result = exp1.result;
                                break;
                            }
                            break;
                        case "/":
                            if(exp2.isConstant && exp2.result.equals("1.i32")){
                                result = exp1.result;
                                break;
                            }
                            if(exp2.result.equals("0.i32")){
                                //divide by 0 Error.
                            }
                            break;
                        case "*":
                            if(exp1.isConstant && exp1.result.equals("1.i32")){
                                result = exp2.result;
                                break;
                            }if(exp2.isConstant && exp2.result.equals("1.i32")){
                            result = exp1.result;
                            break;
                        }
                        default:
                    }
                }
                if(   !node.getJmmParent().getKind().equals("VarDeclareStatement")){ //needs to be put in a temp variable.
                    var tmp = addTemporaryVariable(localVarsState, resultType);
                    previousReleases.add(tmp);

                    previousStatements.add(
                            new Stringable(
                                tmp.name + typeToOllir(tmp.type) + " :=" + typeToOllir(tmp.type) + " "
                                    + exp1.result + " " + op + typeToOllir(resultType) + " " + exp2.result + ";\n")
                    );
                    result = tmp.name + typeToOllir(tmp.type);
                }
                break;
            }
            case "IndexOp":{
                StringableResult expArray = expressionVisitor(node.getJmmChild(0),semanticsResult,localVarsState),
                        expIndex = expressionVisitor(node.getJmmChild(1),semanticsResult,localVarsState);
                if( ! ollirToType(expIndex.result).getName().equals("int"))
                    {result = "Non integer used as array index.";break;}
                //throw new RuntimeException("Non integer used as array index.");
                if(! ollirToType(expArray.result).isArray())
                    {result = "Using index operator[] on a non array.";break;}
                    //throw new RuntimeException("Using index operator[] on a non array.");
                previousStatements = expArray.previousStatements;
                previousStatements.addAll(expIndex.previousStatements);
                previousReleases = expArray.freeVars;
                previousReleases.addAll(expIndex.freeVars);
                result = expArray.result.substring(0,expArray.result.length()-typeToOllir(expArray.type).length()) + "["+expIndex.result+"].i32";
                if(
                        (node.getJmmParent().getKind().equals("ArrayAccess")  && node.getJmmParent().getJmmChild(0) == node)
                        || node.getJmmParent().getKind().equals("IndexOp")
                ){ //creates a temp variable if this IndexOp is itself an IndexOp
                    var tmp=addTemporaryVariable(localVarsState,new Type("int",false));
                    previousReleases.add(tmp);
                    String target = tmp.name + typeToOllir(tmp.type);
                    previousStatements.add(
                            new Stringable(target + " :=.i32 " + result + ";\n")
                    );
                    result = target;
                }
                resultType = new Type("int",false);
                break;
            }
            case "LengthOp": {
                var expression = expressionVisitor(node.getJmmChild(0), semanticsResult, localVarsState);
                previousStatements = expression.previousStatements;
                previousReleases = expression.freeVars;
                var tmp = addTemporaryVariable(localVarsState,ollirToType(".i32"));
                previousReleases.add(tmp);
                previousStatements.add(
                        new Stringable(tmp.name + typeToOllir(tmp.type)+" :=.i32 arraylength(" + expression.result + ").i32;\n")
                );
                result = tmp.name + typeToOllir(tmp.type);
                resultType = new Type("int",false);
                break;
            }
            case "FuncOp":{
                var calledExpression = expressionVisitor(node.getJmmChild(0),semanticsResult,localVarsState);
                previousStatements = calledExpression.previousStatements;
                previousReleases = calledExpression.freeVars;
                String invokeType,
                        target = calledExpression.result;
                List<String> arguments = new ArrayList<>();
                if(calledExpression.result.equals("this")){
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
                    resultType = semanticsResult.getSymbolTable().getReturnType(node.get("name"));
                }else {
                    if(calledExpression.type.isArray()) {
                        //throw new RuntimeException("Calling function of array");
                        result = "Calling function of array";break;
                    }
                    switch(calledExpression.type.getName()){
                        case "int":
                        case "boolean":
                            //throw new RuntimeException("Calling function of a primitive type);
                        case "import":
                            invokeType="static";
                            resultType = new Type("void",false);
                            break;
                        default:
                            invokeType="virtual";
                            if(calledExpression.type.getName().equals(semanticsResult.getSymbolTable().getClassName()))
                                resultType = semanticsResult.getSymbolTable().getReturnType(node.get("name"));
                            else
                                resultType = new Type("void",false);
                            break;
                    }
                }
                for(int i =1;i<node.getChildren().size();++i){
                    var expressionResult = expressionVisitor(node.getJmmChild(i),semanticsResult,localVarsState);
                    previousStatements.addAll(expressionResult.previousStatements);
                    previousReleases.addAll(expressionResult.freeVars);
                    if(node.getJmmChild(i).getKind().equals("IndexOp")){
                        var tmp = addTemporaryVariable(localVarsState,ollirToType(expressionResult.result));
                        previousReleases.add(tmp);
                        previousStatements.add(
                                new Stringable(tmp.name +typeToOllir(tmp.type)+" :="+typeToOllir(tmp.type) + " " + expressionResult.result + ";\n")
                        );
                        arguments.add(tmp.name +typeToOllir(tmp.type));
                    }else {
                        arguments.add(expressionResult.result);
                    }
                }

                if (invokeType.equals("static") || node.getJmmParent().getKind().equals("SemiColon")) {
                    result = "invoke"+ invokeType + "(" + target + ", \"" + node.get("name") + (arguments.size()>0?"\", ":"\"") + arguments.stream().reduce((String s1,String s2)->s1+", "+s2).orElse("") +")" + typeToOllir(resultType);
                } else {
                    var tmp = addTemporaryVariable(localVarsState, resultType);
                    previousReleases.add(tmp);
                    previousStatements.add(
                            new Stringable(tmp.name + typeToOllir(tmp.type) + " :=" + typeToOllir(tmp.type) +
                                " invoke" + invokeType + "(" + target + ", \"" + node.get("name") + (arguments.size() > 0 ? "\", " : "\"") + arguments.stream().reduce((String s1, String s2) -> s1 + ", " + s2).orElse("") + ")" + typeToOllir(resultType) + ";\n")
                    );
                    result = tmp.name + typeToOllir(tmp.type);
                }
                break;
            }
            case "NewArr":{
                var expression = expressionVisitor(node.getJmmChild(0),semanticsResult,localVarsState);
                previousStatements = expression.previousStatements;
                previousReleases = expression.freeVars;
                result = "new(array, " + expression.result + ").array.i32";
                resultType = new Type("int",true);
                break;
            }
            case "NewFunc": { //TODO if in VarDeclareStatement, use var name;
                var tmp = addTemporaryVariable(localVarsState, new Type(node.get("name"), false));
                previousReleases.add(tmp);
                previousStatements.add(
                        new Stringable(tmp.name + typeToOllir(tmp.type) + " :=" + typeToOllir(tmp.type) + " new(" + tmp.type.getName() + ")" + typeToOllir(tmp.type) + ";\n")
                );
                previousStatements.add(
                        new Stringable("invokespecial(" + tmp.name + typeToOllir(tmp.type) + ", \"<init>\").V;\n")
                );
                result = tmp.name + typeToOllir(tmp.type);
                resultType = new Type(node.get("name"), false);
                break;
            }
            case "NegationOp": {
                var expression = expressionVisitor(node.getJmmChild(0), semanticsResult, localVarsState);
                if( ! expression.type.getName().equals("boolean")){
                    result = "Negation operator on a non boolean.";break;
                    //throw new RuntimeException("Negation operator on a non boolean.");
                }
                if(expression.isConstant && oFlagValue){
                    result = (!expression.result.equals("1.bool")?"1":"0") + ".bool";
                    isConstant = true;
                    break;
                }
                result = new StringableResult((StringableResult sr)->{
                    if(sr.watchingExpr.isConstant && oFlagValue){
                        sr.isConstant = true;
                        sr.type = ollirToType(".bool");
                        return (!sr.watchingExpr.result.equals("1.bool")?"1":"0") + ".bool";
                    }//else
                    var tmp = addTemporaryVariable(sr.localVarsState, ollirToType(sr.watchingExpr.result));
                    sr.addFreeableVar(tmp);
                    sr.previousStatements = sr.watchingExpr.previousStatements;
                    sr.freeVars.addAll(sr.watchingExpr.freeVars);
                    sr.previousStatements.add(
                            new Stringable(tmp.name + ".bool" + " :=.bool !.bool " + sr.watchingExpr.result + ";\n")
                    );
                    sr.type = new Type("boolean", false);
                    return tmp.name + ".bool";
                },expression,localVarsState);
                if(expression.isResolved()){
                    ((StringableResult)result).resolve();
                }else{
                    expression.dependants.add((StringableResult) result);
                }
                return (StringableResult) result;
            }
            case "ParOp":
                return expressionVisitor(node.getJmmChild(0),semanticsResult,localVarsState);
            case "Integer":
                result = node.get("value")+".i32";
                isConstant = true;
                resultType = new Type("int",false);
                break;
            case "Identifier": {
                var varState = getVariableState(node.get("value"), localVarsState);
                if(varState == null){ //check imports
                    var imported = semanticsResult.getSymbolTable().getImports().stream().filter((String n)-> n.equals(node.get("value"))).findFirst();
                    if(imported.isEmpty()) {
                        result = "Undeclared identifier.";break;
                        //throw new RuntimeException("Undeclared identifier.");
                    }
                    else {
                        result = imported.get();
                        resultType = new Type("import",false);//.import is a local tag, not added to ollir. Important for FuncOp
                    }
                    break;
                }
                if (varState.a) { //is Field
                    var tmp = addTemporaryVariable(localVarsState,varState.b.type);
                    previousReleases.add(tmp);
                    previousStatements.add(
                            new Stringable(tmp.name + typeToOllir(tmp.type) + " :=" + typeToOllir(tmp.type) + " " + "getfield(this," + varState.b.name + typeToOllir(varState.b.type) + ")" + typeToOllir(varState.b.type) + ";\n")
                    );
                    result = tmp.name + typeToOllir(tmp.type);
                    resultType = tmp.type;
                } else {
                    resultType = varState.b.type;
                    varState.b.isRead = true;
                    if(varState.b.isConstant && oFlagValue){ //return constant value instead.
                        if(varState.b.isUnresolved){
                            resultIsString = false;
                            result = new StringableResult((StringableResult sr)->{
                                if(sr.watchingVar.isConstant){
                                    sr.isConstant = true;
                                    return sr.watchingVar.value + typeToOllir(sr.watchingVar.type);
                                }
                                //TODO maybe check if initialized? shouldn't be necessary.
                                return sr.watchingVar.name + typeToOllir(sr.watchingVar.type);
                            },varState.b,localVarsState);
                            varState.b.callOnResolved.add((StringableResult) result);
                        }
                        else {
                            result = varState.b.value + typeToOllir(varState.b.type);
                            isConstant = true;
                            break;
                        }
                    }
                    if (!varState.b.isInitialized) { //not initialized
                        previousStatements.add(
                                new Stringable(
                                    varState.b.name + typeToOllir(varState.b.type)
                                    + " :=" + typeToOllir(varState.b.type)
                                    + " " + getTypeDefaultValue(varState.b.type) + ";\n"
                                )
                        );
                    }
                    result = varState.b.name + typeToOllir(varState.b.type);
                    resultType = varState.b.type;
                }
                break;
            }
            case "This":
                resultType = new Type(semanticsResult.getSymbolTable().getClassName(),false);
                result = "this" + typeToOllir(resultType);
                break;
            case "Bool":
                result = node.get("value")+".bool";
                resultType = new Type("boolean" , false);
                isConstant = true;
                break;
        }
        if(result.equals("undefined"))
            result = "node:[" + node.getKind() + "]"; //throw new RuntimeException();
        //String p1, ArrayList<Object> p2, ArrayList<VarState> p3, Boolean p4, Type p5){
        if(resultIsString)
            return new StringableResult( (String)result,previousStatements, previousReleases,isConstant,resultType);
        return new StringableResult( (Function<StringableResult,String>) result,previousStatements, previousReleases,isConstant,resultType);
    }
    StringableStatementResult statementsVisitor(List<JmmNode> nodeList, JmmSemanticsResult semanticsResult, VarContext localVarsState, String methodName){
        LinkedList<Stringable> result = new LinkedList<>();
        for(JmmNode child : nodeList){
            switch (child.getKind()) {
                case "VarDeclareStatement": {
                    var state = getVariableState(child.get("name"),localVarsState);
                    var expression = expressionVisitor(child.getJmmChild(0), semanticsResult, localVarsState);
                    state.b.isConstant = false;
                    state.b.isWritten = true;
                    result.addAll(expression.previousStatements);
                    for(VarState openVar : expression.freeVars)
                        releaseTemporaryVariable(openVar);
                    state.b.isInitialized = true;
                    if(expression.result.equals(state.b.name + typeToOllir(state.b.type)) && oFlagValue){
                        break; // if lhs == rhs, skip instruction.
                    }
                    if (state.a){
                        result.add( new Stringable("putfield(this, " + state.b.name + typeToOllir(state.b.type) + ", " + expression.result + ").V;\n"));
                        break;
                    }
                    if(expression.isConstant && oFlagValue){
                        state.b.isConstant = true;
                        state.b.isInitialized = false;
                        state.b.value = expression.result.substring(0,expression.result.lastIndexOf("."));
                    }else{
                        result.add(new Stringable(state.b.name+typeToOllir(state.b.type)+" :="+typeToOllir(state.b.type)+" "+expression.result+";\n"));
                    }
                    //if(! (getOllirType(expression.a).getName().equals(state.b.getName()) && getOllirType(expression.a).isArray() == state.b.isArray()))
                    //throw new RuntimeException("Assigning mismatching type.");
                    break;
                }
                case "SemiColon": {
                    while(child.getJmmChild(0).getKind().equals("ParOp")) //ParOp inside SemiColon is to be ignored.
                        child=child.getJmmChild(0);
                    if(child.getJmmChild(0).getKind().matches("(NewFunc|FuncOp)")) { //only FuncOp is not a NoOp. TODO confirm newArr is not required.
                        var statements = expressionVisitor(child.getJmmChild(0), semanticsResult, localVarsState);
                        result.addAll(statements.previousStatements);
                        if (!statements.result.equals("")) //TODO
                            result.add(new Stringable(statements.result+";\n"));
                        for (VarState openVar : statements.freeVars)
                            releaseTemporaryVariable(openVar);
                    }
                    break;
                }
                case "Brackets": {
                    StringableStatementResult res = statementsVisitor(child.getChildren(), semanticsResult, localVarsState, methodName);
                    result.addAll(res.getResultStatements());
                    break;
                }
                case "ArrayAccess": { //TODO constant propagation.
                    var arrayVar = getVariableState(child.get("name"), localVarsState);
                    StringableResult expIndex = expressionVisitor(child.getJmmChild(0), semanticsResult, localVarsState),
                            expValue = expressionVisitor(child.getJmmChild(1), semanticsResult, localVarsState);
                    result.addAll(expIndex.previousStatements);
                    result.addAll(expValue.previousStatements);
                    String target;
                    target = expIndex.result;
                    result.add( new Stringable(arrayVar.b.name+"["+target+"]"+typeToOllir(new Type(arrayVar.b.type.getName(), false))
                            + " :=" + typeToOllir(arrayVar.b.type)+" "
                            + expValue.result+";\n")
                    );
                    for(VarState openVar : expIndex.freeVars)
                        releaseTemporaryVariable(openVar);
                    for(VarState openVar : expValue.freeVars)
                        releaseTemporaryVariable(openVar);
                    break;
                }
                case "IfElseStatement": {
                    var ifExpression = expressionVisitor(child.getJmmChild(0),semanticsResult,localVarsState);
                    if(ifExpression.isConstant && oFlagValue){
                        StringableStatementResult stmtVisit;
                        if(ifExpression.result.equals("1.bool")){ // skip branching and labels.
                            stmtVisit = statementsVisitor(new ArrayList<>(Collections.singleton(child.getJmmChild(1))),semanticsResult,localVarsState,methodName);
                        }else{
                            stmtVisit = statementsVisitor(new ArrayList<>(Collections.singleton(child.getJmmChild(2))),semanticsResult,localVarsState,methodName);
                        }
                        result.addAll(stmtVisit.previousStatements);
                        result.addAll(stmtVisit.getResultStatements());
                        break;
                    }
                    result.addAll(ifExpression.previousStatements);
                    for(String s : localVarsState.keySet()){
                        VarState vs = localVarsState.get(s);
                        if(!vs.isConstant)
                            continue;
                        StringableResult initialize_const = new StringableResult((StringableResult sr)->{
                                if(sr.watchingVar.isWritten && sr.watchingVar.isRead){
                                    return sr.watchingVar.name + typeToOllir(sr.watchingVar.type) + " :="+typeToOllir(sr.watchingVar.type) + " " + sr.watchingVar.value + typeToOllir(sr.watchingVar.type) + ";\n";
                                }
                                return "";
                            },vs,null);
                        vs.callOnIfMerge.add(initialize_const);
                        result.add(initialize_const);
                    }
                    for(VarState openVar : ifExpression.freeVars)
                        releaseTemporaryVariable(openVar);
                    int ifLabel = ifLabelCount;
                    ifLabelCount++;

                    ArrayList<VarContext> branches = new ArrayList<>();
                    branches.add(duplicateVarsState(localVarsState));
                    branches.add(duplicateVarsState(localVarsState));
                    StringableStatementResult s1,s2;

                    s1 = statementsVisitor(new ArrayList<>(Collections.singleton(child.getJmmChild(1))), semanticsResult, branches.get(0), methodName);
                    result.addAll(s1.previousStatements);
                    s2 = statementsVisitor(new ArrayList<>(Collections.singleton(child.getJmmChild(2))), semanticsResult, branches.get(1), methodName);
                    result.addAll(s2.previousStatements);

                    result.add(new Stringable("if("+ifExpression.result+") goto ifbody_"+ifLabel+";\n"));
                    result.addAll(s2.getResultStatements());
                    result.add(new Stringable("goto endif_"+ifLabel+";\n"));
                    result.add(new Stringable("ifbody_"+ifLabel+":\n"));
                    result.addAll(s1.getResultStatements());
                    result.add(new Stringable("endif_"+ifLabel+":\n"));
                    mergeVarsState(localVarsState,branches);
                    break;
                }
                case "WhileStatement": {
                    VarContext dupe = duplicateVarsState(localVarsState);
                    List<Stringable> previousInitializers = new ArrayList<>();
                    int whileLabel = whileLabelCount;
                    whileLabelCount++;
                    StringableStatementResult statementsVisit;
                    boolean certainlyEnters = false;
                    StringableResult whileConditionExpression = expressionVisitor(child.getJmmChild(0),semanticsResult,dupe);
                    if(whileConditionExpression.isConstant && oFlagValue){
                        if(whileConditionExpression.result.equals("0.bool"))
                            break; // never enters loop.
                        certainlyEnters = true;
                    }
                    dupe = duplicateVarsState(localVarsState);
                    while(true){
                        statementsVisit= statementsVisitor(new ArrayList<>(Collections.singleton(child.getJmmChild(1))), semanticsResult, dupe, methodName);
                        var check = checkValidState(localVarsState, dupe);
                        if(certainlyEnters){
                            whileConditionExpression = expressionVisitor(child.getJmmChild(0),semanticsResult,dupe);
                            if(whileConditionExpression.isConstant){
                                break;
                            }
                        }
                        if(check==null) {
                            whileConditionExpression = expressionVisitor(child.getJmmChild(0),semanticsResult,dupe);
                            break;
                        }
                        dupe=check.a;
                        for(String s:check.b) {
                            VarState varState = localVarsState.get(s);
                            previousInitializers.add(new Stringable(varState.name+typeToOllir(varState.type)+" :="+typeToOllir(varState.type)+" "+varState.value+typeToOllir(varState.type)+";\n"));
                            varState.isInitialized = true;
                            varState.isConstant = false;
                        }
                    }
                    mergeVarsState(localVarsState,new ArrayList<>(Collections.singleton(dupe)));

                    if(certainlyEnters){ //already checked for oFlag

                        //no goto start.
                        //no start label.
                        if(whileConditionExpression.isConstant){
                            if(whileConditionExpression.result.equals("0.bool")){
                                //loop only EVER runs once. ergo not a loop.
                                //overwrite constants.
                                localVarsState = dupe; //TODO confirm valid.
                            }else{
                                //loop is infinite.
                                //while_code:
                                // statements;
                                // goto while_code;
                                //remove isConstant if written to.
                            }
                        }
                        //loop runs once then becomes unverifiable.
                    }else {
                        //loop may or may not enter.
                        if (whileConditionExpression.isConstant && oFlagValue) {
                            if (whileConditionExpression.result.equals("0.bool")) {
                                //if entered, loop only runs once. ergo not a loop.
                                // if(condition) goto while_code;
                                // goto endwhile;
                                // while_code:
                                // statements;
                                // endwhile:
                            } else {
                                //if entered, loop is infinite.
                                // if(condition) goto inf_loop;
                                // goto leave_loop;
                                // inf_loop:
                                // statements;
                                // goto inf_loop;
                                // leave_loop:
                            }
                        }
                        result.addAll(previousInitializers);
                        result.add(new Stringable("goto whilestart_"+whileLabel+";\n"));
                        result.add(new Stringable("whilebody_"+whileLabel+":\n"));
                        result.addAll(statementsVisit.getResultStatements());
                        result.add(new Stringable("whilestart_"+whileLabel+":\n"));
                        result.addAll(statementsVisit.previousStatements);
                        result.add(new Stringable("if("+whileConditionExpression.result+") goto whilebody_"+whileLabel+";\n"));
                        for (VarState openVar : whileConditionExpression.freeVars)
                            releaseTemporaryVariable(openVar);
                    }
                    break;
                }

                    //catch all 'expression' types, as this means the node is a return statement.
                case "BinaryOp": case "IndexOp": case "LengthOp": case "FuncOp": case "NewArr":
                case "NewFunc": case "NegationOp": case "ParOp": case "Integer": case "Bool":
                case "Identifier": case "This": {
                    var expression = expressionVisitor(child, semanticsResult, localVarsState);
                    result.addAll(expression.previousStatements);
                    var retType = typeToOllir(semanticsResult.getSymbolTable().getReturnType(methodName));
                    result.add(new Stringable("ret"+retType+" "+expression.result+";\n"));
                    for(VarState openVar : expression.freeVars)
                        releaseTemporaryVariable(openVar);
                    break;
                }
            }
        }
        StringableStatementResult ret = new StringableStatementResult(result);
        return ret;
    }
    String methodVisitor(JmmNode methodNode, JmmSemanticsResult semanticsResult){
        String methodName = methodNode.get("name");
        VarContext localVarsState = new VarContext();
        for(var aVar:semanticsResult.getSymbolTable().getLocalVariables(methodName))
            localVarsState.put(aVar.getName(), new VarState(false, aVar.getType(),aVar.getName(),false, false,false));
        for(int i=0; i<semanticsResult.getSymbolTable().getParameters(methodName).size() ;++i) {
            var aVar= semanticsResult.getSymbolTable().getParameters(methodName).get(i);
            localVarsState.put(aVar.getName(), new VarState(true, aVar.getType(), "$"+(i+1)+"."+aVar.getName(),false, false, false));
        }
        StringableStatementResult stmtVisit = statementsVisitor(methodNode.getChildren(),semanticsResult,localVarsState,methodName);

        List<Stringable> result = stmtVisit.getResultStatements();
        if(result.size()>0 && result.get(result.size()-1).toString().endsWith(":\n")){
            result.add(new Stringable("ret.V;\n"));
        }
        StringBuilder ret=new StringBuilder();
        for(Stringable o : result) {
            //if(!s.equals("[]"))
            ret.append(o.toString());
        }
        return ret.toString();
    }
    VarContext duplicateVarsState(VarContext localVarsState) {
        VarContext ret = new VarContext();
        for (Map.Entry<String, VarState> p : localVarsState.entrySet()) {
            VarState dupe = new VarState(p.getValue().isInitialized,
                    new Type(p.getValue().type.getName(), p.getValue().type.isArray()),
                    p.getValue().name,
                    p.getValue().isOpen,
                    p.getValue().isConstant,
                    p.getValue().isTemporary,
                    p.getValue().value
            );
            ret.put(p.getKey(), dupe);
        }
        return ret;
    }
    Pair<VarContext,ArrayList<String>> checkValidState(VarContext origState, VarContext branchState){
        var ret = duplicateVarsState(origState);
        var toInitialize = new ArrayList<String>();
        boolean isValid = true;
        for(Map.Entry<String,VarState> e : ret.entrySet()) {
            VarState oVar = e.getValue(), nVar = branchState.get(e.getKey());
            if(oVar.isOpen || oVar.isTemporary) continue;
            if(oVar.isConstant && (!nVar.isConstant || !nVar.value.equals(oVar.value))){
                oVar.isConstant = false;
                isValid = false;
                if(nVar.isRead)
                    toInitialize.add(e.getKey());
                e.setValue(new VarState(true,oVar.type,oVar.name,oVar.isOpen,false, oVar.isTemporary, oVar.value));
                origState.get(e.getKey()).isConstant = false;
            }
        }
        if(isValid)
            return null; // state was valid; Otherwise, repeat branch with returned map.
        else
            return new Pair<>(ret,toInitialize);
    }
    void mergeVarsState(VarContext origState, List<VarContext> branchStates){
        for(Map.Entry<String,VarState> e : origState.entrySet()){
            VarState oVar = e.getValue();
            if(oVar.isOpen || (oVar.isInitialized && !oVar.isConstant))
                continue;
            boolean isConstant = true,
                    isInitalized = true;
            String value = oVar.value;

            List<VarState> branchVars = new ArrayList<>(branchStates.size());
            for(var branch:branchStates)
                branchVars.add(branch.get(e.getKey()));
            for(var branched:branchVars){
                if(!isConstant && !isInitalized)
                    break;
                if(!branched.isInitialized){
                    isInitalized = false;
                }
                if(!branched.isConstant) {
                    isConstant = false;
                }
                else{
                    if(value==null) {
                        value = branched.value;
                    }else {
                        if (!value.equals(branched.value)){
                            isConstant = false;
                            break;
                        }
                    }
                }
            }
            e.setValue(new VarState(isInitalized,oVar.type,oVar.name,oVar.isOpen,isConstant,oVar.isTemporary,value));
        }
    }
    private class StatementVisitResult{
        public ArrayList<Object> result = null;
        public ArrayList<String> previousStatements = null;
    }
    static class VarState {
        public boolean isInitialized; //is initialized
        public Type type; //type of var
        public String name; //name of var
        public boolean isOpen; //is open for reuse
        public boolean isConstant; //is constant
        public String value;
        public boolean isTemporary;
        public boolean isRead = false;
        public boolean isWritten = false;
        public boolean isUnresolved = false;
        public VarState parent = null;
        public List<StringableResult> callOnIfMerge = new ArrayList<>(), callOnResolved = new ArrayList<>();
        public VarState(boolean init, Type type, String name, boolean open, boolean constant, boolean temporary){
            isInitialized = init;
            this.type = type;
            this.name = name;
            isOpen = open;
            isConstant = constant;
            isTemporary = temporary;
            value = "";
        }
        public VarState(boolean init, Type type, String name, boolean open, boolean constant, boolean temporary, String value){
            isInitialized =init;
            this.type =type;
            this.name =name;
            this.isOpen =open;
            isConstant =constant;
            isTemporary = temporary;
            this.value = value;
        }
    }
    public class VarContext{
        Map<String, VarState> map = new HashMap<>();

        public void put(String key, VarState v){
            map.put(key,v);
        }
        public VarState get(String key){
            return map.get(key);
        }
        public boolean containsKey(String key){
            return map.containsKey(key);
        }
        public Set<Map.Entry<String,VarState>> entrySet(){
            return map.entrySet();
        }
        public Set<String> keySet(){
            return map.keySet();
        }

        VarContext duplicateUnresolved() {
            VarContext ret = new VarContext();
            for (Map.Entry<String, VarState> p : map.entrySet()) {
                VarState dupe = new VarState(
                        p.getValue().isInitialized,
                        new Type(p.getValue().type.getName(), p.getValue().type.isArray()),
                        p.getValue().name,
                        p.getValue().isOpen,
                        p.getValue().isConstant, 
                        p.getValue().isTemporary,
                        p.getValue().value
                );
                dupe.parent = p.getValue();
                if(p.getValue().isConstant){
                    dupe.isUnresolved = true;
                }
                ret.map.put(p.getKey(), dupe);
            }
            return ret; //TODO
        }

        void mergeIf(ArrayList<VarContext> branches){
            for(String key : map.keySet()){
                VarState oVar = map.get(key);
                if(oVar.isOpen || (oVar.isInitialized && !oVar.isConstant))
                    continue;
                boolean madeConstant = true,
                        madeInitalized = true, //given a value on any branch.
                        wasWritten = false,
                        wasRead = false;
                String value = null;
                for(var branch:branches){
                    var nVar = branch.get(key);
                    if(nVar == null)
                        continue;
                    if(nVar.isWritten){
                        wasWritten = true;
                    }
                    if(nVar.isRead){
                        wasRead = true;
                    }
                    if(madeConstant == false || !nVar.isConstant){
                        madeConstant = false;
                    }else{
                        if(value==null){
                            value = nVar.value;
                        }else{
                            if(!value.equals(nVar.value)){
                                madeConstant = false;
                            }
                        }
                    }
                    if(!nVar.isInitialized){
                        madeInitalized = false;
                    }
                }
                oVar.isWritten = wasWritten;
                oVar.isRead = wasRead;
                for(var dep : oVar.callOnIfMerge){
                    dep.resolve();
                }
                oVar.isConstant = madeConstant;
                oVar.isInitialized = oVar.isConstant ? false : madeInitalized;
                if(oVar.isConstant)
                    oVar.value = value;
            }
        }
        void mergeReplace(VarContext branch){ //overwrite from guaranteed execution.
            for(Map.Entry<String,VarState> e: map.entrySet()){
                if(e.getValue().isOpen)
                    continue;

            }

            /*for(Map.Entry<String,VarState> e : origState.entrySet()){
                VarState oVar = e.getValue();
                if(oVar.isOpen || (oVar.isInitialized && !oVar.isConstant))
                    continue;
                boolean isConstant = true,
                        isInitalized = true;
                String value = oVar.value;

                List<VarState> branchVars = new ArrayList<>(branchStates.size());
                for(var branch:branchStates)
                    branchVars.add(branch.get(e.getKey()));
                for(var branched:branchVars){
                    if(!isConstant && !isInitalized)
                        break;
                    if(!branched.isInitialized){
                        isInitalized = false;
                    }
                    if(!branched.isConstant) {
                        isConstant = false;
                    }
                    else{
                        if(value==null) {
                            value = branched.value;
                        }else {
                            if (!value.equals(branched.value)){
                                isConstant = false;
                                break;
                            }
                        }
                    }
                }
                e.setValue(new VarState(isInitalized,oVar.type,oVar.name,oVar.isOpen,isConstant,oVar.isTemporary,value));
            }
            */
        }

    }
    public class Stringable {
        public String string;
        public LinkedList<Stringable> getPreviousStatements(){
            return null;
        }

        @Override
        public String toString() {
            return string;
        }
        public Stringable(String string){
            this.string = string;
        }
        Stringable(){}
        public boolean compact(){
            boolean compacted = true;
            for(Stringable s : getPreviousStatements()){
                boolean compS = s.compact();
                if(compacted && !compS)
                    compacted = false;
                if(compS)
                    getPreviousStatements().addAll(s.getPreviousStatements());
            }
            return compacted;
        }
    }
    public class StringableResult extends Stringable {
        String result = null;
        LinkedList<Stringable> previousStatements = new LinkedList<>();
        ArrayList<VarState> freeVars = new ArrayList<>();
        Function<StringableResult,String> f = null;
        public VarState watchingVar = null;
        public Object info = null;
        public VarContext localVarsState = null;
        public StringableResult watchingExpr = null;
        public List<StringableResult> dependants = new ArrayList<>(), dependencies = new ArrayList<>();
        public boolean isConstant = false;
        public Type type = null;

        public StringableResult(String s, boolean constant, Type t){
            this.result = s;
            isConstant = constant;
            type = t;
        }
        public StringableResult(Function<StringableResult,String> func, VarState watch, VarContext localVarsState){
            this.f = func;
            this.watchingVar = watch;
            this.localVarsState = localVarsState;
        }
        public StringableResult(Function<StringableResult,String> func, StringableResult watch, VarContext localVarsState){
            this.f = func;
            this.watchingExpr = watch;
            this.localVarsState = localVarsState;
        }
        public StringableResult(Function<StringableResult,String> func, LinkedList<Stringable> p2, ArrayList<VarState> p3, Boolean p4, Type p5){
            this.f = func;
            this.previousStatements = p2;
            this.isConstant = p4;
            this.freeVars = p3;
            this.type = p5;
        }
        public StringableResult(String p1, LinkedList<Stringable> p2, ArrayList<VarState> p3, Boolean p4, Type p5){
            this.result = p1;
            this.previousStatements=p2;
            this.isConstant = p4;
            this.freeVars = p3;
            this.type = p5;
        }

        StringableResult() {
        }


        public void addPreviousStatement(Stringable o){
            previousStatements.add(o);
        }
        @Override
        public String toString() {
            if(result == null)
                return "";
            return result;
        }
        public LinkedList<Stringable> getPreviousStatements(){
            return previousStatements;
        }
        public void addFreeableVar(VarState freeable){
            freeVars.add(freeable);
        }
        public void releaseFreeVars(){
            for(VarState v : freeVars)
                v.isOpen = true;
        }
        public boolean isResolved(){
            return result !=null;
        }
        public void resolve(){
            if(result != null)
                throw new RuntimeException("Tried resolving a resolved StringableResult.\n");
            result = f.apply(this);
            for(StringableResult sr : dependants){
                sr.resolve();
            }
        }
    }
    public class StringableStatementResult extends StringableResult{
        LinkedList<Stringable> resultStatements = new LinkedList<>();
        public StringableStatementResult(String s, boolean constant, Type t) {
            super(s, constant, t);
        }
        public StringableStatementResult(LinkedList<Stringable> resultStatements){
            super();
            this.resultStatements = resultStatements;
        }

        public LinkedList<Stringable> getResultStatements() {
            return resultStatements;
        }
    }
}
