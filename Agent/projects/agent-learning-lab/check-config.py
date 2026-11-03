"""mvn compile && python3 check-config.py；本地 HTTPS 检查，不调用百炼。"""
import http.server
import json
import os
from pathlib import Path
import ssl
import subprocess
import tempfile
import threading
import xml.etree.ElementTree as ET

project = Path(__file__).resolve().parent
fake_key = "test-key-not-a-real-secret"
received = []
pom = ET.parse(project / "pom.xml")
version = pom.find("{*}dependencies/{*}dependency/{*}version").text
jar = Path.home() / f".m2/repository/com/alibaba/fastjson2/fastjson2/{version}/fastjson2-{version}.jar"
classpath = os.pathsep.join([str(project / "target/classes"), str(jar)])


class Handler(http.server.BaseHTTPRequestHandler):
    status = 200
    response = '{"model":"qwen3.8-max","message":"模拟响应"}'
    queued_responses = None
    content_type = "application/json; charset=utf-8"

    def do_POST(self):
        body = self.rfile.read(int(self.headers["Content-Length"]))
        received.append((self.path, self.headers, json.loads(body)))
        status, response = (self.queued_responses.pop(0) if self.queued_responses is not None
                            else (self.status, self.response))
        self.send_response(status)
        self.send_header("Content-Type", self.content_type)
        self.end_headers()
        self.wfile.write(response.encode())

    def log_message(self, *args):
        pass


