package run.yigou.gxzy.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;

import java.io.Serializable;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class About implements Serializable {
    private static final long serialVersionUID = 101L;
    @Id(autoincrement = true)
    private Long aboutId;
    private String text;
    private String name;
    @Generated(hash = 335493714)
    public About(Long aboutId, String text, String name) {
        this.aboutId = aboutId;
        this.text = text;
        this.name = name;
    }
    @Generated(hash = 1356061360)
    public About() {
    }
    public Long getAboutId() {
        return this.aboutId;
    }
    public void setAboutId(Long aboutId) {
        this.aboutId = aboutId;
    }
    public String getText() {
        return this.text;
    }
    public void setText(String text) {
        this.text = text;
    }
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
}
