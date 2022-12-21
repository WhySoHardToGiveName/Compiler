package ir;

public class GlobalVariable extends User {
    private final boolean isConstant;
    private final Module parentModule;

    public GlobalVariable(String name, Type type,boolean isArr,String dataType, boolean isConstant, Module parentModule) {
        super(name, type, isArr, dataType);
        this.isConstant = isConstant;
        this.parentModule = parentModule;
        parentModule.addGlobalVariable(this);
    }

    public String toString() {
        if(!this.isArray())
            return this.getName() + " = dso_local global i32 " + this.getOperands().get(0).getName() + ", align 4\n";
        // @c = dso_local constant [2 x [1 x i32]] [[1 x i32] [i32 1], [1 x i32] [i32 3]]
        StringBuilder str = new StringBuilder(this.getName() + " = dso_local ");
        if(this.isConstant)
            str.append("constant ");
        else
            str.append("global ");
        if(this.getOperands().size() == 0){ // @d = dso_local global [5 x i32] zeroinitializer
            if(this.getArrayInfo().arrayDim == 1)
                str.append("[").append(this.getArrayInfo().arraySize1).append(" x i32]");
            else
                str.append("[").append(this.getArrayInfo().arraySize1).append(" x [").append(this.getArrayInfo().arraySize2).append(" x i32]]");
            str.append(" zeroinitializer");
        } else {
            if(this.getArrayInfo().arrayDim == 1)
                arrInitDimBase(str, 0, this.getArrayInfo().arraySize1);
            else
                arrInitDimMid(str, this.getArrayInfo().arraySize1, this.getArrayInfo().arraySize2);
        }
        str.append("\n");
        return str.toString();
    }

    private void arrInitDimBase(StringBuilder str, int dimMidIndex, int dimBaseSize) {   // 最底层的数组初始化[3 x i32] [i32 1, i32 2, i32 0]
        str.append("[").append(dimBaseSize).append(" x i32] [");
        for(int i = 0; i < dimBaseSize; i++){
            str.append("i32 ").append(this.getOperands().get(dimMidIndex * dimBaseSize + i).getName());
            if(i != dimBaseSize - 1)
                str.append(", ");
        }
        str.append("]");
    }

    private void arrInitDimMid(StringBuilder str, int dimMidSize, int dimBaseSize){      // 中层数组初始化[2 x [1 x i32]] [[1 x i32] [i32 1], [1 x i32] [i32 3]]
        str.append("[").append(dimMidSize).append(" x [").append(dimBaseSize).append(" x i32]] [");
        for(int i = 0; i < dimMidSize; i++){
            arrInitDimBase(str, i, dimBaseSize);
            if(i != dimMidSize - 1)
                str.append(", ");
        }
        str.append("]");
    }

    public boolean isGlobalConstant() {
        return isConstant;
    }
}
