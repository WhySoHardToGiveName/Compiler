import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class GrammarTree {
    private String vname;
    private int kidsNum = 0;
    private ArrayList<GrammarTree> kidTreeList;
    private final boolean isVt;


    public GrammarTree(String vname, boolean isVt){
        this.vname = vname;
        this.isVt = isVt;
    }
    public String getVname(){
        return vname;
    }
    public void setVname(String vname){
        this.vname = vname;
    }
    public int getKidsNum(){
        return kidsNum;
    }
    public ArrayList<GrammarTree> getKidTreeList(){
        return kidTreeList;
    }
    public void insertKidTree(GrammarTree kidTree){
        if(this.kidsNum == 0){
            this.kidTreeList = new ArrayList<>();
        }
        this.kidTreeList.add(kidTree);
        this.kidsNum++;
    }
    // 用于获取exp结点子树中的标识符节点所在的kidTreeList,LVal → Ident {'[' Exp ']'}或UnaryExp → Ident '(' [FuncRParams] ')'
    public ArrayList<GrammarTree> getIdentKidTreeList(){
        if(this.isVt){
            return null;
        }
        if(this.kidTreeList.get(0).getVname().equals("Ident")){
            return this.kidTreeList;
        }
        for(GrammarTree kidTree : this.kidTreeList){
            ArrayList<GrammarTree> identKidTreeList = kidTree.getIdentKidTreeList();
            if(identKidTreeList != null){
                return identKidTreeList;
            }
        }
        return null;
    }

    public void printTree(String outputPath) throws IOException {
        FileWriter fw = new FileWriter(outputPath);
        BufferedWriter bw = new BufferedWriter(fw);
        recurWrite(bw);
        bw.close();
        fw.close();
    }
    private void recurWrite(BufferedWriter bw) throws IOException{
        if(this.isVt){
            bw.write(((VtNode) this).getToken().getTokenCode() + ' ' + ((VtNode) this).getToken().getTokenValue());
            bw.newLine();
            bw.flush();
        } else{
            if(this.kidsNum == 0){
                System.err.println("Error: No kids for non-terminal node." + this.vname);
            } else{
                for(GrammarTree kidTree : this.kidTreeList){
                    kidTree.recurWrite(bw);
                }
                if(!(this.vname.equals("BlockItem") || this.vname.equals("Decl") || this.vname.equals("BType"))){
                    bw.write('<' + this.vname + '>');
                    bw.newLine();
                    bw.flush();
                }
            }
        }
    }
}

