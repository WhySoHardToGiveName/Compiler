public class Error {
    private final int row;
    private final String errorCode;
    public Error(int row, String errorCode){
        this.row = row;
        this.errorCode = errorCode;
    }
    public int getRow(){
        return row;
    }
    public String getErrorCode(){
        return errorCode;
    }

    @Override
    public String toString() {
        return getRow() + " " + getErrorCode();
    }
}
