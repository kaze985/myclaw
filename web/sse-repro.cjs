const http = require('http');
// 模拟 Spring SseEmitter 输出：LF 帧分隔 \n\n
const server = http.createServer((req, res) => {
  if (req.method === 'POST' && req.url === '/api/chat') {
    res.writeHead(200, { 'Content-Type': 'text/event-stream', 'Cache-Control': 'no-cache' });
    res.write('event:thought\ndata:{"text":"正在调用 webSearch 工具"}\n\n');
    setTimeout(() => {
      const content = '已完成。\n结果如下：\n- 第一行\n- 第二行';
      res.write('event:done\ndata:' + JSON.stringify({ content }) + '\n\n');
      res.end();
    }, 100);
  }
});
server.listen(4199, () => console.log('sse-mock on 4199'));

// 与前端 useChat 完全相同的解析逻辑
(async () => {
  const res = await fetch('http://localhost:4199/api/chat', { method: 'POST' });
  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  const frames = [];
  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    let frameEnd;
    while ((frameEnd = buffer.indexOf('\n\n')) >= 0) {
      const frame = buffer.slice(0, frameEnd);
      buffer = buffer.slice(frameEnd + 2);
      frames.push(frame);
    }
  }
  console.log('PARSED_FRAMES=' + JSON.stringify(frames));
  console.log('REMAINDER=' + JSON.stringify(buffer));
  server.close();
})();
