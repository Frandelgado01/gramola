import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http'; // Para hablar con Java
import { Stripe, StripeCardElement, loadStripe } from '@stripe/stripe-js';
import { PaymentService } from '../services/payment.service';
import { SpotifyService } from '../services/spotify.service'; // <--- IMPORTANTE
import { switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="payment-container">
      <div class="card">
        <h2>Pasarela de Pago Segura</h2>
        
        <div *ngIf="mode === 'subscription'">
            <p class="muted" *ngIf="welcome === '1'">Cuenta confirmada. Para activar el servicio, elige una suscripción.</p>

            <div class="choice">
              <label class="radio">
                <input type="radio" name="subType" [(ngModel)]="subscriptionType" value="monthly" />
                <span>Mensual</span>
                <strong>{{ formatPrice('monthly') }}</strong>
              </label>
              <label class="radio">
                <input type="radio" name="subType" [(ngModel)]="subscriptionType" value="annual" />
                <span>Anual</span>
                <strong>{{ formatPrice('annual') }}</strong>
              </label>
            </div>

            <p class="muted" *ngIf="!pricingLoaded">Cargando precios…</p>
            <p class="muted" *ngIf="pricingLoaded && pricingMissing('monthly','annual')">
              Faltan precios en BD (tipos: monthly/annual).
            </p>
        </div>
        <div *ngIf="mode === 'song'">
            <p>Pagar Canción: <strong>{{ formatPrice('song') }}</strong></p>
            <p class="muted" *ngIf="pricingLoaded && !pricingByType['song']">Falta el precio 'song' en la BD.</p>
            <p class="song-preview" *ngIf="cancionPendiente">🎵 {{ cancionPendiente.name }}</p>
        </div>

        <div id="card-element" class="stripe-field"></div>
        <div id="card-errors" role="alert" style="color:red; margin-top:10px;"></div>

        <button (click)="finalizarPago()" class="btn-pay" [disabled]="loading">
          {{ loading ? 'Procesando...' : 'Pagar Ahora' }}
        </button>
        
        <button (click)="cancelar()" class="btn-cancel">Cancelar</button>
      </div>
    </div>
  `,
  styles: [`
    .payment-container { display: grid; place-items: start center; padding: 42px 18px; }
    .card { padding: 26px; width: 100%; max-width: 460px; text-align: center; }
    h2 { margin: 0 0 10px 0; letter-spacing: 0.2px; }
    p { margin: 8px 0; color: rgba(255,255,255,0.82); }
    .muted { color: rgba(255,255,255,0.68); }
    .choice { display: grid; gap: 10px; margin: 14px 0 4px 0; }
    .radio { display: grid; grid-template-columns: 18px 1fr auto; gap: 10px; align-items: center; padding: 10px 12px; border-radius: 14px; border: 1px solid rgba(255,255,255,0.12); background: rgba(0,0,0,0.18); }
    .radio strong { color: rgba(255,255,255,0.92); }
    .stripe-field { border: 1px solid rgba(255,255,255,0.14); padding: 12px; border-radius: 12px; margin: 18px 0; background: rgba(0,0,0,0.22); }
    .btn-pay { width: 100%; font-size: 1rem; }
    .btn-pay:disabled { opacity: 0.7; cursor: not-allowed; }
    .btn-cancel { background: transparent; border: 1px solid rgba(255,255,255,0.16); color: rgba(255,255,255,0.82); margin-top: 10px; width: 100%; box-shadow: none; }
    .btn-cancel:hover { filter: brightness(1.03); }
    .song-preview { color: rgba(29,185,84,0.95); font-weight: 800; font-style: italic; }
  `]
})
export class PaymentComponent implements OnInit {
  stripe: Stripe | null = null;
  card: StripeCardElement | null = null;
  mode: 'subscription' | 'song' = 'subscription';
  welcome: string | null = null;
  subscriptionType: 'monthly' | 'annual' = 'monthly';

  pricingLoaded = false;
  pricingByType: Record<string, { type: string; price: number; currency: string }> = {};

  cancionPendiente: any = null;
  loading = false;
  private transactionId: string | null = null;
  private clientSecret: string | null = null;

  private emailOverride: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient,
    private paymentService: PaymentService,
    private spotifyService: SpotifyService // <--- Inyectamos servicio Spotify
  ) {}

  async ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.mode = params['mode'] || 'subscription';
      this.welcome = params['welcome'] || null;
      this.emailOverride = params['email'] || null;

      if (this.mode === 'song') {
        const trackStr = localStorage.getItem('cancion_pendiente');
        if (trackStr) this.cancionPendiente = JSON.parse(trackStr);
      }
    });

    // Load pricing from DB so UI shows non-hardcoded prices.
    this.paymentService.getSubscriptionPrices().subscribe({
      next: (rows: any[]) => {
        const map: any = {};
        for (const r of rows || []) {
          if (r?.type) map[r.type] = r;
        }
        this.pricingByType = map;
        this.pricingLoaded = true;
      },
      error: () => {
        this.pricingLoaded = true;
      }
    });

    this.stripe = await loadStripe('pk_test_51SivngF9tcwX14SXN6UL6UKrHlqN1LqPaklONnn4ZTVDX3QS3Yg9LrsN5PydM6tSYNUze4sKIG4gQTk3rQhMHslh00JdJK3LNl');
    if (this.stripe) {
      const elements = this.stripe.elements();
      this.card = elements.create('card');
      this.card.mount('#card-element');
    }
  }

  async finalizarPago() {
    if (!this.stripe || !this.card) return;
    this.loading = true;

    const email = this.getPayerEmail();
    if (!email) {
      alert('No se encontró el email. Inicia sesión o usa el enlace de confirmación.');
      this.loading = false;
      return;
    }

    const paymentType = this.mode === 'song' ? 'song' : this.subscriptionType;

    // 1) Pedimos al backend crear un PaymentIntent (Stripe)
    this.paymentService.prepay(email, paymentType).subscribe({
      next: async (tx: any) => {
        try {
          this.transactionId = tx?.id || null;
          const dataJson = typeof tx?.data === 'string' ? JSON.parse(tx.data) : null;
          this.clientSecret = dataJson?.client_secret || null;

          if (!this.transactionId || !this.clientSecret) {
            throw new Error('No se pudo obtener client_secret/transactionId del backend');
          }

          // 2) Confirmamos el pago en el cliente con Stripe.js
          const { error, paymentIntent } = await this.stripe!.confirmCardPayment(this.clientSecret, {
            payment_method: {
              card: this.card!
            }
          });

          if (error) {
            const displayError = document.getElementById('card-errors');
            if (displayError) displayError.textContent = error.message || 'Error';
            this.loading = false;
            return;
          }
          if (!paymentIntent || paymentIntent.status !== 'succeeded') {
            throw new Error('Pago no completado');
          }

          // 3) Pedimos al backend validar que el pago está confirmado
          this.http.post('/gramola/payments/confirm', { transactionId: this.transactionId }).subscribe({
            next: () => {
              if (this.mode === 'subscription') {
                this.procesarSuscripcion(email);
              } else {
                this.procesarCancion();
              }
            },
            error: (err) => {
              const msg = err?.error?.message || err?.message || 'Error';
              alert('Error confirmando pago: ' + msg);
              this.loading = false;
            }
          });
        } catch (e: any) {
          console.error(e);
          alert(e?.message || 'Error procesando el pago');
          this.loading = false;
        }
      },
      error: (err) => {
        const msg = err?.error?.message || err?.message || 'Error';
        alert('Error iniciando el pago: ' + msg);
        this.loading = false;
      }
    });
  }

  procesarSuscripcion(email: string) {
    this.paymentService.activateSubscription(email).subscribe({
      next: () => {
        alert('¡Suscripción Activada!');

        // Si el usuario está logueado, lo actualizamos y vamos al Home.
        const raw = localStorage.getItem('usuario_gramola');
        if (raw) {
          try {
            const user = JSON.parse(raw);
            user.subscriptionActive = true;
            localStorage.setItem('usuario_gramola', JSON.stringify(user));
          } catch {
          }
          this.router.navigate(['/home']);
          return;
        }

        // Si veníamos de confirmación por email, mandamos al login.
        this.router.navigate(['/login'], { queryParams: { subscribed: '1' } });
      },
      error: () => this.loading = false
    });
  }

  procesarCancion() {
    // A. Recuperamos datos
    const track = this.cancionPendiente;
    const deviceId = localStorage.getItem('device_pendiente');
    const spotifyToken = localStorage.getItem('spotify_access_token');
    const transactionId = this.transactionId;

    if (!track || !deviceId || !spotifyToken || !transactionId) {
        alert("Error: Faltan datos de la canción o dispositivo.");
        this.loading = false;
        return;
    }

    // B + C. Primero añadimos a Spotify, luego guardamos en BD.
    this.spotifyService.addToQueue(track.uri, spotifyToken, deviceId).pipe(
      switchMap(() => this.http.post('/gramola/add-song', { ...track, transactionId }))
    ).subscribe({
      next: () => {
        alert(`🎵 ¡Pago recibido! "${track.name}" añadida a la cola.`);

        // Limpieza
        localStorage.removeItem('cancion_pendiente');
        localStorage.removeItem('device_pendiente');

        this.router.navigate(['/home']);
      },
      error: (err) => {
        console.error('Error al añadir canción:', err);
        alert('No se pudo añadir la canción a la cola. Asegúrate de tener Spotify Premium y un dispositivo activo.');
        this.loading = false;
      }
    });
  }

  cancelar() {
    const raw = localStorage.getItem('usuario_gramola');
    if (raw) {
      this.router.navigate(['/home']);
    } else {
      this.router.navigate(['/login']);
    }
  }

  private getPayerEmail(): string | null {
    if (this.emailOverride) return this.emailOverride;

    const pending = localStorage.getItem('pending_email');
    if (pending) return pending;

    try {
      const user = JSON.parse(localStorage.getItem('usuario_gramola') || '{}');
      return user?.email || null;
    } catch {
      return null;
    }
  }

  formatPrice(type: string): string {
    const row = this.pricingByType[type];
    if (!row) return '—';
    const amount = typeof row.price === 'number' ? row.price : Number(row.price);
    const currency = row.currency || 'eur';
    const symbol = currency.toLowerCase() === 'eur' ? '€' : currency.toUpperCase();
    if (!Number.isFinite(amount)) return symbol;
    return `${amount.toFixed(2)}${symbol}`;
  }

  pricingMissing(...types: string[]): boolean {
    return types.some((t) => !this.pricingByType[t]);
  }
}