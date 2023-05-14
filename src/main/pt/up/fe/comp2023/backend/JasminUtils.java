package pt.up.fe.comp2023.backend;

import org.specs.comp.ollir.ArrayType;
import org.specs.comp.ollir.ClassType;
import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.ElementType;
import org.specs.comp.ollir.Type;

public class JasminUtils {

    public static String translateType(ClassUnit ollirClass, Type type) {
        ElementType elementType = type.getTypeOfElement();

        return switch (elementType) {
            case ARRAYREF -> "[" + translateType(((ArrayType) type).getArrayType());
            case OBJECTREF, CLASS -> "L" + getFullClassName(ollirClass, ((ClassType) type).getName()) + ";";
            default -> translateType(elementType);
        };
    }

    public static String translateType(ElementType type){
        return switch (type) {
            case INT32 -> "I";
            case BOOLEAN -> "Z";
            case STRING -> "Ljava/lang/String;";
            case THIS -> "this";
            case VOID -> "V";
            default -> "";
        };
    }

    public static String getFullClassName(ClassUnit ollirClass, String className) {
        if (ollirClass.isImportedClass(className)) {
            for (String fullImport : ollirClass.getImports()) {
                int lastSeparatorIndex = className.lastIndexOf(".");

                if (lastSeparatorIndex < 0 && fullImport.equals(className)) {
                    return className;
                } else if (fullImport.substring(lastSeparatorIndex + 1).equals(className)) {
                    return fullImport;
                }
            }
        }

        return className;
    }

    public static String trimLiteral(String literal) {
        if (literal.charAt(0) != '"') {
            return literal;
        }
        return literal.length() == 1 ? literal : literal.substring(1, literal.length() - 1);
    }
}
