package ir;

import java.util.ArrayList;

public class Function extends Value{
    private final Module parentModule;
    private final ArrayList<Arg> args;
    private final ArrayList<BasicBlock> basicBlocks;
    private final ArrayList<Instruction> instructions;

    public Function(String name, String dataType, Module parentModule) {
        super(name, Type.Function, false, dataType);
        this.parentModule = parentModule;
        this.args = new ArrayList<>();
        this.basicBlocks = new ArrayList<>();
        this.instructions = new ArrayList<>();
        parentModule.addFunction(this);
    }
    public Module getParentModule() {
        return parentModule;
    }
    public ArrayList<Arg> getArgs() {
        return args;
    }
    public ArrayList<BasicBlock> getBasicBlocks() {
        return basicBlocks;
    }
    public void addArg(Arg arg) {
        args.add(arg);
    }
    public void addBasicBlock(BasicBlock basicBlock) {
        basicBlocks.add(basicBlock);
    }
    public void addInstruction(Instruction instruction) {
        instructions.add(instruction);
    }
    public ArrayList<Instruction> getInstructions() {
        return instructions;
    }

    public String toString() {
        if(this.getName().equals("@getint")) {
            return "declare i32 @getint()\n";
        }
        if(this.getName().equals("@putint") || this.getName().equals("@putch")){
            return "declare void " + this.getName() + "(i32)\n";
        }
        if(this.getName().equals("@putstr")) {
            return "declare void @putstr(i8*)\n";
        }
        StringBuilder sb = new StringBuilder();
        String functype = this.getDataType().equals("void") ? "void" : "i32";
        sb.append("define dso_local ").append(functype).append(" ").append(this.getName()).append("(");
        for (int i = 0; i < args.size(); i++) {
            sb.append("i32");
            if(i != args.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(") #0 {\n");
        for (Instruction instruction : instructions) {
            sb.append(instruction.toString());
        }
        sb.append("}\n");
        return sb.toString();
    }
}
