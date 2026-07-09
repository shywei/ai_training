<template>
  <div class="chat-container">
    <!-- 顶部栏 -->
    <div class="chat-header">
      <h1>🛒 导购 Agent</h1>
      <span class="version">v1.0-prompt</span>
      <span class="model-info">{{ currentModel }}</span>
    </div>

    <!-- 消息区域 -->
    <div class="chat-messages" ref="messages">
      <div v-if="messages.length === 0" class="empty-state">
        <div><i class="el-icon-chat-dot-round"></i></div>
        <p>你好！我是导购助手，有什么可以帮你的？</p>
      </div>

      <div v-for="(msg, index) in messages" :key="index"
           :class="['message-row', msg.role]">
        <div class="message-avatar">
          <i :class="msg.role === 'user' ? 'el-icon-user' : 'el-icon-chat-dot-round'"></i>
        </div>
        <div class="message-bubble">
          <div v-if="msg.loading" class="typing-dots">
            <span></span><span></span><span></span>
          </div>
          <div v-else-if="msg.role === 'assistant'" v-html="renderMarkdown(msg.content)"></div>
          <template v-else>{{ msg.content }}</template>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="1"
        :autosize="{ minRows: 1, maxRows: 4 }"
        placeholder="输入消息... (Enter 发送, Shift+Enter 换行)"
        @keydown.native="handleKeydown"
        :disabled="loading"
      ></el-input>
      <el-button
        type="primary"
        icon="el-icon-s-promotion"
        :loading="loading"
        @click="sendMessage"
        circle
        size="medium"
      ></el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'App',
  data() {
    return {
      inputText: '',
      loading: false,
      currentModel: '',
      messages: []
    }
  },
  methods: {
    renderMarkdown(text) {
      if (!text) return ''
      var html = text
        .replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>')
        .replace(/`([^`]+)`/g, '<code>$1</code>')
        .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
        .replace(/^### (.+)$/gm, '<h3>$1</h3>')
        .replace(/^## (.+)$/gm, '<h2>$1</h2>')
        .replace(/^# (.+)$/gm, '<h1>$1</h1>')
        .replace(/^[\-*] (.+)$/gm, '<li>$1</li>')
        .replace(/^\d+\. (.+)$/gm, '<li>$1</li>')
        .replace(/\n\n/g, '</p><p>')
        .replace(/\n/g, '<br>')
      html = '<p>' + html + '</p>'
      html = html.replace(/<p>\s*<\/p>/g, '')
      return html
    },

    handleKeydown(e) {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault()
        this.sendMessage()
      }
    },

    async sendMessage() {
      const text = this.inputText.trim()
      if (!text || this.loading) return

      this.messages.push({ role: 'user', content: text })
      this.inputText = ''
      this.loading = true

      const assistantIndex = this.messages.length
      this.messages.push({ role: 'assistant', content: '', loading: true })

      this.$nextTick(() => this.scrollToBottom())

      try {
        const response = await fetch(
          `/api/agent/chat?message=${encodeURIComponent(text)}`
        )

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`)
        }

        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        let fullText = ''

        this.messages[assistantIndex].loading = false

        while (true) {
          const { done, value } = await reader.read()
          if (done) break

          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop()

          for (const line of lines) {
            if (line.startsWith('data:')) {
              const content = line.substring(5)
              if (content.trim() === '[DONE]') continue
              fullText += content
              this.messages[assistantIndex].content = fullText
              this.$nextTick(() => this.scrollToBottom())
            }
          }
        }

        if (buffer.startsWith('data:')) {
          const content = buffer.substring(5)
          if (content.trim() !== '[DONE]') {
            fullText += content
            this.messages[assistantIndex].content = fullText
          }
        }

        if (!fullText) {
          this.messages[assistantIndex].content = '（无响应）'
        }
      } catch (err) {
        this.messages[assistantIndex].loading = false
        this.messages[assistantIndex].content = `请求失败: ${err.message}`
        this.$message.error('请求失败，请检查后端服务是否启动')
      } finally {
        this.loading = false
        this.$nextTick(() => this.scrollToBottom())
      }
    },

    scrollToBottom() {
      const el = this.$refs.messages
      if (el) el.scrollTop = el.scrollHeight
    }
  }
}
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body { height: 100%; }
body {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif;
  background: #f5f7fa;
}
#app { height: 100%; }

