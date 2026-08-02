<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import request from '../api/request'

/**
 * 考生登录:账号密码 + 短信验证码(未注册自动注册)。
 */
const router = useRouter()
const auth = useAuthStore()

const mode = ref<'password' | 'sms'>('password')
const username = ref('')
const password = ref('')
const phone = ref('')
const code = ref('')
const error = ref('')
const smsSent = ref(false)
const countdown = ref(0)

async function submit() {
  error.value = ''
  try {
    if (mode.value === 'password') {
      await auth.login(username.value, password.value)
    } else {
      await auth.smsLogin(phone.value, code.value)
    }
    router.push('/')
  } catch (e) {
    error.value = (e as Error).message
  }
}

async function sendCode() {
  error.value = ''
  try {
    await request.get('/auth/sms/code', { params: { phone: phone.value } })
    smsSent.value = true
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e) {
    error.value = (e as Error).message
  }
}
</script>

<template>
  <div class="login-wrap">
    <div class="ef-card login-card">
      <h2 class="login-title">考生登录</h2>
      <div class="login-tabs">
        <button :class="['tab', { active: mode === 'password' }]" @click="mode = 'password'">账号密码</button>
        <button :class="['tab', { active: mode === 'sms' }]" @click="mode = 'sms'">短信验证码</button>
      </div>
      <form @submit.prevent="submit">
        <template v-if="mode === 'password'">
          <div style="margin-bottom: 16px">
            <label class="ef-form-label required">账号</label>
            <input v-model="username" class="ef-input" required />
          </div>
          <div style="margin-bottom: 20px">
            <label class="ef-form-label required">密码</label>
            <input v-model="password" type="password" class="ef-input" required />
          </div>
        </template>
        <template v-else>
          <div style="margin-bottom: 16px">
            <label class="ef-form-label required">手机号</label>
            <div style="display: flex; gap: 8px">
              <input v-model="phone" class="ef-input" placeholder="11 位手机号" required />
              <button type="button" class="ef-btn ef-btn-outline" style="white-space: nowrap" :disabled="countdown > 0" @click="sendCode">
                {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
              </button>
            </div>
          </div>
          <div style="margin-bottom: 20px">
            <label class="ef-form-label required">验证码</label>
            <input v-model="code" class="ef-input" required />
          </div>
        </template>
        <p v-if="error" class="login-error">{{ error }}</p>
        <button type="submit" class="ef-btn ef-btn-primary" style="width: 100%">登 录</button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.login-wrap {
  display: flex;
  justify-content: center;
  padding: 48px 0;
}

.login-card {
  width: 440px;
  border-top: 3px solid var(--ef-primary);
}

.login-title {
  font-family: var(--ef-font-heiti);
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 16px;
  padding-left: 12px;
  border-left: 4px solid var(--ef-primary);
}

.login-tabs {
  display: flex;
  border-bottom: 1px solid var(--ef-hairline);
  margin-bottom: 20px;
}

.tab {
  flex: 1;
  padding: 10px 0;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  font-family: var(--ef-font-heiti);
  font-size: 15px;
  cursor: pointer;
  color: var(--ef-ink-muted);
}

.tab.active {
  color: var(--ef-primary);
  border-bottom-color: var(--ef-primary);
  font-weight: 700;
}

.login-error {
  color: var(--ef-error);
  font-size: 13px;
  margin-bottom: 12px;
}
</style>
