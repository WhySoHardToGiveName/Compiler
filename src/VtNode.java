public class VtNode extends GrammarTree {
    private Token token;

    public VtNode(String vname, Token token) {
        super(vname, true);
        this.token = token;
    }

    public Token getToken() {
        return this.token;
    }

    public void setToken(Token token) {
        this.token = token;
    }
}
