import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  // Public routes (no auth required)
  { path: '/', component: () => import('./views/PublicHomeView.vue') },
  { path: '/servers', component: () => import('./views/PublicServersView.vue') },
  { path: '/players', component: () => import('./views/PublicPlayersView.vue') },
  { path: '/players/:uuid', component: () => import('./views/PublicPlayerView.vue') },
  { path: '/bans', component: () => import('./views/PublicBansView.vue') },
  { path: '/leaderboard', component: () => import('./views/PublicLeaderboardView.vue') },

  // Auth routes
  { path: '/login', component: () => import('./views/LoginView.vue') },
  { path: '/player-login', component: () => import('./views/PlayerLoginView.vue') },

  // Admin routes
  { path: '/admin', component: () => import('./views/DashboardView.vue'), meta: { auth: true, admin: true } },
  { path: '/admin/players', component: () => import('./views/PlayersView.vue'), meta: { auth: true, admin: true } },
  { path: '/admin/players/:uuid', component: () => import('./views/PlayerDetailView.vue'), meta: { auth: true, admin: true } },
  { path: '/admin/ranks', component: () => import('./views/RanksView.vue'), meta: { auth: true, admin: true } },
  { path: '/admin/punishments', component: () => import('./views/PunishmentsView.vue'), meta: { auth: true, admin: true } },
  { path: '/admin/economy', component: () => import('./views/EconomyView.vue'), meta: { auth: true, admin: true } },
  { path: '/admin/tickets', component: () => import('./views/TicketsView.vue'), meta: { auth: true, admin: true } },
  { path: '/admin/tickets/:id', component: () => import('./views/TicketDetailView.vue'), meta: { auth: true, admin: true } },
  { path: '/admin/stats', component: () => import('./views/StatsView.vue'), meta: { auth: true, admin: true } },
  { path: '/admin/servers', component: () => import('./views/ServersView.vue'), meta: { auth: true, admin: true } },
  { path: '/admin/chat', component: () => import('./views/ChatView.vue'), meta: { auth: true, admin: true } },
  { path: '/admin/staff', component: () => import('./views/StaffView.vue'), meta: { auth: true, admin: true } },
  { path: '/admin/reports', component: () => import('./views/ReportsView.vue'), meta: { auth: true, admin: true } },
  { path: '/admin/appeals', component: () => import('./views/AppealsView.vue'), meta: { auth: true, admin: true } },
  { path: '/admin/pterodactyl', component: () => import('./views/PterodactylView.vue'), meta: { auth: true, admin: true } },

  // Player portal routes
  { path: '/portal', component: () => import('./views/PlayerPortalView.vue'), meta: { auth: true } },
  { path: '/portal/appeals', component: () => import('./views/PlayerAppealsView.vue'), meta: { auth: true } },
  { path: '/portal/tickets', component: () => import('./views/PlayerTicketsView.vue'), meta: { auth: true } },
]

export const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('brennon_token')
  const role = localStorage.getItem('brennon_role')

  if (to.meta.auth && !token) {
    // Redirect unauthenticated users: portal routes go to player login, others to admin login
    if (to.path.startsWith('/portal')) {
      next('/player-login')
    } else {
      next('/login')
    }
  } else if (to.path === '/login' && token && role !== 'player') {
    next('/admin')
  } else if (to.path === '/player-login' && token && role === 'player') {
    next('/portal')
  } else {
    next()
  }
})
