package ir;

import java.util.LinkedList;

public class Value {
    private final String name;
    private final Type type;
    private final String dataType;
    private final LinkedList<Use> useList;
    private boolean isArray;
    private ArrayInfo arrayInfo;
    public class ArrayInfo{
        public int arrayDim;
        public int arraySize1;
        public int arraySize2;
    }

    public Value(String name, Type type, boolean isArray, String dataType) {
        this.name = name;
        this.type = type;
        this.isArray = isArray;
        this.dataType = dataType;
        this.useList = new LinkedList<>();
    }
    public void setArrayInfo(int arrayDim, int arraySize1, int arraySize2){
        this.arrayInfo = new ArrayInfo();
        this.arrayInfo.arrayDim = arrayDim;
        this.arrayInfo.arraySize1 = arraySize1;
        this.arrayInfo.arraySize2 = arraySize2;
    }
    public ArrayInfo getArrayInfo(){
        return this.arrayInfo;
    }
    public String getName() {
        return name;
    }
    public Type getType() {
        return type;
    }
    public LinkedList<Use> getUseList() {
        return useList;
    }
    public void addUse(Use use) {
        useList.add(use);
    }
    public String getDataType() {
        return dataType;
    }
    public boolean isConstant() {
        return type == Type.Const;
    }
    public boolean isGlobalConstant() {
        if(this instanceof GlobalVariable) {
            return ((GlobalVariable) this).isGlobalConstant();
        }
        return false;
    }
}
