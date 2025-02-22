package run.yigou.gxzy.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;

import java.io.Serializable;
import org.greenrobot.greendao.annotation.Generated;
@Entity
public class AiConfigBody implements Serializable {
    private static final long serialVersionUID = 1016L;
    @Id
    private String AiConfigBodyId;
    private String AiConfigId;
    private String GptModelName;
    @Generated(hash = 839356201)
    public AiConfigBody(String AiConfigBodyId, String AiConfigId,
            String GptModelName) {
        this.AiConfigBodyId = AiConfigBodyId;
        this.AiConfigId = AiConfigId;
        this.GptModelName = GptModelName;
    }
    @Generated(hash = 1473803907)
    public AiConfigBody() {
    }
    public String getAiConfigBodyId() {
        return this.AiConfigBodyId;
    }
    public void setAiConfigBodyId(String AiConfigBodyId) {
        this.AiConfigBodyId = AiConfigBodyId;
    }
    public String getAiConfigId() {
        return this.AiConfigId;
    }
    public void setAiConfigId(String AiConfigId) {
        this.AiConfigId = AiConfigId;
    }
    public String getGptModelName() {
        return this.GptModelName;
    }
    public void setGptModelName(String GptModelName) {
        this.GptModelName = GptModelName;
    }
}
