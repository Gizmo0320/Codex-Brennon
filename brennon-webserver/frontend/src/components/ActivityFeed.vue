<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { wsClient } from '../ws'

const events = ref<any[]>([])
const maxEvents = 50

function handleEvent(msg: any) {
  events.value.unshift({
    type: msg.type,
    data: msg.data,
    timestamp: msg.timestamp || Date.now()
  })
  if (events.value.length > maxEvents) {
    events.value.pop()
  }
}

function formatType(type: string): string {
  return type.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
}

function formatTime(ts: number): string {
  return new Date(ts).toLocaleTimeString()
}

onMounted(() => { wsClient.on('*', handleEvent) })
onUnmounted(() => { wsClient.off('*', handleEvent) })
</script>

<template>
  <div class="activity-feed card">
    <h3>Live Activity</h3>
    <div v-if="events.length === 0" class="empty">Waiting for events...</div>
    <div v-for="(event, i) in events" :key="i" class="event-item">
      <span class="event-time">{{ formatTime(event.timestamp) }}</span>
      <span class="event-type">{{ formatType(event.type) }}</span>
      <span class="event-detail" v-if="event.data?.name">{{ event.data.name }}</span>
      <span class="event-detail" v-else-if="event.data?.player">{{ event.data.player }}</span>
    </div>
  </div>
</template>

<style scoped>
.activity-feed { max-height: 400px; overflow-y: auto; }
.activity-feed h3 { margin-bottom: 12px; font-size: 16px; }
.empty { color: var(--text-secondary); font-size: 14px; }
.event-item {
  display: flex;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px solid #2d3748;
  font-size: 13px;
}
.event-time { color: var(--text-secondary); min-width: 70px; }
.event-type { color: var(--accent); font-weight: 500; }
.event-detail { color: var(--text-primary); }
</style>
