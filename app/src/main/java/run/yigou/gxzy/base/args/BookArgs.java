package run.yigou.gxzy.base.args;

public class BookArgs {

   private String  bookNo;
   private int bookLastReadPosition;
   private boolean isShowBookCollect;

    public String getBookNo() {
        return bookNo;
    }

    public void setBookNo(String bookNo) {
        this.bookNo = bookNo;
    }

    public int getBookLastReadPosition() {
        return bookLastReadPosition;
    }

    public void setBookLastReadPosition(int bookLastReadPosition) {
        this.bookLastReadPosition = bookLastReadPosition;
    }

    public boolean isShowBookCollect() {
        return isShowBookCollect;
    }

    public void setShowBookCollect(boolean showBookCollect) {
        isShowBookCollect = showBookCollect;
    }

    public static BookArgs newInstance(String bookNo, int bookLastReadPosition, boolean isShowBookCollect) {
        BookArgs bookArgs = new BookArgs();
        bookArgs.setBookNo(bookNo);
        bookArgs.setBookLastReadPosition(bookLastReadPosition);
        bookArgs.setShowBookCollect(isShowBookCollect);
        return bookArgs;
    }




}
