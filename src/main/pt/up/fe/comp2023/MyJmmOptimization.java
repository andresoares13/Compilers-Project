package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;

public class MyJmmOptimization implements JmmOptimization {
    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        return JmmOptimization.super.optimize(semanticsResult);
    }

    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {
        StringBuilder codeBuilder = new StringBuilder();
        List< Report > reports = new ArrayList<>();
        SymbolTableStore st = (SymbolTableStore) jmmSemanticsResult.getSymbolTable();
        for(String s:st.getImports()) {
            codeBuilder.append("import "+s+";\n");
        }
        codeBuilder.append('\n');
        codeBuilder.append(
                st.getClassName() + " {\n" +
                    "\t.constuct "+st.getClassName()+"().V {\n" +
                        "\t\tinvokespecial(this, \"<init>\").V;\n" +
                    "\t}\n\n");
        for(String s :st.getMethods()){
            codeBuilder.append(
                    "\t.method ");
            if(st.getMethodIsPublic(s))
                codeBuilder.append("public ");
            codeBuilder.append(s+"(");
            boolean first=true;
            for(Symbol p:st.getParameters(s)){
                if(first)
                    first=false;
                else
                    codeBuilder.append(", ");
                codeBuilder.append(p.getName() + toOllirType(p.getType()));
            }
            codeBuilder.append(")");
            codeBuilder.append(toOllirType(st.getReturnType(s)));
            codeBuilder.append(" {\n");
            //method statements
            //TODO
            codeBuilder.append("\t{\n");
        }
        codeBuilder.append("}\n");

        String ollirCode = codeBuilder.toString();
        OllirResult result = new OllirResult(jmmSemanticsResult,ollirCode,reports);

        return result;
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        return JmmOptimization.super.optimize(ollirResult);
    }

    String toOllirType(Type type){
        StringBuilder sb=new StringBuilder();
        if(type.isArray())
            sb.append(".array");

        switch(type.getName()){
            case "int":
                sb.append(".i32");
                break;
            case "boolean":
                sb.append(".bool");
                break;
            case "void":
                sb.append(".V");
                break;
            default:
                sb.append("."+type.getName());
        }
        return sb.toString();
    }
}
