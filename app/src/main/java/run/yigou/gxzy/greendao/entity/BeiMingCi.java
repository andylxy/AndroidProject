package run.yigou.gxzy.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;

import java.io.Serializable;
import org.greenrobot.greendao.annotation.Generated;
@Entity
public class BeiMingCi implements Serializable {
    private static final long serialVersionUID = 2L;
    @Id(autoincrement = true)
    private Long beiMingCiId;
    private int ID;
    private String mingCiList;
    private String name;
    private int height;
    private String text;
    private long signatureId;
    private String signature;

    @Generated(hash = 805985940)
    public BeiMingCi(Long beiMingCiId, int ID, String mingCiList, String name,
            int height, String text, long signatureId, String signature) {
        this.beiMingCiId = beiMingCiId;
        this.ID = ID;
        this.mingCiList = mingCiList;
        this.name = name;
        this.height = height;
        this.text = text;
        this.signatureId = signatureId;
        this.signature = signature;
    }

    @Generated(hash = 122937484)
    public BeiMingCi() {
    }

    public long getSignatureId() {
        return signatureId;
    }

    public void setSignatureId(long signatureId) {
        this.signatureId = signatureId;
    }




    public Long getBeiMingCiId() {
        return this.beiMingCiId;
    }
    public void setBeiMingCiId(Long beiMingCiId) {
        this.beiMingCiId = beiMingCiId;
    }
    public int getID() {
        return this.ID;
    }
    public void setID(int ID) {
        this.ID = ID;
    }
    public String getMingCiList() {
        return this.mingCiList;
    }
    public void setMingCiList(String mingCiList) {
        this.mingCiList = mingCiList;
    }
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getHeight() {
        return this.height;
    }
    public void setHeight(int height) {
        this.height = height;
    }
    public String getText() {
        return this.text;
    }
    public void setText(String text) {
        this.text = text;
    }

    public String getSignature() {
        return this.signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
