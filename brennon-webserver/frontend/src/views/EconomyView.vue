<script setup lang="ts">
import { ref } from 'vue'
import api from '../api'

const searchName = ref('')
const playerUuid = ref('')
const balance = ref<number | null>(null)
const actionType = ref('deposit')
const amount = ref(0)
const transferTo = ref('')
const message = ref('')

async function search() {
  message.value = ''
  try {
    const playerRes = await api.get(`/players/name/${searchName.value}`)
    playerUuid.value = playerRes.data.uuid
    const res = await api.get(`/economy/${playerRes.data.uuid}`)
    balance.value = res.data.balance
  } catch {
    balance.value = null
    message.value = 'Player not found'
  }
}

async function execute() {
  message.value = ''
  if (!playerUuid.value) return
  try {
    if (actionType.value === 'transfer') {
      const toPlayer = await api.get(`/players/name/${transferTo.value}`)
      await api.post('/economy/transfer', { from: playerUuid.value, to: toPlayer.data.uuid, amount: amount.value })
    } else {
      await api.post(`/economy/${actionType.value}`, { uuid: playerUuid.value, amount: amount.value })
    }
    // Refresh balance
    const res = await api.get(`/economy/${playerUuid.value}`)
    balance.value = res.data.balance
    message.value = 'Success!'
  } catch (e: any) {
    message.value = e.response?.data?.error || 'Action failed'
  }
}
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Economy</h1>
    </div>

    <div class="card" style="margin-bottom: 16px;">
      <div style="display: flex; gap: 8px;">
        <input v-model="searchName" placeholder="Player name..." @keyup.enter="search" />
        <button @click="search">Lookup</button>
      </div>
      <div v-if="balance !== null" style="margin-top: 12px; font-size: 24px; font-weight: 700;">
        Balance: ${{ balance.toFixed(2) }}
      </div>
    </div>

    <div class="card" v-if="playerUuid">
      <h3 style="margin-bottom: 12px;">Actions</h3>
      <div class="form-group">
        <label>Action</label>
        <select v-model="actionType">
          <option value="deposit">Deposit</option>
          <option value="withdraw">Withdraw</option>
          <option value="set">Set</option>
          <option value="transfer">Transfer</option>
        </select>
      </div>
      <div class="form-group">
        <label>Amount</label>
        <input v-model.number="amount" type="number" step="0.01" />
      </div>
      <div class="form-group" v-if="actionType === 'transfer'">
        <label>Transfer To (name)</label>
        <input v-model="transferTo" />
      </div>
      <button @click="execute">Execute</button>
      <span v-if="message" style="margin-left: 12px;" :style="{ color: message === 'Success!' ? 'var(--success)' : 'var(--danger)' }">
        {{ message }}
      </span>
    </div>
  </div>
</template>
