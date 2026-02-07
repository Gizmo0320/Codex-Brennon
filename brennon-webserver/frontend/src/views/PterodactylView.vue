<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../api'

const servers = ref<any[]>([])
const selectedServer = ref<string | null>(null)
const serverStatus = ref<any>(null)
const commandInput = ref('')
const loading = ref(false)

// File browser state
const showFiles = ref(false)
const currentDir = ref('/')
const files = ref<any[]>([])
const editingFile = ref<string | null>(null)
const fileContent = ref('')
const fileSaving = ref(false)

onMounted(loadServers)

async function loadServers() {
  try {
    const res = await api.get('/pterodactyl/servers')
    servers.value = res.data
  } catch (e: any) {
    alert(e.response?.data?.error || 'Failed to load servers')
  }
}

async function selectServer(name: string) {
  selectedServer.value = name
  showFiles.value = false
  editingFile.value = null
  try {
    const res = await api.get(`/pterodactyl/servers/${name}/status`)
    serverStatus.value = res.data
  } catch {
    serverStatus.value = null
  }
}

async function sendPower(action: string) {
  if (!selectedServer.value) return
  loading.value = true
  try {
    await api.post(`/pterodactyl/servers/${selectedServer.value}/power`, { action })
    setTimeout(() => selectServer(selectedServer.value!), 2000)
  } catch (e: any) {
    alert(e.response?.data?.error || 'Power action failed')
  }
  loading.value = false
}

async function sendCommand() {
  if (!selectedServer.value || !commandInput.value.trim()) return
  loading.value = true
  try {
    await api.post(`/pterodactyl/servers/${selectedServer.value}/command`, {
      command: commandInput.value
    })
    commandInput.value = ''
  } catch (e: any) {
    alert(e.response?.data?.error || 'Command failed')
  }
  loading.value = false
}

async function openFiles() {
  if (!selectedServer.value) return
  showFiles.value = true
  editingFile.value = null
  currentDir.value = '/'
  await loadFiles()
}

async function loadFiles() {
  if (!selectedServer.value) return
  try {
    const res = await api.get(`/pterodactyl/servers/${selectedServer.value}/files`, {
      params: { dir: currentDir.value }
    })
    files.value = res.data
  } catch (e: any) {
    alert(e.response?.data?.error || 'Failed to list files')
  }
}

function navigateDir(name: string) {
  if (currentDir.value === '/') {
    currentDir.value = '/' + name
  } else {
    currentDir.value += '/' + name
  }
  editingFile.value = null
  loadFiles()
}

function navigateUp() {
  const parts = currentDir.value.split('/')
  parts.pop()
  currentDir.value = parts.join('/') || '/'
  editingFile.value = null
  loadFiles()
}

async function openFile(name: string) {
  if (!selectedServer.value) return
  const filePath = currentDir.value === '/' ? name : currentDir.value + '/' + name
  try {
    const res = await api.get(`/pterodactyl/servers/${selectedServer.value}/files/content`, {
      params: { file: filePath }
    })
    editingFile.value = filePath
    fileContent.value = res.data.content
  } catch (e: any) {
    alert(e.response?.data?.error || 'Failed to read file')
  }
}

async function saveFile() {
  if (!selectedServer.value || !editingFile.value) return
  fileSaving.value = true
  try {
    await api.put(`/pterodactyl/servers/${selectedServer.value}/files/content`, {
      file: editingFile.value,
      content: fileContent.value
    })
    alert('File saved')
  } catch (e: any) {
    alert(e.response?.data?.error || 'Failed to save file')
  }
  fileSaving.value = false
}

function stateColor(state: string) {
  if (state === 'running') return 'var(--success)'
  if (state === 'starting') return 'var(--warning)'
  if (state === 'stopping') return 'var(--warning)'
  return 'var(--danger)'
}

