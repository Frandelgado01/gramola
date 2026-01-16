import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UserService } from '../services/user.service';

@Component({
  selector: 'app-confirm-account',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './confirm-account.html',
  styleUrls: ['./confirm-account.css']
})
export class ConfirmAccountComponent implements OnInit {
  loading = true;
  errorMsg = '';
  successMsg = '';

  private token: string | null = null;
  private email: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      this.token = params.get('token');
      this.email = params.get('email');

      if (!this.token) {
        this.loading = false;
        this.errorMsg = 'Falta el token en el enlace.';
        return;
      }

      this.loading = true;
      this.errorMsg = '';
      this.successMsg = '';

      this.userService.confirmAccount(this.token).subscribe({
        next: () => {
          this.loading = false;
          this.successMsg = '✅ Cuenta confirmada. Continúa con el pago para activar el servicio.';

          // Guardamos email si viene en el enlace (para permitir pagar sin login previo)
          if (this.email) {
            localStorage.setItem('pending_email', this.email);
          }

          // Redirigimos a pago (suscripción)
          const qp: any = { mode: 'subscription', welcome: '1' };
          if (this.email) qp.email = this.email;
          this.router.navigate(['/payment'], { queryParams: qp });
        },
        error: (err: any) => {
          this.loading = false;
          const msg = err?.error?.message || err?.message || 'Error confirmando la cuenta.';
          this.errorMsg = msg;
        }
      });
    });
  }
}