.chat-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  max-width: 900px;
  margin: 0 auto;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}

.chat-header {
  padding: 16px 24px;
  background: #409EFF;
  color: #fff;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}
.chat-header h1 { font-size: 18px; font-weight: 600; }
.chat-header .version {
  font-size: 11px;
  padding: 2px 8px;
  background: rgba(255,255,255,0.2);
  border-radius: 10px;
}
.chat-header .model-info {
  margin-left: auto;
  font-size: 12px;
  opacity: 0.8;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  background: #fafbfc;
}
.message-row {
  display: flex;
  margin-bottom: 16px;
  align-items: flex-start;
}
.message-row.user { justify-content: flex-end; }
.message-row.assistant { justify-content: flex-start; }

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}
.message-row.user .message-avatar { background: #409EFF; color: #fff; margin-left: 10px; order: 2; }
.message-row.assistant .message-avatar { background: #67C23A; color: #fff; margin-right: 10px; }

.message-bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}
.message-row.user .message-bubble {
  background: #409EFF;
  color: #fff;
  border-bottom-right-radius: 4px;
  white-space: pre-wrap;
}
.message-row.assistant .message-bubble {
  background: #fff;
  color: #303133;
  border: 1px solid #ebeef5;
  border-bottom-left-radius: 4px;
}

/* Markdown 样式 */
.message-row.assistant .message-bubble p { margin: 0 0 8px 0; }
.message-row.assistant .message-bubble p:last-child { margin-bottom: 0; }
.message-row.assistant .message-bubble ul,
.message-row.assistant .message-bubble ol { margin: 4px 0 8px 0; padding-left: 20px; }
.message-row.assistant .message-bubble li { margin-bottom: 2px; }
.message-row.assistant .message-bubble code {
  background: #f0f2f5; padding: 1px 4px; border-radius: 3px;
  font-family: Consolas, Monaco, monospace; font-size: 13px; color: #e74c3c;
}
.message-row.assistant .message-bubble pre {
  background: #1e293b; color: #e2e8f0; padding: 12px; border-radius: 6px;
  overflow-x: auto; margin: 8px 0;
}
.message-row.assistant .message-bubble pre code {
  background: none; color: inherit; padding: 0; font-size: 13px;
}
.message-row.assistant .message-bubble strong { color: #1a1a1a; }
.message-row.assistant .message-bubble h1,
.message-row.assistant .message-bubble h2,
.message-row.assistant .message-bubble h3 { margin: 12px 0 6px 0; color: #1a1a1a; }
.message-row.assistant .message-bubble table { border-collapse: collapse; margin: 8px 0; width: 100%; }
.message-row.assistant .message-bubble th,
.message-row.assistant .message-bubble td { border: 1px solid #ebeef5; padding: 6px 10px; text-align: left; font-size: 13px; }
.message-row.assistant .message-bubble th { background: #f5f7fa; font-weight: 600; }

.chat-input {
  padding: 16px 24px;
  background: #fff;
  border-top: 1px solid #ebeef5;
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}
.chat-input .el-input { flex: 1; }
.chat-input .el-textarea__inner {
  border-radius: 8px;
  resize: none;
  font-size: 14px;
}

.typing-dots {
  display: inline-flex;
  gap: 4px;
  padding: 4px 0;
}
.typing-dots span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #c0c4cc;
  animation: bounce 1.4s infinite ease-in-out both;
}
.typing-dots span:nth-child(1) { animation-delay: -0.32s; }
.typing-dots span:nth-child(2) { animation-delay: -0.16s; }
@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
}
.empty-state i { font-size: 48px; color: #dcdfe6; margin-bottom: 16px; }
.empty-state p { font-size: 14px; }
</style>
