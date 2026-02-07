import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  // Admin login
  { path: '/login', component: () => import('./views/LoginView.vue') },

  // Player login
  { path: '/player-login', component: () => import('./views/PlayerLoginView.vue') },

  // Admin routes
  { path: '/', component: () => import('./views/DashboardView.vue'), meta: { auth: true } },
  { path: '/players', component: () => import('./views/PlayersView.vue'), meta: { auth: true } },
  { path: '/players/:uuid', component: () => import('./views/PlayerDetailView.vue'), meta: { auth: true } },
  { path: '/ranks', component: () => import('./views/RanksView.vue'), meta: { auth: true } },
  { path: '/punishments', component: () => import('./views/PunishmentsView.vue'), meta: { auth: true } },
  { path: '/economy', component: () => import('./views/EconomyView.vue'), meta: { auth: true } },
  { path: '/tickets', component: () => import('./views/TicketsView.vue'), meta: { auth: true } },
  { path: '/tickets/:id', component: () => import('./views/TicketDetailView.vue'), meta: { auth: true } },
  { path: '/stats', component: () => import('./views/StatsView.vue'), meta: { auth: true } },
  { path: '/servers', component: () => import('./views/ServersView.vue'), meta: { auth: true } },
  { path: '/chat', component: () => import('./views/ChatView.vue'), meta: { auth: true } },
  { path: '/staff', component: () => import('./views/StaffView.vue'), meta: { auth: true } },
  { path: '/reports', component: () => import('./views/ReportsView.vue'), meta: { auth: true } },
  { path: '/appeals', component: () => import('./views/AppealsView.vue'), meta: { auth: true } },
  { path: '/pterodactyl', component: () => import('./views/PterodactylView.vue'), meta: { auth: true } },

  // Player portal routes
  { path: '/portal', component: () => import('./views/PlayerPortalView.vue'), meta: { auth: true } },
  { path: '/portal/appeals', component: () => import('./views/PlayerAppealsView.vue'), meta: { auth: true } },
  { path: '/portal/tickets', component: () => import('./views/PlayerTicketsView.vue'), meta: { auth: true } }
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
    next('/')
  } else if (to.path === '/player-login' && token && role === 'player') {
    next('/portal')
  } else {
    next()
  }
})
