package pt.up.fe.comp2023.registerAlloc;

import org.specs.comp.ollir.*;
import java.util.*;



public class MyLifeTimeCalculator {
    private Method method;
    private List<Set<String>> out = new ArrayList<>();
    private List<Set<String>> def = new ArrayList<>();
    private List<Set<String>> use = new ArrayList<>();
    private List<Node> nodes;



    public MyLifeTimeCalculator(Method method) {
        this.method = method;
    }

    private void orderNodes() {
        Node beginNode = method.getBeginNode();
        this.nodes = new ArrayList<>();
        OrderNodesAux(beginNode, new ArrayList<>());
    }

    private void OrderNodesAux(Node node, ArrayList<Node> visited) {

        if (node == null || nodes.contains(node) || visited.contains(node)) {
            return;
        }

        if (node.getClass().getSimpleName().equals("Instruction") && !method.getInstructions().contains(node)) {
           return;
        }
        visited.add(node);

        for (Node successor: node.getSuccessors()) {
            OrderNodesAux(successor, visited);
        }

        nodes.add(node);
    }

    public void InOutGenerator() {
        List<Set<String>> in = new ArrayList<>();
        orderNodes();
        for (Node node: nodes) {
            in.add(new HashSet<>());
            out.add(new HashSet<>());
            def.add(new HashSet<>());
            use.add(new HashSet<>());
            UseDefGenerator(node,null);
        }

        boolean characterDevelopment = true;

        while(characterDevelopment) {
            characterDevelopment = false;

            for (int index = 0; index < nodes.size(); index++) {
                Node node = nodes.get(index);

                Set<String> origIn = new HashSet<>(in.get(index));
                Set<String> origOut = new HashSet<>(out.get(index));

                out.get(index).clear();

                for (Node successor : node.getSuccessors()) {
                    int successorIndex = nodes.indexOf(successor);
                    if (successorIndex == -1){
                        continue;
                    }
                    Set<String> in_successorIndex = in.get(successorIndex);

                    out.get(index).addAll(in_successorIndex);
                }

                in.get(index).clear();

                Set<String> outDefDiff = new HashSet<>(out.get(index));
                outDefDiff.removeAll(def.get(index));

                outDefDiff.addAll(use.get(index));
                in.get(index).addAll(outDefDiff);

                characterDevelopment = characterDevelopment || !origIn.equals(in.get(index)) || !origOut.equals(out.get(index));
            }
        }
    }

    private void addToUseDefSet(Node node, Element val, List<Set<String>> array) {
        int index = nodes.indexOf(node);
       

        if (ArrayOperand.class.isInstance(val)) {
            ArrayOperand arrayOp = (ArrayOperand) val;
            for (Element element : arrayOp.getIndexOperands()) {
                setUse(node, element);
            }
            array.get(index).add(arrayOp.getName());
        }

        if (Operand.class.isInstance(val)) {
            Operand op = (Operand) val;
            if (!op.getType().getTypeOfElement().equals(ElementType.THIS)) {
                array.get(index).add(op.getName());
            }
        }
    }


    public List<Node> getNodes(){
        return this.nodes;
    }

    public List<Set<String>> getDef(){
        return this.def;
    }

    public List<Set<String>> getOut(){
        return this.out;
    }

    public Method getMethod() {
        return method;
    }

    private void setDef(Node node, Element dest) {
        addToUseDefSet(node, dest, def);
    }

    private void setUse(Node node, Element val) {
        addToUseDefSet(node, val, use);
    }


    private void UseDefGenerator(Node node, Node parentNode) {

        if (node == null){
            return;
        }

        Node useDefNode = parentNode == null ? node : parentNode;

        if (node.getNodeType().equals(NodeType.END) || node.getNodeType().equals(NodeType.BEGIN)) {
            return;
        }


        switch (node.getClass().getSimpleName()) {
            case "SingleOpInstruction":
                SingleOpInstruction singleOpInstruction = (SingleOpInstruction) node;
                setUse(useDefNode, singleOpInstruction.getSingleOperand());
                break;
            case "BinaryOpInstruction":
                BinaryOpInstruction binaryOpInstruction = (BinaryOpInstruction) node;
                setUse(useDefNode, binaryOpInstruction.getLeftOperand());
                setUse(useDefNode, binaryOpInstruction.getRightOperand());
                break;
            case "UnaryOpInstruction":
                UnaryOpInstruction unaryOpInstruction = (UnaryOpInstruction) node;
                setUse(useDefNode, unaryOpInstruction.getOperand());
                break;
            case "GetFieldInstruction":
                GetFieldInstruction getFieldInstruction = (GetFieldInstruction) node;
                setUse(useDefNode, getFieldInstruction.getFirstOperand());
                break;
            case "ReturnInstruction":
                ReturnInstruction returnInstruction = (ReturnInstruction) node;
                setUse(useDefNode, returnInstruction.getOperand());
                break;
            case "OpCondInstruction":
                OpCondInstruction opCondInstruction = (OpCondInstruction) node;
                for (Element operand: opCondInstruction.getOperands()) {
                    setUse(useDefNode, operand);
                }
                break;
            case "AssignInstruction":
                AssignInstruction assignInstruction = (AssignInstruction) node;
                setDef(useDefNode, assignInstruction.getDest());
                UseDefGenerator(assignInstruction.getRhs(), node);
                break;
            case "PutFieldInstruction":
                PutFieldInstruction putFieldInstruction = (PutFieldInstruction) node;
                setUse(useDefNode, putFieldInstruction.getFirstOperand());
                setUse(useDefNode, putFieldInstruction.getThirdOperand());
                break;
            case "CallInstruction":
                CallInstruction callInstruction = (CallInstruction) node;
                setUse(useDefNode, callInstruction.getFirstArg());
                if (callInstruction.getListOfOperands() != null) {
                    for (Element arg: callInstruction.getListOfOperands()) {
                        setUse(useDefNode, arg);
                    }
                }
                break;
            case "SingleOpCondInstruction":
                SingleOpCondInstruction singleOpCondInstruction = (SingleOpCondInstruction) node;
                for (Element operand: singleOpCondInstruction.getOperands()) {
                    setUse(useDefNode, operand);
                }
                break;
            default:
                break;
        }
    }



}