with tempfile.TemporaryDirectory() as directory:
    directory = Path(directory)
    cert, key, trust = [directory / name for name in ("cert.pem", "key.pem", "trust.p12")]
    subprocess.run([
        "openssl", "req", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "1",
        "-subj", "/CN=localhost", "-addext", "subjectAltName=DNS:localhost",
        "-keyout", str(key), "-out", str(cert),
    ], check=True, capture_output=True)
    subprocess.run([
        "keytool", "-importcert", "-noprompt", "-alias", "local-test", "-file", str(cert),
        "-keystore", str(trust), "-storetype", "PKCS12", "-storepass", "test-password",
    ], check=True, capture_output=True)
    server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Handler)
    tls = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    tls.load_cert_chain(cert, key)
    server.socket = tls.wrap_socket(server.socket, server_side=True)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    fake_url = f"https://localhost:{server.server_port}/compatible-mode/v1"
    try:
        for api_key, url, status, expected in [
            (None, fake_url, 1, "缺少 DASHSCOPE_API_KEY"),
            ("  ", fake_url, 1, "缺少 DASHSCOPE_API_KEY"),
            (fake_key, None, 1, "缺少 DASHSCOPE_BASE_URL"),
            (fake_key, "  ", 1, "缺少 DASHSCOPE_BASE_URL"),
            (fake_key, "not a url", 1, "DASHSCOPE_BASE_URL 格式错误"),
            (fake_key, "http://localhost/v1", 1, "DASHSCOPE_BASE_URL 格式错误"),
            (fake_key + "\n", fake_url, 1, "请求构造失败"),
            (fake_key, fake_url, 0, "HTTP Status: 200"),
            (fake_key, fake_url + "/", 0, "HTTP Status: 200"),
            (fake_key, fake_url, 1, "HTTP Status: 400"),
            (fake_key, fake_url, 1, "响应 JSON 解析失败"),
            (fake_key, fake_url, 1, "响应缺少有效的 model 字段"),
            (fake_key, fake_url, 1, "响应中的 choices 必须是非空数组"),
            (fake_key, fake_url, 1, "响应缺少有效的 choices[0].message.content 文本字段"),
            (fake_key, fake_url, 1, "响应缺少有效的 finish_reason 字段"),
            (fake_key, fake_url, 1, "响应缺少有效的 usage 对象"),
            (fake_key, fake_url, 1, "usage.completion_tokens 必须是非负整数"),
            (fake_key, fake_url, 1, "usage.prompt_tokens 必须是非负整数"),
            (fake_key, fake_url, 1, "usage.total_tokens 必须是非负整数"),
        ]:
            Handler.status = 400 if expected == "HTTP Status: 400" else 200
            Handler.response = json.dumps({"model": "qwen3.8-max", "choices": [
                {"message": {"content": "模拟回答"}, "finish_reason": "stop"}
            ], "usage": {"prompt_tokens": 19, "completion_tokens": 22, "total_tokens": 41}}, ensure_ascii=False)
            if expected == "HTTP Status: 400" or expected == "响应 JSON 解析失败":
                Handler.response = "not json"
            elif expected == "响应缺少有效的 model 字段":
                Handler.response = '{"model":123}'
            elif expected == "响应中的 choices 必须是非空数组":
                Handler.response = '{"model":"qwen3.8-max","choices":[]}'
            elif expected == "响应缺少有效的 choices[0].message.content 文本字段":
                Handler.response = '{"model":"qwen3.8-max","choices":[{"message":{"content":123}}]}'
            elif expected == "响应缺少有效的 finish_reason 字段":
                payload = json.loads(Handler.response)
                payload["choices"][0]["finish_reason"] = None
                Handler.response = json.dumps(payload, ensure_ascii=False)
            elif expected == "响应缺少有效的 usage 对象":
                payload = json.loads(Handler.response)
                del payload["usage"]
                Handler.response = json.dumps(payload, ensure_ascii=False)
            elif expected.startswith("usage."):
                payload = json.loads(Handler.response)
                field = expected.split()[0].split(".")[1]
                if field == "completion_tokens":
                    payload["usage"][field] = 1.5
                elif field == "prompt_tokens":
                    payload["usage"][field] = -1
                else:
                    del payload["usage"][field]
                Handler.response = json.dumps(payload, ensure_ascii=False)
            env = os.environ.copy()
            for name, value in [("DASHSCOPE_API_KEY", api_key), ("DASHSCOPE_BASE_URL", url)]:
                env.pop(name, None)
                if value is not None:
                    env[name] = value
            result = subprocess.run([
                "java", f"-Djavax.net.ssl.trustStore={trust}",
                "-Djavax.net.ssl.trustStorePassword=test-password", "-cp", classpath,
                "com.avexvertex.agent.FirstModelCall",
            ], cwd=project, env=env, capture_output=True, text=True, timeout=20)
            output = result.stdout + result.stderr
            assert result.returncode == status and expected in output, output
            assert fake_key not in output and fake_url not in output
            if expected.startswith("HTTP Status"):
                assert Handler.response in output
            if status == 0:
                assert "Model: qwen3.8-max" in output
                assert "Content: 模拟回答" in output
                assert "Finish Reason: stop" in output
                assert "Prompt Tokens: 19" in output
                assert "Completion Tokens: 22" in output
                assert "Total Tokens: 41" in output
            else:
                assert "Total Tokens: " not in output
            if Handler.status == 400:
                assert "响应 JSON 解析失败" not in output
        assert len(received) == 12
        for path, headers, body in received:
            assert path == "/compatible-mode/v1/chat/completions"
            assert headers["Authorization"] == "Bearer " + fake_key
            assert headers["Content-Type"] == "application/json"
            assert body == {"model": "qwen3.8-max", "messages": [
                {"role": "user", "content": "请用一句话解释 Java 是什么。"}
            ], "reasoning_effort": "none"}

        # 用带引号、换行和反斜杠的回答，检查 B 确实复用 seed 内容并正确转义。
        seed_answer = '实际回复："收到"\n路径 C:\\demo'
        first_question = "本次对话的实验代号是青柠47。请只回复“收到”。"
        second_question = "刚才我告诉你的实验代号是什么？如果当前对话信息不足，请回答“无法确定”。"
        expected_messages = [
            [{"role": "user", "content": first_question}],
            [{"role": "user", "content": second_question}],
            [{"role": "user", "content": first_question},
             {"role": "assistant", "content": seed_answer},
             {"role": "user", "content": second_question}],
        ]

        def success(content):
            return 200, json.dumps({"model": "qwen3.8-max", "choices": [
                {"message": {"content": content}, "finish_reason": "stop"}
            ], "usage": {"prompt_tokens": 30, "completion_tokens": 4, "total_tokens": 34}},
                ensure_ascii=False)

        for failed_at, failure, expected in [
            (None, None, None),
            (0, (400, "mock error"), "HTTP 请求未成功"),
            (0, (200, "not json"), "响应 JSON 解析失败"),
            (0, (200, '{"model":"qwen3.8-max","choices":[]}'), "choices 必须是非空数组"),
            (0, (200, '{"model":"qwen3.8-max","choices":[{"message":{"content":null}}]}'),
             "message.content 文本字段"),
            (1, (400, "mock error"), "HTTP 请求未成功"),
            (2, (400, "mock error"), "HTTP 请求未成功"),
        ]:
            received.clear()
            Handler.queued_responses = [success(seed_answer), success("无法确定"), success("青柠47")]
            if failed_at is not None:
                Handler.queued_responses[failed_at] = failure
            env = os.environ.copy()
            env.update(DASHSCOPE_API_KEY=fake_key, DASHSCOPE_BASE_URL=fake_url)
            result = subprocess.run([
                "java", f"-Djavax.net.ssl.trustStore={trust}",
                "-Djavax.net.ssl.trustStorePassword=test-password", "-cp", classpath,
                "com.avexvertex.agent.MessageHistoryDemo",
            ], cwd=project, env=env, capture_output=True, text=True, timeout=20)
            output = result.stdout + result.stderr
            assert result.returncode == (0 if failed_at is None else 1), output
            assert fake_key not in output and fake_url not in output
            expected_count = 3 if failed_at is None else failed_at + 1
            assert len(received) == expected_count, output
            for index, (path, headers, body) in enumerate(received):
                assert path == "/compatible-mode/v1/chat/completions"
                assert headers["Authorization"] == "Bearer " + fake_key
                assert headers["Content-Type"] == "application/json"
                assert body == {"model": "qwen3.8-max", "messages": expected_messages[index],
                                "reasoning_effort": "none"}, body
                assert json.loads(output.split("Messages: ")[index + 1].splitlines()[0]) == expected_messages[index]
            if failed_at is None:
                assert output.count("HTTP Status: 200") == 3
                assert output.count("Finish Reason: stop") == 3
                assert output.count("Prompt Tokens: 30") == 3
                assert output.count("Completion Tokens: 4") == 3
                assert output.count("Total Tokens: 34") == 3
                assert f"Content: {seed_answer}" in output
                assert "Content: 无法确定" in output and "Content: 青柠47" in output
            else:
                assert expected in output, output
                assert output.count("Total Tokens: ") == failed_at, output
                if failure[0] == 400:
                    assert "Error Response: mock error" in output
                    assert "响应 JSON 解析失败" not in output

        history_questions = ["本次实验的变量名是 alpha-21。请回复“收到”。",
                             "刚才我告诉你的变量名是什么？", "你上一轮回答了什么？请复述原话。"]
        history_answers = [seed_answer, '模拟变量回答："alpha-21"\n第二行', "模拟第三轮复述"]
        history_messages = []
        full_history = []
        for question, answer in zip(history_questions, history_answers):
            full_history.append({"role": "user", "content": question})
            history_messages.append(full_history.copy())
            full_history.append({"role": "assistant", "content": answer})

        scenarios = [("--compare", None, None, None), ("--history", None, None, None)]
        for failed_at in range(3):
            scenarios.extend([
                ("--history", failed_at, (400, "mock error"), "HTTP 请求未成功"),
                ("--history", failed_at, (200, "not json"), "响应 JSON 解析失败"),
                ("--history", failed_at, (200, success("不应保存的回答")[1].replace('"usage"', '"missing_usage"')),
                 "响应缺少有效的 usage 对象"),
            ])
        for mode, failed_at, failure, expected in scenarios:
            received.clear()
            answers = history_answers if mode == "--history" else [seed_answer, "无法确定", "青柠47"]
            requests = history_messages if mode == "--history" else expected_messages
            Handler.queued_responses = [success(answer) for answer in answers]
            if failed_at is not None:
                Handler.queued_responses[failed_at] = failure
            result = subprocess.run([
                "java", f"-Djavax.net.ssl.trustStore={trust}",
                "-Djavax.net.ssl.trustStorePassword=test-password", "-cp", classpath,
                "com.avexvertex.agent.MessageHistoryDemo", mode,
            ], cwd=project, env=env, capture_output=True, text=True, timeout=20)
            output = result.stdout + result.stderr
            assert result.returncode == (0 if failed_at is None else 1), output
            assert fake_key not in output and fake_url not in output
            completed = 3 if failed_at is None else failed_at
            assert len(received) == (3 if failed_at is None else failed_at + 1), output
            for index, (path, headers, body) in enumerate(received):
                assert path == "/compatible-mode/v1/chat/completions"
                assert headers["Authorization"] == "Bearer " + fake_key
                assert body == {"model": "qwen3.8-max", "messages": requests[index],
                                "reasoning_effort": "none"}, body
                assert json.loads(output.split("Messages: ")[index + 1].splitlines()[0]) == requests[index]
                assert json.loads(output.split("Roles: ")[index + 1].splitlines()[0]) == [
                    message["role"] for message in requests[index]]
            assert output.count("Total Tokens: 34") == completed, output
            if mode == "--history":
                counts = [int(part.splitlines()[0]) for part in output.split("发送前消息条数: ")[1:]]
                assert counts == [1, 3, 5][:len(received)], output
                saved_counts = [int(part.splitlines()[0]) for part in output.split("本轮成功后历史条数: ")[1:]]
                assert saved_counts == [2, 4, 6][:completed], output
                assert f"已保存历史条数: {completed * 2}" in output
                saved = json.loads(output.split("已保存历史 JSON: ")[1].splitlines()[0])
                assert saved == full_history[:completed * 2], saved
            if failed_at is not None:
                assert expected in output, output
                if failure[0] == 400:
                    assert "Error Response: mock error" in output
                    assert "响应 JSON 解析失败" not in output
        system_prompt = "\n".join([
            "你是一名 Java Code Review Assistant。", "",
            "你的任务是审查用户提供的 Java 代码。", "", "规则：",
            "1. 先给一句总体结论。", "2. 最多指出 3 个最重要的问题。",
            "3. 优先关注正确性、空指针风险、并发安全和资源管理。",
            "4. 只分析用户实际提供的代码；如果信息不足，明确说明，不要虚构不存在的上下文。",
            "5. 使用中文回答。",
        ])
        review_prompt = ("请帮我 Review 下面这段代码：\n\n"
                         "public String getUserName(User user) {\n"
                         "    return user.getProfile().getName();\n}")
        review_messages = [
            [{"role": "user", "content": review_prompt}],
            [{"role": "system", "content": system_prompt},
             {"role": "user", "content": review_prompt}],
        ]
        system_cases = [(None, None, None), (0, (400, "mock error"), "HTTP 请求未成功"),
                        (1, (400, "mock error"), "HTTP 请求未成功"),
                        (0, (200, "not json"), "响应 JSON 解析失败")]
        for field, value, expected in [
            ("model", None, "model 字段"), ("choices", [], "choices 必须是非空数组"),
            ("content", None, "message.content 文本字段"),
            ("finish_reason", None, "finish_reason 字段"),
            ("usage", None, "usage 对象"), ("prompt_tokens", -1, "usage.prompt_tokens"),
            ("completion_tokens", 1.5, "usage.completion_tokens"),
        ]:
            payload = json.loads(success("模拟回答")[1])
            if field == "content":
                payload["choices"][0]["message"][field] = value
            elif field == "finish_reason":
                payload["choices"][0][field] = value
            elif field.endswith("_tokens"):
                payload["usage"][field] = value
            else:
                payload[field] = value
            system_cases.append((0, (200, json.dumps(payload)), expected))
        for failed_at, failure, expected in system_cases:
            received.clear()
            Handler.queued_responses = [success('模拟 A 回答："风险"\n不应进入 B'), success("模拟 B 回答")]
            if failed_at is not None:
                Handler.queued_responses[failed_at] = failure
            result = subprocess.run([
                "java", f"-Djavax.net.ssl.trustStore={trust}",
                "-Djavax.net.ssl.trustStorePassword=test-password", "-cp", classpath,
                "com.avexvertex.agent.SystemPromptDemo",
            ], cwd=project, env=env, capture_output=True, text=True, timeout=20)
            output = result.stdout + result.stderr
            assert result.returncode == (0 if failed_at is None else 1), output
            assert fake_key not in output and fake_url not in output
            assert len(received) == (2 if failed_at is None else failed_at + 1), output
            for index, (path, headers, body) in enumerate(received):
                assert path == "/compatible-mode/v1/chat/completions"
                assert headers["Authorization"] == "Bearer " + fake_key
                assert headers["Content-Type"] == "application/json"
                assert body == {"model": "qwen3.8-max", "messages": review_messages[index],
                                "reasoning_effort": "none"}, body
                assert json.loads(output.split("Messages: ")[index + 1].splitlines()[0]) == review_messages[index]
                assert json.loads(output.split("Roles: ")[index + 1].splitlines()[0]) == [
                    message["role"] for message in review_messages[index]]
            completed = 2 if failed_at is None else failed_at
            for expected_field in ["Model: qwen3.8-max", "Finish Reason: stop", "Prompt Tokens: 30",
                                   "Completion Tokens: 4", "Total Tokens: 34"]:
                assert output.count(expected_field) == completed, output
            if failed_at is None:
                assert "发送前消息条数: 1" in output and "发送前消息条数: 2" in output
                assert 'Content: 模拟 A 回答："风险"\n不应进入 B' in output
                assert "Content: 模拟 B 回答" in output
            else:
                assert expected in output, output
                if failure[0] == 400:
                    assert "Error Response: mock error" in output
                    assert "响应 JSON 解析失败" not in output
        history_system_prompt = "\n".join([
            "你是一名 Java Code Review Assistant。",
            "回答每个问题时，第一行必须以“JAVA-REVIEW:”开头。",
            "只讨论 Java 代码或 Java 后端工程问题。", "使用中文回答。",
        ])
        system_questions = ["ThreadLocal 最常见的风险是什么？请用一句话回答。",
                            "那在线程池里为什么更需要小心？仍然用一句话回答。"]
        system_answers = ['模拟首轮回复："原话"\n路径 C:\\demo', "模拟第二轮回复"]
        saved_history = []
        system_requests = []
        for question, answer in zip(system_questions, system_answers):
            current_user = {"role": "user", "content": question}
            system_requests.append([{"role": "system", "content": history_system_prompt}]
                                   + saved_history + [current_user])
            saved_history = saved_history + [current_user, {"role": "assistant", "content": answer}]
        system_history_cases = [(None, None, None)]
        for failed_at in range(2):
            system_history_cases.extend([
                (failed_at, (400, "mock error"), "HTTP 请求未成功"),
                (failed_at, (200, "not json"), "响应 JSON 解析失败"),
                (failed_at, (200, success("不得保存")[1].replace('"usage"', '"missing_usage"')),
                 "响应缺少有效的 usage 对象"),
            ])
        for failed_at, failure, expected in system_history_cases:
            received.clear()
            Handler.queued_responses = [success(answer) for answer in system_answers]
            if failed_at is not None:
                Handler.queued_responses[failed_at] = failure
            result = subprocess.run([
                "java", f"-Djavax.net.ssl.trustStore={trust}",
                "-Djavax.net.ssl.trustStorePassword=test-password", "-cp", classpath,
                "com.avexvertex.agent.SystemPromptDemo", "--history",
            ], cwd=project, env=env, capture_output=True, text=True, timeout=20)
            output = result.stdout + result.stderr
            assert result.returncode == (0 if failed_at is None else 1), output
            assert fake_key not in output and fake_url not in output
            assert len(received) == (2 if failed_at is None else failed_at + 1), output
            for index, (path, headers, body) in enumerate(received):
                assert path == "/compatible-mode/v1/chat/completions"
                assert headers["Authorization"] == "Bearer " + fake_key
                assert headers["Content-Type"] == "application/json"
                assert body == {"model": "qwen3.8-max", "messages": system_requests[index],
                                "reasoning_effort": "none"}, body
                assert json.loads(output.split("Messages: ")[index + 1].splitlines()[0]) == system_requests[index]
                assert json.loads(output.split("Roles: ")[index + 1].splitlines()[0]) == [
                    message["role"] for message in system_requests[index]]
            completed = 2 if failed_at is None else failed_at
            assert output.count("Total Tokens: 34") == completed, output
            before = [int(part.splitlines()[0]) for part in output.split("Chat History Count before: ")[1:]]
            after = [int(part.splitlines()[0]) for part in output.split("Chat History Count after: ")[1:]]
            counts = [int(part.splitlines()[0]) for part in output.split("发送前消息条数: ")[1:]]
            assert before == [0, 2][:len(received)], output
            assert after == [2, 4][:completed], output
            assert counts == [2, 4][:len(received)], output
            assert f"已保存历史条数: {completed * 2}" in output
            saved = json.loads(output.split("已保存历史 JSON: ")[1].splitlines()[0])
            assert saved == saved_history[:completed * 2], saved
            if failed_at is not None:
                assert expected in output, output
                if failure[0] == 400:
                    assert "Error Response: mock error" in output
                    assert "响应 JSON 解析失败" not in output
        counter_input = ("public class CounterService {\n    private int count = 0;\n\n"
                         "    public int increase() {\n        return ++count;\n    }\n}")
        vague_prompt = "帮我看看下面这段代码有什么问题：\n\n" + counter_input
        structured_prompt = "\n".join([
            "Role:", "你是一名 Java 后端代码审查助手。", "",
            "Task:", "检查下面代码中最值得优先修复的问题，重点判断是否存在并发安全风险。", "",
            "Context:", "这段代码位于 Spring Boot 单例 Service 中，同一个 Service 实例可能被多个 HTTP 请求线程并发访问。", "",
            "Input:", counter_input, "", "Constraints:", "1. 最多指出 3 个问题。",
            "2. 只基于给出的代码和 Context 分析。", "3. 不要虚构不存在的数据库、缓存、接口或调用链。", "",
            "Output Format:", "先给一句总体结论。", "然后按“问题 / 原因 / 建议”输出。",
        ])
        # C 只去掉背景段，并按手册调整两条约束；其余结构和代码与 B 相同。
        contextless_prompt = structured_prompt.replace(
            "Context:\n这段代码位于 Spring Boot 单例 Service 中，同一个 Service 实例可能被多个 HTTP 请求线程并发访问。\n\n", "")
        contextless_prompt = contextless_prompt.replace(
            "2. 只基于给出的代码和 Context 分析。", "2. 只基于给出的代码分析。")
        contextless_prompt = contextless_prompt.replace(
            "3. 不要虚构不存在的数据库、缓存、接口或调用链。",
            "3. 信息不足时明确说明，不要自行假设对象生命周期或并发场景。")
        assert "Context:" not in contextless_prompt and "Spring Boot" not in contextless_prompt
        for mode_args, prompt_texts in [([], [vague_prompt, structured_prompt]),
                                       (["--context"], [structured_prompt, contextless_prompt])]:
            # 沿用前面定义的成功、HTTP 错误、非法 JSON 和缺失/无效字段场景。
            for failed_at, failure, expected in system_cases:
                received.clear()
                Handler.queued_responses = [success('模拟 A："问题"\n不要放进 B'), success("模拟 B 回答")]
                if failed_at is not None:
                    Handler.queued_responses[failed_at] = failure
                result = subprocess.run([
                    "java", f"-Djavax.net.ssl.trustStore={trust}",
                    "-Djavax.net.ssl.trustStorePassword=test-password", "-cp", classpath,
                    "com.avexvertex.agent.PromptBasicsDemo", *mode_args,
                ], cwd=project, env=env, capture_output=True, text=True, timeout=20)
                output = result.stdout + result.stderr
                assert result.returncode == (0 if failed_at is None else 1), output
                assert fake_key not in output and fake_url not in output
                assert len(received) == (2 if failed_at is None else failed_at + 1), output
                for index, (path, headers, body) in enumerate(received):
                    assert path == "/compatible-mode/v1/chat/completions"
                    assert headers["Authorization"] == "Bearer " + fake_key
                    assert headers["Content-Type"] == "application/json"
                    messages = [{"role": "user", "content": prompt_texts[index]}]
                    assert body == {"model": "qwen3.8-max", "messages": messages,
                                    "reasoning_effort": "none"}, body
                    assert json.loads(output.split("Messages: ")[index + 1].splitlines()[0]) == messages
                    assert json.loads(output.split("Roles: ")[index + 1].splitlines()[0]) == ["user"]
                    assert body["messages"][0]["content"].count(counter_input) == 1
                    if index == 0 and not mode_args:
                        assert body["messages"][0]["content"].split("\n\n", 1)[1] == counter_input
                    else:
                        assert body["messages"][0]["content"].split("Input:\n", 1)[1].split("\n\nConstraints:", 1)[0] == counter_input
                    assert f"User Prompt 字符数: {len(prompt_texts[index])}" in output
                completed = 2 if failed_at is None else failed_at
                for field in ["Model: qwen3.8-max", "Finish Reason: stop", "Prompt Tokens: 30",
                              "Completion Tokens: 4", "Total Tokens: 34"]:
                    assert output.count(field) == completed, output
                if failed_at is None:
                    assert output.count("发送前消息条数: 1") == 2
                    assert 'Content: 模拟 A："问题"\n不要放进 B' in output
                    assert "Content: 模拟 B 回答" in output
                    if mode_args:
                        assert "=== B（有 Context） ===" in output and "=== C（无 Context） ===" in output
                        assert "=== A（模糊 Prompt） ===" not in output
                    else:
                        assert "=== A（模糊 Prompt） ===" in output and "=== B（结构化 Prompt） ===" in output
                else:
                    assert expected in output, output
                    if failure[0] == 400:
                        assert "Error Response: mock error" in output
                        assert "响应 JSON 解析失败" not in output
        campaign_schema = {
            "type": "object",
            "properties": {
                "campaignId": {"type": "integer", "description": "广告活动 ID"},
                "riskLevel": {"type": "string", "enum": ["HIGH", "MEDIUM", "LOW"],
                              "description": "广告活动当前风险等级"},
                "reason": {"type": "string", "description": "风险判断的主要原因"},
                "suggestion": {"type": "string", "description": "给出的简短优化建议"},
            },
            "required": ["campaignId", "riskLevel", "reason", "suggestion"],
            "additionalProperties": False,
        }
        # 本步继续将 content 转成 DTO，并做最小字段检查；不声称完成全部 Schema 校验。
        mock_content = '{"campaignId":10001,"riskLevel":"HIGH","reason":"模拟原因","suggestion":"模拟建议"}'
        wrong_id = mock_content.replace('10001', '"abc"')
        missing_suggestion = '{"campaignId":10001,"riskLevel":"HIGH","reason":"模拟原因"}'
        invalid_risk = mock_content.replace('"HIGH"', '"CRITICAL"')
        for status, raw_response, expected_content, error in [
            (200, success(mock_content)[1], mock_content, None),
            (200, success("普通文本")[1], "普通文本", "Structured Content 无法反序列化"),
            (200, success(wrong_id)[1], wrong_id, "Structured Content 无法反序列化"),
            (200, success(missing_suggestion)[1], missing_suggestion, "reason 和 suggestion 必须有非空白文本"),
            (200, success(invalid_risk)[1], invalid_risk, "riskLevel 必须是 HIGH、MEDIUM 或 LOW"),
            (200, success("null")[1], "null", "DTO 或 campaignId 为空"),
            (200, "not json", None, "响应 JSON 解析失败"),
            (200, "null", None, "choices 必须是非空数组"),
            (200, '{"choices":[]}', None, "choices 必须是非空数组"),
            (200, '{"choices":[{}]}', None, "message.content 文本字段"),
            (200, '{"choices":[{"message":{"content":123}}]}', None, "message.content 文本字段"),
            (200, success("  ")[1], None, "message.content 文本字段"),
            (400, "mock error", None, "HTTP 请求未成功"),
        ]:
            received.clear()
            Handler.queued_responses = [(status, raw_response)]
            env = os.environ.copy()
            env.update(DASHSCOPE_API_KEY=fake_key, DASHSCOPE_BASE_URL=fake_url)
            result = subprocess.run([
                "java", f"-Djavax.net.ssl.trustStore={trust}",
                "-Djavax.net.ssl.trustStorePassword=test-password", "-cp", classpath,
                "com.avexvertex.agent.StructuredOutputDemo",
            ], cwd=project, env=env, capture_output=True, text=True, timeout=20)
            output = result.stdout + result.stderr
            assert result.returncode == (0 if error is None else 1), output
            assert f"HTTP Status: {status}" in output and raw_response in output
            assert fake_key not in output and fake_url not in output
            assert "Model: " not in output
            if expected_content is not None:
                assert "Structured Content:\n" + expected_content in output
            else:
                assert "Structured Content:" not in output
            if error is None:
                assert "CampaignAnalysis: CampaignAnalysis{" in output
                assert "Campaign ID: 10001" in output and "Risk Level: HIGH" in output
                assert "Reason: 模拟原因" in output and "Suggestion: 模拟建议" in output
                assert "响应 JSON 解析失败" not in output
            else:
                assert error in output, output
                assert "Campaign ID: " not in output and "CampaignAnalysis: " not in output
                if status == 400:
                    assert "响应 JSON 解析失败" not in output
            assert len(received) == 1, output
            path, headers, body = received[0]
            assert path == "/compatible-mode/v1/chat/completions"
            assert headers["Authorization"] == "Bearer " + fake_key
            assert headers["Content-Type"] == "application/json"
            assert body == {
                "model": "qwen3.8-max", "reasoning_effort": "none",
                "messages": [{"role": "user", "content": "请分析下面广告活动的风险。Campaign ID：10001；日预算：5000 元；近 7 天转化率：0.3%。"}],
                "response_format": {"type": "json_schema", "json_schema": {
                    "name": "campaign_analysis", "strict": True, "schema": campaign_schema}},
            }, body
        # SSE 样例包含空角色块、空格正文、多 data 行事件、独立 Usage 和结束标记。
        def sse_event(payload):
            return "data: " + json.dumps(payload, ensure_ascii=False) + "\n\n"

        role_event = sse_event({"model": "qwen3.8-max", "choices": [
            {"delta": {"role": "assistant", "content": ""}, "finish_reason": None}]})
        text_event = sse_event({"choices": [{"delta": {"content": "HashMap"}, "finish_reason": None}]})
        space_event = sse_event({"choices": [{"delta": {"content": " "}, "finish_reason": None}]})
        multiline_event = "\n".join("data: " + line for line in json.dumps({"choices": [
            {"delta": {"content": "不是线程安全的。\n"}, "finish_reason": None}]},
            ensure_ascii=False, indent=2).splitlines()) + "\n\n"
        finish_event = sse_event({"choices": [{"delta": {}, "finish_reason": "stop"}]})
        usage_event = sse_event({"choices": [], "usage": {
            "prompt_tokens": 30, "completion_tokens": 20, "total_tokens": 50}})
        done_event = "data: [DONE]\n\n"
        prefix = ": heartbeat\n\nevent: message\n" + role_event + text_event + space_event + multiline_event
        complete_stream = prefix + finish_event + usage_event + done_event
        for mode_args, status, response_text, content_type, expected_error in [
            ([], 200, complete_stream, "text/event-stream; charset=utf-8", None),
            (["--debug"], 200, complete_stream, "text/event-stream; charset=utf-8", None),
            ([], 400, "mock error", "application/json", "HTTP 请求未成功"),
            ([], 200, prefix + finish_event + usage_event, "text/event-stream", "未收到 [DONE]"),
            ([], 200, prefix + finish_event + done_event, "text/event-stream", "缺少正文、Model、Finish Reason 或 Usage"),
            ([], 200, role_event + "data: not json\n\n", "text/event-stream", "SSE 数据不是有效 JSON"),
            ([], 200, role_event + sse_event({"choices": [{"delta": {"content": 123}}]}),
             "text/event-stream", "delta.content 必须是文本"),
            ([], 200, prefix + finish_event + usage_event.replace('"prompt_tokens": 30', '"prompt_tokens": -1') + done_event,
             "text/event-stream", "usage.prompt_tokens 必须是非负整数"),
            ([], 200, complete_stream, "application/json", "不是 text/event-stream"),
        ]:
            received.clear()
            Handler.content_type = content_type
            Handler.queued_responses = [(status, response_text)]
            env = os.environ.copy()
            env.update(DASHSCOPE_API_KEY=fake_key, DASHSCOPE_BASE_URL=fake_url)
            result = subprocess.run([
                "java", f"-Djavax.net.ssl.trustStore={trust}",
                "-Djavax.net.ssl.trustStorePassword=test-password", "-cp", classpath,
                "com.avexvertex.agent.StreamingDemo", *mode_args,
            ], cwd=project, env=env, capture_output=True, text=True, timeout=20)
            output = result.stdout + result.stderr
            assert result.returncode == (0 if expected_error is None else 1), output
            assert fake_key not in output and fake_url not in output
            assert len(received) == 1, output
            path, headers, body = received[0]
            assert path == "/compatible-mode/v1/chat/completions"
            assert headers["Authorization"] == "Bearer " + fake_key
            assert body == {"model": "qwen3.8-max", "messages": [{"role": "user", "content":
                "请用 3 到 5 句话解释 Java HashMap 为什么不是线程安全的。"}],
                "stream": True, "stream_options": {"include_usage": True}, "reasoning_effort": "none"}, body
            if expected_error is None:
                assert "Full Content（已拼接正文）:\nHashMap 不是线程安全的。\n" in output
                assert 'First Content: "HashMap"' in output
                assert "Finish Reason: stop" in output and "[DONE] received: true" in output
                for field in ["Model: qwen3.8-max", "Prompt Tokens: 30", "Completion Tokens: 20", "Total Tokens: 50"]:
                    assert field in output, output
                ttft = float(output.split("TTFT（首个非空正文，ms）: ")[1].splitlines()[0])
                total_time = float(output.split("Total Time（收到 DONE，ms）: ")[1].splitlines()[0])
                assert 0 <= ttft <= total_time
                assert ("[SSE]" in output) == bool(mode_args), output
                if mode_args:
                    assert "[SSE] [DONE]" in output
            else:
                assert expected_error in output, output
                assert "Full Content（已拼接正文）:" not in output
                assert "[DONE] received: true" not in output
                if status == 400:
                    assert "Error Response: mock error" in output
                    assert "SSE 数据不是有效 JSON" not in output
    finally:
        server.shutdown()
        server.server_close()
        thread.join()

print("检查通过：既有实验、Structured Output DTO 与 Streaming SSE 场景；不访问百炼。")
