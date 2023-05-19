package pt.up.fe.comp2023.registerAlloc;


import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.*;


public class MyLifeTime {

    public void allocate(OllirResult ollirResult) {
        ollirResult.getOllirClass().buildCFGs();

        Integer registers = Integer.parseInt(ollirResult.getConfig().get("registerAllocation"));

        for (int i=0; i< ollirResult.getOllirClass().getMethods().size(); i++) {
            Method method = ollirResult.getOllirClass().getMethods().get(i);
            List<Set<String>> def = new ArrayList<>();
            List<Set<String>> use = new ArrayList<>();
            List<Set<String>> in = new ArrayList<>();
            List<Set<String>> out = new ArrayList<>();
            List<Node> nodeOrder = new ArrayList<>();
            orderNodes(nodeOrder, method);
            InOutGeneratorAux(def, use, in, out, nodeOrder);

            List<List<String>> temp = initVarsParams(method);

            MyGraph graph = new MyGraph(temp.get(0),temp.get(1));

            graph.initGraph(method, nodeOrder, def, out);

            graph.colorGraph(registers, ollirResult);

            HashMap<String, Descriptor> varTable = method.getVarTable();
            for (MyNode node: graph.localVars) {
                varTable.get(node.name).setVirtualReg(node.getReg());
            }
            for (MyNode node: graph.params) {
                varTable.get(node.name).setVirtualReg(node.getReg());
            }

            if (varTable.get("this") != null) {
                varTable.get("this").setVirtualReg(0);
            }



        }
    }

    public void DeadVarsDealer(OllirResult ollirResult){
        boolean hasDeadVars = false;
        do {
            ollirResult.getOllirClass().buildCFGs();
            for (int i=0; i< ollirResult.getOllirClass().getMethods().size(); i++) {
                Method method = ollirResult.getOllirClass().getMethods().get(i);
                List<Set<String>> def = new ArrayList<>();
                List<Set<String>> use = new ArrayList<>();
                List<Set<String>> in = new ArrayList<>();
                List<Set<String>> out = new ArrayList<>();
                List<Node> nodeOrder = new ArrayList<>();
                orderNodes(nodeOrder, method);
                InOutGeneratorAux(def, use, in, out, nodeOrder);
                hasDeadVars = eliminateDeadVars(method,nodeOrder,def,out) || hasDeadVars;
            }
        }
        while(hasDeadVars);
    }

    public void InOutGeneratorAux(List<Set<String>> def, List<Set<String>> use, List<Set<String>> in, List<Set<String>> out, List<Node> nodeOrder) {
        in = new ArrayList<>();
        out = new ArrayList<>();
        def = new ArrayList<>();
        use = new ArrayList<>();
        for (Node node: nodeOrder) {
            in.add(new HashSet<>());
            out.add(new HashSet<>());
            def.add(new HashSet<>());
            use.add(new HashSet<>());
            UseDefGenerator(node,nodeOrder, use, def);
        }

        boolean livenessHasChanged;

        do  {
            livenessHasChanged = false;

            for (int index = 0; index < nodeOrder.size(); index++) {
                Node node = nodeOrder.get(index);

                // out[n] = (union (for all s that belongs to succ[n])) in[s]
                // in[n] = use[n] union (out[n] - def[n])

                Set<String> origIn = new HashSet<>(in.get(index));
                Set<String> origOut = new HashSet<>(out.get(index));

                out.get(index).clear();

                for (Node succ : node.getSuccessors()) {
                    int succIndex = nodeOrder.indexOf(succ);
                    if (succIndex == -1) continue;
                    Set<String> in_succIndex = in.get(succIndex);

                    out.get(index).addAll(in_succIndex);
                }

                in.get(index).clear();

                Set<String> outDefDiff = new HashSet<>(out.get(index));
                outDefDiff.removeAll(def.get(index));

                outDefDiff.addAll(use.get(index));
                in.get(index).addAll(outDefDiff);

                livenessHasChanged = livenessHasChanged ||
                        !origIn.equals(in.get(index)) || !origOut.equals(out.get(index));
            }

        } while (livenessHasChanged);
    }


    private void orderNodes(List<Node> nodeOrder, Method method) {
        Node beginNode = method.getBeginNode();
        dfsOrderNodes(beginNode, new ArrayList<>(), nodeOrder, method);
    }

    private void dfsOrderNodes(Node node, ArrayList<Node> visited, List<Node> nodeOrder, Method method) {

        if (node == null || nodeOrder.contains(node) || visited.contains(node)) {
            return;
        }

        if (node instanceof Instruction instruction && !method.getInstructions().contains(instruction))
            return;

        visited.add(node);

        for (Node successor: node.getSuccessors()) {
            dfsOrderNodes(successor, visited, nodeOrder, method);
        }

        nodeOrder.add(node);
    }

    private void UseDefGenerator(Node node, List<Node> nodeOrder, List<Set<String>> use, List<Set<String>> def) {
        UseDefGenerator(node, null, nodeOrder, use, def);
    }

