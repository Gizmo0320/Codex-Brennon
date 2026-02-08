<script setup lang="ts">
import { computed } from 'vue'

const isLoggedIn = computed(() => !!localStorage.getItem('brennon_token'))
const role = computed(() => localStorage.getItem('brennon_role'))

const navItems = [
  { path: '/', label: 'Home' },
  { path: '/servers', label: 'Servers' },
  { path: '/players', label: 'Players' },
  { path: '/bans', label: 'Bans' },
  { path: '/leaderboard', label: 'Leaderboard' },
]
</script>

<template>
  <nav class="public-navbar">
    <div class="navbar-inner">
      <router-link to="/" class="navbar-brand">Brennon</router-link>

      <div class="navbar-links">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-link"
          active-class="active"
          :exact="item.path === '/'"
        >
          {{ item.label }}
        </router-link>
      </div>

      <div class="navbar-actions">
        <router-link v-if="isLoggedIn && role === 'player'" to="/portal" class="nav-btn portal-btn">
          Portal
        </router-link>
        <router-link v-else-if="isLoggedIn" to="/admin" class="nav-btn admin-btn">
          Dashboard
        </router-link>
        <router-link v-else to="/login" class="nav-btn login-btn">
          Login
        </router-link>
      </div>
    </div>
  </nav>
</template>

<style scoped>
.public-navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: var(--navbar-height);
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border);
  z-index: 100;
}

.navbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 24px;
  gap: 32px;
}

.navbar-brand {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  text-decoration: none;
}

.navbar-links {
  display: flex;
  gap: 4px;
  flex: 1;
}

.nav-link {
  padding: 8px 14px;
  border-radius: 6px;
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 14px;
  transition: all 0.15s;
}

.nav-link:hover {
  background: rgba(99, 102, 241, 0.1);
  color: var(--text-primary);
}

.nav-link.active {
  background: var(--accent);
  color: white;
}

.navbar-actions {
  display: flex;
  gap: 8px;
}

.nav-btn {
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  text-decoration: none;
  font-weight: 500;
}

.login-btn {
  background: var(--accent);
  color: white;
}

.login-btn:hover {
  background: var(--accent-hover);
  color: white;
}

.admin-btn, .portal-btn {
  background: rgba(99, 102, 241, 0.15);
  color: var(--accent);
}

.admin-btn:hover, .portal-btn:hover {
  background: rgba(99, 102, 241, 0.25);
  color: var(--accent-hover);
}
</style>
