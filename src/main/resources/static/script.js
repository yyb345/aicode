// DOM元素引用
const messagesContainer = document.getElementById('messages');
const questionInput = document.getElementById('questionInput');
const sendButton = document.getElementById('sendButton');
const typingIndicator = document.getElementById('typingIndicator');
const serviceSelect = document.getElementById('serviceSelect');

// 当前选择的服务
let currentService = 'openai';

// 处理键盘事件
function handleKeyPress(event) {
    if (event.key === 'Enter') {
        sendQuestion();
    }
}

// 切换服务
function changeService() {
    currentService = serviceSelect.value;
    console.log('切换到服务:', currentService);
    
    // 添加系统消息显示当前使用的服务
    addMessage(`已切换到 ${currentService === 'openai' ? 'OpenAI' : 'Spring AI Alibaba'} 服务`, false);
}

// 检查是否使用了工具
function checkToolUsage(response) {
    try {
        const data = JSON.parse(response);
        if (data.tool_used === 'weather') {
            return true;
        }
    } catch (e) {
        // 不是JSON响应，继续正常处理
    }
    return false;
}

// 显示工具调用指示器
function showToolIndicator(toolName) {
    const toolDiv = document.createElement('div');
    toolDiv.className = 'message tool-message';
    toolDiv.innerHTML = `🔧 正在使用 ${toolName} 工具...`;
    messagesContainer.appendChild(toolDiv);
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
}

// 添加消息到聊天容器
function addMessage(content, isUser = false, isError = false) {
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${isUser ? 'user-message' : 'ai-message'}${isError ? ' error-message' : ''}`;
    messageDiv.textContent = content;
    messagesContainer.appendChild(messageDiv);
    // 使用 setTimeout 确保DOM更新后再滚动
    setTimeout(() => {
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
        adjustContainerHeight();
    }, 0);
}

// 显示打字指示器
function showTypingIndicator() {
    typingIndicator.classList.add('show');
    setTimeout(() => {
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }, 0);
}

// 隐藏打字指示器
function hideTypingIndicator() {
    typingIndicator.classList.remove('show');
}

// 设置加载状态
function setLoading(loading) {
    sendButton.disabled = loading;
    questionInput.disabled = loading;
    if (loading) {
        sendButton.textContent = '发送中...';
        showTypingIndicator();
    } else {
        sendButton.textContent = '发送';
        hideTypingIndicator();
    }
}

// 发送问题到后端
async function sendQuestion() {
    const question = questionInput.value.trim();
    if (!question) {
        return;
    }

    // 添加用户消息
    addMessage(question, true);
    questionInput.value = '';
    setLoading(true);

    try {
        // 根据选择的服务确定API端点
        const apiEndpoint = currentService === 'openai' ? '/api/qa/stream' : '/api/qa/spring-ai/stream';
        
        const response = await fetch(apiEndpoint, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ question: question })
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let aiResponse = '';
        let buffer = '';

        // 创建AI回复消息容器
        const aiMessageDiv = document.createElement('div');
        aiMessageDiv.className = 'message ai-message';
        messagesContainer.appendChild(aiMessageDiv);

        while (true) {
            const { done, value } = await reader.read();
            if (done) break;

            const chunk = decoder.decode(value, { stream: true });
            buffer += chunk;
            
            // 按行分割处理
            const lines = buffer.split('\n');
            // 保留最后一个可能不完整的行
            buffer = lines.pop() || '';
            
            for (const line of lines) {
                console.log('Received line:', line);
                if (line.startsWith('data: ')) {
                    const content = line.substring(6); // 跳过 "data: "
                    if (content.trim()) {
                        aiResponse += content;
                        aiMessageDiv.textContent = aiResponse;
                        // 使用 requestAnimationFrame 确保平滑滚动和高度调整
                        requestAnimationFrame(() => {
                            messagesContainer.scrollTop = messagesContainer.scrollHeight;
                            adjustContainerHeight();
                        });
                    }
                } else if (line.startsWith('data:')) {
                    const content = line.substring(5); // 跳过 "data:"
                    if (content.trim()) {
                        aiResponse += content;
                        aiMessageDiv.textContent = aiResponse;
                        // 使用 requestAnimationFrame 确保平滑滚动和高度调整
                        requestAnimationFrame(() => {
                            messagesContainer.scrollTop = messagesContainer.scrollHeight;
                            adjustContainerHeight();
                        });
                    }
                }
            }
        }
        
        // 处理最后剩余的内容
        if (buffer.trim()) {
            console.log('Processing remaining buffer:', buffer);
            if (buffer.startsWith('data: ')) {
                const content = buffer.substring(6);
                if (content.trim()) {
                    aiResponse += content;
                    aiMessageDiv.textContent = aiResponse;
                }
            } else if (buffer.startsWith('data:')) {
                const content = buffer.substring(5);
                if (content.trim()) {
                    aiResponse += content;
                    aiMessageDiv.textContent = aiResponse;
                }
            }
        }

        // 流式响应完成后，最终调整高度
        setTimeout(() => {
            adjustContainerHeight();
        }, 100);

    } catch (error) {
        console.error('Error:', error);
        addMessage(`抱歉，发生了错误：${error.message}`, false, true);
    } finally {
        setLoading(false);
    }
}

// 调整容器高度以适应内容
function adjustContainerHeight() {
    const container = document.querySelector('.container');
    const messages = document.getElementById('messages');
    const header = document.querySelector('.header');
    const chatContainer = document.querySelector('.chat-container');
    const inputContainer = document.querySelector('.input-container');
    
    // 计算所需的最小高度
    const headerHeight = header.offsetHeight;
    const messagesHeight = messages.scrollHeight;
    const inputHeight = inputContainer.offsetHeight;
    const chatPadding = 40; // chat-container的padding (20px * 2)
    
    const minRequiredHeight = headerHeight + messagesHeight + inputHeight + chatPadding;
    
    // 设置容器高度，但不超过视口高度的95%
    const maxHeight = window.innerHeight * 0.95;
    const newHeight = Math.min(Math.max(minRequiredHeight, 400), maxHeight);
    
    // 只有当新高度大于当前高度时才更新，避免收缩
    if (newHeight > container.offsetHeight) {
        container.style.height = newHeight + 'px';
    }
    
    // 确保消息容器能够滚动
    if (messagesHeight > (newHeight - headerHeight - inputHeight - chatPadding)) {
        messages.style.maxHeight = (newHeight - headerHeight - inputHeight - chatPadding) + 'px';
    } else {
        messages.style.maxHeight = 'none';
    }
}

// 页面加载完成后聚焦输入框并调整高度
window.addEventListener('load', () => {
    questionInput.focus();
    adjustContainerHeight();
});

// 监听窗口大小变化，动态调整容器高度
window.addEventListener('resize', () => {
    adjustContainerHeight();
});

// 监听消息容器内容变化，自动调整高度
const observer = new MutationObserver(() => {
    adjustContainerHeight();
});

// 开始观察消息容器的变化
observer.observe(messagesContainer, {
    childList: true,
    subtree: true,
    characterData: true
});
