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
        return this.getName() + " = dso_local global i32 " + this.getOperands().get(0).getName() + ", align 4\n";
    }
    public boolean isGlobalConstant() {
        return isConstant;
    }
}
