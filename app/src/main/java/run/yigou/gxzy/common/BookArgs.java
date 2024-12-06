package run.yigou.gxzy.common;

public class BookArgs {

   private int  bookNo;
   private int bookLastReadPosition;
   private boolean isShowBookCollect;

    public int getBookNo() {
        return bookNo;
    }

    public void setBookNo(int bookNo) {
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

    public static BookArgs newInstance(int bookNo, int bookLastReadPosition, boolean isShowBookCollect) {
        BookArgs bookArgs = new BookArgs();
        bookArgs.setBookNo(bookNo);
        bookArgs.setBookLastReadPosition(bookLastReadPosition);
        bookArgs.setShowBookCollect(isShowBookCollect);
        return bookArgs;
    }




}
