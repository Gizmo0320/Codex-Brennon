type EventHandler = (data: any) => void

class WebSocketClient {
  private ws: WebSocket | null = null
  private handlers: Map<string, EventHandler[]> = new Map()
  private reconnectTimer: number | null = null
  private url: string = ''

  connect() {
    const token = localStorage.getItem('brennon_token')
    if (!token) return

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    this.url = `${protocol}//${window.location.host}/ws?token=${token}`

    this.ws = new WebSocket(this.url)

    this.ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data)
        const handlers = this.handlers.get(msg.type) || []
        handlers.forEach(h => h(msg.data))

        // Also fire a wildcard handler for activity feeds
        const wildcardHandlers = this.handlers.get('*') || []
        wildcardHandlers.forEach(h => h(msg))
      } catch (e) {
        console.error('WebSocket message parse error:', e)
      }
    }

    this.ws.onclose = () => {
      this.scheduleReconnect()
    }

    this.ws.onerror = () => {
      this.ws?.close()
    }
  }

  private scheduleReconnect() {
    if (this.reconnectTimer) return
    this.reconnectTimer = window.setTimeout(() => {
      this.reconnectTimer = null
      if (localStorage.getItem('brennon_token')) {
        this.connect()
      }
    }, 3000)
  }

  on(eventType: string, handler: EventHandler) {
    if (!this.handlers.has(eventType)) {
      this.handlers.set(eventType, [])
    }
    this.handlers.get(eventType)!.push(handler)
  }

  off(eventType: string, handler: EventHandler) {
    const handlers = this.handlers.get(eventType)
    if (handlers) {
      const idx = handlers.indexOf(handler)
      if (idx >= 0) handlers.splice(idx, 1)
    }
  }

  subscribe(events: string[]) {
    this.send({ type: 'subscribe', data: { events } })
  }

  send(data: any) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(data))
    }
  }

  disconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    this.ws?.close()
    this.ws = null
  }
}

export const wsClient = new WebSocketClient()
