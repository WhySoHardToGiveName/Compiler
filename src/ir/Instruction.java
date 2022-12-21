package ir;

import java.util.ArrayList;

public class Instruction extends User{
    private BasicBlock parentBB;
    private OPType opType;

    public enum OPType{
        add, sub, mul, sdiv, srem, shl, shr, and, or, xor, cmp, call, ret, br, phi, load, store, alloca, eq, ne, sgt, sge, slt, sle, getelementptr;

        public static OPType getOpType(String op) {
            switch (op) {
                case "+":
                    return add;
                case "-":
                    return sub;
                case "*":
                    return mul;
                case "/":
                    return sdiv;
                case "%":
                    return srem;
                case "==":
                    return eq;
                case "!=":
                    return ne;
                case ">":
                    return sgt;
                case ">=":
                    return sge;
                case "<":
                    return slt;
                case "<=":
                    return sle;
            }
            return null;
        }
    }
    public Instruction(String name, Type type, boolean isArr, BasicBlock parentBB, OPType opType, String dataType) {
        super(name, type, isArr, dataType);
        this.parentBB = parentBB;
        this.opType = opType;
        parentBB.addInstruction(this);
        parentBB.getParentFunction().addInstruction(this);
    }
    public String toString(){
        String str = "";
        switch (opType){
            case add:
                str = this.getName() + " = add i32 " + this.getOperands().get(0).getName() + ", " + this.getOperands().get(1).getName() + "\n";
                break;
            case sub:
                str = this.getName() + " = sub i32 " + this.getOperands().get(0).getName() + ", " + this.getOperands().get(1).getName() + "\n";
                break;
            case mul:
                str = this.getName() + " = mul i32 " + this.getOperands().get(0).getName() + ", " + this.getOperands().get(1).getName() + "\n";
                break;
            case sdiv:
                str = this.getName() + " = sdiv i32 " + this.getOperands().get(0).getName() + ", " + this.getOperands().get(1).getName() + "\n";
                break;
            case srem:
                str = this.getName() + " = srem i32 " + this.getOperands().get(0).getName() + ", " + this.getOperands().get(1).getName() + "\n";
                break;
            case eq:
                str = this.getName() + " = icmp eq i32 " + this.getOperands().get(0).getName() + ", " + this.getOperands().get(1).getName() + "\n";
                break;
            case ne:
                str = this.getName() + " = icmp ne i32 " + this.getOperands().get(0).getName() + ", " + this.getOperands().get(1).getName() + "\n";
                break;
            case sgt:
                str = this.getName() + " = icmp sgt i32 " + this.getOperands().get(0).getName() + ", " + this.getOperands().get(1).getName() + "\n";
                break;
            case sge:
                str = this.getName() + " = icmp sge i32 " + this.getOperands().get(0).getName() + ", " + this.getOperands().get(1).getName() + "\n";
                break;
            case slt:
                str = this.getName() + " = icmp slt i32 " + this.getOperands().get(0).getName() + ", " + this.getOperands().get(1).getName() + "\n";
                break;
            case sle:
                str = this.getName() + " = icmp sle i32 " + this.getOperands().get(0).getName() + ", " + this.getOperands().get(1).getName() + "\n";
                break;
            case call:
                if(this.getType() == Type.Void)
                    str = "call void " + this.getName() + "(";
                else
                    str = this.getName() + " = call i32 " + this.getOperands().get(0).getName() + "(";
                for (int i = 1; i < this.getOperands().size(); i++) {
                    if(this.getOperands().get(i).getDataType().equals("i32*"))
                        str += "i32* " + this.getOperands().get(i).getName();
                    else
                        str += "i32 " + this.getOperands().get(i).getName();
                    if (i != this.getOperands().size() - 1) {
                        str += ", ";
                    }
                }
                str += ")\n";
                break;
            case ret:
                if(this.getOperands().size() > 0)
                    str = "ret i32 " + this.getOperands().get(0).getName() + "\n";
                else
                    str = "ret void\n";
                break;
            case load:
                if(this.getDataType().equals("int"))
                    str = this.getName() + " = load i32, i32* " + this.getOperands().get(0).getName() + ", align 4\n";
                else
                    str = this.getName() + " = load " + this.getDataType() + ", " + this.getDataType() + " * " + this.getOperands().get(0).getName() + "\n";
                break;
            case store:
                String dataType = this.getOperands().get(0).getDataType();
                if(dataType.equals("int"))
                    str = "store i32 " + this.getOperands().get(0).getName() + ", i32* " + this.getOperands().get(1).getName() + ", align 4\n";
                else
                    str = "store " + dataType + ' ' + this.getOperands().get(0).getName() + ", " + dataType + " * " + this.getOperands().get(1).getName() + "\n";
                break;
            case alloca:
                if(this.isArray()){
                    dataType = this.getDataType();
                    str = this.getName() + " = alloca " + dataType.substring(0, dataType.length() - 1) + "\n";
                } else {
                    str = this.getName() + " = alloca i32, align 4\n";
                }
                break;
            case br:
                if(this.getOperands().size() == 1)
                    str = "br label %" + this.getOperands().get(0).getName() + "\n";
                else
                    str = "br i1 " + this.getOperands().get(0).getName() + ", label %" + this.getOperands().get(1).getName() + ", label %" + this.getOperands().get(2).getName() + "\n";
                break;
            case getelementptr:
                ArrayList<Value> operands = this.getOperands();
                dataType = operands.get(0).getDataType();
                //去掉dataType后面的*
                dataType = dataType.substring(0, dataType.length() - 1);
                str = this.getName() + " = getelementptr " + dataType + ", " + dataType + "* " + operands.get(0).getName();
                for (int i = 1; i < operands.size(); i++) {
                    str += ", i32 " + operands.get(i).getName();
                }
                str += "\n";
                break;
        }
        return str;
    }
}
