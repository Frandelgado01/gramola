import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { SpotifyService } from '../services/spotify.service';
import { WebSocketService } from '../services/websocket.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <nav class="topbar">
      <div class="topbar-left">
        <div class="brand">
          <div class="brand-icon">♪</div>
          <div class="brand-text">
            <div class="brand-title">Gramola</div>
            <div class="brand-subtitle">Bar <strong>{{ usuario?.bar || '—' }}</strong></div>
          </div>
        </div>

        <div class="pills">
          <span class="pill" [class.ok]="!!spotifyToken" [class.warn]="!spotifyToken">
            Spotify: {{ spotifyToken ? 'Conectado' : 'Desconectado' }}
          </span>
          <span class="pill" *ngIf="spotifyToken">
            Cola: {{ colaGramola.length || 0 }}
          </span>
        </div>
      </div>

      <div class="topbar-actions">
        <button class="btn-ghost" (click)="cargarColaInicial()" [disabled]="!spotifyToken" aria-label="Actualizar cola">
          Actualizar cola
        </button>
        <button class="btn-ghost" (click)="cargarDispositivos()" [disabled]="!spotifyToken" aria-label="Actualizar dispositivos">
          Actualizar dispositivos
        </button>
        <button class="btn-outline" (click)="logout()">Cerrar sesión</button>
      </div>
    </nav>

    <main class="page">
      <section class="card hero" *ngIf="!spotifyToken">
        <div class="hero-inner">
          <div class="hero-copy">
            <h1 class="hero-title">Conecta Spotify para empezar</h1>
            <p class="hero-subtitle">
              Gestiona la cola en vivo, elige el dispositivo y añade canciones con pago.
            </p>
            <div class="hero-actions">
              <button (click)="conectarSpotify()" class="btn-secondary">Conectar con Spotify</button>
              <button class="btn-ghost" (click)="cargarColaInicial()" [disabled]="true">Ver cola</button>
            </div>
            <div class="alert alert--info" style="margin-top: 14px;">
              Tip: abre Spotify en tu móvil/PC y deja algo reproduciéndose para detectar el dispositivo activo.
            </div>
          </div>
          <div class="hero-art" aria-hidden="true">
            <div class="blob blob-a"></div>
            <div class="blob blob-b"></div>
            <div class="hero-badge">En vivo</div>
          </div>
        </div>
      </section>

      <section class="grid" *ngIf="spotifyToken">
        <div class="card section">
          <header class="section-head">
            <div>
              <h2 class="section-title">Cola en vivo</h2>
              <p class="section-subtitle">La primera es la que toca (o está sonando).</p>
            </div>
            <button class="btn-ghost" (click)="cargarColaInicial()">Refrescar</button>
          </header>

          <div *ngIf="colaGramola.length > 0" class="queue-list">
            <div *ngFor="let song of colaGramola; let i = index"
                 class="queue-item swing-in-top-fwd"
                 [class.now-playing]="isNowPlaying(song)"
                 [class.up-next]="isUpNext(i)">
              <div class="queue-avatar" aria-hidden="true">🎵</div>
              <div class="queue-meta">
                <div class="queue-title">{{ song.title }}</div>
                <div class="queue-artist">{{ song.artist }}</div>
              </div>
              <span class="queue-badge" *ngIf="isNowPlaying(song)">Sonando</span>
              <span class="queue-badge" *ngIf="!isNowPlaying(song) && isUpNext(i)">Siguiente</span>
            </div>
          </div>

          <div *ngIf="colaGramola.length === 0" class="empty">
            <div class="empty-icon" aria-hidden="true">🎧</div>
            <div class="empty-title">La cola está vacía</div>
            <div class="empty-subtitle">Busca una canción y pulsa “Poner (0.50€)”.</div>
          </div>
        </div>

        <div class="card section">
          <header class="section-head">
            <div>
              <h2 class="section-title">Dispositivos</h2>
              <p class="section-subtitle">Elige el que está activo (🔊 Sonando).</p>
            </div>
            <button class="btn-ghost" (click)="cargarDispositivos()">Refrescar</button>
          </header>

          <div class="device-list" *ngIf="devices.length > 0">
            <div *ngFor="let device of devices" class="device-item" [class.active]="device.is_active">
              <div class="device-left">
                <div class="device-icon" aria-hidden="true">{{ device.type === 'Smartphone' ? '📱' : '💻' }}</div>
                <div class="device-meta">
                  <div class="device-name">{{ device.name }}</div>
                  <div class="device-type muted">{{ device.type }}</div>
                </div>
              </div>
              <div class="device-right">
                <span class="pill" *ngIf="device.is_active">🔊 Sonando</span>
              </div>
            </div>
          </div>

          <div class="empty" *ngIf="devices.length === 0">
            <div class="empty-icon" aria-hidden="true">📡</div>
            <div class="empty-title">No se detectan dispositivos</div>
            <div class="empty-subtitle">Abre Spotify en un dispositivo y vuelve a refrescar.</div>
          </div>
        </div>
      </section>

      <section class="card section" *ngIf="spotifyToken">
        <header class="section-head">
          <div>
            <h2 class="section-title">Buscar canciones</h2>
            <p class="section-subtitle">Escribe un título o artista y añade a la cola.</p>
          </div>
        </header>

        <div class="search-row" role="search">
          <label class="sr-only" for="search">Buscar canción</label>
          <input id="search" type="text" [(ngModel)]="busqueda" (keyup.enter)="buscar()" placeholder="Ej: Coldplay, Shakira, rock...">
          <button (click)="buscar()" [disabled]="!busqueda">Buscar</button>
        </div>

        <div class="results" *ngIf="resultados.length > 0">
          <div *ngFor="let track of resultados" class="track-item">
            <img class="cover" [src]="track.album.images[0]?.url" alt="Portada de {{ track.name }}">
            <div class="track-info">
              <div class="track-title">{{ track.name }}</div>
              <div class="track-artist">{{ track.artists[0].name }}</div>
              <div class="track-meta muted">{{ track.album?.name }}</div>
            </div>
            <button class="btn-add" (click)="add(track)">Poner (0.50€)</button>
          </div>
        </div>

        <div class="empty" *ngIf="resultados.length === 0 && busqueda">
          <div class="empty-icon" aria-hidden="true">🔎</div>
          <div class="empty-title">Sin resultados todavía</div>
          <div class="empty-subtitle">Pulsa “Buscar” para ver coincidencias.</div>
        </div>
      </section>
    </main>
  `,
  styles: [`
    .topbar {
      position: sticky;
      top: 0;
      z-index: 30;
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 14px;
      padding: 14px 16px;
      margin: 0 auto 18px;
      max-width: 980px;
      border-radius: 18px;
      background: linear-gradient(180deg, rgba(255,255,255,0.12), rgba(255,255,255,0.06));
      border: 1px solid rgba(255,255,255,0.14);
      backdrop-filter: blur(10px);
      box-shadow: var(--shadow-soft);
    }

    .topbar-left { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; }
    .brand { display: flex; align-items: center; gap: 12px; }
    .brand-icon {
      width: 40px; height: 40px;
      display: grid; place-items: center;
      border-radius: 14px;
      background: radial-gradient(circle at 30% 20%, rgba(29,185,84,0.8), rgba(29,185,84,0.05));
      border: 1px solid rgba(29,185,84,0.35);
      box-shadow: 0 14px 30px rgba(29,185,84,0.18);
      color: rgba(255,255,255,0.95);
      font-weight: 900;
    }
    .brand-title { font-weight: 1000; letter-spacing: 0.2px; font-size: 1.05rem; }
    .brand-subtitle { color: rgba(255,255,255,0.7); font-size: 0.95rem; }
    .brand-subtitle strong { color: rgba(29,185,84,0.95); }

    .pills { display: flex; gap: 8px; align-items: center; }
    .pill {
      padding: 6px 10px;
      border-radius: 999px;
      border: 1px solid rgba(255,255,255,0.16);
      background: rgba(0,0,0,0.16);
      color: rgba(255,255,255,0.86);
      font-weight: 800;
      font-size: 0.85rem;
      white-space: nowrap;
    }
    .pill.ok { border-color: rgba(29,185,84,0.35); color: rgba(29,185,84,0.95); }
    .pill.warn { border-color: rgba(255,93,93,0.35); color: rgba(255,93,93,0.95); }

    .topbar-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; justify-content: flex-end; }

    .page { max-width: 980px; margin: 0 auto; display: grid; gap: 14px; padding: 6px 0 22px; }

    .hero { padding: 22px; }
    .hero-inner { display: grid; grid-template-columns: 1.25fr 0.75fr; gap: 18px; align-items: center; }
    .hero-title { margin: 0 0 8px; font-size: 1.6rem; letter-spacing: 0.1px; }
    .hero-subtitle { margin: 0; color: rgba(255,255,255,0.72); max-width: 52ch; }
    .hero-actions { display: flex; gap: 10px; margin-top: 14px; flex-wrap: wrap; }
    .hero-art { position: relative; height: 160px; border-radius: 18px; overflow: hidden; border: 1px solid rgba(255,255,255,0.12); background: rgba(0,0,0,0.12); }
    .blob { position: absolute; filter: blur(22px); opacity: 0.85; }
    .blob-a { width: 200px; height: 200px; left: -60px; top: -70px; background: rgba(29,185,84,0.65); border-radius: 50%; }
    .blob-b { width: 240px; height: 240px; right: -80px; bottom: -90px; background: rgba(88,101,242,0.55); border-radius: 50%; }
    .hero-badge {
      position: absolute; right: 12px; top: 12px;
      padding: 8px 12px; border-radius: 999px;
      border: 1px solid rgba(255,255,255,0.16);
      background: rgba(0,0,0,0.18);
      font-weight: 900;
    }

    .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
    .section { padding: 18px; }
    .section-head { display: flex; align-items: start; justify-content: space-between; gap: 12px; }
    .section-title { margin: 0; font-size: 1.15rem; font-weight: 1000; color: rgba(29,185,84,0.95); }
    .section-subtitle { margin: 6px 0 0; color: rgba(255,255,255,0.68); font-size: 0.95rem; }

    .queue-list { display: grid; gap: 10px; margin-top: 12px; }
    .queue-item {
      padding: 12px;
      border-radius: 14px;
      display: flex;
      align-items: center;
      gap: 12px;
      border: 1px solid rgba(255,255,255,0.12);
      background: rgba(0,0,0,0.16);
    }
    .queue-item.now-playing { border-color: rgba(29,185,84,0.65); box-shadow: 0 0 0 1px rgba(29,185,84,0.22) inset; }
    .queue-item.up-next { border-color: rgba(255,255,255,0.26); box-shadow: 0 0 0 1px rgba(255,255,255,0.10) inset; }
    .queue-avatar {
      width: 40px; height: 40px;
      display: grid; place-items: center;
      border-radius: 14px;
      background: rgba(255,255,255,0.06);
      border: 1px solid rgba(255,255,255,0.12);
      font-size: 1.2rem;
    }
    .queue-meta { display: grid; gap: 2px; min-width: 0; }
    .queue-title { font-weight: 950; color: rgba(255,255,255,0.92); }
    .queue-artist { color: rgba(255,255,255,0.68); font-size: 0.95rem; }
    .queue-badge {
      margin-left: auto;
      font-size: 0.78rem;
      padding: 6px 10px;
      border-radius: 999px;
      border: 1px solid rgba(255,255,255,0.18);
      background: rgba(0,0,0,0.18);
      color: rgba(255,255,255,0.85);
      white-space: nowrap;
      font-weight: 900;
    }
    .queue-item.now-playing .queue-badge { border-color: rgba(29,185,84,0.55); color: rgba(29,185,84,0.95); }
    .swing-in-top-fwd { animation: slide-in 0.32s ease-out both; }
    @keyframes slide-in { 0% { transform: translateY(-10px); opacity: 0; } 100% { transform: translateY(0); opacity: 1; } }

    .device-list { display: grid; gap: 10px; margin-top: 12px; }
    .device-item {
      padding: 12px;
      border-radius: 14px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      border: 1px solid rgba(255,255,255,0.12);
      background: rgba(0,0,0,0.16);
    }
    .device-item.active { border-color: rgba(29,185,84,0.55); }
    .device-left { display: flex; align-items: center; gap: 12px; min-width: 0; }
    .device-icon {
      width: 40px; height: 40px;
      display: grid; place-items: center;
      border-radius: 14px;
      border: 1px solid rgba(255,255,255,0.12);
      background: rgba(255,255,255,0.06);
      font-size: 1.2rem;
    }
    .device-meta { display: grid; gap: 2px; min-width: 0; }
    .device-name { font-weight: 950; color: rgba(255,255,255,0.92); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .device-type { font-size: 0.92rem; }

    .search-row { display: grid; grid-template-columns: 1fr auto; gap: 10px; margin-top: 12px; }
    .results { display: grid; gap: 10px; margin-top: 14px; }
    .track-item {
      display: grid;
      grid-template-columns: 54px 1fr auto;
      align-items: center;
      gap: 12px;
      padding: 12px;
      border-radius: 16px;
      border: 1px solid rgba(255,255,255,0.12);
      background: rgba(0,0,0,0.16);
    }
    .cover { width: 54px; height: 54px; border-radius: 14px; object-fit: cover; box-shadow: 0 12px 22px rgba(0,0,0,0.25); }
    .track-info { display: grid; gap: 2px; min-width: 0; }
    .track-title { font-weight: 950; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .track-artist { color: rgba(255,255,255,0.72); font-size: 0.95rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .track-meta { font-size: 0.9rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .btn-add { border-radius: 999px; padding: 10px 14px; }

    .empty {
      margin-top: 14px;
      padding: 18px;
      border-radius: 16px;
      border: 1px dashed rgba(255,255,255,0.18);
      background: rgba(0,0,0,0.14);
      text-align: center;
    }
    .empty-icon { font-size: 1.6rem; margin-bottom: 6px; }
    .empty-title { font-weight: 1000; }
    .empty-subtitle { margin-top: 4px; color: rgba(255,255,255,0.68); }

    .sr-only {
      position: absolute;
      width: 1px;
      height: 1px;
      padding: 0;
      margin: -1px;
      overflow: hidden;
      clip: rect(0, 0, 0, 0);
      white-space: nowrap;
      border: 0;
    }

    @media (max-width: 880px) {
      .grid { grid-template-columns: 1fr; }
      .hero-inner { grid-template-columns: 1fr; }
      .hero-art { height: 140px; }
    }

    @media (max-width: 560px) {
      .topbar { flex-direction: column; align-items: stretch; }
      .topbar-actions { justify-content: stretch; }
      .topbar-actions button { width: 100%; }
      .search-row { grid-template-columns: 1fr; }
      .track-item { grid-template-columns: 54px 1fr; }
      .btn-add { grid-column: 1 / -1; width: 100%; }
    }
  `]
})
export class HomeComponent implements OnInit, OnDestroy { 
  usuario: any = null;
  spotifyToken: string | null = null;
  
  busqueda: string = '';
  resultados: any[] = [];
  devices: any[] = [];
  colaGramola: any[] = []; 

  private nowPlayingPollId: any = null;
  private consuming = false;
  private syncingQueue = false;
  private lastQueueSyncAt = 0;
  private nowPlayingSpotifyId: string | null = null;
  private headWasPlayingSpotifyId: string | null = null;

  constructor(
    private router: Router, 
    private spotify: SpotifyService,
    private webSocketService: WebSocketService,
    private http: HttpClient
  ) {
    // 1. Cargar usuario
    const userStr = localStorage.getItem('usuario_gramola');
    if (userStr) this.usuario = JSON.parse(userStr);
    
    // 2. Intentar recuperar token guardado
    this.spotifyToken = localStorage.getItem('spotify_access_token');
    
    // 3. Si no hay token, miramos si acabamos de volver del login de Spotify
    // Con PKCE, Spotify devuelve ?code=...&state=... (no un #access_token).
    if (!this.spotifyToken) {
      const query = new URLSearchParams(window.location.search);
      const code = query.get('code');
      const state = query.get('state');
      const error = query.get('error');

      // Si venías del flujo antiguo (implícito) o hubo error, Spotify puede devolverlo en el hash.
      const hash = new URLSearchParams((window.location.hash || '').replace(/^#/, ''));
      const hashError = hash.get('error');

      if (error || hashError) {
        console.error('Spotify auth error:', error || hashError);
        // Limpiamos URL para que no se quede el error visible
        window.location.hash = '';
        window.history.replaceState({}, document.title, '/home');
      } else if (code) {
        const expectedState = this.spotify.getExpectedState();
        if (expectedState && state && state !== expectedState) {
          console.error('Estado OAuth inválido (posible CSRF).');
          this.spotify.clearPkceSession();
        } else {
          this.spotify.exchangeCodeForToken(code).subscribe({
            next: (tokenRes: any) => {
              this.spotify.clearPkceSession();

              const accessToken = tokenRes?.access_token;
              const refreshToken = tokenRes?.refresh_token;
              const expiresIn = tokenRes?.expires_in;

              if (!accessToken) {
                console.error('Respuesta de token inválida:', tokenRes);
                return;
              }

              this.spotifyToken = accessToken;
              localStorage.setItem('spotify_access_token', accessToken);
              if (refreshToken) localStorage.setItem('spotify_refresh_token', refreshToken);
              if (typeof expiresIn === 'number') {
                localStorage.setItem('spotify_expires_at', String(Date.now() + expiresIn * 1000));
              }

              // Limpiamos parámetros (?code=...) sin recargar duro
              this.router.navigate(['/home'], { replaceUrl: true });

              // Iniciamos carga de datos inmediatamente
              this.cargarDispositivos();
              this.cargarColaInicial();
				this.startNowPlayingPoll();
            },
            error: (e) => {
              console.error('Error intercambiando code por token:', e);
              this.spotify.clearPkceSession();
            }
          });
        }
      }
    }
  }

  ngOnInit() {
    // 1. Conectar WebSocket
    this.webSocketService.connect();

    // 2. Escuchar novedades en tiempo real
    this.webSocketService.messages$.subscribe(msg => {
      console.log('🔥 Notificación recibida:', msg);
      if (msg.type === 'NUEVA_CANCION' || msg.type === 'COLA_ACTUALIZADA') {
        // Keep queue stable + with ids by fetching from backend.
        // This also ensures correct ordering for "up next".
        this.cargarColaInicial();
      }
    });

    // 3. Cargar cola inicial (si ya estamos logueados)
    if (this.spotifyToken) {
        this.cargarColaInicial();
        this.cargarDispositivos();
			this.startNowPlayingPoll();
    }
  }

  ngOnDestroy(): void {
    if (this.nowPlayingPollId) {
      clearInterval(this.nowPlayingPollId);
      this.nowPlayingPollId = null;
    }
  }

  cargarColaInicial() {
    this.http.get<any[]>('/gramola/queue').subscribe({
      next: (canciones) => {
        // Backend returns ordered by insertion (id ASC) -> this is "up next".
        this.colaGramola = canciones;
        // Reset derived state so we don't accidentally delete a new head.
        if (this.headWasPlayingSpotifyId) {
          const headSpotifyId = this.colaGramola?.[0]?.spotifyId ?? null;
          if (headSpotifyId !== this.headWasPlayingSpotifyId) {
            this.headWasPlayingSpotifyId = null;
          }
        }
        console.log("Cola cargada de BD:", this.colaGramola);

        // Best-effort: if Spotify queue no longer contains some DB songs (played/cleared while app was closed),
        // prune them so they don't reappear forever.
        this.syncStaleQueueItemsWithSpotify();
      },
      error: (e) => console.error("Error cargando cola (¿Backend apagado?):", e)
    });
  }

  private syncStaleQueueItemsWithSpotify(): void {
    if (!this.spotifyToken) return;
    if (this.syncingQueue) return;

    // Throttle to avoid spamming Spotify API.
    const now = Date.now();
    if (now - this.lastQueueSyncAt < 15000) return;

    this.syncingQueue = true;
    this.lastQueueSyncAt = now;

    this.spotify.getPlayerQueue(this.spotifyToken).subscribe({
      next: (res: any) => {
        const currentlyPlayingId: string | null = res?.currently_playing?.id ?? null;
        const queuedIds: string[] = Array.isArray(res?.queue)
          ? res.queue.map((t: any) => t?.id).filter((id: any) => typeof id === 'string' && id.length > 0)
          : [];

        const present = new Set<string>();
        if (currentlyPlayingId) present.add(currentlyPlayingId);
        for (const id of queuedIds) present.add(id);

        // If Spotify doesn't report anything (no active player / no queue), don't delete data.
        // This avoids wiping the DB because of transient Spotify states/errors.
        if (present.size === 0) {
          this.syncingQueue = false;
          return;
        }

        const staleIds: number[] = (this.colaGramola || [])
          .filter((s: any) => typeof s?.id === 'number' && typeof s?.spotifyId === 'string' && s.spotifyId.length > 0)
          .filter((s: any) => !present.has(s.spotifyId))
          .map((s: any) => s.id);

        if (staleIds.length === 0) {
          this.syncingQueue = false;
          return;
        }

        this.http.post('/gramola/queue/prune', { ids: staleIds }).subscribe({
          next: () => {
            this.syncingQueue = false;
            this.cargarColaInicial();
          },
          error: () => {
            this.syncingQueue = false;
          }
        });
      },
      error: () => {
        this.syncingQueue = false;
      }
    });
  }

  private startNowPlayingPoll(): void {
    if (this.nowPlayingPollId) return;
    this.nowPlayingPollId = setInterval(() => this.checkCurrentlyPlaying(), 5000);
    // Also do an immediate check
    this.checkCurrentlyPlaying();
  }

  private checkCurrentlyPlaying(): void {
    if (!this.spotifyToken) return;
    if (!this.colaGramola || this.colaGramola.length === 0) return;
    if (this.consuming) return;

    this.spotify.getCurrentlyPlaying(this.spotifyToken).subscribe({
      next: (res: any) => {
        const status = res?.status;
        if (status === 204) return;

        const item = res?.body?.item;
        const spotifyId: string | null = item?.id || null;
        if (!spotifyId) return;

        this.nowPlayingSpotifyId = spotifyId;

        const first = this.colaGramola[0];
        const firstSpotifyId = first?.spotifyId;
        const firstId = first?.id;

        if (!firstSpotifyId || !firstId) return;

        // Keep the currently playing queued song visible.
        // Only remove it after playback has moved to a different track (end/skip).
        if (spotifyId === firstSpotifyId) {
          this.headWasPlayingSpotifyId = firstSpotifyId;
          return;
        }

        // If the head was playing before and now Spotify moved to another track,
        // consume (remove) it from the backend queue.
        if (this.headWasPlayingSpotifyId && this.headWasPlayingSpotifyId === firstSpotifyId) {
          this.consumeFirstFromQueue(firstId, firstSpotifyId);
          return;
        }

        // If the head is not the current track and it wasn't observed playing in this session,
        // it might be stale (played/cleared while app was closed). Try a best-effort sync.
        this.syncStaleQueueItemsWithSpotify();
      },
      error: (e: any) => {
        if (e?.status === 401) {
          localStorage.removeItem('spotify_access_token');
          this.spotifyToken = null;
			this.stopNowPlayingPoll();
        }
      }
    });
  }

  private stopNowPlayingPoll(): void {
    if (this.nowPlayingPollId) {
      clearInterval(this.nowPlayingPollId);
      this.nowPlayingPollId = null;
    }
  }

  private consumeFirstFromQueue(id: number, spotifyId: string): void {
    this.consuming = true;
    this.http.delete(`/gramola/queue/${id}`).subscribe({
      next: () => {
        this.headWasPlayingSpotifyId = null;
        // Remove locally to keep UI snappy
        if (this.colaGramola.length > 0 && this.colaGramola[0]?.id === id) {
          this.colaGramola.shift();
        } else {
          // Fallback: refresh
          this.cargarColaInicial();
        }
        this.consuming = false;
      },
      error: () => {
        this.consuming = false;
      }
    });
  }

  isNowPlaying(song: any): boolean {
    return !!this.nowPlayingSpotifyId && song?.spotifyId === this.nowPlayingSpotifyId;
  }

  isUpNext(index: number): boolean {
    if (!this.colaGramola || this.colaGramola.length === 0) return false;
    const headSpotifyId = this.colaGramola[0]?.spotifyId ?? null;

    // If the head is currently playing, the next item (index 1) is "up next".
    if (this.nowPlayingSpotifyId && headSpotifyId && this.nowPlayingSpotifyId === headSpotifyId) {
      return index === 1;
    }

    // Otherwise, the head is "up next".
    return index === 0;
  }

  async conectarSpotify() {
    const url = await this.spotify.getAuthUrl();
    window.location.href = url;
  }

  cargarDispositivos() {
    if (!this.spotifyToken) return;
    this.spotify.getDevices(this.spotifyToken).subscribe({
      next: (res: any) => this.devices = res.devices,
      error: (e) => {
          console.error("Error dispositivos:", e);
          if(e.status === 401) {
              // Si el token caducó, cerramos sesión
              localStorage.removeItem('spotify_access_token');
              this.spotifyToken = null;
				this.stopNowPlayingPoll();
          }
      }
    });
  }

  buscar() {
    if (!this.busqueda || !this.spotifyToken) return;
    this.spotify.searchTrack(this.busqueda, this.spotifyToken).subscribe({
      next: (res: any) => this.resultados = res.tracks.items,
      error: (e) => console.error('Error buscando en Spotify:', e)
    });
  }

  add(track: any) {
    console.log("Canción seleccionada para pagar:", track.name);

    // 1. Buscamos qué dispositivo está sonando
    const activeDevice = this.devices.find(d => d.is_active) || this.devices[0];
    
    if (!activeDevice) {
      alert("⚠️ No hay ningún dispositivo Spotify activo. Abre Spotify en tu móvil/PC.");
      return;
    }

    // 2. Guardamos la canción Y el ID del dispositivo
    localStorage.setItem('cancion_pendiente', JSON.stringify(track));
    localStorage.setItem('device_pendiente', activeDevice.id);

    // 3. Vamos a pagar
    this.router.navigate(['/payment'], { queryParams: { mode: 'song' } });
  }

  logout() {
    localStorage.clear();
    this.router.navigate(['/login']);
  }
}