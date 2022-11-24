package ir;

public class Instruction extends User{
    private BasicBlock parentBB;
    private OPType opType;

    public enum OPType{
        add, sub, mul, sdiv, srem, shl, shr, and, or, xor, cmp, call, ret, br, phi, load, store, alloca, eq, ne, sgt, sge, slt, sle;

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
                str = this.getName() + " = load i32, i32* " + this.getOperands().get(0).getName() + ", align 4\n";
                break;
            case store:
                str = "store i32 " + this.getOperands().get(0).getName() + ", i32* " + this.getOperands().get(1).getName() + ", align 4\n";
                break;
            case alloca:
                str = this.getName() + " = alloca i32, align 4\n";
                break;
        }
        return str;
    }
}
