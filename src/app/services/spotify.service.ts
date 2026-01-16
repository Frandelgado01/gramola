import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { of } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SpotifyService {
  // 1. TU CLIENT ID (Este es el tuyo, está bien)
  private clientId = '2a73d667d298429191a4795bf1c6fe8c'; 
  
  // 2. URL DE LOGIN REAL (OFICIAL DE SPOTIFY)
  private authUrl = 'https://accounts.spotify.com/authorize';

  // 2b. URL PARA INTERCAMBIO DE TOKEN
  private tokenUrl = 'https://accounts.spotify.com/api/token';
  
  // 3. TU REDIRECT URI
  // IMPORTANTE: Debe coincidir EXACTAMENTE con el Redirect URI configurado en tu app de Spotify.
  private redirectUri = 'http://127.0.0.1:4200/home'; 
  
  private scopes = 'user-read-private user-read-email user-modify-playback-state user-read-playback-state';

  constructor(private http: HttpClient) { }

  private isE2eMode(): boolean {
    try {
      return typeof window !== 'undefined' && window.localStorage?.getItem('e2e_mode') === 'true';
    } catch {
      return false;
    }
  }

  // --- OAUTH (Authorization Code + PKCE) ---
  // Nota: Spotify ya no recomienda el flujo implícito (response_type=token) para SPAs.
  // Usamos Code + PKCE para evitar errores como `unsupported_response_type`.

  private readonly codeVerifierStorageKey = 'spotify_code_verifier';
  private readonly authStateStorageKey = 'spotify_auth_state';

  async getAuthUrl(): Promise<string> {
    const state = this.generateRandomString(16);
    const verifier = this.generateRandomString(64);
    const challenge = await this.generateCodeChallenge(verifier);

    sessionStorage.setItem(this.authStateStorageKey, state);
    sessionStorage.setItem(this.codeVerifierStorageKey, verifier);

    const params = new URLSearchParams({
      client_id: this.clientId,
      response_type: 'code',
      redirect_uri: this.redirectUri,
      scope: this.scopes,
      state,
      code_challenge_method: 'S256',
      code_challenge: challenge,
      show_dialog: 'true'
    });

    return `${this.authUrl}?${params.toString()}`;
  }

  getExpectedState(): string | null {
    return sessionStorage.getItem(this.authStateStorageKey);
  }

  clearPkceSession(): void {
    sessionStorage.removeItem(this.authStateStorageKey);
    sessionStorage.removeItem(this.codeVerifierStorageKey);
  }

  exchangeCodeForToken(code: string): Observable<any> {
    const verifier = sessionStorage.getItem(this.codeVerifierStorageKey);
    if (!verifier) {
      throw new Error('No se encontró code_verifier en sessionStorage. Vuelve a pulsar Conectar.');
    }

    const body = new URLSearchParams({
      client_id: this.clientId,
      grant_type: 'authorization_code',
      code,
      redirect_uri: this.redirectUri,
      code_verifier: verifier
    });

    const headers = new HttpHeaders({ 'Content-Type': 'application/x-www-form-urlencoded' });
    return this.http.post(this.tokenUrl, body.toString(), { headers });
  }

  refreshAccessToken(refreshToken: string): Observable<any> {
    const body = new URLSearchParams({
      client_id: this.clientId,
      grant_type: 'refresh_token',
      refresh_token: refreshToken
    });

    const headers = new HttpHeaders({ 'Content-Type': 'application/x-www-form-urlencoded' });
    return this.http.post(this.tokenUrl, body.toString(), { headers });
  }

  private generateRandomString(length: number): string {
    const possible = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~';
    const values = new Uint8Array(length);
    crypto.getRandomValues(values);
    return Array.from(values, (v) => possible[v % possible.length]).join('');
  }

  private async generateCodeChallenge(verifier: string): Promise<string> {
    const data = new TextEncoder().encode(verifier);
    const digest = await crypto.subtle.digest('SHA-256', data);
    return this.base64UrlEncode(new Uint8Array(digest));
  }

  private base64UrlEncode(bytes: Uint8Array): string {
    let binary = '';
    for (const b of bytes) binary += String.fromCharCode(b);
    return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
  }

  // --- FUNCIONES API (CON LAS URLS OFICIALES) ---
  
  searchTrack(query: string, token: string): Observable<any> {
    if (this.isE2eMode()) {
      return of({
        tracks: {
          items: [
            {
              id: 'e2e_track_1',
              name: `E2E Song: ${query || 'Test'}`,
              uri: 'spotify:track:e2e_track_1',
              artists: [{ name: 'E2E Artist' }],
              album: { images: [{ url: 'https://via.placeholder.com/50' }] }
            }
          ]
        }
      });
    }

    const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });
    const params = new HttpParams()
      .set('q', query)
      .set('type', 'track')
      .set('limit', '10');

    return this.http.get('https://api.spotify.com/v1/search', { headers, params });
  }

  getDevices(token: string): Observable<any> {
    if (this.isE2eMode()) {
      return of({
        devices: [
          {
            id: 'e2e_device_1',
            is_active: true,
            name: 'E2E Device',
            type: 'Computer'
          }
        ]
      });
    }

    const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });
    // ✅ URL OFICIAL DE LA API:
    return this.http.get('https://api.spotify.com/v1/me/player/devices', { headers });
  }

  addToQueue(uri: string, token: string, deviceId: string): Observable<any> {
    if (this.isE2eMode()) {
      // Simulamos éxito sin requerir Spotify Premium/Dispositivo real.
      return of('');
    }

    const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });

    const params = new HttpParams()
      .set('uri', uri)
      .set('device_id', deviceId);

    // Spotify suele responder 204 (sin body). Con responseType:'text' evitamos errores de parseo JSON.
    return this.http.post('https://api.spotify.com/v1/me/player/queue', null, {
      headers,
      params,
      responseType: 'text' as 'json'
    });
  }

  getCurrentlyPlaying(token: string): Observable<any> {
    const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });
    // Spotify can return 204 (no content) if nothing is playing.
    return this.http.get('https://api.spotify.com/v1/me/player/currently-playing', {
      headers,
      observe: 'response'
    });
  }

  getPlayerQueue(token: string): Observable<any> {
    if (this.isE2eMode()) {
      return of({
        currently_playing: {
          id: 'e2e_track_1',
          name: 'E2E Song: now playing'
        },
        queue: []
      });
    }

    const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });
    return this.http.get('https://api.spotify.com/v1/me/player/queue', { headers });
  }
}