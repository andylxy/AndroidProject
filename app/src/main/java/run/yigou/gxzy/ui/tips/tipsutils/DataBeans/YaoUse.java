package run.yigou.gxzy.ui.tips.tipsutils.DataBeans;

import java.io.Serializable;

public class YaoUse implements Serializable {
    private int YaoID;
    private String amount;
    private  String extraProcess;
    private  float maxWeight;
    private  String showName;
    private  String suffix;
    private float weight;
    private long signatureId;
    private String signature;

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

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getShowName() {
        return showName;
    }

    public void setShowName(String showName) {
        this.showName = showName;
    }

    public float getMaxWeight() {
        return maxWeight;
    }

    public void setMaxWeight(float maxWeight) {
        this.maxWeight = maxWeight;
    }

    public String getExtraProcess() {
        return extraProcess;
    }

    public void setExtraProcess(String extraProcess) {
        this.extraProcess = extraProcess;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public int getYaoID() {
        return YaoID;
    }

    public void setYaoID(int yaoID) {
        YaoID = yaoID;
    }
}