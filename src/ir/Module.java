package ir;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Module extends BasicBlock{
    private final ArrayList<GlobalVariable> globalVariables;
    private final ArrayList<Function> funcList;
    private static final Module module = new Module();

    private Module() {
        super("Module", null);
        this.globalVariables = new ArrayList<>();
        this.funcList = new ArrayList<>();
        new Function("@getint", "int", this);
        new Function("@putint", "void", this);
        new Function("@putch", "void", this);
        new Function("@putstr", "void", this);
        new Function("@memset", "void", this);
    }

    public static Module getModule() {
        return module;
    }
    public ArrayList<GlobalVariable> getGlobalVariables() {
        return globalVariables;
    }
    public ArrayList<Function> getFuncList() {
        return funcList;
    }
    public void addGlobalVariable(GlobalVariable globalVariable) {
        globalVariables.add(globalVariable);
    }
    public void addFunction(Function function) {
        funcList.add(function);
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (GlobalVariable globalVariable : globalVariables) {
            sb.append(globalVariable.toString());
        }
        for (Function function : funcList) {
            sb.append(function.toString());
        }
        return sb.toString();
    }
    public void print(String outputPath) throws IOException {
        FileWriter fw = new FileWriter(outputPath);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(this.toString());
        bw.close();
        fw.close();
    }
}
