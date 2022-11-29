import ir.BasicBlock;

public class EqExpBlock {
    private final GrammarTree EqExpNode;
    private final BasicBlock basicBlock;
    private BasicBlock trueBlock;
    private BasicBlock falseBlock;
    public EqExpBlock(GrammarTree EqExpNode, BasicBlock basicBlock) {
        this.EqExpNode = EqExpNode;
        this.basicBlock = basicBlock;
    }
    public void setTrueBlock(BasicBlock trueBlock) {
        this.trueBlock = trueBlock;
    }
    public void setFalseBlock(BasicBlock falseBlock) {
        this.falseBlock = falseBlock;
    }
    public GrammarTree getEqExpNode() {
        return EqExpNode;
    }
    public BasicBlock getBasicBlock() {
        return basicBlock;
    }
    public BasicBlock getTrueBlock() {
        return trueBlock;
    }
    public BasicBlock getFalseBlock() {
        return falseBlock;
    }
}