    private void UseDefGenerator(Node node, Node parentNode, List<Node> nodeOrder, List<Set<String>> use,List<Set<String>> def) {

        if (node == null){
            return;
        }

        Node useDefNode = parentNode == null ? node : parentNode;

        if (node.getNodeType().equals(NodeType.BEGIN)) {
            return;
        }

        if (node.getNodeType().equals(NodeType.END)) {
            return;
        }

        if (node instanceof AssignInstruction instruction) {
            addToUseDefSet(useDefNode, instruction.getDest(),def,use,nodeOrder);
            UseDefGenerator(instruction.getRhs(), node,nodeOrder,use,def);
        } else if (node instanceof UnaryOpInstruction instruction) {
            addToUseDefSet(useDefNode, instruction.getOperand(), use,def, nodeOrder);
        } else if (node instanceof BinaryOpInstruction instruction) {
            addToUseDefSet(useDefNode, instruction.getLeftOperand(), use,def, nodeOrder);
            addToUseDefSet(useDefNode, instruction.getRightOperand(), use,def, nodeOrder);
        } else if (node instanceof ReturnInstruction instruction) {
            addToUseDefSet(useDefNode, instruction.getOperand(), use,def, nodeOrder);
        } else if (node instanceof CallInstruction instruction) {
            addToUseDefSet(useDefNode, instruction.getFirstArg(), use,def, nodeOrder);
            if (instruction.getListOfOperands() != null) {
                for (Element arg: instruction.getListOfOperands()) {
                    addToUseDefSet(useDefNode, arg, use,def, nodeOrder);
                }
            }
        } else if (node instanceof GetFieldInstruction instruction) {
            addToUseDefSet(useDefNode, instruction.getFirstOperand(), use,def, nodeOrder);
        } else if (node instanceof PutFieldInstruction instruction) {
            addToUseDefSet(useDefNode, instruction.getFirstOperand(), use,def, nodeOrder);
            addToUseDefSet(useDefNode, instruction.getThirdOperand(), use,def, nodeOrder);
        } else if (node instanceof SingleOpInstruction instruction) {
            addToUseDefSet(useDefNode, instruction.getSingleOperand(), use,def, nodeOrder);
        } else if (node instanceof OpCondInstruction instruction) {
            for (Element operand: instruction.getOperands()) {
                addToUseDefSet(useDefNode, operand, use,def, nodeOrder);
            }
        } else if (node instanceof SingleOpCondInstruction instruction) {
            for (Element operand: instruction.getOperands()) {
                addToUseDefSet(useDefNode, operand, use,def, nodeOrder);
            }
        }
    }


    private void addToUseDefSet(Node node, Element val, List<Set<String>> arr,List<Set<String>> arr2,  List<Node> nodeOrder) {
        int index = nodeOrder.indexOf(node);

        if (val instanceof ArrayOperand arrop) {
            for (Element element: arrop.getIndexOperands()) {
                addToUseDefSet(node, element, arr, arr2, nodeOrder);
            }
            arr.get(index).add(arrop.getName());
        }

        if (val instanceof Operand op && !op.getType().getTypeOfElement().equals(ElementType.THIS)) {
            arr.get(index).add(op.getName());
        }
    }

    public List<List<String>> initVarsParams(Method method){
        List<String> variables = new ArrayList<>();
        List<String> params = new ArrayList<>();

        for (String variable: method.getVarTable().keySet()) {
            List<String> names = new ArrayList<>();
            List<Element> parameters = method.getParams();
            for (Element element: parameters) {
                if (element instanceof Operand operand){
                    names.add(operand.getName());
                }
                else{
                    names.add(null);
                }
            }
            if (names.contains(variable)) {
                params.add(variable);
            } else if (!variable.equals("this")) {
                variables.add(variable);
            }
        }

        List<List<String>> temp = new ArrayList<>();
        temp.add(variables);
        temp.add(params);
        return temp;
    }



    public boolean eliminateDeadVars(Method method, List<Node> nodeOrder, List<Set<String>> def,List<Set<String>> out) {
        boolean hasDeadVars = false;
        ArrayList<Instruction> instructions = method.getInstructions();
        ArrayList<Instruction> copyInstructions = new ArrayList<>(instructions);
        for (Instruction instruction: copyInstructions) {
            int index = nodeOrder.indexOf(instruction);

            if (instruction instanceof AssignInstruction assignInstruction) {
                String name = null;
                if (assignInstruction.getDest() instanceof Operand operand){
                    name = operand.getName();
                }


                if (name != null && def.get(index).contains(name) && !out.get(index).contains(name)) {
                    List<Node> predecessors = instruction.getPredecessors();
                    List<Node> successors = instruction.getSuccessors();

                    for (Node predecessor: predecessors) {
                        for (Node successor: successors) {
                            predecessor.addSucc(successor);
                            successor.addPred(predecessor);
                        }
                    }
                    List<String> labels = method.getLabels(instruction);

                    for (String label: labels) {
                        method.getLabels().remove(label);
                        for (Node successor: successors) {
                            method.addLabel(label, (Instruction) successor);
                        }
                    }

                    instructions.remove(instruction);
                    hasDeadVars = true;
                }
            }
        }
        if (hasDeadVars) method.buildVarTable();

        return hasDeadVars;
    }









}
