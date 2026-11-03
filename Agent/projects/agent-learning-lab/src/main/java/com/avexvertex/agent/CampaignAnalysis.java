package com.avexvertex.agent;

/**
 * 保存模型返回的广告活动分析结果；字段名与后面 Schema 中的业务字段保持一致。
 * 这里只存数据，不发送 HTTP 请求，也不负责判断模型结论是否正确。
 */
public class CampaignAnalysis {
    // 活动 ID。使用 Long 而不是 long，缺少值时可以保留 null，便于后续检查。
    private Long campaignId;

    // 风险等级，预期为 HIGH / MEDIUM / LOW；先用 String，后续代码再检查取值。
    private String riskLevel;

    // 模型给出风险判断的原因。
    private String reason;

    // 模型给出的建议；这只是待审查的文本，不会自动执行任何业务操作。
    private String suggestion;

    // getter 读取字段：例如 analysis.getCampaignId() 取得活动 ID。
    public Long getCampaignId() {
        return campaignId;
    }

    // setter 写入字段：this.campaignId 是对象的字段，右侧 campaignId 是传入的值。
    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    // 打印对象时展示四个字段，方便学习；这段显示文本不是 JSON 序列化结果。
    @Override
    public String toString() {
        return "CampaignAnalysis{" +
                "campaignId=" + campaignId +
                ", riskLevel='" + riskLevel + '\'' +
                ", reason='" + reason + '\'' +
                ", suggestion='" + suggestion + '\'' +
                '}';
    }
}
