<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../api'
import ConfirmDialog from '../components/ConfirmDialog.vue'

const ranks = ref<any[]>([])
const lpActive = ref(false)
const showCreate = ref(false)
const showConfirm = ref(false)
const deleteTarget = ref('')
const form = ref({ id: '', displayName: '', prefix: '', weight: 0, permissions: '', isDefault: false, isStaff: false })
const editing = ref(false)
const editorLoading = ref(false)
const editorError = ref('')

onMounted(async () => {
  try {
    const modRes = await api.get('/modules')
    lpActive.value = modRes.data.luckperms === true
  } catch { /* ignore */ }
  await loadRanks()
})

async function loadRanks() {
  const res = await api.get('/ranks')
  ranks.value = res.data
}

function openCreate() {
  form.value = { id: '', displayName: '', prefix: '', weight: 0, permissions: '', isDefault: false, isStaff: false }
  editing.value = false
  showCreate.value = true
}

function openEdit(rank: any) {
  form.value = {
    id: rank.id,
    displayName: rank.displayName,
    prefix: rank.prefix,
    weight: rank.weight,
    permissions: rank.permissions.join('\n'),
    isDefault: rank.isDefault,
    isStaff: rank.isStaff
  }
  editing.value = true
  showCreate.value = true
}

async function save() {
  const data = {
    id: form.value.id,
    displayName: form.value.displayName,
    prefix: form.value.prefix,
    weight: form.value.weight,
    permissions: form.value.permissions.split('\n').map(s => s.trim()).filter(s => s),
    isDefault: form.value.isDefault,
    isStaff: form.value.isStaff
  }

  if (editing.value) {
    await api.put(`/ranks/${data.id}`, data)
  } else {
    await api.post('/ranks', data)
  }
  showCreate.value = false
  await loadRanks()
}

function confirmDelete(id: string) {
  deleteTarget.value = id
  showConfirm.value = true
}

async function doDelete() {
  await api.delete(`/ranks/${deleteTarget.value}`)
  showConfirm.value = false
  await loadRanks()
}

async function openLpEditor() {
  editorLoading.value = true
  editorError.value = ''
  try {
    const res = await api.post('/luckperms/editor')
    if (res.data.url) {
      window.open(res.data.url, '_blank')
    }
  } catch (e: any) {
    editorError.value = e.response?.data?.error || 'Failed to open editor'
  }
  editorLoading.value = false
}
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Ranks</h1>
      <div style="display: flex; gap: 8px; align-items: center;">
        <button v-if="lpActive" @click="openLpEditor" :disabled="editorLoading">
          {{ editorLoading ? 'Opening...' : 'Open LuckPerms Editor' }}
        </button>
        <button v-if="!lpActive" @click="openCreate">Create Rank</button>
      </div>
    </div>

    <div v-if="lpActive" class="lp-banner card">
      Permissions are managed by LuckPerms. Use the web editor to make changes.
      Ranks shown below are synced from LuckPerms and are read-only.
    </div>

    <div v-if="editorError" class="error-banner">{{ editorError }}</div>

    <div class="card">
      <table>
        <thead>
          <tr>
            <th>ID</th><th>Name</th><th>Prefix</th><th>Weight</th><th>Staff</th><th>Default</th>
            <th v-if="!lpActive">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in ranks" :key="r.id">
            <td>{{ r.id }}</td>
            <td>{{ r.displayName }}</td>
            <td>{{ r.prefix }}</td>
            <td>{{ r.weight }}</td>
            <td>{{ r.isStaff ? 'Yes' : 'No' }}</td>
            <td>{{ r.isDefault ? 'Yes' : 'No' }}</td>
            <td v-if="!lpActive">
              <button @click="openEdit(r)" style="margin-right: 4px;">Edit</button>
              <button class="danger" @click="confirmDelete(r.id)">Delete</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <teleport to="body">
      <div v-if="showCreate && !lpActive" class="overlay" @click.self="showCreate = false">
        <div class="dialog card" style="min-width: 420px;">
          <h3>{{ editing ? 'Edit Rank' : 'Create Rank' }}</h3>
          <form @submit.prevent="save">
            <div class="form-group" v-if="!editing">
              <label>ID</label>
              <input v-model="form.id" required />
            </div>
            <div class="form-group">
              <label>Display Name</label>
              <input v-model="form.displayName" />
            </div>
            <div class="form-group">
              <label>Prefix</label>
              <input v-model="form.prefix" />
            </div>
            <div class="form-group">
              <label>Weight</label>
              <input v-model.number="form.weight" type="number" />
            </div>
            <div class="form-group">
              <label>Permissions (one per line)</label>
              <textarea v-model="form.permissions" rows="4"></textarea>
            </div>
            <div style="display: flex; gap: 16px; margin-bottom: 16px;">
              <label><input type="checkbox" v-model="form.isDefault" /> Default</label>
              <label><input type="checkbox" v-model="form.isStaff" /> Staff</label>
            </div>
            <div style="display: flex; gap: 8px; justify-content: flex-end;">
              <button type="button" @click="showCreate = false" style="background: var(--bg-input);">Cancel</button>
              <button type="submit">Save</button>
            </div>
          </form>
        </div>
      </div>
    </teleport>

    <ConfirmDialog
      :show="showConfirm"
      title="Delete Rank"
      :message="`Are you sure you want to delete rank '${deleteTarget}'?`"
      @confirm="doDelete"
      @cancel="showConfirm = false"
    />
  </div>
</template>

<style scoped>
.overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.6);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.lp-banner {
  background: rgba(99, 102, 241, 0.15);
  border: 1px solid rgba(99, 102, 241, 0.3);
  color: var(--text-secondary);
  font-size: 14px;
  padding: 12px 16px;
}
.error-banner {
  background: rgba(239, 68, 68, 0.15);
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: var(--danger);
  font-size: 14px;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 16px;
}
</style>
