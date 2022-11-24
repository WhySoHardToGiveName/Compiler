import java.util.ArrayList;

public class SymbolTable {
    private ArrayList<Symbol> symbolList = new ArrayList<>();
    private SymbolTable parentTable;
    private ArrayList<SymbolTable> childTableList = new ArrayList<>();

    public void setParentTable(SymbolTable parentTable){
        this.parentTable = parentTable;
    }
    public SymbolTable getParentTable(){
        return parentTable;
    }
    public void addChildTable(SymbolTable childTable){
        childTableList.add(childTable);
    }
    public ArrayList<SymbolTable> getChildTableList(){
        return childTableList;
    }
    public void addSymbol(Symbol symbol){
        symbolList.add(symbol);
    }
    public ArrayList<Symbol> getSymbolList(){
        return symbolList;
    }
    public Symbol getSymbol(String name){
        for (Symbol symbol : symbolList) {
            if (symbol.getName().equals(name)) {
                return symbol;
            }
        }
        return null;
    }
    public Symbol getDeclaredSymbol(String name){
        for (Symbol symbol : symbolList) {
            if (symbol.getName().equals(name)) {
                return symbol;
            }
        }
        if(parentTable != null){
            return parentTable.getDeclaredSymbol(name);
        }
        return null;
    }
    public Symbol getTopSymbol(){
        if(symbolList.size() == 0){
            return null;
        }
        return symbolList.get(symbolList.size() - 1);
    }
    public SymbolTable getGlobalTable(){
        SymbolTable globalTable = this;
        while(globalTable.getParentTable() != null){
            globalTable = globalTable.getParentTable();
        }
        return globalTable;
    }
}
