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

    def do_POST(self):
        body = self.rfile.read(int(self.headers["Content-Length"]))
        received.append((self.path, self.headers, json.loads(body)))
        self.send_response(self.status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.end_headers()
        self.wfile.write(self.response.encode())

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
    finally:
        server.shutdown()
        server.server_close()
        thread.join()

print("检查通过：配置校验、POST 路径/请求头/JSON、200/400 响应及不泄露配置值。")
