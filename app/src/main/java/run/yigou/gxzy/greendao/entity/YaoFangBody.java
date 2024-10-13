package run.yigou.gxzy.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;

import java.io.Serializable;
import org.greenrobot.greendao.annotation.Generated;
@Entity
public class YaoFangBody implements Serializable {
    private static final long serialVersionUID = 301L;

    @Id
    private String yaoFangBodyId;
    private String yaoFangID;
    private String suffix;
    private String amount;
    private int yaoID;
    private float weight;
    private String showName;
    private String extraProcess;
    private long signatureId;
    private String signature;

    @Generated(hash = 290227427)
    public YaoFangBody(String yaoFangBodyId, String yaoFangID, String suffix,
            String amount, int yaoID, float weight, String showName,
            String extraProcess, long signatureId, String signature) {
        this.yaoFangBodyId = yaoFangBodyId;
        this.yaoFangID = yaoFangID;
        this.suffix = suffix;
        this.amount = amount;
        this.yaoID = yaoID;
        this.weight = weight;
        this.showName = showName;
        this.extraProcess = extraProcess;
        this.signatureId = signatureId;
        this.signature = signature;
    }

    @Generated(hash = 2051687020)
    public YaoFangBody() {
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



    public String getYaoFangBodyId() {
        return this.yaoFangBodyId;
    }
    public void setYaoFangBodyId(String yaoFangBodyId) {
        this.yaoFangBodyId = yaoFangBodyId;
    }
    public String getYaoFangID() {
        return this.yaoFangID;
    }
    public void setYaoFangID(String yaoFangID) {
        this.yaoFangID = yaoFangID;
    }
    public String getSuffix() {
        return this.suffix;
    }
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }
    public String getAmount() {
        return this.amount;
    }
    public void setAmount(String amount) {
        this.amount = amount;
    }
    public int getYaoID() {
        return this.yaoID;
    }
    public void setYaoID(int yaoID) {
        this.yaoID = yaoID;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }
    public String getShowName() {
        return this.showName;
    }
    public void setShowName(String showName) {
        this.showName = showName;
    }
    public String getExtraProcess() {
        return this.extraProcess;
    }
    public void setExtraProcess(String extraProcess) {
        this.extraProcess = extraProcess;
    }

    public float getWeight() {
        return this.weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }


}
