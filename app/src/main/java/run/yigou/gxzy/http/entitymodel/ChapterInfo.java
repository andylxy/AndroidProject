package run.yigou.gxzy.http.entitymodel;

import java.io.Serializable;
import java.util.List;

/**
 *  作者:  zhs
 *  时间:  2023-07-10 11:47:39
 *  包名:  run.yigou.gxzy.http.entitymodel
 *  类名:  ChapterInfo
 *  版本:  1.0
 *  描述:
 *
*/
public class ChapterInfo  implements Serializable {
  private  int  Id ;
  private  String  Title ;
  private  String  Section ;
  private List<ChapterInfoBody> ChapterInfoBody;
  private  String  Creator;

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public String getTitle() {
        return Title;
    }

    public void setTitle(String title) {
        Title = title;
    }

    public String getSection() {
        return Section;
    }

    public void setSection(String section) {
        Section = section;
    }

    public List<ChapterInfoBody> getChapterInfoBody() {
        return ChapterInfoBody;
    }

    public void setChapterInfoBody(List<ChapterInfoBody> chapterInfoBody) {
        ChapterInfoBody = chapterInfoBody;
    }

    public String getCreator() {
        return Creator;
    }

    public void setCreator(String creator) {
        Creator = creator;
    }
}