function formatBytes(mb: number) {
  if (mb >= 1024) return (mb / 1024).toFixed(1) + ' GB'
  return mb + ' MB'
}
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Server Management</h1>
    </div>

    <!-- Server List -->
    <div class="server-grid">
      <div v-for="s in servers" :key="s.name" class="card server-card"
        :class="{ active: selectedServer === s.name }"
        @click="selectServer(s.name)">
        <div class="server-header">
          <strong>{{ s.name }}</strong>
          <span class="state-dot" :style="{ background: stateColor(s.state || 'offline') }"></span>
        </div>
        <div class="server-info" v-if="s.state && s.state !== 'unknown'">
          <span>CPU: {{ s.cpu?.toFixed(1) || 0 }}%</span>
          <span>RAM: {{ formatBytes(s.memoryMb || 0) }} / {{ formatBytes(s.memoryLimitMb || 0) }}</span>
        </div>
        <div class="server-info" v-else>
          <span style="color: var(--text-secondary);">{{ s.error || s.state || 'Unknown' }}</span>
        </div>
      </div>
    </div>

    <!-- Selected Server Controls -->
    <template v-if="selectedServer">
      <div class="card">
        <h3 style="margin-bottom: 12px;">{{ selectedServer }}</h3>

        <div v-if="serverStatus" class="status-grid">
          <div class="status-item">
            <span class="status-label">State</span>
            <span :style="{ color: stateColor(serverStatus.state) }">{{ serverStatus.state }}</span>
          </div>
          <div class="status-item">
            <span class="status-label">CPU</span>
            <span>{{ serverStatus.cpu?.toFixed(1) }}%</span>
          </div>
          <div class="status-item">
            <span class="status-label">Memory</span>
            <span>{{ formatBytes(serverStatus.memoryMb) }} / {{ formatBytes(serverStatus.memoryLimitMb) }}</span>
          </div>
          <div class="status-item">
            <span class="status-label">Disk</span>
            <span>{{ formatBytes(serverStatus.diskMb) }}</span>
          </div>
          <div class="status-item">
            <span class="status-label">Network</span>
            <span>{{ formatBytes(serverStatus.networkRxMb || 0) }} in / {{ formatBytes(serverStatus.networkTxMb || 0) }} out</span>
          </div>
        </div>

        <div style="display: flex; gap: 8px; margin-top: 16px;">
          <button class="success" @click="sendPower('start')" :disabled="loading">Start</button>
          <button @click="sendPower('restart')" :disabled="loading">Restart</button>
          <button class="danger" @click="sendPower('stop')" :disabled="loading">Stop</button>
          <button class="danger" @click="sendPower('kill')" :disabled="loading"
            style="background: #991b1b;">Kill</button>
          <button style="margin-left: auto; background: var(--bg-input);" @click="openFiles">Files</button>
        </div>
      </div>

      <!-- Console Command -->
      <div class="card">
        <h3 style="margin-bottom: 8px;">Send Command</h3>
        <div style="display: flex; gap: 8px;">
          <input v-model="commandInput" placeholder="say Hello World" @keyup.enter="sendCommand" />
          <button @click="sendCommand" :disabled="loading">Send</button>
        </div>
      </div>

      <!-- File Browser -->
      <div v-if="showFiles" class="card">
        <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 12px;">
          <h3>Files</h3>
          <span style="color: var(--text-secondary); font-family: monospace; font-size: 13px;">
            {{ currentDir }}
          </span>
          <button v-if="currentDir !== '/'" @click="navigateUp"
            style="margin-left: auto; background: var(--bg-input); font-size: 12px; padding: 4px 10px;">
            Up
          </button>
        </div>

        <!-- File Editor -->
        <div v-if="editingFile">
          <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px;">
            <span style="font-family: monospace; font-size: 13px; color: var(--accent);">{{ editingFile }}</span>
            <button @click="editingFile = null"
              style="margin-left: auto; background: var(--bg-input); font-size: 12px; padding: 4px 10px;">
              Close
            </button>
            <button @click="saveFile" :disabled="fileSaving" class="success"
              style="font-size: 12px; padding: 4px 10px;">
              {{ fileSaving ? 'Saving...' : 'Save' }}
            </button>
          </div>
          <textarea v-model="fileContent" rows="20"
            style="font-family: monospace; font-size: 13px; resize: vertical;"></textarea>
        </div>

        <!-- File List -->
        <table v-else>
          <thead>
            <tr>
              <th>Name</th>
              <th>Size</th>
              <th>Modified</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="f in files" :key="f.name" style="cursor: pointer;"
              @click="f.isFile ? (f.isEditable ? openFile(f.name) : null) : navigateDir(f.name)">
              <td>
                <span style="margin-right: 8px;">{{ f.isFile ? '~' : '>' }}</span>
                {{ f.name }}
              </td>
              <td>{{ f.isFile ? (f.size > 1024 ? (f.size / 1024).toFixed(1) + ' KB' : f.size + ' B') : '-' }}</td>
              <td>{{ f.modifiedAt || '-' }}</td>
            </tr>
            <tr v-if="files.length === 0">
              <td colspan="3" style="text-align: center; color: var(--text-secondary);">Empty directory</td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </div>
</template>

<style scoped>
.server-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.server-card {
  cursor: pointer;
  transition: border 0.15s;
  border: 2px solid transparent;
}
.server-card:hover { border-color: rgba(99, 102, 241, 0.3); }
.server-card.active { border-color: var(--accent); }
.server-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.state-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}
.server-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 13px;
  color: var(--text-secondary);
}
.status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}
.status-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.status-label {
  color: var(--text-secondary);
  font-size: 11px;
  text-transform: uppercase;
}
</style>
