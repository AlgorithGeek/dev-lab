package com.avexvertex.smarttext.llm;

/**
 * 大模型客户端接口
 *
 * 屏蔽不同大模型 Provider 的具体调用方式。
 * 业务层只依赖这个接口，不需要关心底层使用的是 Qwen、OpenAI 还是其他模型。
 */
public interface LlmClient {

    /**
     * 向大模型发送 Prompt，并获取模型返回的文本内容。
     *
     * @param prompt 发送给大模型的提示词
     * @return 大模型返回的文本内容
     */
    String chat(String prompt);

}
