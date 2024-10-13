package run.yigou.gxzy.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;

import org.greenrobot.greendao.annotation.Generated;

import java.io.Serializable;

@Entity
public class ZhongYao implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id(autoincrement = true)
    private Long yaoId;
    private int ID;
    private String yaoList;
    private String name;
    private int height;
    private String text;
    private long signatureId;
    private String signature;

    @Generated(hash = 678019167)
    public ZhongYao(Long yaoId, int ID, String yaoList, String name, int height,
            String text, long signatureId, String signature) {
        this.yaoId = yaoId;
        this.ID = ID;
        this.yaoList = yaoList;
        this.name = name;
        this.height = height;
        this.text = text;
        this.signatureId = signatureId;
        this.signature = signature;
    }

    @Generated(hash = 703178808)
    public ZhongYao() {
    }

    public long getSignatureId() {
        return signatureId;
    }

    public void setSignatureId(long signatureId) {
        this.signatureId = signatureId;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }




    public Long getYaoId() {
        return this.yaoId;
    }

    public void setYaoId(Long yaoId) {
        this.yaoId = yaoId;
    }

    public int getID() {
        return this.ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getYaoList() {
        return this.yaoList;
    }

    public void setYaoList(String yaoList) {
        this.yaoList = yaoList;
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

}
