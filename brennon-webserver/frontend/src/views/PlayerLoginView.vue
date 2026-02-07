<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const code = ref('')
const error = ref('')
const loading = ref(false)

async function verify() {
  error.value = ''
  if (code.value.trim().length !== 6) {
    error.value = 'Code must be 6 digits'
    return
  }
  loading.value = true
  try {
    const res = await api.post('/player-auth/verify', { code: code.value.trim() })
    localStorage.setItem('brennon_token', res.data.token)
    localStorage.setItem('brennon_role', 'player')
    localStorage.setItem('brennon_player_name', res.data.name)
    localStorage.setItem('brennon_player_uuid', res.data.uuid)
    router.push('/portal')
    window.location.reload()
  } catch {
    error.value = 'Invalid or expired code. Run /link in-game to get a new one.'
  }
  loading.value = false
}
</script>

<template>
  <div class="login-page">
    <div class="login-card card">
      <h1>Brennon</h1>
      <p>Player Portal</p>
      <div class="instructions">
        <span class="step">1</span> Join the server and run <code>/link</code>
        <br />
        <span class="step">2</span> Enter the 6-digit code below
      </div>
      <form @submit.prevent="verify">
        <div class="form-group">
          <label>Link Code</label>
          <input v-model="code" type="text" maxlength="6" placeholder="123456"
            autofocus style="text-align: center; font-size: 24px; letter-spacing: 8px;" />
        </div>
        <div v-if="error" class="error">{{ error }}</div>
        <button type="submit" :disabled="loading" style="width: 100%; margin-top: 8px;">
          {{ loading ? 'Verifying...' : 'Link Account' }}
        </button>
      </form>
      <div class="switch-link">
        <router-link to="/login">Admin Login</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
}
.login-card {
  width: 400px;
  text-align: center;
}
.login-card h1 { font-size: 28px; margin-bottom: 4px; }
.login-card p { color: var(--text-secondary); margin-bottom: 20px; }
.error { color: var(--danger); font-size: 13px; margin-bottom: 8px; }
.instructions {
  text-align: left;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 2;
  margin-bottom: 20px;
  padding: 12px;
  background: var(--bg-input);
  border-radius: 6px;
}
.instructions code {
  background: var(--bg-primary);
  padding: 2px 6px;
  border-radius: 4px;
  color: var(--accent);
}
.step {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--accent);
  color: white;
  font-size: 11px;
  font-weight: 700;
  margin-right: 6px;
}
.switch-link {
  margin-top: 16px;
  font-size: 13px;
  color: var(--text-secondary);
}
</style>
