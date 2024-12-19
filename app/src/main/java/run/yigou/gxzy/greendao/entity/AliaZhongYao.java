package run.yigou.gxzy.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

import java.io.Serializable;

@Entity
public class AliaZhongYao implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id(autoincrement = true)
    private Long yaoAliaId;
    private int id;
    private String name;
    private String bieming;
    private int height;
    private String text;
    private long signatureId;
    private String signature;
    @Generated(hash = 1925581821)
    public AliaZhongYao(Long yaoAliaId, int id, String name, String bieming,
            int height, String text, long signatureId, String signature) {
        this.yaoAliaId = yaoAliaId;
        this.id = id;
        this.name = name;
        this.bieming = bieming;
        this.height = height;
        this.text = text;
        this.signatureId = signatureId;
        this.signature = signature;
    }
    @Generated(hash = 575411074)
    public AliaZhongYao() {
    }
    public Long getYaoAliaId() {
        return this.yaoAliaId;
    }
    public void setYaoAliaId(Long yaoAliaId) {
        this.yaoAliaId = yaoAliaId;
    }
    public int getId() {
        return this.id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getBieming() {
        return this.bieming;
    }
    public void setBieming(String bieming) {
        this.bieming = bieming;
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
    public long getSignatureId() {
        return this.signatureId;
    }
    public void setSignatureId(long signatureId) {
        this.signatureId = signatureId;
    }
    public String getSignature() {
        return this.signature;
    }
    public void setSignature(String signature) {
        this.signature = signature;
    }



}
