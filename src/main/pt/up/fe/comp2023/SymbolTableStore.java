package pt.up.fe.comp2023;

import org.antlr.v4.runtime.misc.Triple;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp2023.visitors.ClassesVisitor;
import pt.up.fe.comp2023.visitors.ImportsVisitor;
import pt.up.fe.comp2023.visitors.MethodsVisitor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolTableStore implements SymbolTable {

    private List<String> imports = new ArrayList<>();

    private List<String> classParameters = new ArrayList<>();
    private String className;
    private String superName;
    private List<Symbol> fields;
    private Map<String, Triple<Type,List<Symbol>,List<Symbol>>> methods_parameters = new HashMap<>();



    public SymbolTableStore(JmmParserResult parserResult) {
        ImportsVisitor importsVisitor = new ImportsVisitor();
        importsVisitor.visit(parserResult.getRootNode(),this.imports);

        ClassesVisitor classVisitor = new ClassesVisitor();
        classVisitor.visit(parserResult.getRootNode(), this.classParameters);
        if (this.classParameters.size() > 0){
            this.className = classParameters.get(0);
            if (this.classParameters.size() > 1){
                this.superName = classParameters.get(1);
            }
        }
        MethodsVisitor methodsVisitor = new MethodsVisitor();
        methodsVisitor.visit(parserResult.getRootNode(), this.methods_parameters);

    }

    @Override
    public List<String> getImports() {
        return this.imports;
    }

    @Override
    public String getClassName() {
        return this.className;
    }

    @Override
    public String getSuper() {
        return superName;
    }

    @Override
    public List<Symbol> getFields() {
        return this.fields;
    }

    @Override
    public List<String> getMethods() {
        return new ArrayList<String>(methods_parameters.keySet());
    }

    @Override
    public Type getReturnType(String methodName) {
        return methods_parameters.get(methodName).a;
    }

    @Override
    public List<Symbol> getParameters(String methodName) {
        return methods_parameters.get(methodName).b;
    }

    @Override
    public List<Symbol> getLocalVariables(String methodName) {
        return methods_parameters.get(methodName).c;
    }

    public void printImports(){

        for (int i = 0; i<this.imports.size();i++){
            System.out.println(this.imports.get(i));
        }
    }
}
