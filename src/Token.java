public class Token {
    private String tokenCode;
    private String tokenValue;
    private int row;

    public Token(String tokenCode, String tokenValue, int row){
        this.tokenCode = tokenCode;
        this.tokenValue = tokenValue;
        this.row = row;
    }
    public String getTokenCode(){
        return tokenCode;
    }
    public String getTokenValue(){
        return tokenValue;
    }
    public int getRow(){
        return row;
    }
    public void setTokenCode(String tokenCode){
        this.tokenCode = tokenCode;
    }
    public void setTokenValue(String tokenValue){
        this.tokenValue = tokenValue;
    }
    public void setRow(int row){
        this.row = row;
    }
}
